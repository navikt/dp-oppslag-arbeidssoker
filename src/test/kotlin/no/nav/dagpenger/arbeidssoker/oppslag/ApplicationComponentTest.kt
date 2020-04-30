package no.nav.dagpenger.arbeidssoker.oppslag

import com.fasterxml.jackson.databind.JsonNode
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.shouldBe
import io.ktor.util.KtorExperimentalAPI
import io.mockk.every
import io.mockk.mockk
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@KtorExperimentalAPI
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ApplicationComponentTest {
    private val arbeidssøkeroppslag: Arbeidssøkeroppslag = mockk<Arbeidssøkeroppslag>().also {
        every {
            it.bestemRegistrertArbeidssøker("12345")
        } returns RegistrertArbeidssøker(erReellArbeidssøker = true)
    }
    private val rapid = TestRapid().apply {
        LøsningService(this, arbeidssøkeroppslag)
    }

    @Test
    fun `skal motta behov og produsere RegistrertArbeidssøker-løsning`() {
        rapid.sendTestMessage(
            """{
                    "@id": "1", 
                    "@behov": ["RegistrertArbeidssøker"], 
                    "fødselsnummer":"12345"
                }""".trimIndent()
        )

        with(rapid.inspektør) {
            size shouldBeExactly 1

            field(0, "@behov").map(JsonNode::asText).shouldContain("RegistrertArbeidssøker")
            field(0, "@løsning").hasNonNull("RegistrertArbeidssøker")
            field(0, "@løsning")["RegistrertArbeidssøker"]["erReellArbeidssøker"].asBoolean() shouldBe true
        }
    }
}
