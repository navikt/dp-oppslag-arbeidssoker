package no.nav.dagpenger.arbeidssoker.oppslag

import no.nav.dagpenger.arbeidssoker.oppslag.adapter.OppfølgingsstatusClient

class Arbeidssøkeroppslag(private val oppfølgingsstatusClient: OppfølgingsstatusClient) {
    fun bestemReellArbeidssøker(fnr: String): ReellArbeidssøker {
        val formidlingsgruppeKode = oppfølgingsstatusClient.hentFormidlingsgruppeKode(fnr)
        val arbeidssøker = "ARBS"

        return ReellArbeidssøker(formidlingsgruppeKode == arbeidssøker)
    }
}