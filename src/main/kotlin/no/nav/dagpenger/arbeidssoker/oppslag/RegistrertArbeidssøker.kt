package no.nav.dagpenger.arbeidssoker.oppslag

import java.time.LocalDate

data class RegistrertArbeidssøker(
    val erRegistrert: Boolean,
    val formidlingsgruppe: String,
    val registreringsdato: LocalDate
)
