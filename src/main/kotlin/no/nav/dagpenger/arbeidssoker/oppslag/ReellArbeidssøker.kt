package no.nav.dagpenger.arbeidssoker.oppslag

class ReellArbeidssøker(val erReellArbeidssøker: Boolean) {
    companion object {
        const val ER_REELL_ARBEIDSSØKER = "erReellArbeidssøker"
    }

    fun toMap(): Map<String, Any?> {
        return mapOf(
                ER_REELL_ARBEIDSSØKER to erReellArbeidssøker
        )
    }
}