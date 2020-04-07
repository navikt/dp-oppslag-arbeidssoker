package no.nav.dagpenger.arbeidssoker.oppslag

import io.kotlintest.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class BestemReellArbeidssøkerTest {

    @Test
    fun `Skjekk at ordinær arbeidssøker er en reell arbeidssøker`() {
        val veilarbregistreringClient: VeilarbregistreringClient = mockk()

        val fnr = "123456"
        val registreringsdato = LocalDateTime.of(2020, 4, 1, 0, 0, 0)

        val arbeidssøker = Arbeidssøker(ArbeidssøkerType.ORDINAER, registreringsdato)

        every { veilarbregistreringClient.hentArbeidssøker(fnr) }.returns(arbeidssøker)

        val arbeidssøkeroppslag = Arbeidssøkeroppslag(veilarbregistreringClient)

        val reellArbeidssøker = arbeidssøkeroppslag.bestemReellArbeidssøker(fnr)

        reellArbeidssøker.erReellArbeidssøker shouldBe true
        reellArbeidssøker.registreringsdato shouldBe registreringsdato
    }

    @Test
    fun `Skjekk at sykmeldt arbeidssøker ikke er en reell arbeidssøker`() {
        val veilarbregistreringClient: VeilarbregistreringClient = mockk()

        val fnr = "123456"
        val registreringsdato = LocalDateTime.of(2020, 4, 1, 0, 0, 0)

        val arbeidssøker = Arbeidssøker(ArbeidssøkerType.SYKMELDT, registreringsdato)

        every { veilarbregistreringClient.hentArbeidssøker(fnr) }.returns(arbeidssøker)

        val arbeidssøkeroppslag = Arbeidssøkeroppslag(veilarbregistreringClient)

        val reellArbeidssøker = arbeidssøkeroppslag.bestemReellArbeidssøker(fnr)

        reellArbeidssøker.erReellArbeidssøker shouldBe false
        reellArbeidssøker.registreringsdato shouldBe null
    }

    @Test
    fun `Skjekk at ikke-registret arbeidssøker ikke er en reell arbeidssøker`() {
        val veilarbregistreringClient: VeilarbregistreringClient = mockk()

        val fnr = "123456"

        every { veilarbregistreringClient.hentArbeidssøker(fnr) }.returns(null)

        val arbeidssøkeroppslag = Arbeidssøkeroppslag(veilarbregistreringClient)

        val reellArbeidssøker = arbeidssøkeroppslag.bestemReellArbeidssøker(fnr)

        reellArbeidssøker.erReellArbeidssøker shouldBe false
        reellArbeidssøker.registreringsdato shouldBe null
    }
}