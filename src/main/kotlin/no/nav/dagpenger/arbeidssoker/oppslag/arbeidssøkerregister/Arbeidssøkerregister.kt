package no.nav.dagpenger.arbeidssoker.oppslag.arbeidssøkerregister

import com.fasterxml.jackson.annotation.JsonIgnore
import java.time.LocalDate

interface Arbeidssøkerregister {
    suspend fun hentRegistreringsperiode(fnr: String): List<Periode>
}

data class Periode(
    val fom: LocalDate,
    val tom: LocalDate,
    @JsonIgnore private val range: ClosedRange<LocalDate> = fom..tom,
) {
    operator fun contains(date: LocalDate): Boolean = date in range
}
