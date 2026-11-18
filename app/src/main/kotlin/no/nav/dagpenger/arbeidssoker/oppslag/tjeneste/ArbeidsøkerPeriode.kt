package no.nav.dagpenger.arbeidssoker.oppslag.tjeneste

import no.nav.dagpenger.arbeidssoker.oppslag.arbeidssøkerregister.Periode
import no.nav.dagpenger.arbeidssoker.oppslag.arbeidssøkerregister.Periode.Companion.filtrerFør
import java.time.LocalDate

data class ArbeidsøkerPeriode(
    val registert: Boolean,
    val fom: LocalDate,
    val tom: LocalDate,
) : ClosedRange<LocalDate> by (fom..tom),
    Comparable<ArbeidsøkerPeriode> {
    override fun compareTo(other: ArbeidsøkerPeriode) = compareValuesBy(this, other, { it.fom }, { it.tom })

    companion object {
        fun List<Periode>.slåSammen(neiPeriode: Periode): List<ArbeidsøkerPeriode> {
            val hullrommene = (neiPeriode - this).map { ArbeidsøkerPeriode(registert = false, fom = it.fom, tom = it.tom) }
            val jaPerioder =
                this
                    .filtrerFør(neiPeriode)
                    .map { ArbeidsøkerPeriode(registert = true, fom = it.fom, tom = it.tom) }
                    .sorted()
                    .slåSammenOverlappende()
            return (hullrommene + jaPerioder).sorted()
        }

        private fun List<ArbeidsøkerPeriode>.slåSammenOverlappende(): List<ArbeidsøkerPeriode> =
            fold(emptyList()) { slått, neste ->
                val forrige = slått.lastOrNull()
                when {
                    forrige != null && neste.fom <= forrige.tom ->
                        slått.dropLast(1) + forrige.copy(tom = maxOf(forrige.tom, neste.tom))

                    else -> slått + neste
                }
            }
    }
}
