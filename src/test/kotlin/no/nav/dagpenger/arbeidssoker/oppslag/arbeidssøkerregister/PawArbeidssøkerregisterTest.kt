package no.nav.dagpenger.arbeidssoker.oppslag.arbeidssøkerregister

import `in`.specmatic.stub.ContractStub
import `in`.specmatic.stub.createStub
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.LocalDate

class PawArbeidssøkerregisterTest {
    private val klient =
        PawArbeidssøkerregister(
            baseUrl = "http://0.0.0.0:9000",
            tokenProvider = { "token" },
        )

    private val enkelPeriode by lazy { javaClass.getResource("/enkel.json")!!.readText() }

    @Test
    @Disabled
    fun `test something`() {
        stub.setExpectation(enkelPeriode)

        val perioder = runBlocking { klient.hentRegistreringsperiode("12345678910", LocalDate.now(), LocalDate.now()) }
        perioder shouldHaveSize 1
        perioder.first().apply {
            fom shouldBe LocalDate.of(2017, 7, 21)
            tom shouldBe LocalDate.of(2017, 7, 21)
        }
    }

    companion object {
        private lateinit var stub: ContractStub

        @JvmStatic
        @BeforeAll
        fun setup() {
            stub = createStub("0.0.0.0", 9000)
        }

        @JvmStatic
        @AfterAll
        fun tearDown() {
            stub.close()
        }
    }
}
