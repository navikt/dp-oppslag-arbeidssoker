package no.nav.dagpenger.arbeidssoker.oppslag.arbeidssøkerregister

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName
import org.testcontainers.utility.MountableFile
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.LocalDate

class PawArbeidssøkerregisterTest {
    private val klient by lazy {
        PawArbeidssøkerregister(
            baseUrl = baseUrl,
            tokenProvider = { "token" },
        )
    }

    private val enkelPeriode by lazy { javaClass.getResource("/enkel.json")!!.readText() }
    private val feil by lazy { javaClass.getResource("/feil.json")!!.readText() }

    @Test
    fun `test henting av arbeidssøkerperioder`() {
        setExpectation(enkelPeriode)

        val perioder = runBlocking { klient.hentRegistreringsperiode("12345678910") }
        perioder shouldHaveSize 1
        perioder.first().apply {
            fom shouldBe LocalDate.of(2017, 7, 21)
            tom shouldBe LocalDate.of(2017, 7, 21)
        }
    }

    @Test
    fun `test henting av arbeidssøkerperioder ved feil`() {
        setExpectation(feil)

        runBlocking {
            val exception =
                shouldThrow<ServerResponseException> {
                    klient.hentRegistreringsperiode("01987654321")
                }

            exception.response.status shouldBe HttpStatusCode.InternalServerError
            exception.response.bodyAsText() shouldBe "Internal Server Error"
        }
    }

    @Test
    fun `avviser forventninger som bryter OpenAPI-kontrakten`() {
        setExpectation(
            enkelPeriode.replace("UKJENT_VERDI", "UGYLDIG_TYPE"),
            expectedStatus = 400,
        )
    }

    private fun setExpectation(
        expectation: String,
        expectedStatus: Int = 200,
    ) {
        val request =
            HttpRequest
                .newBuilder(URI("$baseUrl/_specmatic/expectations"))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(expectation))
                .build()
        val response = expectationClient.send(request, HttpResponse.BodyHandlers.ofString())

        withClue(response.body()) {
            response.statusCode() shouldBe expectedStatus
        }
    }

    companion object {
        private val stub =
            GenericContainer<Nothing>(DockerImageName.parse("specmatic/specmatic:2.55.3")).apply {
                withExposedPorts(9000)
                withCopyFileToContainer(
                    MountableFile.forClasspathResource("paw-openapi.yaml"),
                    "/contracts/paw.yaml",
                )
                withCommand("mock", "/contracts/paw.yaml", "--host", "0.0.0.0", "--port", "9000", "--strict")
                waitingFor(Wait.forHttp("/_specmatic/health").forStatusCode(200))
                withStartupTimeout(Duration.ofMinutes(2))
            }
        private val expectationClient =
            HttpClient
                .newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build()
        private val baseUrl get() = "http://${stub.host}:${stub.getMappedPort(9000)}"

        @JvmStatic
        @BeforeAll
        fun setup() {
            stub.start()
        }

        @JvmStatic
        @AfterAll
        fun tearDown() {
            expectationClient.close()
            stub.stop()
        }
    }
}
