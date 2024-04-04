package no.nav.dagpenger.arbeidssoker.oppslag.arbeidssøkerregister

import java.time.LocalDate

interface Arbeidssøkerregister {
    suspend fun hentRegistreringsperiode(
        fnr: String,
        fom: LocalDate,
        tom: LocalDate,
    ): List<Periode>
}

data class Periode(
    val fom: LocalDate,
    val tom: LocalDate,
    val range: ClosedRange<LocalDate> = fom..tom,
) : ClosedRange<LocalDate> by range
