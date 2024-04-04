package no.nav.dagpenger.arbeidssoker.oppslag.tjeneste

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext
import mu.KotlinLogging
import no.nav.dagpenger.arbeidssoker.oppslag.SØKNAD_ID
import no.nav.dagpenger.arbeidssoker.oppslag.arbeidssøkerregister.Arbeidssøkerregister
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.withMDC
import java.time.LocalDate

class PawRegistreringsperioderService(
    rapidsConnection: RapidsConnection,
    private val arbeidssøkerRegister: Arbeidssøkerregister,
) : River.PacketListener {
    private companion object {
        private const val BEHOV = "Registreringsperioder"
        private val log = KotlinLogging.logger {}
        private val sikkerlogg = KotlinLogging.logger("tjenestekall")
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
            runCatching {
                val perioder =
                    runBlocking(MDCContext()) {
                        arbeidssøkerRegister.hentRegistreringsperiode(
                            fnr,
                            fom = LocalDate.now().minusDays(105),
                            tom = LocalDate.now(),
                        )
                    }
                val min = perioder.minByOrNull { it.fom }
                val maks = perioder.maxByOrNull { it.fom }
                log.info { "PAW - Fant ${perioder.size} med første dato=$min og siste dato=$maks" }
            }.onFailure {
                log.error(it) { "Noe gikk galt mot PAW" }
            }
        }
    }

    override fun onError(
        problems: MessageProblems,
        context: MessageContext,
    ) {
        log.error { problems.toString() }
        sikkerlogg.error { problems.toExtendedReport() }
    }
}
