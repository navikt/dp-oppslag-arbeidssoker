package no.nav.dagpenger.arbeidssoker.oppslag

import de.huxhorn.sulky.ulid.ULID
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldBeInteger
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotBeEmpty
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.Url
import io.ktor.http.headersOf
import io.ktor.http.hostWithPort
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import org.junit.jupiter.api.Test

internal class VeilarbArbeidssøkerRegisterTest {
    private val json = Json(JsonConfiguration.Stable)
    private val client =
        VeilarbArbeidssøkerRegister(
            tokenProvider = { "" },
            baseUrl = "https://test.local:8080",
            httpClientEngine = MockEngine { request ->
                when (request.url.fullUrl) {
                    "https://test.local:8080/arbeidssoker/perioder" -> {
                        validerRequest(request)

                        respondJson(
                            json.stringify(
                                Arbeidssokerperioder.serializer(),

                                Arbeidssokerperioder(
                                    arbeidssokerperioder = listOf(
                                        ResponsePeriode(
                                            fraOgMedDato = LocalDateTime.now(),
                                            tilOgMedDato = LocalDateTime.now()
                                        )
                                    )

                                )

                            )
                        )
                    }
                    else -> error("Unhandled URL ${request.url.fullUrl}")
                }
            })

    @Test
    fun `funker dette da?`() {
        val response = client.hentRegistreringsperiode(
            fnr = "123",
            fom = LocalDate.now(),
            tom = LocalDate.now()
        )
        response.size shouldBe 1
    }

    private fun validerRequest(request: HttpRequestData) {
        request.headers[HttpHeaders.Authorization].shouldContain("Bearer")
        request.headers["Nav-Consumer-Id"].shouldNotBeEmpty()
        request.url.parameters["fnr"].shouldBeInteger()

        shouldNotThrowAny {
            ULID.parseULID(request.headers["Nav-Call-Id"])

            LocalDate.parse(request.url.parameters["fraOgMed"])
            LocalDate.parse(request.url.parameters["tilOgMed"])
        }
    }
}

private fun MockRequestHandleScope.respondJson(content: String): HttpResponseData {
    return respond(
        content = content,
        headers = headersOf(
            HttpHeaders.ContentType to listOf(ContentType.Application.Json.toString())
        )
    )
}

private val Url.hostWithPortIfRequired: String get() = if (port == protocol.defaultPort) host else hostWithPort
private val Url.fullUrl: String get() = "${protocol.name}://$hostWithPortIfRequired$encodedPath"
