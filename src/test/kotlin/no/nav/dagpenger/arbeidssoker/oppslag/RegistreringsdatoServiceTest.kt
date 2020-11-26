package no.nav.dagpenger.arbeidssoker.oppslag

import io.mockk.coEvery
import io.mockk.mockk
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class RegistreringsdatoServiceTest {
    private val testRapid = TestRapid()
    private val arbeidsøkerRegister: ArbeidssøkerRegister = mockk()

    @BeforeEach
    fun setup() {
        testRapid.reset()
        RegistreringsdatoService(testRapid, arbeidsøkerRegister)
    }

    @Test
    fun `svarer på behov`() {
        val startDato = LocalDate.of(2020, 1, 1)
        val sluttDato = startDato.plusDays(7)
        val startDato2 = sluttDato.plusDays(1)
        val sluttDato2 = sluttDato.plusDays(8)

        coEvery {
            arbeidsøkerRegister.hentRegistreringsperiode(any(), any(), any())
        } returns listOf(
            Periode(startDato, sluttDato),
            Periode(startDato2, sluttDato2)
        )
        testRapid.sendTestMessage(behovJson)

        assertEquals(1, testRapid.inspektør.size)
        val message = testRapid.inspektør.message(0)
        assertEquals("faktum_svar", message["@event_name"].asText())
        assertEquals(startDato.toString(), message["fakta"][0]["svar"][0]["fom"].asText())
        assertEquals(sluttDato.toString(), message["fakta"][0]["svar"][0]["tom"].asText())
        assertEquals(startDato2.toString(), message["fakta"][0]["svar"][1]["fom"].asText())
        assertEquals(sluttDato2.toString(), message["fakta"][0]["svar"][1]["tom"].asText())
    }
}

@Language("json")
private val behovJson =
    """
    {
      "@event_name": "behov",
      "@opprettet": "2020-11-18T11:04:32.867824",
      "@id": "930e2beb-d394-4024-b713-dbeb6ad3d4bf",
      "fnr": "123",
      "søknad_uuid": "41621ac0-f5ee-4cce-b1f5-88a79f25f1a5",
      "fakta": [ 
        {
          "behov": "Registreringsdato"
        }
      ],
      "@behov": [
        "Registreringsdato"
      ],
        "Søknadstidspunkt": "2020-11-09"
    }
    """.trimIndent()
