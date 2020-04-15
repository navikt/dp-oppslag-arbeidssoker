package no.nav.dagpenger.arbeidssoker.oppslag.adapter.soap

import no.nav.cxf.metrics.MetricFeature
import no.nav.tjeneste.virksomhet.oppfoelgingsstatus.v2.binding.OppfoelgingsstatusV2
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean
import org.apache.cxf.ws.addressing.WSAddressingFeature
import javax.xml.namespace.QName

object SoapPort {

    fun oppfoelgingsstatusV2(serviceUrl: String): OppfoelgingsstatusV2 {
        return createServicePort(
                serviceUrl = serviceUrl,
                serviceClazz = OppfoelgingsstatusV2::class.java,
                wsdl = "wsdl/no/nav/tjeneste/virksomhet/oppfoelgingsstatus/v2/Binding.wsdl",
                namespace = "http://nav.no/tjeneste/virksomhet/oppfoelgingsstatus/v2/Binding",
                svcName = "Oppfoelgingsstatus_v2",
                portName = "Oppfoelgingsstatus_v2Port"
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
