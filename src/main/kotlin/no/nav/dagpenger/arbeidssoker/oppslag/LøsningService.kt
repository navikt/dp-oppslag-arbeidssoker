package no.nav.dagpenger.arbeidssoker.oppslag

import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

private val log = KotlinLogging.logger {}

class LøsningService(
    rapidsConnection: RapidsConnection,
    private val arbeidssøkeroppslag: Arbeidssøkeroppslag
) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate { it.demandAll("@behov", listOf("RegistrertArbeidssøker")) }
            validate { it.rejectKey("@løsning") }
            validate { it.requireKey("@id") }
            validate { it.requireKey("fødselsnummer") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
        withLoggingContext(
            "behovId" to packet["@id"].asText()
        ) {
            val fnr = packet["fødselsnummer"].asText()

            try {
                val registrertArbeidssøker = arbeidssøkeroppslag.bestemRegistrertArbeidssøker(fnr)
                packet["@løsning"] = mapOf(
                    "RegistrertArbeidssøker" to registrertArbeidssøker.toMap()
                )

                log.info {
                    "Registrert arbeidssøker: ${registrertArbeidssøker.erReellArbeidssøker}"
                }

                log.info { "løser behov for ${packet["@id"].asText()}" }

                context.send(packet.toJson())
            } catch (e: Exception) {
                log.error(e) {
                    "feil ved henting av arbeidssøker-data: ${e.message}"
                }
            }
        }
    }
}
