package no.nav.dagpenger.arbeidssoker.oppslag

import no.nav.dagpenger.arbeidssoker.oppslag.arbeidssøkerregister.PawArbeidssøkerregister
import no.nav.dagpenger.arbeidssoker.oppslag.arbeidssøkerregister.VeilarbArbeidssøkerregister
import no.nav.dagpenger.arbeidssoker.oppslag.tjeneste.PAWRegistrertSomArbeidssøkerService
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
                tokenProvider = pawArbeidssøkerregisterTokenSupplier,
            )

        // For dp-behandling
        PAWRegistrertSomArbeidssøkerService(
            this,
            pawArbeidssøkerRegister,
        )
        RegistrertSomArbeidssøkerService(
            this,
            arbeidssøkerRegister,
        )
    }.start()
}
