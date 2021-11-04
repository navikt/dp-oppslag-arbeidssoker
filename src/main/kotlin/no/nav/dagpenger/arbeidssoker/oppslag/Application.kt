package no.nav.dagpenger.arbeidssoker.oppslag

import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import no.nav.dagpenger.aad.api.ClientCredentialsClient
import no.nav.helse.rapids_rivers.RapidApplication

private val log = KotlinLogging.logger {}
fun main() {
    val configuration = Configuration()
    val veilarbArbeidssøkerRegister = createVeilarbArbeidssøkerRegister(configuration)

    RapidApplication.create(configuration.kafka.rapidApplication).apply {
        RegistreringsperioderService(this, veilarbArbeidssøkerRegister)
    }.start()
}

private fun createVeilarbArbeidssøkerRegister(configuration: Configuration): VeilarbArbeidssøkerRegister {
    return ClientCredentialsClient {
        scope {
            add(configuration.veilarbregistrering.scope)
        }
    }.let { clientCredentialsClient ->
        VeilarbArbeidssøkerRegister(
            configuration.veilarbregistrering.endpoint,
            {
                runBlocking {
                    runCatching {
                        clientCredentialsClient.getAccessToken()

                    }
                        .onFailure { log.error("Fikk ikke tak i token:", it) }
                        .getOrThrow()

                }
            }
        )
    }
}
