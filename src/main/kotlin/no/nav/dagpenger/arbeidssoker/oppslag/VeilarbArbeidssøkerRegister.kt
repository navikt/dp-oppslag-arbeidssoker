package no.nav.dagpenger.arbeidssoker.oppslag

import de.huxhorn.sulky.ulid.ULID
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.ClientRequestException
import io.ktor.client.features.auth.Auth
import io.ktor.client.features.defaultRequest
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.logging.LogLevel
import io.ktor.client.features.logging.Logger
import io.ktor.client.features.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.readText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import no.nav.dagpenger.ktor.client.auth.providers.bearer
import no.nav.dagpenger.ktor.client.metrics.PrometheusMetrics
import org.slf4j.MDC
import java.time.LocalDate

private val ulid = ULID()
private val log = KotlinLogging.logger {}
private val sikkerlogg = KotlinLogging.logger("tjenestekall")

internal class VeilarbArbeidssøkerRegister(
    val baseUrl: String? = null,
    tokenProvider: () -> String,
    httpClientEngine: HttpClientEngine = CIO.create {}
) : ArbeidssøkerRegister {
    private val client: HttpClient = HttpClient(httpClientEngine) {
        install(JsonFeature) {
            serializer = JacksonSerializer()
        }
        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    sikkerlogg.info { message }
                }
            }

            level = LogLevel.ALL
        }

        install(Auth) {
            bearer {
                this.tokenProvider = tokenProvider
                sendWithoutRequest = true
            }
        }

        install(PrometheusMetrics) {
            baseName = "ktor_client_veilarbregistrering"
        }

        defaultRequest {
            header("Nav-Consumer-Id", "dp-oppslag-arbeidssoker")
            header("Nav-Call-Id", runCatching { MDC.get(mdcSøknadIdKey) }.getOrElse { ulid.nextULID() })
        }
    }

    override suspend fun hentRegistreringsperiode(
        fnr: String,
        fom: LocalDate,
        tom: LocalDate
    ): List<Periode> = withContext(Dispatchers.IO) {
        log.info { "Henter arbeidssøkerperioder for fra og med $fom til og med $tom" }

        try {
            client.get<Arbeidssokerperioder>("$baseUrl/arbeidssoker/perioder") {
                parameter("fnr", fnr)
                parameter("fraOgMed", fom)
                parameter("tilOgMed", tom)
            }.let {
                it.arbeidssokerperioder.map { responsePeriode ->
                    Periode(
                        fom = responsePeriode.fraOgMedDato,
                        tom = responsePeriode.tilOgMedDato ?: LocalDate.MAX
                    )
                }
            }.also {
                log.info { "Fant ${it.size} arbeidssøkerperioder" }
            }
        } catch (e: ClientRequestException) {
            val responseBody = e.response.readText()
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
    val tilOgMedDato: LocalDate?
)
