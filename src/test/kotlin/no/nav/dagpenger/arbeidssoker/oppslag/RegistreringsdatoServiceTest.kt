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
        coEvery {
            arbeidsøkerRegister.hentRegistreringsperiode(any(), any(), any())
        } returns listOf(Periode(LocalDate.now(), LocalDate.now()))
        testRapid.sendTestMessage(behovJson)
        assertEquals(1, testRapid.inspektør.size)
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
      "InnsendtSøknadsId": "123"
    }
    """.trimIndent()
