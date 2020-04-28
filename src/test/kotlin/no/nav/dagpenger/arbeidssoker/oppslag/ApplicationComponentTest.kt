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

    val arbeidssøkeroppslag: Arbeidssøkeroppslag = mockk<Arbeidssøkeroppslag>().also {
        every {
            it.bestemRegistrertArbeidssøker("12345")
        } returns RegistrertArbeidssøker(erReellArbeidssøker = true)
    }

    private val rapid = TestRapid().apply { Application(this, arbeidssøkeroppslag) }

    @Test
    fun `skal motta behov og produsere RegistrertArbeidssøker-løsning`() {

        rapid.sendTestMessage(
                """{
                    "@id": "1", 
                    "@behov": ["RegistrertArbeidssøker"], 
                    "fnr":"12345"
                }""".trimIndent()
        )

        val inspektør = rapid.inspektør
        inspektør.size shouldBeExactly 1
        val message = inspektør.message(0)
        message["@behov"].map(JsonNode::asText).shouldContain("RegistrertArbeidssøker")
        message["@løsning"].hasNonNull("RegistrertArbeidssøker")
        message["@løsning"]["RegistrertArbeidssøker"]["erReellArbeidssøker"].asBoolean() shouldBe true
    }
}
