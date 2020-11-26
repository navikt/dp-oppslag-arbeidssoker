package no.nav.dagpenger.arbeidssoker.oppslag

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDate
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
            validate { it.demandValue("@event_name", "behov") }
            validate { it.requireKey("fnr") }
            validate { it.requireKey("fakta") }
            validate { it.requireKey(SØKNADS_TIDSPUNKT) }
        }.register(this)
    }

    companion object {
        const val REGISTRERINGS_DATO = "Registreringsdato"
        const val SØKNADS_TIDSPUNKT = "Søknadstidspunkt"
    }

    override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
        val fnr = packet["fnr"].asText()
        val søknadstidspunkt = packet[SØKNADS_TIDSPUNKT].asLocalDate()
        packet["@event_name"] = "faktum_svar"
        runBlocking {
            arbeidssøkerRegister.hentRegistreringsperiode(fnr, søknadstidspunkt, LocalDate.now())
                .also { registreringsperioder ->
                    packet["fakta"]
                        .map { (it as ObjectNode) to it["behov"].asText() }
                        .filter { (_, behov) -> behov == REGISTRERINGS_DATO }
                        .forEach { (faktum) ->
                            faktum.set<ArrayNode>("svar", toJson(registreringsperioder))
                        }
                    context.send(packet.toJson())
                }
        }
    }

    private fun toJson(registreringsPerioder: List<Periode>): ArrayNode {
        val mapper = ObjectMapper()
        mapper.createArrayNode().also { root ->
            registreringsPerioder.forEach { periode ->
                mapper.createObjectNode().also { node ->
                    node.put("fom", periode.fom.toString())
                    node.put("tom", periode.tom.toString())
                    root.add(node)
                }
            }
            return root
        }
    }

    override fun onError(problems: MessageProblems, context: RapidsConnection.MessageContext) {
        log.info { problems.toString() }
    }
}
