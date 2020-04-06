package no.nav.dagpenger.arbeidssoker.oppslag

import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.*
import io.ktor.http.HttpHeaders
import no.nav.dagpenger.arbeidssoker.oppslag.Configuration.Serviceuser

class Stubs {
    companion object {

        const val stsToken = "aValidAccessToken"
        const val FNR = "12345"

        val stsTokenJson = """
        {
            "access_token": "$stsToken",
            "expires_in": "3600"
        }
    """.trimIndent()

        fun sts(serviceuser: Serviceuser) = get(urlPathEqualTo("/rest/v1/sts/token"))
                .withBasicAuth(serviceuser.username, serviceuser.password)
                .withHeader("Accept", equalTo("application/json"))
                .withQueryParam("grant_type", equalTo("client_credentials"))
                .withQueryParam("scope", equalTo("openid"))
                .willReturn(okJson(stsTokenJson))

        fun stubRegistreringGet(): MappingBuilder =
                WireMock.get(urlPathEqualTo(registreringPath))
                        .withQueryParam("fnr", equalTo(FNR))
                        .withHeader(HttpHeaders.Authorization, equalTo("Bearer $stsToken"))
                        .willReturn(
                                WireMock.okJson("""
                                {
                                    "type": "ORDINAER",
                                     "registrering" : {
                                        "opprettetDato" : "2020-04-03T14:33:08.606505+02:00"
                                     }
                                }
                            """.trimIndent())
                        )
    }
}