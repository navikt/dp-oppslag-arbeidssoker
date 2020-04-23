package no.nav.dagpenger.arbeidssoker.oppslag

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.shouldBe
import io.ktor.util.KtorExperimentalAPI
import io.mockk.every
import io.mockk.mockk
import no.nav.helse.rapids_rivers.InMemoryRapid
import no.nav.helse.rapids_rivers.inMemoryRapid
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@KtorExperimentalAPI
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ApplicationComponentTest {

    @Test
    fun `skal motta behov og produsere RegistrertArbeidssøker-løsning`() {

        val arbeidssøkeroppslag: Arbeidssøkeroppslag = mockk()

        every {
            arbeidssøkeroppslag.bestemRegistrertArbeidssøker("12345")
        } returns RegistrertArbeidssøker(erReellArbeidssøker = true)

        val rapid = createRapid {
            Application(it, arbeidssøkeroppslag)
        }
        rapid.sendToListeners(
                """{
                    "@id": "1", 
                    "@behov": ["RegistrertArbeidssøker"], 
                    "fnr":"12345"
                }""".trimIndent()
        )

        validateMessages(rapid) { messages ->
            messages.size.shouldBeExactly(1)
            messages.first().also { message ->
                message["@behov"].map(JsonNode::asText).shouldContain("RegistrertArbeidssøker")
                message["@løsning"].hasNonNull("RegistrertArbeidssøker")
                message["@løsning"]["RegistrertArbeidssøker"]["erReellArbeidssøker"].asBoolean() shouldBe true
            }
        }
    }

    private fun createRapid(service: (InMemoryRapid) -> Any): InMemoryRapid {
        return inMemoryRapid { }.also { service(it) }
    }

    private fun validateMessages(rapid: InMemoryRapid, assertions: (messages: List<JsonNode>) -> Any) {
        rapid.outgoingMessages.map { jacksonObjectMapper().readTree(it.value) }.also { assertions(it) }
    }
}
