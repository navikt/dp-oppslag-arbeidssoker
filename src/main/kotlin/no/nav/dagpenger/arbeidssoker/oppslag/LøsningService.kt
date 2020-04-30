package no.nav.dagpenger.arbeidssoker.oppslag

import mu.KotlinLogging
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

private val log = KotlinLogging.logger {}

class LøsningService(
    rapidsConnection: RapidsConnection,
    private val arbeidssøkeroppslag: Arbeidssøkeroppslag
) : River.PacketListener {
    companion object {
        const val LØSNING = "@løsning"
        const val BEHOV = "@behov"
        const val REGISTRERT_ARBEIDSSØKER = "RegistrertArbeidssøker"
        const val FNR = "fnr"
        const val ID = "@id"
    }

    init {
        River(rapidsConnection).apply {
            validate { it.forbid(LØSNING) }
            validate { it.requireKey(FNR) }
            validate { it.requireAll(BEHOV, listOf(REGISTRERT_ARBEIDSSØKER)) }
            validate { it.interestedIn(ID) }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
        try {
            val fnr = packet[FNR].asText()
            val registrertArbeidssøker = arbeidssøkeroppslag.bestemRegistrertArbeidssøker(fnr)
            packet[LØSNING] = mapOf(REGISTRERT_ARBEIDSSØKER to registrertArbeidssøker.toMap())

            log.info {
                "Registrert arbeidssøker: ${registrertArbeidssøker.erReellArbeidssøker}"
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
