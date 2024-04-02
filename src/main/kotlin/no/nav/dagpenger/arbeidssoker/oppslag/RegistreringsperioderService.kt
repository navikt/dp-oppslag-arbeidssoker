package no.nav.dagpenger.arbeidssoker.oppslag

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext
import mu.KotlinLogging
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.withMDC
import java.time.LocalDate

private val log = KotlinLogging.logger {}
private val sikkerlogg = KotlinLogging.logger("tjenestekall")

class RegistreringsperioderService(
    rapidsConnection: RapidsConnection,
    private val arbeidssøkerRegister: ArbeidssøkerRegister,
) : River.PacketListener {
    companion object {
        private const val BEHOV = "Registreringsperioder"
    }

    init {
        River(rapidsConnection).apply {
            validate { it.demandAll("@behov", listOf(BEHOV)) }
            validate { it.rejectKey("@løsning") }
            validate { it.requireKey("identer") }
            validate { it.requireKey("fakta") }
            validate { it.interestedIn("søknad_uuid", "@behovId") }
        }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val fnr =
            packet["identer"].first { it["type"].asText() == "folkeregisterident" && !it["historisk"].asBoolean() }["id"].asText()

        val søknadId = packet["søknad_uuid"].asText()

        withMDC(
            mapOf(
                SØKNAD_ID to søknadId,
                "behovId" to packet["@behovId"].asText(),
            ),
        ) {
            runBlocking(MDCContext()) {
                arbeidssøkerRegister.hentRegistreringsperiode(
                    fnr,
                    fom = LocalDate.now().minusDays(105),
                    tom = LocalDate.now(),
                )
            }.also { registreringsperioder ->
                packet["@løsning"] =
                    mapOf(
                        BEHOV to registreringsperioder,
                    )
            }
        }

        log.info { "løser behov for $søknadId" }

        context.publish(packet.toJson())
    }

    override fun onError(
        problems: MessageProblems,
        context: MessageContext,
    ) {
        log.error { problems.toString() }
        sikkerlogg.error { problems.toExtendedReport() }
    }
}
