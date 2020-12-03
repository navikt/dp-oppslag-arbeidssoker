package no.nav.dagpenger.arbeidssoker.oppslag

import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDate
import java.time.LocalDate

private val log = KotlinLogging.logger {}
private val sikkerlogg = KotlinLogging.logger("tjenestekall")

class RegistreringsperioderService(
    rapidsConnection: RapidsConnection,
    private val arbeidssøkerRegister: ArbeidssøkerRegister
) : River.PacketListener {

    companion object {
        private const val behov = "Registreringsperioder"
    }

    init {
        River(rapidsConnection).apply {
            validate { it.demandAll("@behov", listOf(behov)) }
            validate { it.requireKey("fnr") }
            validate { it.requireKey("fakta") }
            validate { it.requireKey("Søknadstidspunkt") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
        val fnr = packet["fnr"].asText()
        val søknadstidspunkt = packet["Søknadstidspunkt"].asLocalDate()

        runBlocking {
            arbeidssøkerRegister.hentRegistreringsperiode(fnr, søknadstidspunkt, LocalDate.now())
        }.also { registreringsperioder ->
            packet["@løsning"] = mapOf(
                behov to registreringsperioder
            )
        }

        log.info { "løser behov for ${packet["@id"].asText()}" }

        context.send(packet.toJson())
    }

    override fun onError(problems: MessageProblems, context: RapidsConnection.MessageContext) {
        log.error { problems.toString() }
        sikkerlogg.error { problems.toExtendedReport() }
    }
}
