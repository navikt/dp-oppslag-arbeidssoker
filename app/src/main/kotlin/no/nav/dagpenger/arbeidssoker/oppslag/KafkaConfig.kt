package no.nav.dagpenger.arbeidssoker.oppslag

import com.github.navikt.tbd_libs.kafka.AivenConfig
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
        }

    internal fun consumer(
        topicName: String,
        groupId: String,
        withShutdownHook: Boolean = true,
    ): KafkaConsumer<Long, Periode> =
        KafkaConsumer(config.consumerConfig(groupId, defaultConsumerProperties), LongDeserializer(), PeriodeAvroDeserializer()).also {
            if (withShutdownHook) {
                Runtime.getRuntime().addShutdownHook(
                    Thread {
                        it.wakeup()
                    },
                )
            }
            it.subscribe(listOf(topicName))
        }

    class PeriodeAvroDeserializer : SpecificAvroDeserializer<Periode>()
}
