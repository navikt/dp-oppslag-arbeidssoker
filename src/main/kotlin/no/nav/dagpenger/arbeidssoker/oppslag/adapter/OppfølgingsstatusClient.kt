package no.nav.dagpenger.arbeidssoker.oppslag.adapter

interface OppfølgingsstatusClient {
    suspend fun hentFormidlingsgruppeKode(fnr: String): String
}
