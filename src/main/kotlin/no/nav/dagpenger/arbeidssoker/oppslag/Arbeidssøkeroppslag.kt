package no.nav.dagpenger.arbeidssoker.oppslag

class Arbeidssøkeroppslag(private val veilarbregistreringClient: VeilarbregistreringClient) {
    fun bestemReellArbeidssøker(fnr: String): ReellArbeidssøker {
        val arbeidssøker = veilarbregistreringClient.hentArbeidssøker(fnr)

        return arbeidssøker?.let {
            mapToReelArbeidssøker(arbeidssøker)
        } ?: ReellArbeidssøker(false, null)
    }

    private fun mapToReelArbeidssøker(arbeidssøker: Arbeidssøker): ReellArbeidssøker {
        return if (arbeidssøker.type == ArbeidssøkerType.ORDINAER) {
            ReellArbeidssøker(true, arbeidssøker.opprettetDato)
        } else ReellArbeidssøker(false, null)
    }
}