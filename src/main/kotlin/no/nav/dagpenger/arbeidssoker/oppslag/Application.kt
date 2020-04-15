package no.nav.dagpenger.arbeidssoker.oppslag

import mu.KotlinLogging
import no.nav.dagpenger.arbeidssoker.oppslag.adapter.soap.STS_SAML_POLICY_NO_TRANSPORT_BINDING
import no.nav.dagpenger.arbeidssoker.oppslag.adapter.soap.SoapPort
import no.nav.dagpenger.arbeidssoker.oppslag.adapter.soap.arena.SoapArenaClient
import no.nav.dagpenger.arbeidssoker.oppslag.adapter.soap.configureFor
import no.nav.dagpenger.arbeidssoker.oppslag.adapter.soap.stsClient
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

private val log = KotlinLogging.logger {}

fun main() {
    val configuration = Configuration()

    val stsConsumer = StsConsumer(
            baseUrl = configuration.sts.url,
            username = configuration.serviceuser.username,
            password = configuration.serviceuser.password
    )

    val veilarbregistreringClient = VeilarbregistreringClient(
            baseUrl = configuration.veilarbregistrering.url,
            stsConsumer = stsConsumer
    )

    val oppfoelgingsstatusV2 = SoapPort.oppfoelgingsstatusV2(configuration.oppfoelgingsstatus.endpoint)

    val soapStsClient = stsClient(
            stsUrl = configuration.soapSTSClient.endpoint,
            credentials = configuration.soapSTSClient.username to configuration.soapSTSClient.password
    )
    if (configuration.soapSTSClient.allowInsecureSoapRequests) {
        soapStsClient.configureFor(oppfoelgingsstatusV2, STS_SAML_POLICY_NO_TRANSPORT_BINDING)
    } else {
        soapStsClient.configureFor(oppfoelgingsstatusV2)
    }

    val oppfølgingsstatusClient = SoapArenaClient(oppfoelgingsstatusV2)

    RapidApplication.create(configuration.kafka.rapidApplication).apply {
        Application(
                this,
                Arbeidssøkeroppslag(oppfølgingsstatusClient)
        )
    }.start()
}

class Application(
    rapidsConnection: RapidsConnection,
    private val arbeidssøkeroppslag: Arbeidssøkeroppslag
) : River.PacketListener {
    companion object {
        const val LØSNING = "@løsning"
        const val BEHOV = "@behov"
        const val REELL_ARBEIDSSØKER = "ReellArbeidssøker"
        const val FNR = "fnr"
        const val ID = "@id"
    }

    init {
        River(rapidsConnection).apply {
            validate { it.forbid(LØSNING) }
            validate { it.requireKey(FNR) }
            validate { it.requireAll(BEHOV, listOf(REELL_ARBEIDSSØKER)) }
            validate { it.interestedIn(ID) }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
        try {
            val fnr = packet[FNR].asText()
            val reellArbeidssøker = arbeidssøkeroppslag.bestemReellArbeidssøker(fnr)
            packet[LØSNING] = mapOf(REELL_ARBEIDSSØKER to reellArbeidssøker.toMap())

            log.info {
                "Reell arbeidssøker: ${reellArbeidssøker.erReellArbeidssøker}"
            }

            context.send(packet.toJson()).also {
                log.info { "Behandlet: ${packet[ID].textValue()}" }
            }
        } catch (e: Exception) {
            log.error(e) {
                "feil ved henting av arbeidssøker-data: ${e.message}"
            }
        }
    }
}