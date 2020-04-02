package no.nav.dagpenger.arbeidssoker.oppslag

import mu.KotlinLogging
import no.nav.dagpenger.ytelser.oppslag.sts.StsConsumer
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

    RapidApplication.create(configuration.kafka.rapidApplication).apply {
        Application(
                this,
                veilarbregistreringClient
        )
    }.start()
}

class Application(
    rapidsConnection: RapidsConnection,
    private val veilarbregistreringClient: VeilarbregistreringClient
) : River.PacketListener {
    companion object {
        const val LØSNING = "@løsning"
        const val BEHOV = "@behov"
        const val REELL_ARBEIDSSØKER = "ReellArbeidssøker"
        const val FNR = "fnr"
    }

    init {
        River(rapidsConnection).apply {
            validate { it.forbid(LØSNING) }
            validate { it.requireKey(FNR) }
            validate { it.requireAll(BEHOV, listOf(REELL_ARBEIDSSØKER)) }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
        try {
            val fnr = packet[FNR].asText()

            veilarbregistreringClient.hentArbeidssøker(fnr)

            val reellArbeidssøker = ReellArbeidssøker(false)
            packet[LØSNING] = mapOf(REELL_ARBEIDSSØKER to reellArbeidssøker.toMap())

            context.send(packet.toJson())
        } catch (e: Exception) {
            log.error(e) {
                "feil ved henting av arbeidssøker-data: ${e.message}"
            }
        }
    }
}