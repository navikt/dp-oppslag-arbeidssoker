package no.nav.dagpenger.arbeidssoker.oppslag.adapter.soap.arena

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import no.nav.dagpenger.arbeidssoker.oppslag.adapter.OppfølgingsstatusClient
import no.nav.tjeneste.virksomhet.oppfoelgingsstatus.v2.binding.OppfoelgingsstatusV2
import no.nav.tjeneste.virksomhet.oppfoelgingsstatus.v2.informasjon.Person
import no.nav.tjeneste.virksomhet.oppfoelgingsstatus.v2.meldinger.HentOppfoelgingsstatusRequest

private val log = KotlinLogging.logger {}

class SoapArenaClient(
    private val oppfoelgingsstatusV2: OppfoelgingsstatusV2
) : OppfølgingsstatusClient {

    override suspend fun hentFormidlingsgruppeKode(fnr: String): String = withContext(Dispatchers.IO) {
        val request = HentOppfoelgingsstatusRequest().apply {
            bruker = Person().apply { this.ident = fnr }
        }
        val response = oppfoelgingsstatusV2.hentOppfoelgingsstatus(request)

        response.formidlingsgruppeKode.value
    }
}
