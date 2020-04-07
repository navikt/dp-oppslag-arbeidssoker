package no.nav.dagpenger.arbeidssoker.oppslag

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import no.nav.common.KafkaEnvironment
import no.nav.dagpenger.ytelser.oppslag.sts.StsConsumer
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.*
import java.io.IOException
import java.net.HttpURLConnection
import java.net.ServerSocket
import java.net.URL
import java.time.Duration
import java.util.*
import java.util.concurrent.TimeUnit

@KtorExperimentalAPI
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ApplicationComponentTest {

    companion object {
        const val FNR = "12345"
    }

    @Test
    fun `skal motta behov og produsere ReellArbeidssøker-løsning`() {
        val behov =
                """{"@id": "1", "fnr":"$FNR",  "@behov": ["ReellArbeidssøker"]}"""
        behovProducer.send(ProducerRecord(testTopic, "123", behov))

        assertLøsning(Duration.ofSeconds(10)) { alleSvar ->
            alleSvar.size shouldBe 1
            alleSvar.first()["@løsning"]["ReellArbeidssøker"] shouldNotBe null
        }
    }

    @Test
    fun `skal kun behandle opprinnelig behov`() {
        val behovAlleredeBesvart =
                """{"@id": "1", "fnr":"$FNR", "@behov": ["ReellArbeidssøker"], "@løsning": { "ReellArbeidssøker": { "erReellArbeidssøker": true } } }"""
        val behovSomTrengerSvar =
                """{"@id": "2", "fnr":"$FNR", "@behov": ["ReellArbeidssøker"]}"""
        behovProducer.send(ProducerRecord(testTopic, "1", behovAlleredeBesvart))
        behovProducer.send(ProducerRecord(testTopic, "2", behovSomTrengerSvar))

        assertLøsning(Duration.ofSeconds(10)) { alleSvarMedLøsning ->
            Assertions.assertEquals(1, alleSvarMedLøsning.medId("1").size)
            Assertions.assertEquals(1, alleSvarMedLøsning.medId("2").size)

            val svar = alleSvarMedLøsning.medId("2").first()

            svar["@løsning"].hasNonNull("ReellArbeidssøker") shouldBe true
        }
    }

    private fun List<JsonNode>.medId(id: String) = filter { it["@id"].asText() == id }

    @Test
    fun `ignorerer hendelser med ugyldig json`() {
        val id = "1"
        val behovSomTrengerSvar =
                """{"@id": "$id", "fnr":"$FNR", "@behov": ["ReellArbeidssøker"]}"""
        behovProducer.send(ProducerRecord(testTopic, UUID.randomUUID().toString(), "THIS IS NOT JSON"))
        behovProducer.send(ProducerRecord(testTopic, id, behovSomTrengerSvar))

        assertLøsning(Duration.ofSeconds(10)) { alleSvar ->
            Assertions.assertEquals(1, alleSvar.medId(id).size)

            val svar = alleSvar.medId(id).first()
            svar["@løsning"].hasNonNull("ReellArbeidssøker") shouldBe true
        }
    }

    private fun assertLøsning(duration: Duration, assertion: (List<JsonNode>) -> Unit) =
            mutableListOf<JsonNode>().apply {
                await()
                        .atMost(duration)
                        .untilAsserted {
                            this.addAll(behovConsumer.poll(Duration.ofMillis(100)).mapNotNull {
                                try {
                                    println(it.value())
                                    objectMapper.readTree(it.value())
                                } catch (err: JsonParseException) {
                                    null
                                }
                            }.filter { it.hasNonNull("@løsning") })

                            assertion(this)
                        }
            }

    internal val objectMapper: ObjectMapper = jacksonObjectMapper()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .registerModule(JavaTimeModule())

    private fun isOkResponse(path: String) =
            try {
                (URL("$appUrl$path")
                        .openConnection() as HttpURLConnection)
                        .responseCode in 200..299
            } catch (err: IOException) {
                false
            }

    private val testTopic = "test-topic-v1"
    private val embeddedKafkaEnvironment = KafkaEnvironment(
            autoStart = false,
            noOfBrokers = 1,
            topicInfos = listOf(KafkaEnvironment.TopicInfo(name = testTopic, partitions = 1)),
            withSchemaRegistry = false,
            withSecurity = false
    )

    private val wireMockServer = WireMockServer(WireMockConfiguration.options().dynamicPort())

    private lateinit var appUrl: String

    private lateinit var rapidsConnection: RapidsConnection
    private lateinit var behovProducer: Producer<String, String>
    private lateinit var behovConsumer: Consumer<String, String>

    @BeforeAll
    fun setup() {
        embeddedKafkaEnvironment.start()

        wireMockServer.start()

        behovProducer = KafkaProducer<String, String>(Properties().apply {
            this[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = embeddedKafkaEnvironment.brokersURL
            this[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
            this[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
            this[ProducerConfig.LINGER_MS_CONFIG] = "0"
        })
        behovConsumer = KafkaConsumer<String, String>(Properties().apply {
            this[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = embeddedKafkaEnvironment.brokersURL
            this[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
            this[ConsumerConfig.GROUP_ID_CONFIG] = "test-consumer"
            this[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
            this[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
            this[ConsumerConfig.MAX_POLL_RECORDS_CONFIG] = "1000"
        }).also {
            it.subscribe(listOf(testTopic))
        }

        val randomPort = ServerSocket(0).use { it.localPort }
        appUrl = "http://localhost:$randomPort"

        val config = Configuration(
                application = Configuration.Application(httpPort = randomPort),
                kafka = Configuration.Kafka(
                        brokers = embeddedKafkaEnvironment.brokersURL,
                        topic = testTopic
                ),
                sts = Configuration.Sts(wireMockServer.baseUrl()),
                veilarbregistrering = Configuration.Veilarbregistrering(wireMockServer.baseUrl())
        )

        val stsConsumer = StsConsumer(
                baseUrl = config.sts.url,
                username = config.serviceuser.username,
                password = config.serviceuser.password
        )

        val veilarbregistreringClient = VeilarbregistreringClient(
                baseUrl = config.veilarbregistrering.url,
                stsConsumer = stsConsumer
        )

        wireMockServer.stubFor(Stubs.sts(config.serviceuser))
        wireMockServer.stubFor(Stubs.stubRegistreringGet())

        // kjør opp app
        val rapidConfig = mapOf(
                "KAFKA_BOOTSTRAP_SERVERS" to config.kafka.brokers,
                "KAFKA_RAPID_TOPIC" to config.kafka.topic,
                "KAFKA_CONSUMER_GROUP_ID" to config.kafka.consumerGroupId,
                "HTTP_PORT" to randomPort.toString())

        rapidsConnection = RapidApplication.create(rapidConfig).apply {
            Application(this, Arbeidssøkeroppslag(veilarbregistreringClient))
        }

        GlobalScope.launch {
            rapidsConnection.start()
        }

        await("wait until the rapid has started")
                .atMost(10, TimeUnit.SECONDS)
                .until { isOkResponse("/isalive") }

        val adminClient = embeddedKafkaEnvironment.adminClient
        await("wait until the rapid consumer is assigned the topic")
                .atMost(10, TimeUnit.SECONDS)
                .until {
                    adminClient?.describeConsumerGroups(listOf(config.kafka.consumerGroupId))
                            ?.describedGroups()
                            ?.get(config.kafka.consumerGroupId)
                            ?.get()
                            ?.members()
                            ?.any { it.assignment().topicPartitions().any { it.topic() == testTopic } }
                            ?: false
                }

        @AfterAll
        fun teardown() {
            rapidsConnection.stop()
            wireMockServer.stop()
            embeddedKafkaEnvironment.close()
        }
    }
}
