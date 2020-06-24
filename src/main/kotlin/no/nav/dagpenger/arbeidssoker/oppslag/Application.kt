package no.nav.dagpenger.arbeidssoker.oppslag

import no.nav.dagpenger.arbeidssoker.oppslag.adapter.soap.STS_SAML_POLICY_NO_TRANSPORT_BINDING
import no.nav.dagpenger.arbeidssoker.oppslag.adapter.soap.SoapPort
import no.nav.dagpenger.arbeidssoker.oppslag.adapter.soap.arena.SoapArenaClient
import no.nav.dagpenger.arbeidssoker.oppslag.adapter.soap.configureFor
import no.nav.dagpenger.arbeidssoker.oppslag.adapter.soap.stsClient
import no.nav.dagpenger.oidc.StsOidcClient
import no.nav.helse.rapids_rivers.RapidApplication

fun main() {
    val configuration = Configuration()
    val oppfølgingsstatusClient = createOppfølgingsstatusClient(configuration)
    val veilarbArbeidssøkerRegister = createVeilarbArbeidssøkerRegister(configuration)

    RapidApplication.create(configuration.kafka.rapidApplication).apply {
        RegistrertArbeidssøkerLøsningService(this, Arbeidssøkeroppslag(oppfølgingsstatusClient))
        ArbeidssøkerPerioderLøsningService(this, veilarbArbeidssøkerRegister)
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

private fun createOppfølgingsstatusClient(configuration: Configuration): SoapArenaClient {
    val oppfoelgingsstatusV2 = SoapPort.oppfoelgingsstatusV2(configuration.oppfoelgingsstatus.endpoint)

    stsClient(
        stsUrl = configuration.soapSTSClient.endpoint,
        credentials = configuration.soapSTSClient.username to configuration.soapSTSClient.password
    ).also {
        if (configuration.soapSTSClient.allowInsecureSoapRequests) {
            it.configureFor(oppfoelgingsstatusV2, STS_SAML_POLICY_NO_TRANSPORT_BINDING)
        } else {
            it.configureFor(oppfoelgingsstatusV2)
        }
    }

    return SoapArenaClient(oppfoelgingsstatusV2)
}
