package no.nav.dagpenger.arbeidssoker.lytter

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.paw.arbeidssokerregisteret.api.v1.BrukerType
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.errors.WakeupException
import java.time.Duration
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicBoolean

internal class ArbeidssøkerStatusLytter(
    private val consumer: Consumer<Long, Periode>,
    private val rapidsConnection: RapidsConnection,
) : RapidsConnection.StatusListener {
    private val running = AtomicBoolean(true)
    private val consumerThread: Thread

    init {
        consumerThread = Thread { consumeLoop() }.apply { isDaemon = true }
    }

    private companion object {
        val logger = KotlinLogging.logger {}
        val objectMapper: ObjectMapper =
            jacksonObjectMapper()
                .registerModule(JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    }

    override fun onReady(rapidsConnection: RapidsConnection) {
        start()
    }

    override fun onShutdown(rapidsConnection: RapidsConnection) {
        stop()
    }

    fun start() {
        logger.info("Starter ArbeidssøkerStatusLytter")
        if (!consumerThread.isAlive) {
            consumerThread.start()
        }
    }

    fun stop() {
        logger.info("Stopper ArbeidssøkerStatusLytter")
        running.set(false)
        consumer.wakeup() // Interrupts poll() safely
        consumerThread.join() // Ensure thread shutdown
        rapidsConnection.stop()
    }

    private fun consumeLoop() {
        try {
            while (running.get()) {
                val records = consumer.poll(Duration.ofSeconds(1))
                processRecords(records)
            }
        } catch (e: WakeupException) {
            if (running.get()) throw e
        } catch (e: Exception) {
            logger.error(e) { "Noe feil skjedde i consumeringen" }
        } finally {
            closeResources()
        }
    }

    private fun processRecords(records: ConsumerRecords<Long, Periode>) {
        if (records.isEmpty) return

        val currentPositions =
            records
                .groupBy { TopicPartition(it.topic(), it.partition()) }
                .mapValues { partition -> partition.value.minOf { it.offset() } }
                .toMutableMap()

        try {
            records.forEach { record ->
                val periode = record.value()
                val fom = periode.startet.tidspunkt.atZone(ZoneId.systemDefault()).toLocalDateTime()

                val periodeId = periode.id
                val data = objectMapper.readTree(periode.toString())

                val detaljer =
                    mutableMapOf(
                        "fom" to fom,
                        "ident" to periode.identitetsnummer,
                        "periodeId" to periodeId,
                        "@kilde" to mapOf("data" to objectMapper.convertValue<Map<String, Any>>(data)),
                    )

                if (periode.avsluttet != null) {
                    val tom =
                        periode.avsluttet?.tidspunkt?.atZone(ZoneId.systemDefault())?.toLocalDateTime()
                    val avsluttetAv =
                        when (periode.avsluttet?.utfoertAv?.type) {
                            BrukerType.UKJENT_VERDI -> "ukjent"
                            BrukerType.UDEFINERT -> "udefinert"
                            BrukerType.VEILEDER -> "veileder"
                            BrukerType.SYSTEM -> "system"
                            BrukerType.SLUTTBRUKER -> "sluttbruker"
                            null -> "udefinert"
                        }
                    detaljer["tom"] = tom
                    detaljer["tomSattAv"] = avsluttetAv
                }

                withLoggingContext(
                    "periodeId" to periodeId.toString(),
                ) {
                    logger.info { "Publiserer arbeidssøkerperiode" }
                    rapidsConnection.publish(
                        periode.identitetsnummer,
                        JsonMessage.newMessage("arbeidssøkerstatus_endret", detaljer).toJson(),
                    )
                    logger.info { "Har publisert arbeidssøkerperiode" }
                }

                currentPositions[TopicPartition(record.topic(), record.partition())] = record.offset() + 1
            }
        } catch (err: Exception) {
            logger.info("Feil ved behandling av meldinger. Tilbakestiller offsets: $currentPositions", err)
            currentPositions.forEach { (partition, offset) -> consumer.seek(partition, offset) }
            stop()
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
}
