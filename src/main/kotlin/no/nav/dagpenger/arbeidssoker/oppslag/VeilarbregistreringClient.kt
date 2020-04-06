package no.nav.dagpenger.arbeidssoker.oppslag

import com.github.kittinunf.fuel.core.extensions.authentication
import com.github.kittinunf.fuel.httpGet
import de.huxhorn.sulky.ulid.ULID
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders.Accept
import io.ktor.http.HttpHeaders.XCorrelationId
import no.nav.dagpenger.ytelser.oppslag.sts.StsConsumer
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.ZonedDateTime

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
            .responseString()

        return result.fold(
            { konverterJsonTilArbeidssøker(it) },
            {
                throw RuntimeException("Feil i kallet mot veilarbregistrering", it.exception)
            }
        )
    }

    fun konverterJsonTilArbeidssøker(arbeidssøkerJson: String): Arbeidssøker {
        val type = "type"
        val opprettetDato = "opprettetDato"
        val json = JSONObject(arbeidssøkerJson)

        val opprettetDato2 = ZonedDateTime.parse(
                json.getJSONObject("registrering").getString(opprettetDato))

        return Arbeidssøker(json.getString(type), opprettetDato2.toLocalDateTime())
    }

    data class Arbeidssøker(
        val type: String,
        val opprettetDato: LocalDateTime
    )
}
