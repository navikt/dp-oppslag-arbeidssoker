package no.nav.dagpenger.arbeidssoker.lytter

import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import no.nav.paw.arbeidssokerregisteret.api.v1.AvviksType
import no.nav.paw.arbeidssokerregisteret.api.v1.Bruker
import no.nav.paw.arbeidssokerregisteret.api.v1.BrukerType
import no.nav.paw.arbeidssokerregisteret.api.v1.Metadata
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import org.apache.kafka.clients.consumer.CloseOptions
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.MockConsumer
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.clients.consumer.internals.AutoOffsetResetStrategy
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.errors.WakeupException
import org.junit.jupiter.api.Assertions.assertTimeoutPreemptively
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class ArbeidssøkerStatusLytterTest {
    private val testRapid = TestRapid()
    private val topic = "arbeidssøkerstatus"
    private val testPartition = TopicPartition(topic, 0)
    private val mockConsumer =
        MockConsumer<Long, Periode>(AutoOffsetResetStrategy.EARLIEST.name()).also {
            it.assign(listOf(testPartition))
            it.updateBeginningOffsets(
                mapOf(
                    testPartition to 0L,
                ),
            )
        }

    @Test
    fun `behandlingsfeil skal committe offsets lukke konsumenten og stoppe appen uten deadlock`() {
        val periode =
            mockk<Periode> {
                every { startet } throws IllegalStateException("Simulert behandlingsfeil")
            }
        val records =
            ConsumerRecords(
                mapOf(testPartition to listOf(ConsumerRecord(topic, 0, 0, 0L, periode))),
                mapOf(testPartition to OffsetAndMetadata(1L)),
            )

        val consumer = assertStopsAfterFailure { records }

        verify(exactly = 1) { consumer.commitSync(mapOf(testPartition to OffsetAndMetadata(0L))) }
        verify(exactly = 1) { consumer.seek(testPartition, 0L) }
    }

    @Test
    fun `delvis behandlet batch skal bare committe behandlede offsets for hver partisjon`() {
        val otherPartition = TopicPartition(topic, 1)
        val periode =
            Periode(
                UUID.randomUUID(),
                "12345678910",
                Metadata(Instant.EPOCH, Bruker(BrukerType.SYSTEM, "test", "test"), "test", "1.0", null),
                null,
            )
        val failedPeriode =
            mockk<Periode> {
                every { startet } throws IllegalStateException("Simulert behandlingsfeil")
            }
        val records =
            ConsumerRecords(
                linkedMapOf(
                    testPartition to
                        listOf(
                            ConsumerRecord(topic, 0, 3, 0L, periode),
                            ConsumerRecord(topic, 0, 4, 0L, failedPeriode),
                        ),
                    otherPartition to listOf(ConsumerRecord(topic, 1, 7, 0L, periode)),
                ),
                mapOf(testPartition to OffsetAndMetadata(5L), otherPartition to OffsetAndMetadata(8L)),
            )

        val consumer = assertStopsAfterFailure { records }

        verify(exactly = 1) {
            consumer.commitSync(
                mapOf(testPartition to OffsetAndMetadata(4L), otherPartition to OffsetAndMetadata(7L)),
            )
        }
        verify(exactly = 0) { consumer.commitSync() }
    }

    @Test
    fun `feil ved seek skal ikke committe ubehandlede offsets`() {
        val periode =
            mockk<Periode> {
                every { startet } throws IllegalStateException("Simulert behandlingsfeil")
            }
        val records =
            ConsumerRecords(
                mapOf(testPartition to listOf(ConsumerRecord(topic, 0, 5, 0L, periode))),
                mapOf(testPartition to OffsetAndMetadata(6L)),
            )

        val consumer =
            assertStopsAfterFailure(
                configureConsumer = { consumer ->
                    every { consumer.seek(testPartition, 5L) } throws IllegalStateException("Simulert seek-feil")
                },
            ) { records }

        verify(exactly = 1) { consumer.commitSync(mapOf(testPartition to OffsetAndMetadata(5L))) }
        verify(exactly = 0) { consumer.commitSync() }
    }

    @Test
    fun `poll-feil skal lukke konsumenten og stoppe appen`() {
        assertStopsAfterFailure {
            throw IllegalStateException("Simulert poll-feil")
        }
    }

    @Test
    fun `uventet wakeup skal stoppe appen`() {
        assertStopsAfterFailure { throw WakeupException() }
    }

    @Test
    fun `Error skal lukke konsumenten og stoppe appen`() {
        assertStopsAfterFailure { throw AssertionError("Simulert Error") }
    }

    @Test
    fun `appstopp skal ikke vente ubegrenset på ressurslukking`() {
        val closing = CountDownLatch(1)
        val releaseClose = CountDownLatch(1)
        val stopped = CountDownLatch(1)
        val consumerThread = AtomicReference<Thread>()
        val consumer =
            mockk<Consumer<Long, Periode>>(relaxed = true) {
                every { poll(any<Duration>()) } answers {
                    consumerThread.set(Thread.currentThread())
                    throw IllegalStateException("Simulert poll-feil")
                }
                every { close(any<CloseOptions>()) } answers {
                    closing.countDown()
                    releaseClose.await()
                }
            }
        val rapid = mockk<RapidsConnection>(relaxed = true)
        val lytter = ArbeidssøkerStatusLytter(consumer, rapid)
        every { rapid.stop() } answers {
            lytter.onShutdown(rapid)
            stopped.countDown()
        }

        lytter.start()
        try {
            closing.await(2, TimeUnit.SECONDS) shouldBe true
            stopped.await(7, TimeUnit.SECONDS) shouldBe true
            consumerThread.get().isAlive shouldBe true
            verify(exactly = 1) { rapid.stop() }
        } finally {
            releaseClose.countDown()
            consumerThread.get()?.join(2_000)
        }
        consumerThread.get().isAlive shouldBe false
    }

    @Test
    fun `normal shutdown skal lukke konsumenten uten å stoppe appen på nytt`() {
        val polling = CountDownLatch(1)
        val wakeupSignal = CountDownLatch(1)
        val consumer =
            mockk<Consumer<Long, Periode>>(relaxed = true) {
                every { poll(any<Duration>()) } answers {
                    polling.countDown()
                    wakeupSignal.await()
                    throw WakeupException()
                }
                every { wakeup() } answers { wakeupSignal.countDown() }
            }
        val rapid = mockk<RapidsConnection>(relaxed = true)
        val lytter =
            ArbeidssøkerStatusLytter(consumer, rapid).also {
                it.start()
            }
        try {
            polling.await(2, TimeUnit.SECONDS) shouldBe true
            assertTimeoutPreemptively(Duration.ofSeconds(2)) {
                lytter.onShutdown(rapid)
            }
            verify(exactly = 1) { consumer.unsubscribe() }
            verify(exactly = 1) { consumer.close(any<CloseOptions>()) }
            verify(exactly = 0) { rapid.stop() }
        } finally {
            wakeupSignal.countDown()
        }
    }

    @Test
    fun `normal shutdown skal ha tidsgrense selv når poll ikke avbrytes`() {
        val polling = CountDownLatch(1)
        val releasePoll = CountDownLatch(1)
        val consumerThread = AtomicReference<Thread>()
        val consumer =
            mockk<Consumer<Long, Periode>>(relaxed = true) {
                every { poll(any<Duration>()) } answers {
                    consumerThread.set(Thread.currentThread())
                    polling.countDown()
                    releasePoll.await()
                    ConsumerRecords.empty()
                }
            }
        val rapid = mockk<RapidsConnection>(relaxed = true)
        val lytter =
            ArbeidssøkerStatusLytter(consumer, rapid).also {
                it.start()
            }

        try {
            polling.await(2, TimeUnit.SECONDS) shouldBe true
            assertTimeoutPreemptively(Duration.ofSeconds(7)) {
                lytter.onShutdown(rapid)
            }
            consumerThread.get().isAlive shouldBe true
            verify(exactly = 0) { rapid.stop() }
        } finally {
            releasePoll.countDown()
            consumerThread.get()?.join(2_000)
        }
        consumerThread.get().isAlive shouldBe false
        verify(exactly = 1) { consumer.close(any<CloseOptions>()) }
    }

    private fun assertStopsAfterFailure(
        configureConsumer: (Consumer<Long, Periode>) -> Unit = {},
        pollRecords: () -> ConsumerRecords<Long, Periode>,
    ): Consumer<Long, Periode> {
        val consumerThread = AtomicReference<Thread>()
        val stopped = CountDownLatch(1)
        val consumer =
            mockk<Consumer<Long, Periode>>(relaxed = true) {
                every { poll(any<Duration>()) } answers {
                    consumerThread.set(Thread.currentThread())
                    pollRecords()
                }
            }
        configureConsumer(consumer)
        val rapid = mockk<RapidsConnection>(relaxed = true)
        val lytter = ArbeidssøkerStatusLytter(consumer, rapid)
        every { rapid.stop() } answers {
            val shutdownCallback = Thread { lytter.onShutdown(rapid) }
            shutdownCallback.start()
            shutdownCallback.join(2_000)
            shutdownCallback.isAlive shouldBe false
            stopped.countDown()
        }

        lytter.start()
        try {
            stopped.await(2, TimeUnit.SECONDS) shouldBe true
            consumerThread.get().join(2_000)
            consumerThread.get().isAlive shouldBe false
            verify(exactly = 1) { rapid.stop() }
            verify(exactly = 1) { consumer.unsubscribe() }
            verify(exactly = 1) { consumer.close(any<CloseOptions>()) }
        } finally {
            consumerThread.get()?.let { thread ->
                thread.interrupt()
                thread.join(2_000)
            }
        }
        return consumer
    }

    @Test
    fun `replikere arbeidssøkerstatus til dagpenger domenet`() {
        runBlocking {
            val arbeidssøkerStatusLytter =
                ArbeidssøkerStatusLytter(
                    consumer = mockConsumer,
                    rapidsConnection = testRapid,
                ).also {
                    it.start()
                }
            val ident = "12345678910"
            val fom = Instant.now()
            val tom = Instant.now()
            val periode =
                Periode(
                    UUID.randomUUID(),
                    ident,
                    Metadata(
                        fom,
                        no.nav.paw.arbeidssokerregisteret.api.v1
                            .Bruker(BrukerType.SLUTTBRUKER, ident, "vely sikker"),
                        "arbeidssøkerstatus",
                        "1.0",
                        no.nav.paw.arbeidssokerregisteret.api.v1
                            .TidspunktFraKilde(Instant.now(), AvviksType.RETTING),
                    ),
                    Metadata(
                        tom,
                        no.nav.paw.arbeidssokerregisteret.api.v1
                            .Bruker(BrukerType.SLUTTBRUKER, ident, "vely sikker"),
                        "arbeidssøkerstatus",
                        "1.0",
                        no.nav.paw.arbeidssokerregisteret.api.v1
                            .TidspunktFraKilde(Instant.now(), AvviksType.RETTING),
                    ),
                )

            mockConsumer.addRecord(ConsumerRecord(topic, 0, 0, 0L, periode))

            delay(500)

            testRapid.inspektør.size shouldBe 1
            with(testRapid.inspektør) {
                val message = message(0)
                message["@event_name"].asString() shouldBe "arbeidssøkerstatus_endret"
                message["periodeId"].asString() shouldBe periode.id.toString()
                message["fom"].asLocalDateTime() shouldBe
                    fom.atZone(ZoneId.systemDefault()).toLocalDateTime().truncatedTo(
                        ChronoUnit.MILLIS,
                    )
                message["tom"].asLocalDateTime() shouldBe
                    tom.atZone(ZoneId.systemDefault()).toLocalDateTime().truncatedTo(
                        ChronoUnit.MILLIS,
                    )

                message["avsluttetAv"].asString() shouldBe "sluttbruker"

                message["@kilde"].shouldNotBeNull()
            }
            arbeidssøkerStatusLytter.onShutdown(testRapid)
        }
    }

    @Test
    fun ` lese periode som ikke er avsluttet `() {
        runBlocking {
            val arbeidssøkerStatusLytter =
                ArbeidssøkerStatusLytter(
                    consumer = mockConsumer,
                    rapidsConnection = testRapid,
                ).also {
                    it.start()
                }
            val ident = "12345678910"
            val fom = Instant.now()
            val periode =
                Periode().apply {
                    id = UUID.randomUUID()
                    identitetsnummer = ident
                    startet =
                        Metadata(
                            fom,
                            no.nav.paw.arbeidssokerregisteret.api.v1.Bruker(
                                BrukerType.SLUTTBRUKER,
                                ident,
                                "vely sikker",
                            ),
                            "arbeidssøkerstatus",
                            "1.0",
                            no.nav.paw.arbeidssokerregisteret.api.v1.TidspunktFraKilde(
                                Instant.now(),
                                AvviksType.RETTING,
                            ),
                        )
                }

            mockConsumer.addRecord(ConsumerRecord(topic, 0, 0, 0L, periode))

            delay(500)

            testRapid.inspektør.size shouldBe 1
            with(testRapid.inspektør) {
                val message = message(0)
                message["@event_name"].asString() shouldBe "arbeidssøkerstatus_endret"
                message["periodeId"].asString() shouldBe periode.id.toString()
                message["fom"].asLocalDateTime() shouldBe
                    fom.atZone(ZoneId.systemDefault()).toLocalDateTime().truncatedTo(
                        ChronoUnit.MILLIS,
                    )

                message["@kilde"].shouldNotBeNull()
            }
            arbeidssøkerStatusLytter.onShutdown(testRapid)
        }
    }
}
