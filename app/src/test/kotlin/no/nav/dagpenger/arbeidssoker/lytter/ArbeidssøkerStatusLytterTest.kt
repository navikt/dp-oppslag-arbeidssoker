package no.nav.dagpenger.arbeidssoker.lytter

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import no.nav.paw.arbeidssokerregisteret.api.v1.AvviksType
import no.nav.paw.arbeidssokerregisteret.api.v1.BrukerType
import no.nav.paw.arbeidssokerregisteret.api.v1.Metadata
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.MockConsumer
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.apache.kafka.common.TopicPartition
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class ArbeidssøkerStatusLytterTest {
    private val testRapid = TestRapid()
    private val topic = "arbeidssøkerstatus"
    private val testPartition = TopicPartition(topic, 0)
    private val mockConsumer =
        MockConsumer<Long, no.nav.paw.arbeidssokerregisteret.api.v1.Periode>(OffsetResetStrategy.EARLIEST).also {
            it.assign(listOf(testPartition))
            it.updateBeginningOffsets(
                mapOf(
                    testPartition to 0L,
                ),
            )
        }

    @Test
    fun `replikere arbeidssøkerstatus til dagpenger domenet`() {
        runBlocking {
            val arbeidssøkerStatusLytter =
                ArbeidssøkerStatusLytter(
                    consumer = mockConsumer,
                    rapidsConnection = testRapid,
                ).also { it.start() }
            val ident = "12345678910"
            val periode =
                no.nav.paw.arbeidssokerregisteret.api.v1.Periode(
                    UUID.randomUUID(),
                    ident,
                    Metadata(
                        Instant.now(),
                        no.nav.paw.arbeidssokerregisteret.api.v1.Bruker(BrukerType.SLUTTBRUKER, ident, "vely sikker"),
                        "arbeidssøkerstatus",
                        "1.0",
                        no.nav.paw.arbeidssokerregisteret.api.v1.TidspunktFraKilde(Instant.now(), AvviksType.RETTING),
                    ),
                    Metadata(
                        Instant.now(),
                        no.nav.paw.arbeidssokerregisteret.api.v1.Bruker(BrukerType.SLUTTBRUKER, ident, "vely sikker"),
                        "arbeidssøkerstatus",
                        "1.0",
                        no.nav.paw.arbeidssokerregisteret.api.v1.TidspunktFraKilde(Instant.now(), AvviksType.RETTING),
                    ),
                )

            mockConsumer.addRecord(ConsumerRecord(topic, 0, 0, 0L, periode))

            delay(500)

            testRapid.inspektør.size shouldBe 1
        }
    }
}
