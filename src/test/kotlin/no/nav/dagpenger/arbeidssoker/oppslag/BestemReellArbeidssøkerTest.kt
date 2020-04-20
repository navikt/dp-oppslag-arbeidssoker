package no.nav.dagpenger.arbeidssoker.oppslag

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.dagpenger.arbeidssoker.oppslag.adapter.OppfølgingsstatusClient
import org.junit.jupiter.api.Test

class BestemReellArbeidssøkerTest {

    val ARBEIDSSØKER = "ARBS"
    val IKKE_ARBEIDSSØKER = "IARBS"

    @Test
    fun `Hvis arbs, så reellarbeidssøker`() {
        val oppfølgingsstatusClient: OppfølgingsstatusClient = mockk()
        val arbeidssøkeroppslag = Arbeidssøkeroppslag(oppfølgingsstatusClient)
        val fnr = "12345"

        every { oppfølgingsstatusClient.hentFormidlingsgruppeKode(fnr) } returns ARBEIDSSØKER

        arbeidssøkeroppslag.bestemReellArbeidssøker(fnr).erReellArbeidssøker shouldBe true
    }

    @Test
    fun `Hvis ikke arbs, så ikke reellarbeidssøker`() {
        val oppfølgingsstatusClient: OppfølgingsstatusClient = mockk()
        val arbeidssøkeroppslag = Arbeidssøkeroppslag(oppfølgingsstatusClient)
        val fnr = "12345"

        every { oppfølgingsstatusClient.hentFormidlingsgruppeKode(fnr) } returns IKKE_ARBEIDSSØKER

        arbeidssøkeroppslag.bestemReellArbeidssøker(fnr).erReellArbeidssøker shouldBe false
    }
}