package com.revolut.model

import org.javamoney.moneta.Money

data class TransactionRequest(
    val fromAccountId: String,
    val toAccountId: String,
    val amount: Money
)
