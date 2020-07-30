package no.nav.dagpenger.arbeidssoker.oppslag

import no.nav.dagpenger.arbeidssoker.oppslag.adapter.OppfølgingsstatusClient
import java.time.LocalDate

class Arbeidssøkeroppslag(private val oppfølgingsstatusClient: OppfølgingsstatusClient) {
    suspend fun bestemRegistrertArbeidssøker(fnr: String): RegistrertArbeidssøker {
        val formidlingsgruppeKode = try {
            oppfølgingsstatusClient.hentFormidlingsgruppeKode(fnr)
        } catch (e: Exception) {
            // TODO: Remove this when we have a good way of mocking this data
            "ARBS"
        }

        return RegistrertArbeidssøker(
            erRegistrert = formidlingsgruppeKode == arbeidssøker,
            formidlingsgruppe = formidlingsgruppeKode,
            registreringsdato = LocalDate.now()
        )
    }

    companion object {
        private const val arbeidssøker = "ARBS"
    }
}
