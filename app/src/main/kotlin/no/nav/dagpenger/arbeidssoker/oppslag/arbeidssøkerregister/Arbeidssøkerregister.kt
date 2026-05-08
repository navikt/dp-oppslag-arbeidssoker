package no.nav.dagpenger.arbeidssoker.oppslag.arbeidssøkerregister

import java.time.LocalDate
import kotlin.collections.sortedBy

interface Arbeidssøkerregister {
    suspend fun hentRegistreringsperiode(fnr: String): List<Periode>
}

data class Periode(
    val fom: LocalDate,
    val tom: LocalDate,
) : ClosedRange<LocalDate> by (fom..tom) {
    companion object {
        fun List<Periode>.filtrerFør(utgangspunkt: Periode) = this.filter { it.fom >= utgangspunkt.fom }
    }

    /**
     * Trekk fra flere perioder. Returnerer segmentene som IKKE dekkes av noen av periodene.
     */
    operator fun minus(andre: Collection<Periode>): List<Periode> =
        andre.sortedBy { it.fom }.fold(listOf(this)) { result, nyPeriode ->
            val tidligere = result.dropLast(1)
            val siste = result.lastOrNull()
            tidligere + (siste?.minus(nyPeriode) ?: emptyList())
        }

    fun overlapper(periode: Periode) = this.contains(periode.fom) || periode.contains(this.fom)

    operator fun minus(other: Periode): List<Periode> =
        when {
            // trimmer ingenting
            other.erUtenfor(fom, tom) -> listOf(this)

            // trimmer i midten
            other.erInni(fom, tom) -> listOf(til(other), fra(other))

            // trimmer i slutten
            other.overlapperMedHale(fom, tom) -> listOf(til(other))

            // trimmer i starten
            other.overlapperMedSnute(fom, tom) -> listOf(fra(other))

            // trimmer hele
            else -> emptyList()
        }

    private fun til(other: ClosedRange<LocalDate>) =
        copy(
            tom = other.start.minusDays(1),
        )

    private fun fra(other: ClosedRange<LocalDate>) =
        copy(
            fom = other.endInclusive.plusDays(1),
        )

    private fun ClosedRange<LocalDate>.erUtenfor(
        fom: LocalDate,
        tom: LocalDate,
    ) = maxOf(this.start, fom) > minOf(this.endInclusive, tom)

    private fun ClosedRange<LocalDate>.erInni(
        fom: LocalDate,
        tom: LocalDate,
    ) = this.start > fom && this.endInclusive < tom

    private fun ClosedRange<LocalDate>.overlapperMedHale(
        fom: LocalDate,
        tom: LocalDate,
    ) = this.start > fom && this.endInclusive >= tom

    private fun ClosedRange<LocalDate>.overlapperMedSnute(
        fom: LocalDate,
        tom: LocalDate,
    ) = this.start <= fom && this.endInclusive < tom
}
