package no.nav.dagpenger.arbeidssoker.oppslag

import de.huxhorn.sulky.ulid.ULID
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.auth.Auth
import io.ktor.client.features.defaultRequest
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.features.logging.LogLevel
import io.ktor.client.features.logging.Logger
import io.ktor.client.features.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import no.nav.dagpenger.ktor.client.auth.providers.bearer
import no.nav.dagpenger.ktor.client.metrics.PrometheusMetrics
import org.slf4j.MDC
import java.time.LocalDate

private val ulid = ULID()
private val log = KotlinLogging.logger {}
private val sikkerlogg = KotlinLogging.logger("tjenestekall")

@KtorExperimentalAPI
internal class VeilarbArbeidssøkerRegister(
    val baseUrl: String? = null,
    tokenProvider: () -> String,
    httpClientEngine: HttpClientEngine = CIO.create {}
) : ArbeidssøkerRegister {
    private val client: HttpClient = HttpClient(httpClientEngine) {
        install(JsonFeature) {
            // serializer = KotlinxSerializer(Json(JsonConfiguration.Stable.copy(ignoreUnknownKeys = true)))
            serializer = KotlinxSerializer(
                Json {
                    ignoreUnknownKeys = true
                },
            )
        }

        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    sikkerlogg.info { message }
                }
            }

            level = LogLevel.ALL
        }

        install(Auth) {
            bearer {
                this.tokenProvider = tokenProvider
                sendWithoutRequest = true
            }
        }

        install(PrometheusMetrics) {
            baseName = "ktor_client_veilarbregistrering"
        }

        defaultRequest {
            header("Nav-Consumer-Id", "dp-oppslag-arbeidssoker")
            header("Nav-Call-Id", runCatching { MDC.get(mdcSøknadIdKey) }.getOrElse { ulid.nextULID() })
        }
    }

    override suspend fun hentRegistreringsperiode(
        fnr: String,
        fom: LocalDate,
        tom: LocalDate
    ): List<Periode> = withContext(Dispatchers.IO) {
        log.info { "Henter arbeidssøkerperioder for fra og med $fom til og med $tom" }

        client.get<Arbeidssokerperioder>("$baseUrl/arbeidssoker/perioder") {
            parameter("fnr", fnr)
            parameter("fraOgMed", fom)
            parameter("tilOgMed", tom)
        }.let {
            it.arbeidssokerperioder.map { responsePeriode ->
                Periode(
                    fom = responsePeriode.fraOgMedDato,
                    tom = responsePeriode.tilOgMedDato
                )
            }
        }.also {
            log.info { "Fant ${it.size} arbeidssøkerperioder" }
        }
    }
}

@Serializable
internal data class Arbeidssokerperioder(val arbeidssokerperioder: List<ResponsePeriode>)

@Serializable
internal data class ResponsePeriode(
    @Serializable(with = LocalDateSerializer::class)
    val fraOgMedDato: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val tilOgMedDato: LocalDate?
)

@Serializer(forClass = LocalDate::class)
object LocalDateSerializer : KSerializer<LocalDate> {
    override val descriptor = PrimitiveSerialDescriptor("java.time.LocalDateTime", PrimitiveKind.STRING)
    override fun deserialize(decoder: Decoder): LocalDate = LocalDate.parse(decoder.decodeString())
    override fun serialize(encoder: Encoder, value: LocalDate) = encoder.encodeString(value.toString())
}
