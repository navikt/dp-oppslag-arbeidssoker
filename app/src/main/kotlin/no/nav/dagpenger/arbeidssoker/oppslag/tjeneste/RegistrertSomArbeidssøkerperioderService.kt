package no.nav.dagpenger.arbeidssoker.oppslag.tjeneste

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext
import no.nav.dagpenger.arbeidssoker.oppslag.SØKNAD_ID
import no.nav.dagpenger.arbeidssoker.oppslag.arbeidssøkerregister.Arbeidssøkerregister

class RegistrertSomArbeidssøkerperioderService(
    rapidsConnection: RapidsConnection,
    private val arbeidssøkerRegister: Arbeidssøkerregister,
) : River.PacketListener {
    companion object {
        private val log = KotlinLogging.logger {}
        private val sikkerlogg = KotlinLogging.logger("tjenestekall.RegistrertSomArbeidssøkerService")
        const val BEHOV = "RegistrertSomArbeidssøker"
    }

    init {
        River(rapidsConnection)
            .apply {
                precondition {
                    it.requireValue("@event_name", "behov")
                    it.requireAllOrAny("@behov", listOf(BEHOV))
                    it.forbid("@løsning")
                }
                validate { it.requireKey("ident") }
                validate { it.requireKey(BEHOV) }
                validate { it.requireKey("$BEHOV.InnhentFraOgMed") }
                validate { it.interestedIn("søknadId", "@behovId", "behandlingId") }
            }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val fnr = packet["ident"].asText()
        val søknadId = packet["søknadId"].asText()

        withLoggingContext(
            SØKNAD_ID to søknadId,
            "behandlingId" to packet["behandlingId"].asText(),
            "behovId" to packet["@behovId"].asText(),
        ) {
            val innhentFraOgMed = packet[BEHOV]["InnhentFraOgMed"].asLocalDate()
            val registreringsperioder =
                runBlocking(MDCContext()) {
                    arbeidssøkerRegister.hentRegistreringsperiode(
                        fnr,
                    )
                }
            // Finn den siste perioden som inneholder ønsketDato
            val perioder =
                registreringsperioder.filter {
                    listOf(it.fom, it.tom).any { dato -> dato.isAfter(innhentFraOgMed) || dato.isEqual(innhentFraOgMed) }
                }

            val løsning =
                if (perioder.isEmpty()) {
                    listOf(
                        mapOf(
                            "verdi" to false,
                            "gyldigFraOgMed" to innhentFraOgMed,
                        ).also {
                            log.info { "Ikke registrert som arbeidssøker fra og med $innhentFraOgMed" }
                        },
                    )
                } else {
                    perioder.map { periode ->
                        mapOf(
                            "verdi" to true,
                            "gyldigFraOgMed" to periode.fom,
                            "gyldigTilOgMed" to periode.tom,
                        ).also {
                            log.info { "Registrert som arbeidssøker med ${perioder.size} perioder fra og med $innhentFraOgMed" }
                        }
                    }
                }

            packet["@løsning"] = mapOf(BEHOV to løsning)

            // Ta med ufiltret respons fra arbeidssøkerregisteret for å sikre bedre sporing
            packet["@kilde"] =
                mapOf(
                    "navn" to "paw-arbeidssoekerregisteret-api-oppslag",
                    "data" to registreringsperioder,
                )

            log.info { "løser behov '$BEHOV'" }
            context.publish(packet.toJson())
        }
    }

    override fun onError(
        problems: MessageProblems,
        context: MessageContext,
        metadata: MessageMetadata,
    ) {
        log.error { problems.toString() }
        sikkerlogg.error { problems.toExtendedReport() }
    }
}
