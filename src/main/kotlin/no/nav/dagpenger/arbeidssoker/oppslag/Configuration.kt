package no.nav.dagpenger.arbeidssoker.oppslag

import com.natpryce.konfig.ConfigurationMap
import com.natpryce.konfig.ConfigurationProperties.Companion.systemProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.Key
import com.natpryce.konfig.booleanType
import com.natpryce.konfig.intType
import com.natpryce.konfig.overriding
import com.natpryce.konfig.stringType
import no.nav.dagpenger.arbeidssoker.oppslag.Profile.DEV
import no.nav.dagpenger.arbeidssoker.oppslag.Profile.LOCAL
import no.nav.dagpenger.arbeidssoker.oppslag.Profile.PROD
import no.nav.dagpenger.arbeidssoker.oppslag.Profile.valueOf

const val DAGPENGER_BEHOV_TOPIC = "privat-dagpenger-behov-v2"

private val localProperties = ConfigurationMap(
    mapOf(
        "kafka.bootstrap.servers" to "localhost:9092",
        "application.profile" to LOCAL.toString(),
        "application.httpPort" to "8080",
        "kafka.topic" to "topic",
        "username" to "username",
        "password" to "pass",
        "kafka.reset.policy" to "earliest",
        "nav.truststore.path" to "bla/bla",
        "nav.truststore.password" to "foo",
        "oppfoelgingsstatus.v2.url" to "kttps://localhost/ail_ws/Oppfoelgingsstatus_v2",
        "veilarbregistrering.url" to "https://localhost/ail_ws/Oppfoelgingsstatus_v2",
        "sts.baseUrl" to "http://localhost",
        "soapsecuritytokenservice.url" to "http://localhost",
        "allow.insecure.soap.requests" to true.toString()
    )
)
private val devProperties = ConfigurationMap(
    mapOf(
        "kafka.bootstrap.servers" to "b27apvl00045.preprod.local:8443,b27apvl00046.preprod.local:8443,b27apvl00047.preprod.local:8443",
        "application.profile" to DEV.toString(),
        "application.httpPort" to "8080",
        "kafka.topic" to DAGPENGER_BEHOV_TOPIC,
        "oppfoelgingsstatus.v2.url" to "https://arena-q1.adeo.no/ail_ws/Oppfoelgingsstatus_v2",
        "veilarbregistrering.url" to "http://veilarbregistrering-q1.nais.preprod.local",
        "sts.baseUrl" to "http://security-token-service.default.svc.nais.local",
        "soapsecuritytokenservice.url" to "https://sts-q1.preprod.local/SecurityTokenServiceProvider/",
        "allow.insecure.soap.requests" to true.toString()
    )
)
private val prodProperties = ConfigurationMap(
    mapOf(
        "kafka.bootstrap.servers" to "a01apvl00145.adeo.no:8443,a01apvl00146.adeo.no:8443,a01apvl00147.adeo.no:8443,a01apvl00148.adeo.no:8443,a01apvl00149.adeo.no:8443,a01apvl00150.adeo.no:8443",
        "application.profile" to PROD.toString(),
        "application.httpPort" to "8080",
        "kafka.topic" to DAGPENGER_BEHOV_TOPIC,
        "oppfoelgingsstatus.v2.url" to "https://arena.adeo.no/ail_ws/Oppfoelgingsstatus_v2",
        "veilarbregistrering.url" to "http://veilarbregistrering.nais.adeo.no",
        "sts.baseUrl" to "http://security-token-service.default.svc.nais.local",
        "soapsecuritytokenservice.url" to "https://sts.adeo.no/SecurityTokenServiceProvider/",
        "allow.insecure.soap.requests" to true.toString()
    )
)

private fun config() = when (System.getenv("NAIS_CLUSTER_NAME") ?: System.getProperty("NAIS_CLUSTER_NAME")) {
    "dev-fss" -> systemProperties() overriding EnvironmentVariables overriding devProperties
    "prod-fss" -> systemProperties() overriding EnvironmentVariables overriding prodProperties
    else -> systemProperties() overriding EnvironmentVariables overriding localProperties
}

data class Configuration(
    val serviceuser: Serviceuser = Serviceuser(),
    val application: Application = Application(),
    val oppfoelgingsstatus: OppfoelginsstatusConfig = OppfoelginsstatusConfig(),
    val veilarbregistrering: VeilarbRegistreringConfig = VeilarbRegistreringConfig(),
    val sts: STS = STS(),
    val soapSTSClient: SoapSTSClient = SoapSTSClient(),
    val kafka: Kafka = Kafka()
) {
    data class Application(
        val id: String = config().getOrElse(Key("application.id", stringType), "dp-oppslag-arbeidssoker-alfa-1"),
        val profile: Profile = config()[Key("application.profile", stringType)].let { valueOf(it) },
        val httpPort: Int = config()[Key("application.httpPort", intType)]
    )

    data class Kafka(
        val brokers: String = config()[Key("kafka.bootstrap.servers", stringType)],
        val topic: String = config()[Key("kafka.topic", stringType)],
        val consumerGroupId: String = config().getOrElse(
            Key("application.id", stringType),
            "dp-oppslag-arbeidssoker-alfa-1"
        ),
        val trustStorePath: String = config()[Key("nav.truststore.path", stringType)],
        val trustStorePassword: String = config()[Key("nav.truststore.password", stringType)],
        val rapidApplication: Map<String, String> = mapOf(
            "RAPID_APP_NAME" to "dp-oppslag-arbeidssoker",
            "KAFKA_BOOTSTRAP_SERVERS" to brokers,
            "KAFKA_RESET_POLICY" to "earliest",
            "KAFKA_RAPID_TOPIC" to topic,
            "KAFKA_CONSUMER_GROUP_ID" to consumerGroupId,
            "NAV_TRUSTSTORE_PATH" to trustStorePath,
            "NAV_TRUSTSTORE_PASSWORD" to trustStorePassword
        ) + System.getenv().filter { it.key.startsWith("NAIS_") }
    )

    data class Serviceuser(
        val username: String = config()[Key("username", stringType)],
        val password: String = config()[Key("password", stringType)]
    )

    data class OppfoelginsstatusConfig(
        val endpoint: String = config()[Key("oppfoelgingsstatus.v2.url", stringType)]
    )

    data class VeilarbRegistreringConfig(
        val endpoint: String = config()[Key("veilarbregistrering.url", stringType)]
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
