package no.nav.dagpenger.arbeidssoker.oppslag

import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDate

private val log = KotlinLogging.logger {}
private val sikkerLogg = KotlinLogging.logger("tjenestekall")

class ArbeidssøkerPerioderLøsningService(
    rapidsConnection: RapidsConnection,
    private val arbeidssøkerRegister: ArbeidssøkerRegister
) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate { it.demandAll("@behov", listOf("ArbeidssøkerPerioder")) }
            validate { it.rejectKey("@løsning") }
            validate {
                it.requireKey(
                    "@id",
                    "vedtakId",
                    "fødselsnummer",
                    "tom",
                    "fom"
                )
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
        withLoggingContext(
            "behovId" to packet["@id"].asText(),
            "vedtakid" to packet["vedtakId"].asText()
        ) {
            val fnr = packet["fødselsnummer"].asText()
            val fom = packet["fom"].asLocalDate()
            val tom = packet["tom"].asLocalDate()

            try {
                runBlocking { arbeidssøkerRegister.hentRegistreringsperiode(fnr, tom, fom) }.also {
                    packet["@løsning"] = mapOf(
                        "ArbeidssøkerPerioder" to it
                    )

                    sikkerLogg.info { "Perioder registret som arbeidssøker: $it" }
                }

                log.info { "løser behov for ArbeidssøkerPerioder" }

                context.send(packet.toJson())
            } catch (e: Exception) {
                log.error(e) { "feil ved henting av arbeidssøker-data: ${e.message}" }
            }
        }
    }
}
