package no.nav.dagpenger.arbeidssoker.oppslag.adapter.soap

import no.nav.cxf.metrics.MetricFeature
import no.nav.tjeneste.virksomhet.oppfoelgingsstatus.v2.binding.OppfoelgingsstatusV2
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean
import org.apache.cxf.ws.addressing.WSAddressingFeature
import javax.xml.namespace.QName

object SoapPort {

    fun ytelseskontraktV3(serviceUrl: String): OppfoelgingsstatusV2 {
        return createServicePort(
                serviceUrl = serviceUrl,
                serviceClazz = OppfoelgingsstatusV2::class.java,
                wsdl = "wsdl/tjenestespesifikasjon/no/nav/tjeneste/virksomhet/ytelseskontrakt/v3/Binding.wsdl",
                namespace = "http://nav.no/tjeneste/virksomhet/ytelseskontrakt/v3/Binding",
                svcName = "Ytelseskontrakt_v3",
                portName = "Ytelseskontrakt_v3Port"
        )
    }

    private fun <PORT_TYPE> createServicePort(
        serviceUrl: String,
        serviceClazz: Class<PORT_TYPE>,
        wsdl: String,
        namespace: String,
        svcName: String,
        portName: String
    ): PORT_TYPE {
        val factory = JaxWsProxyFactoryBean().apply {
            address = serviceUrl
            wsdlURL = wsdl
            serviceName = QName(namespace, svcName)
            endpointName = QName(namespace, portName)
            serviceClass = serviceClazz
            features = listOf(WSAddressingFeature(), MetricFeature())
            outInterceptors.add(CallIdInterceptor())
        }

        return factory.create(serviceClazz)
    }
}
