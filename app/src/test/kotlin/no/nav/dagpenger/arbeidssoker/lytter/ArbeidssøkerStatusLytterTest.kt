package no.nav.dagpenger.arbeidssoker.lytter

import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.nulls.shouldNotBeNull
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
import java.time.ZoneId
import java.time.temporal.ChronoUnit
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
            val fom = Instant.now()
            val tom = Instant.now()
            val periode =
                no.nav.paw.arbeidssokerregisteret.api.v1.Periode(
                    UUID.randomUUID(),
                    ident,
                    Metadata(
                        fom,
                        no.nav.paw.arbeidssokerregisteret.api.v1.Bruker(BrukerType.SLUTTBRUKER, ident, "vely sikker"),
                        "arbeidssøkerstatus",
                        "1.0",
                        no.nav.paw.arbeidssokerregisteret.api.v1.TidspunktFraKilde(Instant.now(), AvviksType.RETTING),
                    ),
                    Metadata(
                        tom,
                        no.nav.paw.arbeidssokerregisteret.api.v1.Bruker(BrukerType.SLUTTBRUKER, ident, "vely sikker"),
                        "arbeidssøkerstatus",
                        "1.0",
                        no.nav.paw.arbeidssokerregisteret.api.v1.TidspunktFraKilde(Instant.now(), AvviksType.RETTING),
                    ),
                )

            mockConsumer.addRecord(ConsumerRecord(topic, 0, 0, 0L, periode))

            delay(500)

            testRapid.inspektør.size shouldBe 1
            with(testRapid.inspektør) {
                val message = message(0)
                message["@event_name"].asText() shouldBe "arbeidssøkerstatus_endret"
                message["periodeId"].asText() shouldBe periode.id.toString()
                message["fom"].asLocalDateTime() shouldBe
                    fom.atZone(ZoneId.systemDefault()).toLocalDateTime().truncatedTo(
                        ChronoUnit.MILLIS,
                    )
                message["tom"].asLocalDateTime() shouldBe
                    tom.atZone(ZoneId.systemDefault()).toLocalDateTime().truncatedTo(
                        ChronoUnit.MILLIS,
                    )

                message["tomSattAv"].asText() == "sluttbruker"

                message["@kilde"].shouldNotBeNull()
            }
            arbeidssøkerStatusLytter.stop()
        }
    }

    @Test
    fun ` lese periode som ikke er avsluttet `() {
        runBlocking {
            val arbeidssøkerStatusLytter =
                ArbeidssøkerStatusLytter(
                    consumer = mockConsumer,
                    rapidsConnection = testRapid,
                ).also { it.start() }
            val ident = "12345678910"
            val fom = Instant.now()
            val periode =
                no.nav.paw.arbeidssokerregisteret.api.v1.Periode().apply {
                    id = UUID.randomUUID()
                    identitetsnummer = ident
                    startet =
                        Metadata(
                            fom,
                            no.nav.paw.arbeidssokerregisteret.api.v1.Bruker(BrukerType.SLUTTBRUKER, ident, "vely sikker"),
                            "arbeidssøkerstatus",
                            "1.0",
                            no.nav.paw.arbeidssokerregisteret.api.v1.TidspunktFraKilde(Instant.now(), AvviksType.RETTING),
                        )
                }

            mockConsumer.addRecord(ConsumerRecord(topic, 0, 0, 0L, periode))

            delay(500)

            testRapid.inspektør.size shouldBe 1
            with(testRapid.inspektør) {
                val message = message(0)
                message["@event_name"].asText() shouldBe "arbeidssøkerstatus_endret"
                message["periodeId"].asText() shouldBe periode.id.toString()
                message["fom"].asLocalDateTime() shouldBe
                    fom.atZone(ZoneId.systemDefault()).toLocalDateTime().truncatedTo(
                        ChronoUnit.MILLIS,
                    )

                message["@kilde"].shouldNotBeNull()
            }
            arbeidssøkerStatusLytter.stop()
        }
    }
}
