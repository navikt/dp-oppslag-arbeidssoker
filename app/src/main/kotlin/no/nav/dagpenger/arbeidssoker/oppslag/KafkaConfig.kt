package no.nav.dagpenger.arbeidssoker.oppslag

import io.confluent.kafka.schemaregistry.client.SchemaRegistryClientConfig
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig
import io.confluent.kafka.streams.serdes.avro.SpecificAvroDeserializer
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.security.auth.SecurityProtocol
import java.time.Duration
import java.util.Properties
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import org.apache.kafka.common.serialization.LongDeserializer

object KafkaConfig {
    internal fun consumer(
        topicName: String,
        env: Map<String, String>,
    ): KafkaConsumer<String, GenericRecord> {
        val maxPollRecords = 50
        val maxPollIntervalMs = Duration.ofSeconds(60 + maxPollRecords * 2.toLong()).toMillis()
        return KafkaConsumer<String, GenericRecord>(
            aivenConfig(env).also {
                it[ConsumerConfig.GROUP_ID_CONFIG] = "dp-oppslag-arbeidssoker-alpha"
                it[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"

                //    keyDeserializer = LongDeserializer::class,
                //            valueDeserializer = PeriodeAvroDeserializer::class,
                it[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = LongDeserializer::class.java
                it[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = PeriodeAvroDeserializer::class.java
                it[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = "false"
                it[KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG] = env.getValue("KAFKA_SCHEMA_REGISTRY")
                it[SchemaRegistryClientConfig.BASIC_AUTH_CREDENTIALS_SOURCE] = "USER_INFO"
                it[SchemaRegistryClientConfig.USER_INFO_CONFIG] =
                    env.getValue("KAFKA_SCHEMA_REGISTRY_USER") + ":" + env.getValue("KAFKA_SCHEMA_REGISTRY_PASSWORD")
                it[ConsumerConfig.MAX_POLL_RECORDS_CONFIG] = maxPollRecords
                it[ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG] = "$maxPollIntervalMs"
            },
        ).also {
            it.subscribe(listOf(topicName))
        }
    }

    class PeriodeAvroDeserializer : SpecificAvroDeserializer<Periode>()

    private fun aivenConfig(env: Map<String, String>): Properties {
        return Properties().apply {
            put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, env.getValue("KAFKA_BROKERS"))
            put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, SecurityProtocol.SSL.name)
            put(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, "")
            put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, "jks")
            put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, "PKCS12")
            put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, env.getValue("KAFKA_TRUSTSTORE_PATH"))
            put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, env.getValue("KAFKA_CREDSTORE_PASSWORD"))
            put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, env.getValue("KAFKA_KEYSTORE_PATH"))
            put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, env.getValue("KAFKA_CREDSTORE_PASSWORD"))
        }
    }
}
