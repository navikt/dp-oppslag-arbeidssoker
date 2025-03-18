package no.nav.dagpenger.arbeidssoker.lytter

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
import java.time.ZoneId
import kotlin.coroutines.CoroutineContext

internal class ArbeidssøkerStatusLytter(
    private val consumer: Consumer<Long, Periode>,
    private val rapidsConnection: RapidsConnection,
) : CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job
    private val job: Job = Job()

    private val navn = this::class.java.simpleName

    init {
        Runtime.getRuntime().addShutdownHook(Thread(::shutdownHook))
    }

    private companion object {
        private val logger = KotlinLogging.logger {}
    }

    fun isAlive(): Boolean = job.isActive

    fun start() {
        logger.info("Starter $navn")
        launch {
            run()
        }
    }

    private fun isAlive(check: () -> Any): Boolean =
        runCatching(check).fold(
            { true },
            {
                logger.error("Alive sjekk feilet", it)
                false
            },
        )

    fun stop() {
        logger.info("Stopper $navn")
        consumer.wakeup()
        job.cancel()
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

    private fun onRecords(records: ConsumerRecords<Long, no.nav.paw.arbeidssokerregisteret.api.v1.Periode>) {
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
                val tom = periode.avsluttet?.tidspunkt?.atZone(ZoneId.systemDefault())?.toLocalDateTime()

                val periodeId = periode.id
                val detaljer =
                    mutableMapOf("fom" to fom, "ident" to periode.identitetsnummer, "periodeId" to periodeId)
                if (tom != null) {
                    detaljer["tom"] = tom
                }

                logger.info { "Publiserer arbeidssøkerperiode $periodeId" }
                rapidsConnection.publish(
                    periode.identitetsnummer,
                    JsonMessage.newMessage(
                        "arbeidssoker_periode",
                        detaljer.toMap(),
                    ).toJson(),
                )
                logger.info { "Har publisert arbeidssøkerperiode $periodeId" }

                currentPositions[TopicPartition(record.topic(), record.partition())] = record.offset() + 1
            }
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
