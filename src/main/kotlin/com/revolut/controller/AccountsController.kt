package com.revolut.controller

import com.revolut.model.AccountsResponse
import com.revolut.services.AccountService
import com.revolut.utils.GsonUtil
import spark.Spark.get
import javax.inject.Inject

class AccountsController {

    @Inject
    lateinit var accountService: AccountService

    init {
        get("/accounts") { _, res ->
            res.type("application/json")
            GsonUtil.gson.toJson(AccountsResponse(accountService.getAccounts()))
        }

        get("/accounts/:accountId") { req, res ->
            val accountId = req.params("accountId")
            val account = accountService.getAccount(accountId)
            if (account == null) {
                res.status(404)
            } else {
                res.type("application/json")
                GsonUtil.gson.toJson(account)
            }
        }
    }
}

