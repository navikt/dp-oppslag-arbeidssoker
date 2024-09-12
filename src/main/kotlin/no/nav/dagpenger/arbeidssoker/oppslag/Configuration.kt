package no.nav.dagpenger.arbeidssoker.oppslag

import com.natpryce.konfig.ConfigurationMap
import com.natpryce.konfig.ConfigurationProperties.Companion.systemProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.Key
import com.natpryce.konfig.overriding
import com.natpryce.konfig.stringType
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.oauth2.CachedOauth2Client
import no.nav.dagpenger.oauth2.OAuth2Config

private val defaultProperties =
    ConfigurationMap(
        mapOf(
            "KAFKA_CONSUMER_GROUP_ID" to "dp-oppslag-arbeidssoker-v1",
            "KAFKA_RAPID_TOPIC" to "teamdagpenger.rapid.v1",
            "KAFKA_RESET_POLICY" to "latest",
            "HTTP_PORT" to "8080",
        ),
    )

private val devProperties =
    ConfigurationMap(
        mapOf(
            "paw-arbeidssoekerregisteret.url" to "http://paw-arbeidssoekerregisteret-api-oppslag.paw",
            "paw-arbeidssoekerregisteret.scope" to "api://dev-gcp.paw.paw-arbeidssoekerregisteret-api-oppslag/.default",
        ),
    )
private val prodProperties =
    ConfigurationMap(
        mapOf(
            "paw-arbeidssoekerregisteret.url" to "http://paw-arbeidssoekerregisteret-api-oppslag.paw",
            "paw-arbeidssoekerregisteret.scope" to "api://prod-gcp.paw.paw-arbeidssoekerregisteret-api-oppslag/.default",
        ),
    )

internal val config
    get() =
        when (System.getenv("NAIS_CLUSTER_NAME") ?: System.getProperty("NAIS_CLUSTER_NAME")) {
            "prod-gcp" -> systemProperties() overriding EnvironmentVariables overriding prodProperties overriding defaultProperties
            "dev-gcp" -> systemProperties() overriding EnvironmentVariables overriding devProperties overriding defaultProperties
            else -> systemProperties() overriding EnvironmentVariables overriding devProperties overriding defaultProperties
        }

const val SØKNAD_ID = "søknadId"

private val azureAdClient: CachedOauth2Client by lazy {
    val azureAdConfig = OAuth2Config.AzureAd(config)
    CachedOauth2Client(
        tokenEndpointUrl = azureAdConfig.tokenEndpointUrl,
        authType = azureAdConfig.clientSecret(),
    )
}

val pawArbeidssøkerregisterBaseurl: String = config[Key("paw-arbeidssoekerregisteret.url", stringType)]

val pawArbeidssøkerregisterTokenSupplier by lazy {
    {
        runBlocking {
            azureAdClient
                .clientCredentials(
                    config[Key("paw-arbeidssoekerregisteret.scope", stringType)],
                ).accessToken
        }
    }
}

val kafkaConfig: Map<String, String> by lazy {
    config
        .list()
        .reversed()
        .fold(emptyMap()) { map, pair -> map + pair.second }
}
