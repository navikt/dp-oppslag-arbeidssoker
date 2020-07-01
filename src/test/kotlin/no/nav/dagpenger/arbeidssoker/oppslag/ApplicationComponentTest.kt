package no.nav.dagpenger.arbeidssoker.oppslag

import com.fasterxml.jackson.databind.JsonNode
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.date.shouldBeToday
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.shouldBe
import io.ktor.util.KtorExperimentalAPI
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@KtorExperimentalAPI
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ApplicationComponentTest {
    private val arbeidssøkeroppslag: Arbeidssøkeroppslag = mockk<Arbeidssøkeroppslag>().also {
        every {
            runBlocking { it.bestemRegistrertArbeidssøker("12345") }
        } returns RegistrertArbeidssøker(
            erRegistrert = true,
            formidlingsgruppe = "ARBS",
            registreringsdato = LocalDate.now()
        )
    }
    private val rapid = TestRapid().apply {
        RegistrertArbeidssøkerLøsningService(this, arbeidssøkeroppslag)
    }

    @Test
    fun `skal motta behov og produsere RegistrertArbeidssøker-løsning`() {
        rapid.sendTestMessage(behov)

        with(rapid.inspektør) {
            size shouldBeExactly 1

            field(0, "@behov").map(JsonNode::asText).shouldContain("RegistrertArbeidssøker")
            with(field(0, "@løsning")["RegistrertArbeidssøker"]) {
                this["erRegistrert"].asBoolean() shouldBe true
                this["formidlingsgruppe"].asText() shouldBe "ARBS"
                this["registreringsdato"].asLocalDate().shouldBeToday()
            }
        }
    }
}

@Language("JSON")
private val behov = """{
  "@id": "1",
  "@behov": [
    "RegistrertArbeidssøker"
  ],
  "vedtakId": "12345",
  "fødselsnummer": "12345"
}""".trimIndent()
