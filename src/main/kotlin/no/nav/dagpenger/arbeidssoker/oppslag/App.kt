package no.nav.dagpenger.arbeidssoker.oppslag

import no.nav.helse.rapids_rivers.RapidApplication

fun main() {
    RapidApplication.create(kafkaConfig).apply {
        RegistreringsperioderService(
            this,
            VeilarbArbeidssøkerRegister(
                baseUrl = veilarbregistreringBaseurl,
                tokenProvider = veilarbregistreringTokenSupplier,
            ),
        )
    }.start()
}
