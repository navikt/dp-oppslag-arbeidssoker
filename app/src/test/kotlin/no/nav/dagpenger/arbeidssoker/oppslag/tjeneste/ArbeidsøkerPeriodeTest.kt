package no.nav.dagpenger.arbeidssoker.oppslag.tjeneste

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.arbeidssoker.oppslag.arbeidssøkerregister.Periode
import no.nav.dagpenger.arbeidssoker.oppslag.tjeneste.ArbeidsøkerPeriode.Companion.slåSammen
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ArbeidsøkerPeriodeTest {
    @Test
    fun `Finner arbeidssøkerperioder `() {
        val datoViSpørOm = LocalDate.of(2023, 3, 1)
        val utgangspunkt = Periode(datoViSpørOm, LocalDate.MAX)

        val arbeidsøkerPerioder =
            listOf(
                Periode(LocalDate.parse("2023-01-01"), LocalDate.parse("2023-02-02")),
                Periode(LocalDate.parse("2023-04-01"), LocalDate.parse("2023-05-04")),
                Periode(LocalDate.parse("2024-10-01"), LocalDate.MAX),
            )

        val resultat = arbeidsøkerPerioder.slåSammen(utgangspunkt)

        resultat.shouldHaveSize(4)
        resultat[0].fom shouldBe LocalDate.parse("2023-03-01")
        resultat[0].tom shouldBe LocalDate.parse("2023-03-31")
        resultat[0].registert shouldBe false

        resultat[1].fom shouldBe LocalDate.parse("2023-04-01")
        resultat[1].tom shouldBe LocalDate.parse("2023-05-04")
        resultat[1].registert shouldBe true

        resultat[2].fom shouldBe LocalDate.parse("2023-05-05")
        resultat[2].tom shouldBe LocalDate.parse("2024-09-30")
        resultat[2].registert shouldBe false

        resultat[3].fom shouldBe LocalDate.parse("2024-10-01")
        resultat[3].tom shouldBe LocalDate.MAX
        resultat[3].registert shouldBe true
    }
}
