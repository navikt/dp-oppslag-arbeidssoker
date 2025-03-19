package no.nav.dagpenger.arbeidssoker.lytter

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import mu.KotlinLogging
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.errors.WakeupException
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.coroutines.CoroutineContext

internal class ArbeidssøkerStatusLytter(
    private val consumer: Consumer<Long, Periode>,
    private val rapidsConnection: RapidsConnection,
) : CoroutineScope, RapidsConnection.StatusListener {
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job
    private val job: Job = Job()

    private val navn = this::class.java.simpleName

    override fun onReady(rapidsConnection: RapidsConnection) {
        start()
    }

    override fun onShutdown(rapidsConnection: RapidsConnection) {
        stop()
    }

    private companion object {
        val logger = KotlinLogging.logger {}
        val objectMapper: ObjectMapper =
            jacksonObjectMapper()
                .registerModule(JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    }

    fun start() {
        logger.info("Starter $navn")
        launch {
            run()
        }
    }

    fun stop() {
        logger.info("Stopper $navn")
        consumer.wakeup()
        job.cancel()
        rapidsConnection.stop()
    }

    private fun run() {
        try {
            while (job.isActive) {
                onRecords(consumer.poll(Duration.ofSeconds(1)))
            }
        } catch (e: WakeupException) {
            if (job.isActive) throw e
        } catch (e: Exception) {
            logger.error(e) { "Noe feil skjedde i consumeringen" }
            throw e
        } finally {
            closeResources()
        }
    }

    private fun onRecords(records: ConsumerRecords<Long, Periode>) {
        if (records.isEmpty) return // poll returns an empty collection in case of rebalancing
        val currentPositions =
            records
                .groupBy { TopicPartition(it.topic(), it.partition()) }
                .mapValues { partition -> partition.value.minOf { it.offset() } }
                .toMutableMap()
        try {
            records.onEach { record ->
                val periode = record.value()
                val fom = periode.startet.tidspunkt.atZone(ZoneId.systemDefault()).toLocalDateTime()
                val tom = periode.avsluttet?.tidspunkt?.atZone(ZoneId.systemDefault())?.toLocalDateTime() ?: LocalDateTime.MAX

                val periodeId = periode.id
                val data = objectMapper.readTree(periode.toString())

                val detaljer =
                    mapOf(
                        "fom" to fom,
                        "tom" to tom,
                        "ident" to periode.identitetsnummer,
                        "periodeId" to periodeId,
                        "@kilde" to mapOf("data" to objectMapper.convertValue<Map<String, Any>>(data)),
                    )

                logger.info { "Publiserer arbeidssøkerperiode $periodeId" }
                rapidsConnection.publish(
                    periode.identitetsnummer,
                    JsonMessage.newMessage(
                        "arbeidssoker_status_endret",
                        detaljer,
                    ).toJson(),
                )
                logger.info { "Har publisert arbeidssøkerperiode $periodeId" }

                currentPositions[TopicPartition(record.topic(), record.partition())] = record.offset() + 1
            }
        } catch (err: WakeupException) {
            logger.info("Exiting consumer after ${if (!job.isCancelled) "receiving shutdown signal" else "being interrupted by someone"}")
        } catch (err: Exception) {
            logger.info(
                "due to an error during processing, positions are reset to each next message (after each record that was processed OK):" +
                    currentPositions.map { "\tpartition=${it.key}, offset=${it.value}" }
                        .joinToString(separator = "\n", prefix = "\n", postfix = "\n"),
                err,
            )
            currentPositions.forEach { (partition, offset) -> consumer.seek(partition, offset) }
            throw err
        } finally {
            consumer.commitSync()
        }
    }

    private fun closeResources() {
        tryAndLog(consumer::unsubscribe)
        tryAndLog(consumer::close)
    }

    private fun tryAndLog(block: () -> Unit) {
        try {
            block()
        } catch (err: Exception) {
            logger.error(err.message, err)
        }
    }

    private fun shutdownHook() {
        logger.info("received shutdown signal, stopping app")
        stop()
    }
}
