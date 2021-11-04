package no.nav.dagpenger.arbeidssoker.oppslag

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import io.ktor.client.HttpClient
import io.ktor.client.engine.ProxyBuilder
import io.ktor.client.engine.http
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.logging.LogLevel
import io.ktor.client.features.logging.Logger
import io.ktor.client.features.logging.Logging
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import no.nav.dagpenger.aad.api.ClientCredentialsClient
import no.nav.helse.rapids_rivers.RapidApplication

fun main() {
    val configuration = Configuration()
    val veilarbArbeidssøkerRegister = createVeilarbArbeidssøkerRegister(configuration)

    RapidApplication.create(configuration.kafka.rapidApplication).apply {
        RegistreringsperioderService(this, veilarbArbeidssøkerRegister)
    }.start()
}

private val sikkerlogg = KotlinLogging.logger("tjenestekall")
private fun createVeilarbArbeidssøkerRegister(configuration: Configuration): VeilarbArbeidssøkerRegister {
    return ClientCredentialsClient(
        httpClient = HttpClient() {
            install(JsonFeature) {
                serializer = JacksonSerializer {
                    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    setSerializationInclusion(JsonInclude.Include.NON_NULL)
                }
            }
            engine {
                System.getenv("HTTP_PROXY")?.let {
                    sikkerlogg.info { "Setter proxy til: $it" }
                    this.proxy = ProxyBuilder.http(it)
                }
            }
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        sikkerlogg.info { message }
                    }
                }

                level = LogLevel.ALL
            }
        }
    ) {
        scope {
            add(configuration.veilarbregistrering.scope)
        }
    }.let { clientCredentialsClient ->
        VeilarbArbeidssøkerRegister(
            configuration.veilarbregistrering.endpoint,
            {
                runBlocking {
                    clientCredentialsClient.getAccessToken()
                }
            }
        )
    }
}
