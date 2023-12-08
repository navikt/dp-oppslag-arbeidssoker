package no.nav.dagpenger.arbeidssoker.oppslag

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import de.huxhorn.sulky.ulid.ULID
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.jackson.jackson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.slf4j.MDC
import java.time.LocalDate

private val ulid = ULID()
private val log = KotlinLogging.logger {}
private val sikkerlogg = KotlinLogging.logger("tjenestekall")

internal class VeilarbArbeidssøkerRegister(
    val baseUrl: String? = null,
    tokenProvider: () -> String,
    httpClientEngine: HttpClientEngine = CIO.create {},
) : ArbeidssøkerRegister {
    private val client: HttpClient =
        HttpClient(httpClientEngine) {
            install(ContentNegotiation) {
                jackson {
                    this.registerModule(JavaTimeModule())
                }
            }
            install(Logging) {
                logger =
                    object : Logger {
                        override fun log(message: String) {
                            sikkerlogg.info { message }
                        }
                    }

                level = LogLevel.INFO
            }

            defaultRequest {
                header("Nav-Consumer-Id", "dp-oppslag-arbeidssoker")
                header("Nav-Call-Id", runCatching { MDC.get(SØKNAD_UUID) }.getOrElse { ulid.nextULID() })
                header("Authorization", "Bearer ${tokenProvider.invoke()}")
            }
        }

    override suspend fun hentRegistreringsperiode(
        fnr: String,
        fom: LocalDate,
        tom: LocalDate,
    ): List<Periode> =
        withContext(Dispatchers.IO) {
            log.info { "Henter arbeidssøkerperioder fra og med '$fom' til og med '$tom'" }
            try {
                client.get("$baseUrl/arbeidssoker/perioder") {
                    parameter("fnr", fnr)
                    parameter("fraOgMed", fom)
                    parameter("tilOgMed", tom)
                }.body<Arbeidssokerperioder>().let {
                    it.arbeidssokerperioder.map { responsePeriode ->
                        Periode(
                            fom = responsePeriode.fraOgMedDato,
                            tom = responsePeriode.tilOgMedDato ?: LocalDate.MAX,
                        )
                    }
                }.also {
                    log.info { "Fant ${it.size} arbeidssøkerperioder" }
                }
            } catch (e: ClientRequestException) {
                val responseBody = e.response.bodyAsText()
                log.error(e) { "Kunne ikke hente arbeidssøkerperiode. ${e.message} - Body: $responseBody" }
                throw e
            } catch (e: Exception) {
                log.error("Kunne ikke hente arbeidssøkerperiode.", e)
                throw e
            }
        }
}

internal data class Arbeidssokerperioder(val arbeidssokerperioder: List<ResponsePeriode>)

internal data class ResponsePeriode(
    val fraOgMedDato: LocalDate,
    val tilOgMedDato: LocalDate?,
)
