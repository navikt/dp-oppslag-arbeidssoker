package no.nav.dagpenger.arbeidssoker.oppslag

import java.time.LocalDateTime

class ReellArbeidssøker(val erReellArbeidssøker: Boolean, val registreringsdato: LocalDateTime?) {
    companion object {
        const val ER_REELL_ARBEIDSSØKER = "erReellArbeidssøker"
        const val REGISTRERINGSDATO = "registreringsdato"
    }

    fun toMap(): Map<String, Any?> {
        return mapOf(
                ER_REELL_ARBEIDSSØKER to erReellArbeidssøker,
                REGISTRERINGSDATO to registreringsdato
        )
    }
}