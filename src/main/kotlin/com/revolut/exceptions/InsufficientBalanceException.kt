package com.revolut.exceptions

import java.lang.RuntimeException

class InsufficientBalanceException(message: String) : RuntimeException(message)
