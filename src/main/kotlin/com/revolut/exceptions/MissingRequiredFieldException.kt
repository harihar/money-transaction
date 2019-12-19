package com.revolut.exceptions

import java.lang.RuntimeException

class MissingRequiredFieldException(val fieldName: String) : RuntimeException()
