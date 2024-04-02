package no.nav.dagpenger.arbeidssoker.oppslag

import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDate
import java.time.LocalDate

class RegistrertSomArbeidssøkerService(
    rapidsConnection: RapidsConnection,
    private val arbeidssøkerRegister: ArbeidssøkerRegister,
) : River.PacketListener {
    companion object {
        private val log = KotlinLogging.logger {}
        private val sikkerlogg = KotlinLogging.logger("tjenestekall.RegistrertSomArbeidssøkerService")
    }

    init {
        River(rapidsConnection).apply {
            validate { it.demandAllOrAny("@behov", listOf("RegistrertSomArbeidssøker")) }
            validate { it.rejectKey("@løsning") }
            validate { it.requireKey("ident") }
            validate { it.requireKey("RegistrertSomArbeidssøker") }
            validate { it.interestedIn("søknadId", "@behovId") }
        }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val fnr = packet["ident"].asText()
        val søknadId = packet["søknadId"].asText()

        withLoggingContext(
            SØKNAD_UUID to søknadId,
            "behovId" to packet["@behovId"].asText(),
        ) {
            val ønsketDato = packet["RegistrertSomArbeidssøker"]["Virkningsdato"].asLocalDate()
            val registreringsperioder =
                runBlocking {
                    arbeidssøkerRegister.hentRegistreringsperiode(
                        fnr,
                        fom = ønsketDato.minusDays(105),
                        tom = ønsketDato,
                    )
                }
            // Finn den siste perioden som inneholder ønsketDato
            val periode = registreringsperioder.lastOrNull { ønsketDato in it }
            val løsning =
                when (periode) {
                    null ->
                        mapOf("verdi" to LocalDate.MAX)
                            .also {
                                // TODO: Vi bør gjøre noe annet enn å bruke MAX
                                log.warn { "Fant ingen registrering for i perioden $ønsketDato" }
                            }

                    else ->
                        mapOf(
                            "verdi" to periode.fom,
                            "gyldigFra" to periode.fom,
                        )
                }
            packet["@løsning"] = mapOf("RegistrertSomArbeidssøker" to løsning)
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
