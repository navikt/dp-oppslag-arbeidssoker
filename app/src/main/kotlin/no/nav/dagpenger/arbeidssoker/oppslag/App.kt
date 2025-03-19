package no.nav.dagpenger.arbeidssoker.oppslag

import no.nav.dagpenger.arbeidssoker.lytter.ArbeidssøkerStatusLytter
import no.nav.dagpenger.arbeidssoker.oppslag.arbeidssøkerregister.PawArbeidssøkerregister
import no.nav.dagpenger.arbeidssoker.oppslag.tjeneste.RegistrertSomArbeidssøkerService
import no.nav.helse.rapids_rivers.RapidApplication

fun main() {
    val rapidsConnection =
        RapidApplication.create(kafkaConfig).apply {
            val arbeidssøkerRegister =
                PawArbeidssøkerregister(
                    baseUrl = pawArbeidssøkerregisterBaseurl,
                    tokenProvider = pawArbeidssøkerregisterTokenSupplier,
                )
            RegistrertSomArbeidssøkerService(
                this,
                arbeidssøkerRegister,
            )
        }
    val lytter =
        ArbeidssøkerStatusLytter(
            KafkaConfig.consumer(
                topicName = arbeidssokerperioderTopic,
                groupId = arbeidssokerperioderGroupId,
            ),
            rapidsConnection,
        )
    rapidsConnection.register(lytter)
    rapidsConnection.start()
}
