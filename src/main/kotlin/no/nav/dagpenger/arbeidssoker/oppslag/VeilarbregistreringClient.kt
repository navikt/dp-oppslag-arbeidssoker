package no.nav.dagpenger.arbeidssoker.oppslag

import com.github.kittinunf.fuel.core.extensions.authentication
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.moshi.moshiDeserializerOf
import de.huxhorn.sulky.ulid.ULID
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders.Accept
import io.ktor.http.HttpHeaders.XCorrelationId
import no.nav.dagpenger.ytelser.oppslag.sts.StsConsumer

val registreringPath = "/veilarbregistrering/api/registrering"

class VeilarbregistreringClient(
    private val baseUrl: String,
    private val stsConsumer: StsConsumer
) {

    fun hentArbeidssøker(fnr: String): Arbeidssøker {
        val (_, _, result) = "$baseUrl$registreringPath".httpGet(listOf(("fnr" to fnr)))
            .authentication().bearer(stsConsumer.token())
            .header(Accept, ContentType.Application.Json)
            .header(XCorrelationId, ULID().nextValue())
            .responseObject(moshiDeserializerOf(Arbeidssøker::class.java))

        return result.fold(
            { it },
            {
                throw RuntimeException("Feil i kallet mot veilarbregistrering", it.exception)
            }
        )
    }

    data class Arbeidssøker(
        val type: String
    )
}
