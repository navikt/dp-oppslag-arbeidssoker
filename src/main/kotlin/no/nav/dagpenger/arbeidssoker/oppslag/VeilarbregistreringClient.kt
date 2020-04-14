package no.nav.dagpenger.arbeidssoker.oppslag

import com.github.kittinunf.fuel.core.extensions.authentication
import com.github.kittinunf.fuel.httpGet
import de.huxhorn.sulky.ulid.ULID
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders.Accept
import io.ktor.http.HttpHeaders.XCorrelationId
import io.ktor.http.HttpStatusCode
import org.json.JSONObject
import java.time.ZonedDateTime

val registreringPath = "/veilarbregistrering/api/registrering"

class VeilarbregistreringClient(
    private val baseUrl: String,
    private val stsConsumer: StsConsumer
) {

    fun hentArbeidssøker(fnr: String): Arbeidssøker? {
        val (_, response, result) = "$baseUrl$registreringPath".httpGet(listOf(("fnr" to fnr)))
            .authentication().bearer(stsConsumer.token())
            .header(Accept, ContentType.Application.Json)
            .header(XCorrelationId, ULID().nextValue())
            .responseString()

        return result.fold(
            {
                if (response.statusCode == HttpStatusCode.NoContent.value) {
                    return null
                }
                konverterJsonTilArbeidssøker(it)
            },
            {
                throw RuntimeException("Feil i kallet mot veilarbregistrering", it.exception)
            }
        )
    }

    private fun konverterJsonTilArbeidssøker(arbeidssøkerJson: String): Arbeidssøker {
        val typeField = "type"
        val opprettetDatoField = "opprettetDato"
        val json = JSONObject(arbeidssøkerJson)

        val opprettetDato = ZonedDateTime.parse(
                json.getJSONObject("registrering").getString(opprettetDatoField))

        return Arbeidssøker(ArbeidssøkerType.valueOf(json.getString(typeField)), opprettetDato.toLocalDateTime())
    }
}
