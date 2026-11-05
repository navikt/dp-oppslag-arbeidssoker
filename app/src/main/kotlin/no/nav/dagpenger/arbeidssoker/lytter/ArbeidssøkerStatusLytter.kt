package no.nav.dagpenger.arbeidssoker.lytter

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonMapperBuilder
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import no.nav.paw.arbeidssokerregisteret.api.v1.BrukerType
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import org.apache.kafka.clients.consumer.CloseOptions
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.OffsetAndMetadata
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
    private val applicationStopping = AtomicBoolean(false)
    private val consumerThread: Thread

    init {
        consumerThread = Thread { consumeLoop() }.apply { isDaemon = true }
    }

    private companion object {
        val shutdownTimeout = Duration.ofSeconds(5)
        val logger = KotlinLogging.logger {}
        val sikkerLogg = KotlinLogging.logger("tjenestekall.ArbeidssøkerStatusLytter")
        val objectMapper: ObjectMapper =
            jacksonMapperBuilder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build()
    }

    override fun onReady(rapidsConnection: RapidsConnection) {
        start()
    }

    override fun onShutdown(rapidsConnection: RapidsConnection) {
        if (applicationStopping.compareAndSet(false, true)) {
            stopConsumer()
        }
    }

    fun start() {
        logger.info { "Starter ArbeidssøkerStatusLytter" }
        if (!consumerThread.isAlive) {
            consumerThread.start()
        }
    }

    fun stop() {
        stopConsumer()
        stopApplication()
    }

    private fun stopConsumer() {
        logger.info { "Stopper ArbeidssøkerStatusLytter" }
        if (running.getAndSet(false)) {
            consumer.wakeup()
        }
        awaitConsumerShutdown()
    }

    private fun awaitConsumerShutdown() {
        if (Thread.currentThread() == consumerThread) return
        consumerThread.join(shutdownTimeout.toMillis())
        if (consumerThread.isAlive) {
            logger.warn { "Konsumenten avsluttet ikke innen tidsfristen. Fortsetter appstopp." }
        }
    }

    private fun stopApplication() {
        if (applicationStopping.compareAndSet(false, true)) {
            rapidsConnection.stop()
        }
    }

    private fun consumeLoop() {
        var failed = true
        try {
            while (running.get()) {
                val records = consumer.poll(Duration.ofSeconds(1))
                processRecords(records)
            }
            failed = false
        } catch (e: Throwable) {
            failed = e !is WakeupException || running.get()
            if (failed) {
                logger.error(e) { "Noe feil skjedde i consumeringen" }
            }
        } finally {
            running.set(false)
            if (failed) {
                // Shutdown callbacks may wait for the consumer thread to exit.
                Thread(
                    {
                        awaitConsumerShutdown()
                        stopApplication()
                    },
                    "arbeidssoker-shutdown",
                ).apply {
                    isDaemon = false
                    start()
                }
            }
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
                val fom =
                    periode.startet.tidspunkt
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime()

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
                        periode.avsluttet
                            ?.tidspunkt
                            ?.atZone(ZoneId.systemDefault())
                            ?.toLocalDateTime()
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
                    detaljer["avsluttetAv"] = avsluttetAv
                }

                withLoggingContext(
                    "periodeId" to periodeId.toString(),
                ) {
                    logger.info { "Publiserer arbeidssøkerperiode" }
                    val message = JsonMessage.newMessage("arbeidssøkerstatus_endret", detaljer).toJson()
                    rapidsConnection.publish(
                        periode.identitetsnummer,
                        message,
                    )
                    sikkerLogg.info { "Har publisert: $message" }
                    logger.info { "Har publisert arbeidssøkerperiode" }
                }

                currentPositions[TopicPartition(record.topic(), record.partition())] = record.offset() + 1
            }
        } catch (err: Exception) {
            logger.info(err) { "Feil ved behandling av meldinger. Tilbakestiller offsets: $currentPositions" }
            currentPositions.forEach { (partition, offset) -> consumer.seek(partition, offset) }
            throw err
        } finally {
            consumer.commitSync(currentPositions.mapValues { (_, offset) -> OffsetAndMetadata(offset) })
        }
    }

    private fun closeResources() {
        tryAndLog(consumer::unsubscribe)
        tryAndLog { consumer.close(CloseOptions.timeout(shutdownTimeout)) }
    }

    private fun tryAndLog(block: () -> Unit) {
        try {
            block()
        } catch (err: Exception) {
            logger.error(err) { err.message }
        }
    }
}
