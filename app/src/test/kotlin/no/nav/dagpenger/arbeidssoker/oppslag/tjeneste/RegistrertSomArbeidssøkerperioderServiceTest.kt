package no.nav.dagpenger.arbeidssoker.oppslag.tjeneste

import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.dagpenger.arbeidssoker.oppslag.arbeidssøkerregister.Arbeidssøkerregister
import no.nav.dagpenger.arbeidssoker.oppslag.arbeidssøkerregister.Periode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class RegistrertSomArbeidssøkerperioderServiceTest {
    private val rapid = TestRapid()
    private val arbeidsøkerRegister: Arbeidssøkerregister = mockk()
    private val innhentFraOgMed = LocalDate.of(2020, 1, 1)

    init {
        RegistrertSomArbeidssøkerperioderService(rapid, arbeidsøkerRegister)
    }

    @Test
    fun `svarer på om person er registrert som arbeidssøker`() {
        val startDatoPeriode1 = innhentFraOgMed.minusDays(1)
        val sluttDatoPeriode1 = startDatoPeriode1.plusDays(5)
        val startDatoPeriode2 = startDatoPeriode1.plusDays(2)

        coEvery {
            arbeidsøkerRegister.hentRegistreringsperiode(any())
        } returns
            listOf(
                Periode(fom = innhentFraOgMed.minusDays(10), tom = innhentFraOgMed.minusDays(5)),
                Periode(fom = startDatoPeriode1, tom = sluttDatoPeriode1),
                Periode(fom = startDatoPeriode2, tom = LocalDate.MAX),
            )
        rapid.sendTestMessage(json)

        with(rapid.inspektør) {
            assertEquals(1, size)
            val løsning = field(0, "@løsning")
            val perioder = løsning["RegistrertSomArbeidssøker"]
            perioder.size() shouldBe 2
            perioder.map { it["verdi"].asBoolean() } shouldContainExactly listOf(true, true)
            perioder.map { it["gyldigFraOgMed"].asLocalDate() } shouldContainExactly
                listOf(
                    LocalDate.of(2019, 12, 31),
                    LocalDate.of(2020, 1, 2),
                )
            perioder.map { it["gyldigTilOgMed"].asLocalDate() } shouldContainExactly
                listOf(
                    LocalDate.of(2020, 1, 5),
                    LocalDate.MAX,
                )
        }
    }

    @Test
    fun `svarer på om person ikke er registrert som arbeidssøker`() {
        coEvery {
            arbeidsøkerRegister.hentRegistreringsperiode(any())
        } returns
            emptyList()
        rapid.sendTestMessage(json)

        with(rapid.inspektør) {
            assertEquals(1, size)
            val løsning = field(0, "@løsning")
            val perioder = løsning["RegistrertSomArbeidssøker"]
            perioder.map { it["verdi"].asBoolean() } shouldContainExactly listOf(false)
            perioder.map { it["gyldigFraOgMed"].asLocalDate() } shouldContainExactly listOf(innhentFraOgMed)
        }
    }

    @Test
    fun `skal ikke løse behov som allerede er løst`() {
        coEvery {
            arbeidsøkerRegister.hentRegistreringsperiode(any())
        } returns
            emptyList()
        rapid.sendTestMessage(jsonMedLøsing)

        with(rapid.inspektør) {
            assertEquals(0, size)
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
          "gjelderDato": "${LocalDate.now()}",
          "fagsakId": "123",
          "søknadId": "4afce924-6cb4-4ab4-a92b-fe91e24f31bf",
          "søknad_uuid": "4afce924-6cb4-4ab4-a92b-fe91e24f31bf",
          "RegistrertSomArbeidssøker": {
            "InnhentFraOgMed": "$innhentFraOgMed",
            "InnsendtSøknadsId": {
              "urn": "urn:soknad:4afce924-6cb4-4ab4-a92b-fe91e24f31bf"
            },
            "søknad_uuid": "4afce924-6cb4-4ab4-a92b-fe91e24f31bf"
          },
          "Prøvingsdato": "$innhentFraOgMed",
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

    // language=json
    private val jsonMedLøsing =
        """
        {
          "@event_name": "behov",
          "@behovId": "83894fc2-6e45-4534-abd1-97a441c57b2f",
          "@behov": [
            "RegistrertSomArbeidssøker"
          ],
          "ident": "11109233444",
          "behandlingId": "018e9e8d-35f3-7835-9569-5c59ec0737da",
          "gjelderDato": "${LocalDate.now()}",
          "fagsakId": "123",
          "søknadId": "4afce924-6cb4-4ab4-a92b-fe91e24f31bf",
          "søknad_uuid": "4afce924-6cb4-4ab4-a92b-fe91e24f31bf",
          "RegistrertSomArbeidssøker": {
            "InnhentFraOgMed": "$innhentFraOgMed",
            "InnsendtSøknadsId": {
              "urn": "urn:soknad:4afce924-6cb4-4ab4-a92b-fe91e24f31bf"
            },
            "søknad_uuid": "4afce924-6cb4-4ab4-a92b-fe91e24f31bf"
          },
          "@løsning": {
            "RegistrertSomArbeidssøker": {
              "verdi": true,
              "gyldigFraOgMed": "$innhentFraOgMed"
            }
          },
          "Prøvingsdato": "$innhentFraOgMed",
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
