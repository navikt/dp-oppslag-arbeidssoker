package no.nav.dagpenger.arbeidssoker.oppslag

import com.natpryce.konfig.ConfigurationMap
import com.natpryce.konfig.ConfigurationProperties.Companion.systemProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.Key
import com.natpryce.konfig.booleanType
import com.natpryce.konfig.overriding
import com.natpryce.konfig.stringType
import no.nav.dagpenger.arbeidssoker.oppslag.Profile.DEV
import no.nav.dagpenger.arbeidssoker.oppslag.Profile.LOCAL
import no.nav.dagpenger.arbeidssoker.oppslag.Profile.PROD
import no.nav.dagpenger.arbeidssoker.oppslag.Profile.valueOf

private val defaultProperties = ConfigurationMap(
    mapOf(
        "KAFKA_CONSUMER_GROUP_ID" to "dp-oppslag-arbeidssoker-v1",
        "KAFKA_RAPID_TOPIC" to "teamdagpenger.rapid.v1",
        "KAFKA_RESET_POLICY" to "latest",
        "HTTP_PORT" to "8080",
        "veilarbregistrering.scope" to "api://dev-fss.paw.veilarbregistrering/.default"
    )
)

private val localProperties = ConfigurationMap(
    mapOf(
        "oppfoelgingsstatus.v2.url" to "kttps://localhost/ail_ws/Oppfoelgingsstatus_v2",
        "veilarbregistrering.url" to "https://localhost/ail_ws/Oppfoelgingsstatus_v2",
        "sts.baseUrl" to "http://localhost",
        "soapsecuritytokenservice.url" to "http://localhost",
        "allow.insecure.soap.requests" to true.toString()
    )
)
private val devProperties = ConfigurationMap(
    mapOf(
        "application.profile" to DEV.toString(),
        "oppfoelgingsstatus.v2.url" to "https://arena-q1.adeo.no/ail_ws/Oppfoelgingsstatus_v2",
        "veilarbregistrering.url" to "https://veilarbregistrering-q1.nais.preprod.local/veilarbregistrering/api",
        "sts.baseUrl" to "http://security-token-service.default.svc.nais.local",
        "soapsecuritytokenservice.url" to "https://sts-q1.preprod.local/SecurityTokenServiceProvider/",
        "allow.insecure.soap.requests" to true.toString()
        "veilarbregistrering.scope" to "api://dev-fss.paw.veilarbregistrering/.default"
    )
)
private val prodProperties = ConfigurationMap(
    mapOf(
        "application.profile" to PROD.toString(),
        "oppfoelgingsstatus.v2.url" to "https://arena.adeo.no/ail_ws/Oppfoelgingsstatus_v2",
        "veilarbregistrering.url" to "https://veilarbregistrering.nais.adeo.no/veilarbregistrering/api",
        "veilarbregistrering.scope" to "api://prod-fss.paw.veilarbregistrering/.default",
        "sts.baseUrl" to "http://security-token-service.default.svc.nais.local",
        "soapsecuritytokenservice.url" to "https://sts.adeo.no/SecurityTokenServiceProvider/",
        "allow.insecure.soap.requests" to true.toString()
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
    val sts: STS = STS(),
    val soapSTSClient: SoapSTSClient = SoapSTSClient(),
    val kafka: Kafka = Kafka()
) {
    data class Application(
        val profile: Profile = config()[Key("application.profile", stringType)].let { valueOf(it) },
    )

    data class Kafka(
        val rapidApplication: Map<String, String> = config().list().reversed()
            .fold(emptyMap()) { map, pair -> map + pair.second }
    )

    data class Serviceuser(
        val username: String = config()[Key("username", stringType)],
        val password: String = config()[Key("password", stringType)]
    )

    data class OppfoelginsstatusConfig(
        val endpoint: String = config()[Key("oppfoelgingsstatus.v2.url", stringType)]
    )

    data class VeilarbRegistreringConfig(
        val endpoint: String = config()[Key("veilarbregistrering.url", stringType)],
        val scope: String = config()[Key("veilarbregistrering.scope", stringType)]
    )

    data class STS(
        val baseUrl: String = config()[Key("sts.baseUrl", stringType)]
    )

    data class SoapSTSClient(
        val endpoint: String = config()[Key("soapsecuritytokenservice.url", stringType)],
        val username: String = config()[Key("username", stringType)],
        val password: String = config()[Key("password", stringType)],
        val allowInsecureSoapRequests: Boolean = config()[Key("allow.insecure.soap.requests", booleanType)]
    )
}

enum class Profile {
    LOCAL, DEV, PROD
}
