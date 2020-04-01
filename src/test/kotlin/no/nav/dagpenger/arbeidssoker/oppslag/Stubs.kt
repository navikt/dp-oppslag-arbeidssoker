package no.nav.dagpenger.arbeidssoker.oppslag

import com.github.tomakehurst.wiremock.client.WireMock.*
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
    }
}