package no.nav.dagpenger.arbeidssoker.oppslag.arbeidssøkerregister

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
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
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.jackson.jackson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import no.nav.dagpenger.arbeidssoker.oppslag.SØKNAD_ID
import no.nav.paw.arbeidssøkerregister.api.models.ArbeidssoekerperiodeResponseDTO
import org.slf4j.MDC
import java.time.LocalDate

class PawArbeidssøkerregister(
    private val baseUrl: String? = null,
    private val tokenProvider: () -> String?,
    httpClientEngine: HttpClientEngine = CIO.create {},
) : Arbeidssøkerregister {
    private companion object {
        private val log = KotlinLogging.logger {}
        private val sikkerlogg = KotlinLogging.logger("tjenestekall")
    }

    private val client: HttpClient =
        HttpClient(httpClientEngine) {
            expectSuccess = true
            install(ContentNegotiation) {
                jackson {
                    registerModule(JavaTimeModule())
                    disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
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
                runCatching { MDC.get(SØKNAD_ID) }.onSuccess { header("Nav-Call-Id", it) }
            }
        }

    override suspend fun hentRegistreringsperiode(fnr: String): List<Periode> =
        withContext(Dispatchers.IO) {
            val token = tokenProvider.invoke()!!
            try {
                client
                    .post("$baseUrl/api/v1/veileder/arbeidssoekerperioder") {
                        bearerAuth(token)
                        contentType(ContentType.Application.Json)
                        MDC.get("behandlingId")?.let { header(HttpHeaders.XCorrelationId, it) }
                        MDC.get("behovId")?.let { behovId ->
                            header(HttpHeaders.XRequestId, behovId).also { log.info { "Legger ved behovId=$behovId" } }
                        }
                        setBody(mapOf("identitetsnummer" to fnr))
                    }.body<List<ArbeidssoekerperiodeResponseDTO>>()
                    .let {
                        it.map { arbeidssøkerperiode ->
                            Periode(
                                fom = arbeidssøkerperiode.startet.tidspunkt.toLocalDate(),
                                tom = arbeidssøkerperiode.avsluttet?.tidspunkt?.toLocalDate() ?: LocalDate.MAX,
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
