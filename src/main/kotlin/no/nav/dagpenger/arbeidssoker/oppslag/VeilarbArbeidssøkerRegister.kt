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
import io.ktor.http.URLBuilder
import io.ktor.util.KtorExperimentalAPI
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Decoder
import kotlinx.serialization.Encoder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.PrimitiveDescriptor
import kotlinx.serialization.PrimitiveKind
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import mu.KotlinLogging
import no.nav.dagpenger.ktor.client.auth.providers.bearer

private val ulid = ULID()
private val log = KotlinLogging.logger {}
private val sikkerlogg = KotlinLogging.logger("tjenestekall")

@KtorExperimentalAPI
internal class VeilarbArbeidssøkerRegister(
    baseUrl: String? = null,
    tokenProvider: () -> String,
    httpClientEngine: HttpClientEngine = CIO.create {}
) : ArbeidssøkerRegister {
    private val client: HttpClient = HttpClient(httpClientEngine) {
        install(JsonFeature) {
            serializer = KotlinxSerializer(Json(JsonConfiguration.Stable.copy(ignoreUnknownKeys = true)))
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

        defaultRequest {
            url {
                baseUrl?.let {
                    URLBuilder(it).also {
                        this.host = it.host
                        this.port = it.port
                    }
                }
            }

            header("Nav-Consumer-Id", "dp-oppslag-arbeidssoker")
            header("Nav-Call-Id", ulid.nextValue())
        }
    }

    override fun hentRegistreringsperiode(
        fnr: String,
        fom: LocalDate,
        tom: LocalDate
    ): List<Periode> = runBlocking {
        log.info { "Henter arbeidssøkerperioder for fra og med $fom til og med $tom" }

        client.get<List<Arbeidssøkerperiode>>("/arbeidssoker/perioder") {
            parameter("fnr", fnr)
            parameter("fraOgMed", fom)
            parameter("tilOgMed", tom)
        }.map {
            Periode(
                fom = it.fom,
                tom = it.tom,
                formidlingsgruppe = Formidlingsgruppe.valueOf(it.status.toString())
            )
        }.also {
            log.info { "Fant ${it.size} arbeidssøkerperioder" }
        }
    }
}

@Serializable
internal data class Arbeidssøkerperiode(
    @Serializable(with = LocalDateSerializer::class)
    val fom: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val tom: LocalDate,
    val status: Status
) {
    enum class Status {
        ARBS
    }
}

@Serializer(forClass = LocalDate::class)
object LocalDateSerializer : KSerializer<LocalDate> {
    override val descriptor = PrimitiveDescriptor("java.time.LocalDate", PrimitiveKind.STRING)
    override fun deserialize(decoder: Decoder): LocalDate = LocalDate.parse(decoder.decodeString())
    override fun serialize(encoder: Encoder, value: LocalDate) = encoder.encodeString(value.toString())
}
