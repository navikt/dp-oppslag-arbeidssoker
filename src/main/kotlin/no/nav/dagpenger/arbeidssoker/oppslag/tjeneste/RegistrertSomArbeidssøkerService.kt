package no.nav.dagpenger.arbeidssoker.oppslag.tjeneste

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext
import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.arbeidssoker.oppslag.SØKNAD_ID
import no.nav.dagpenger.arbeidssoker.oppslag.arbeidssøkerregister.Arbeidssøkerregister
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDate

class RegistrertSomArbeidssøkerService(
    rapidsConnection: RapidsConnection,
    private val arbeidssøkerRegister: Arbeidssøkerregister,
) : River.PacketListener {
    companion object {
        private val log = KotlinLogging.logger {}
        private val sikkerlogg = KotlinLogging.logger("tjenestekall.RegistrertSomArbeidssøkerService")
        const val BEHOV = "RegistrertSomArbeidssøker"
    }

    init {
        River(rapidsConnection).apply {
            validate { it.demandAllOrAny("@behov", listOf(BEHOV)) }
            validate { it.rejectKey("@løsning") }
            validate { it.requireKey("ident", "gjelderDato") }
            validate { it.requireKey(BEHOV) }
            validate { it.interestedIn("søknadId", "@behovId", "behandlingId") }
        }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val fnr = packet["ident"].asText()
        val søknadId = packet["søknadId"].asText()

        withLoggingContext(
            SØKNAD_ID to søknadId,
            "behandlingId" to packet["behandlingId"].asText(),
            "behovId" to packet["@behovId"].asText(),
        ) {
            val gjelderDato = packet["gjelderDato"].asLocalDate()
            // TODO: Skal vi bruke denne?
            val ønsketDato = packet["RegistrertSomArbeidssøker"]["Virkningsdato"].asLocalDate()
            val registreringsperioder =
                runBlocking(MDCContext()) {
                    arbeidssøkerRegister.hentRegistreringsperiode(
                        fnr,
                        fom = gjelderDato.minusDays(105),
                        tom = gjelderDato,
                    )
                }
            // Finn den siste perioden som inneholder ønsketDato
            val periode = registreringsperioder.lastOrNull { gjelderDato in it }
            val erRegistrertSomArbeidssøker = periode != null
            val løsning =
                mapOf(
                    "verdi" to erRegistrertSomArbeidssøker,
                    "gyldigFraOgMed" to gjelderDato,
                    "gyldigTilOgMed" to gjelderDato,
                )
            packet["@løsning"] = mapOf("RegistrertSomArbeidssøker" to løsning)
            log.info { "løser behov '$BEHOV'" }

            context.publish(packet.toJson())
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
