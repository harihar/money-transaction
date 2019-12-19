package com.revolut.model

import org.javamoney.moneta.Money

data class Account(val accountId: String, val balance: Money)