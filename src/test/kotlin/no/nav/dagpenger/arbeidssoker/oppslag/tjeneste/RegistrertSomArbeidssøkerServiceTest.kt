package no.nav.dagpenger.arbeidssoker.oppslag.tjeneste

import io.mockk.coEvery
import io.mockk.mockk
import no.nav.dagpenger.arbeidssoker.oppslag.arbeidssøkerregister.Arbeidssøkerregister
import no.nav.dagpenger.arbeidssoker.oppslag.arbeidssøkerregister.Periode
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class RegistrertSomArbeidssøkerServiceTest {
    private val rapid = TestRapid()
    private val arbeidsøkerRegister: Arbeidssøkerregister = mockk()
    private val ønsketDato = LocalDate.of(2020, 1, 1)

    init {

        RegistrertSomArbeidssøkerService(rapid, arbeidsøkerRegister)
    }

    @Test
    fun `svarer på om person er registrert som arbeidssøker`() {
        val sluttDato = ønsketDato.plusDays(7)
        val startDato2 = sluttDato.plusDays(1)
        val sluttDato2 = sluttDato.plusDays(8)

        coEvery {
            arbeidsøkerRegister.hentRegistreringsperiode(any(), any(), any())
        } returns
            listOf(
                Periode(ønsketDato, sluttDato),
                Periode(startDato2, sluttDato2),
            )
        rapid.sendTestMessage(json)

        with(rapid.inspektør) {
            assertEquals(1, size)
            val løsning = field(0, "@løsning")
            val verdi = løsning["RegistrertSomArbeidssøker"]
            assertEquals(true, verdi["verdi"].asBoolean())
            assertEquals(ønsketDato, verdi["gyldigTilOgMed"].asLocalDate())
            assertEquals(ønsketDato, verdi["gyldigFraOgMed"].asLocalDate())
        }
    }

    // language=json
    private val json =
        """
        {
          "@event_name": "behov",
          "@behovId": "83894fc2-6e45-4534-abd1-97a441c57b2f",
          "@behov": [
            "RegistrertSomArbeidssøker"
          ],
          "ident": "11109233444",
          "behandlingId": "018e9e8d-35f3-7835-9569-5c59ec0737da",
          "gjelderDato": "2024-04-02",
          "fagsakId": "123",
          "søknadId": "4afce924-6cb4-4ab4-a92b-fe91e24f31bf",
          "søknad_uuid": "4afce924-6cb4-4ab4-a92b-fe91e24f31bf",
          "RegistrertSomArbeidssøker": {
            "Virkningsdato": "$ønsketDato",
            "InnsendtSøknadsId": {
              "urn": "urn:soknad:4afce924-6cb4-4ab4-a92b-fe91e24f31bf"
            },
            "søknad_uuid": "4afce924-6cb4-4ab4-a92b-fe91e24f31bf"
          },
          "Virkningsdato": "$ønsketDato",
          "InnsendtSøknadsId": {
            "urn": "urn:soknad:4afce924-6cb4-4ab4-a92b-fe91e24f31bf"
          },
          "@id": "0c60ca43-f54b-4a1b-9ab3-5646024a0815",
          "@opprettet": "2024-04-02T13:23:58.789361",
          "system_read_count": 0,
          "system_participating_services": [
            {
              "id": "0c60ca43-f54b-4a1b-9ab3-5646024a0815",
              "time": "2024-04-02T13:23:58.789361"
            }
          ]
        }
        """.trimIndent()
}
