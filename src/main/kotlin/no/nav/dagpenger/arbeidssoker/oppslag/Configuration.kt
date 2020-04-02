package no.nav.dagpenger.arbeidssoker.oppslag

import com.natpryce.konfig.*
import com.natpryce.konfig.ConfigurationProperties.Companion.systemProperties
import no.nav.dagpenger.arbeidssoker.oppslag.Profile.*

val DAGPENGER_BEHOV_TOPIC = "privat-dagpenger-behov-v2"

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
                "sts.url" to "http://localhost",
                "veilarbregistrering.url" to "http://localhost"
        )
)
private val devProperties = ConfigurationMap(
        mapOf(
                "kafka.bootstrap.servers" to "b27apvl00045.preprod.local:8443,b27apvl00046.preprod.local:8443,b27apvl00047.preprod.local:8443",
                "application.profile" to DEV.toString(),
                "application.httpPort" to "8080",
                "kafka.topic" to DAGPENGER_BEHOV_TOPIC,
                "sts.url" to "http://security-token-service.default.svc.nais.local",
                "veilarbregistrering.url" to "https://veilarbregistrering-q0.nais.preprod.local"
        )
)
private val prodProperties = ConfigurationMap(
        mapOf(
                "kafka.bootstrap.servers" to "a01apvl00145.adeo.no:8443,a01apvl00146.adeo.no:8443,a01apvl00147.adeo.no:8443,a01apvl00148.adeo.no:8443,a01apvl00149.adeo.no:8443,a01apvl00150.adeo.no:8443",
                "application.profile" to PROD.toString(),
                "application.httpPort" to "8080",
                "kafka.topic" to DAGPENGER_BEHOV_TOPIC
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
    val sts: Sts = Sts(),
    val veilarbregistrering: Veilarbregistrering = Veilarbregistrering(),
    val kafka: Kafka = Kafka()
) {
    data class Application(
        val id: String = config().getOrElse(Key("application.id", stringType), "dp-arbeidssoker-oppslag-alfa-1"),
        val profile: Profile = config()[Key("application.profile", stringType)].let { valueOf(it) },
        val httpPort: Int = config()[Key("application.httpPort", intType)]
    )

    data class Kafka(
        val brokers: String = config()[Key("kafka.bootstrap.servers", stringType)],
        val topic: String = config()[Key("kafka.topic", stringType)],
        val consumerGroupId: String = config().getOrElse(Key("application.id", stringType), "dp-arbeidssoker-oppslag-alfa-1"),
        val trustStorePath: String = config()[Key("nav.truststore.path", stringType)],
        val trustStorePassword: String = config()[Key("nav.truststore.password", stringType)],
        val rapidApplication: Map<String, String> = mapOf(
            "KAFKA_BOOTSTRAP_SERVERS" to brokers,
            "KAFKA_RESET_POLICY" to "earliest",
            "KAFKA_RAPID_TOPIC" to topic,
            "KAFKA_CONSUMER_GROUP_ID" to consumerGroupId,
            "NAV_TRUSTSTORE_PATH" to trustStorePath,
            "NAV_TRUSTSTORE_PASSWORD" to trustStorePassword)
    )

    data class Serviceuser(
        val username: String = config()[Key("username", stringType)],
        val password: String = config()[Key("password", stringType)]
    )

    data class Sts(
        val url: String = config()[Key("sts.url", stringType)]
    )

    data class Veilarbregistrering(
        val url: String = config()[Key("veilarbregistrering.url", stringType)]
    )
}

enum class Profile {
    LOCAL, DEV, PROD
}
