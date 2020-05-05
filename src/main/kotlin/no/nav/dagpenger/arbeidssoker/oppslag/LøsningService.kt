package no.nav.dagpenger.arbeidssoker.oppslag

import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

private val log = KotlinLogging.logger {}
private val sikkerLogg = KotlinLogging.logger("tjenestekall")

class LøsningService(
    rapidsConnection: RapidsConnection,
    private val arbeidssøkeroppslag: Arbeidssøkeroppslag
) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate { it.demandAll("@behov", listOf("RegistrertArbeidssøker")) }
            validate { it.rejectKey("@løsning") }
            validate { it.requireKey("@id", "vedtakId") }
            validate { it.requireKey("fødselsnummer") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
        withLoggingContext(
            "behovId" to packet["@id"].asText(),
            "vedtakid" to packet["vedtakId"].asText()
        ) {
            val fnr = packet["fødselsnummer"].asText()

            try {
                runBlocking { arbeidssøkeroppslag.bestemRegistrertArbeidssøker(fnr) }.also {
                    packet["@løsning"] = mapOf(
                        "RegistrertArbeidssøker" to it
                    )

                    sikkerLogg.info { "Registrert arbeidssøker: $it" }
                }

                log.info { "løser behov for ${packet["@id"].asText()}" }

                context.send(packet.toJson())
            } catch (e: Exception) {
                log.error(e) { "feil ved henting av arbeidssøker-data: ${e.message}" }
            }
        }
    }
}
