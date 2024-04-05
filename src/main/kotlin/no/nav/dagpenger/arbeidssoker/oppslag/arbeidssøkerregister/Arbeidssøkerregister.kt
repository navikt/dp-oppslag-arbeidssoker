package no.nav.dagpenger.arbeidssoker.oppslag.arbeidssøkerregister

import com.fasterxml.jackson.annotation.JsonIgnore
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
    @JsonIgnore val range: ClosedRange<LocalDate> = fom..tom,
) : ClosedRange<LocalDate> by range
