package com.revolut.controller

import com.revolut.configuration.supportedCurrencies
import com.revolut.exceptions.*
import com.revolut.model.ErrorResponse
import com.revolut.model.TransactionRequest
import com.revolut.model.TransactionResponse
import com.revolut.services.TransactionService
import com.revolut.utils.GsonUtil
import spark.Response
import spark.Spark.exception
import spark.Spark.post
import javax.inject.Inject

class TransactionController {

    @Inject
    lateinit var transactionService: TransactionService

    init {
        // route
        post("/transaction") { req, res ->
            val transactionRequest = GsonUtil.gson.fromJson(req.body(), TransactionRequest::class.java)

            validateRequest(transactionRequest)

            val transactionId = transactionService.execute(transactionRequest)
            res.run {
                status(201)
                type("application/json")
                GsonUtil.gson.toJson(TransactionResponse(transactionId))
            }
        }

        // exception handlers
        exception(InvalidAmountException::class.java) { _, _, res ->
            returnBadRequest(res, "only nonzero positive amount is valid")
        }
        exception(UnsupportedCurrencyException::class.java) { _, _, res ->
            returnBadRequest(res, "unsupported currency code")
        }
        exception(MissingRequiredFieldException::class.java) { ex, _, res ->
            returnBadRequest(res, "missing required property '${ex.fieldName}'")
        }
        exception(InvalidAccountException::class.java) { ex, _, res ->
            returnBadRequest(res, ex.message ?: "")
        }
        exception(InsufficientBalanceException::class.java) { ex, _, res ->
            returnBadRequest(res, ex.message ?: "")
        }
    }

    private fun validateRequest(transactionRequest: TransactionRequest) {
        if (transactionRequest.fromAccountId == null) {
            throw MissingRequiredFieldException("fromAccountId")
        }
        if (transactionRequest.toAccountId == null) {
            throw MissingRequiredFieldException("toAccountId")
        }
        if (transactionRequest.amount == null) {
            throw MissingRequiredFieldException("amount")
        }
        if (transactionRequest.amount.isNegativeOrZero) {
            throw InvalidAmountException()
        }
        if (!supportedCurrencies.contains(transactionRequest.amount.currency.currencyCode)) {
            throw UnsupportedCurrencyException()
        }
    }

    private fun returnBadRequest(res: Response, errorMessage: String) {
        res.run {
            status(400)
            res.type("application/json")
            body(
                GsonUtil.gson.toJson(
                    ErrorResponse("bad_request", errorMessage)
                )
            )
        }
    }
}

