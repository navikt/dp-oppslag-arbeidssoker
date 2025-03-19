package no.nav.dagpenger.arbeidssoker.oppslag

import com.github.navikt.tbd_libs.kafka.AivenConfig
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClientConfig
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig
import io.confluent.kafka.streams.serdes.avro.SpecificAvroDeserializer
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.LongDeserializer
import java.util.Properties

object KafkaConfig {
    private val config = AivenConfig.default

    private val defaultConsumerProperties =
        Properties().apply {
            this[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
            this[KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG] = System.getenv("KAFKA_SCHEMA_REGISTRY")
            this[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = LongDeserializer::class.java
            this[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = PeriodeAvroDeserializer::class.java
            this[KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG] = true
            this[SchemaRegistryClientConfig.BASIC_AUTH_CREDENTIALS_SOURCE] = "USER_INFO"
            this[SchemaRegistryClientConfig.USER_INFO_CONFIG] =
                System.getenv("KAFKA_SCHEMA_REGISTRY_USER") + ":" + System.getenv("KAFKA_SCHEMA_REGISTRY_PASSWORD")
        }

    internal fun consumer(
        topicName: String,
        groupId: String,
    ): KafkaConsumer<Long, Periode> =
        KafkaConsumer<Long, Periode>(config.consumerConfig(groupId, defaultConsumerProperties)).also {
            it.subscribe(listOf(topicName))
        }

    class PeriodeAvroDeserializer : SpecificAvroDeserializer<Periode>()
}
