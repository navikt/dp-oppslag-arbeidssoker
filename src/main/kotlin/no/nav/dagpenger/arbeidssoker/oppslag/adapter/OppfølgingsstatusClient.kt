package no.nav.dagpenger.arbeidssoker.oppslag.adapter

interface OppfølgingsstatusClient {
    fun hentFormidlingsgruppeKode(fnr: String): String
}
