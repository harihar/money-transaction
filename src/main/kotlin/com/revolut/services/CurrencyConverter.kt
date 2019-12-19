package com.revolut.services

import org.javamoney.moneta.Money
import javax.money.Monetary
import javax.money.convert.MonetaryConversions

class CurrencyConverter {
    fun convert(money: Money): Money {
        if (money.currency != Monetary.getCurrency("EUR")) {
            val conversionEUR = MonetaryConversions.getConversion("EUR")
            return money.with(conversionEUR)
        }
        return money
    }
}