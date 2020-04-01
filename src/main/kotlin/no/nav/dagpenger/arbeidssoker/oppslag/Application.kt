package no.nav.dagpenger.arbeidssoker.oppslag

import mu.KotlinLogging
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

private val log = KotlinLogging.logger {}

fun main() {
    val configuration = Configuration()

    RapidApplication.create(configuration.kafka.rapidApplication).apply {
        Application(this)
    }.start()
}

class Application(
    rapidsConnection: RapidsConnection
) : River.PacketListener {
    companion object {
        const val LØSNING = "@løsning"
        const val BEHOV = "@behov"
        const val REELL_ARBEIDSSØKER = "ReellArbeidssøker"
    }

    init {
        River(rapidsConnection).apply {
            validate { it.forbid(LØSNING) }
            validate { it.requireAll(BEHOV, listOf(REELL_ARBEIDSSØKER)) }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
        try {
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