package no.nav.dagpenger.arbeidssoker.oppslag

import no.nav.dagpenger.arbeidssoker.oppslag.arbeidssøkerregister.PawArbeidssøkerregister
import no.nav.dagpenger.arbeidssoker.oppslag.arbeidssøkerregister.VeilarbArbeidssøkerregister
import no.nav.dagpenger.arbeidssoker.oppslag.tjeneste.PawRegistreringsperioderService
import no.nav.dagpenger.arbeidssoker.oppslag.tjeneste.RegistreringsperioderService
import no.nav.dagpenger.arbeidssoker.oppslag.tjeneste.RegistrertSomArbeidssøkerService
import no.nav.helse.rapids_rivers.RapidApplication

fun main() {
    RapidApplication.create(kafkaConfig).apply {
        val arbeidssøkerRegister =
            VeilarbArbeidssøkerregister(
                baseUrl = veilarbregistreringBaseurl,
                tokenProvider = veilarbregistreringTokenSupplier,
            )
        val pawArbeidssøkerRegister =
            PawArbeidssøkerregister(
                baseUrl = pawArbeidssøkerregisterBaseurl,
                tokenProvider = veilarbregistreringTokenSupplier,
            )
        RegistreringsperioderService(
            this,
            arbeidssøkerRegister,
        )
        PawRegistreringsperioderService(
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
