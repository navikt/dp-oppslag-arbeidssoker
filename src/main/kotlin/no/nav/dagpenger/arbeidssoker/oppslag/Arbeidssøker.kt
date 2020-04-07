package no.nav.dagpenger.arbeidssoker.oppslag

import java.time.LocalDateTime

data class Arbeidssøker(
    val type: ArbeidssøkerType,
    val opprettetDato: LocalDateTime
)

enum class ArbeidssøkerType {
    ORDINAER, SYKMELDT
}