package no.nav.dagpenger.arbeidssoker.oppslag

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.arbeidssoker.oppslag.adapter.OppfølgingsstatusClient
import org.junit.jupiter.api.Test

class BestemReellArbeidssøkerTest {
    private val ARBEIDSSØKER = "ARBS"
    private val IKKE_ARBEIDSSØKER = "IARBS"

    @Test
    fun `Hvis arbs, så reellarbeidssøker`() = runBlocking {
        val oppfølgingsstatusClient: OppfølgingsstatusClient = mockk()
        val arbeidssøkeroppslag = Arbeidssøkeroppslag(oppfølgingsstatusClient)
        val fnr = "12345"

        coEvery { oppfølgingsstatusClient.hentFormidlingsgruppeKode(fnr) } returns ARBEIDSSØKER

        arbeidssøkeroppslag.bestemRegistrertArbeidssøker(fnr).erRegistrert shouldBe true
    }

    @Test
    fun `Hvis ikke arbs, så ikke reellarbeidssøker`() = runBlocking {
        val oppfølgingsstatusClient: OppfølgingsstatusClient = mockk()
        val arbeidssøkeroppslag = Arbeidssøkeroppslag(oppfølgingsstatusClient)
        val fnr = "12345"

        coEvery { oppfølgingsstatusClient.hentFormidlingsgruppeKode(fnr) } returns IKKE_ARBEIDSSØKER

        arbeidssøkeroppslag.bestemRegistrertArbeidssøker(fnr).erRegistrert shouldBe false
    }
}
