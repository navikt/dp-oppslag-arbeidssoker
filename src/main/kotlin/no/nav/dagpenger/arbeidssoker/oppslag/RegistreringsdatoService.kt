package no.nav.dagpenger.arbeidssoker.oppslag

import com.fasterxml.jackson.databind.node.ObjectNode
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import java.time.LocalDate

private val log = KotlinLogging.logger {}
private val sikkerLogg = KotlinLogging.logger("tjenestekall")

class RegistreringsdatoService(
    rapidsConnection: RapidsConnection,
    private val arbeidssøkerRegister: ArbeidssøkerRegister
) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate { it.demandAll("@behov", listOf(REGISTRERINGS_DATO)) }
            validate { it.requireKey("fnr") }
            validate { it.requireKey("fakta") }
        }.register(this)
    }

    companion object {
        const val REGISTRERINGS_DATO = "Registreringsdato"
    }

    override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
        val fnr = packet["fnr"].asText()
        packet["@event_name"] = "faktum_svar"
        runBlocking {
            arbeidssøkerRegister.hentRegistreringsperiode(fnr, LocalDate.now(), LocalDate.now())
                .also { registreringsperioder ->
                    if (registreringsperioder.size > 1)
                        throw IllegalArgumentException("ArbeisøkerRegister returnerer flere perioder")
                    else {
                        packet["fakta"]
                            .map { (it as ObjectNode) to it["behov"].asText() }
                            .filter { (_, behov) -> behov == REGISTRERINGS_DATO }
                            .forEach { (faktum) ->
                                faktum.put("svar", registreringsperioder[0].fom.toString())
                            }
                        context.send(packet.toJson())
                    }
                }
        }
    }

    override fun onError(problems: MessageProblems, context: RapidsConnection.MessageContext) {
        log.info { problems.toString() }
    }
}
