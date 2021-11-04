package no.nav.dagpenger.arbeidssoker.oppslag

import com.natpryce.konfig.ConfigurationMap
import com.natpryce.konfig.ConfigurationProperties.Companion.systemProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.Key
import com.natpryce.konfig.overriding
import com.natpryce.konfig.stringType

private val defaultProperties = ConfigurationMap(
    mapOf(
        "KAFKA_CONSUMER_GROUP_ID" to "dp-oppslag-arbeidssoker-v1",
        "KAFKA_RAPID_TOPIC" to "teamdagpenger.rapid.v1",
        "KAFKA_RESET_POLICY" to "latest",
        "HTTP_PORT" to "8080",
    )
)

private val localProperties = ConfigurationMap(
    mapOf(
        "veilarbregistrering.url" to "https://localhost/ail_ws/Oppfoelgingsstatus_v2",
        "veilarbregistrering.scope" to "api://dev-fss.paw.veilarbregistrering/.default",
    )
)
private val devProperties = ConfigurationMap(
    mapOf(
        "veilarbregistrering.url" to "https://veilarbregistrering-q1.nais.preprod.local/veilarbregistrering/api",
        "veilarbregistrering.scope" to "api://dev-fss.paw.veilarbregistrering/.default"
    )
)
private val prodProperties = ConfigurationMap(
    mapOf(
        "veilarbregistrering.url" to "https://veilarbregistrering.nais.adeo.no/veilarbregistrering/api",
        "veilarbregistrering.scope" to "api://prod-fss.paw.veilarbregistrering/.default",
    )
)

private fun config() = when (System.getenv("NAIS_CLUSTER_NAME") ?: System.getProperty("NAIS_CLUSTER_NAME")) {
    "dev-fss" -> systemProperties() overriding EnvironmentVariables overriding devProperties overriding defaultProperties
    "prod-fss" -> systemProperties() overriding EnvironmentVariables overriding prodProperties overriding defaultProperties
    else -> systemProperties() overriding EnvironmentVariables overriding localProperties overriding defaultProperties
}

const val mdcSøknadIdKey = "soknadId"

data class Configuration(
    val veilarbregistrering: VeilarbRegistreringConfig = VeilarbRegistreringConfig(),
    val kafka: Kafka = Kafka()
) {
    data class Kafka(
        val rapidApplication: Map<String, String> = config().list().reversed()
            .fold(emptyMap()) { map, pair -> map + pair.second }
    )

    data class VeilarbRegistreringConfig(
        val endpoint: String = config()[Key("veilarbregistrering.url", stringType)],
        val scope: String = config()[Key("veilarbregistrering.scope", stringType)]
    )
}
