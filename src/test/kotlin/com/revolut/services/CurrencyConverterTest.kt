package com.revolut.services

import org.assertj.core.api.Assertions.assertThat
import org.javamoney.moneta.Money
import org.junit.jupiter.api.Test
import javax.money.Monetary

class CurrencyConverterTest {
    private val currencyConverter = CurrencyConverter()

    @Test
    fun `convert - should convert from USD to EUR`() {
        // given
        val money = Money.of(1.0, "USD")

        // when
        val convertedMoney = currencyConverter.convert(money)

        // then
        assertThat(convertedMoney.currency).isEqualTo(Monetary.getCurrency("EUR"))
    }

    @Test
    fun `convert - should convert from INR to EUR`() {
        // given
        val money = Money.of(1.0, "INR")

        // when
        val convertedMoney = currencyConverter.convert(money)

        // then
        assertThat(convertedMoney.currency).isEqualTo(Monetary.getCurrency("EUR"))
    }

    @Test
    fun `convert - should return the same instance if currency is EUR`() {
        // given
        val money = Money.of(1.0, "EUR")

        // when
        val convertedMoney = currencyConverter.convert(money)

        // then
        assertThat(convertedMoney).isSameAs(money)
    }
}
