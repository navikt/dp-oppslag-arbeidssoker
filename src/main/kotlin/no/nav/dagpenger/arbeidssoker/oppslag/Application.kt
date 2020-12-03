package no.nav.dagpenger.arbeidssoker.oppslag

import no.nav.dagpenger.oidc.StsOidcClient
import no.nav.helse.rapids_rivers.RapidApplication

fun main() {
    val configuration = Configuration()
    val veilarbArbeidssøkerRegister = createVeilarbArbeidssøkerRegister(configuration)

    RapidApplication.create(configuration.kafka.rapidApplication).apply {
        RegistreringsperioderService(this, veilarbArbeidssøkerRegister)
    }.start()
}

private fun createVeilarbArbeidssøkerRegister(configuration: Configuration): VeilarbArbeidssøkerRegister {
    return StsOidcClient(
        stsBaseUrl = configuration.sts.baseUrl,
        username = configuration.serviceuser.username,
        password = configuration.serviceuser.password
    ).run {
        VeilarbArbeidssøkerRegister(
            configuration.veilarbregistrering.endpoint,
            { oidcToken().access_token }
        )
    }
}
