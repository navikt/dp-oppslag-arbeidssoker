package no.nav.dagpenger.arbeidssoker.oppslag

import java.time.LocalDate

interface ArbeidssøkerRegister {
    fun hentRegistreringsperiode(fnr: String, fom: LocalDate, tom: LocalDate): List<Periode>
}

data class Periode(
    val fom: LocalDate,
    val tom: LocalDate,
    val formidlingsgruppe: Formidlingsgruppe
)

enum class Formidlingsgruppe {
    ARBS
}
