package no.nav.dagpenger.arbeidssoker.oppslag

import io.opentelemetry.api.common.AttributeKey.booleanKey
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider
import io.opentelemetry.sdk.trace.ReadableSpan
import io.opentelemetry.sdk.trace.SpanProcessor
import mu.KotlinLogging

val logger = KotlinLogging.logger { }

class RiverSpanProcessor(
    private val delegate: SpanProcessor,
) : SpanProcessor by delegate {
    override fun onEnd(span: ReadableSpan) {
        val onPreconditionError: Boolean? =
            span.getAttribute(booleanKey("nav.rapid_and_rivers.message.onPreconditionError"))

        logger.debug { "onEnd: onPreconditionError=$onPreconditionError" }

        // Define the condition for a "relevant" span.
        if (onPreconditionError == true) {
            // If relevant, forward the span to the delegate for exporting.
            delegate.onEnd(span)
        } else {
            // If not relevant, the span is simply dropped (i.e., not exported).
        }
    }
}

class FilteringSpanProcessorCustomizer : AutoConfigurationCustomizerProvider {
    override fun customize(customizer: AutoConfigurationCustomizer) {
        logger.debug { "Legger til custom span processor" }
        customizer.addSpanProcessorCustomizer { spanProcessor, _ -> RiverSpanProcessor(spanProcessor) }
    }
}
