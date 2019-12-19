package com.revolut.exceptions

import java.lang.RuntimeException

class InvalidAccountException(message: String) : RuntimeException(message)