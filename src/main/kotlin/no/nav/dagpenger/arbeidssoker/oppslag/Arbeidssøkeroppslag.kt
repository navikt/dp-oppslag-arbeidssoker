package no.nav.dagpenger.arbeidssoker.oppslag

import no.nav.dagpenger.arbeidssoker.oppslag.adapter.OppfølgingsstatusClient
import java.time.LocalDate

class Arbeidssøkeroppslag(private val oppfølgingsstatusClient: OppfølgingsstatusClient) {
    fun bestemRegistrertArbeidssøker(fnr: String): RegistrertArbeidssøker {
        val formidlingsgruppeKode = oppfølgingsstatusClient.hentFormidlingsgruppeKode(fnr)
        val arbeidssøker = "ARBS"

        return RegistrertArbeidssøker(
            erRegistrert = formidlingsgruppeKode == arbeidssøker,
            formidlingsgruppe = formidlingsgruppeKode,
            registreringsdato = LocalDate.now()
        )
    }
}
