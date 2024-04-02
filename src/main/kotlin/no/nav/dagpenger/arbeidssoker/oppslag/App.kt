package no.nav.dagpenger.arbeidssoker.oppslag

import no.nav.helse.rapids_rivers.RapidApplication

fun main() {
    RapidApplication.create(kafkaConfig).apply {
        val arbeidssøkerRegister =
            VeilarbArbeidssøkerRegister(
                baseUrl = veilarbregistreringBaseurl,
                tokenProvider = veilarbregistreringTokenSupplier,
            )
        RegistreringsperioderService(
            this,
            arbeidssøkerRegister,
        )
        // For dp-behandling
        RegistrertSomArbeidssøkerService(
            this,
            arbeidssøkerRegister,
        )
    }.start()
}
