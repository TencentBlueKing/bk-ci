/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.auth.service.local

import com.mongodb.BasicDBObject
import com.tencent.bkrepo.auth.constant.RANDOM_KEY_LENGTH
import com.tencent.bkrepo.auth.message.AuthMessageCode
import com.tencent.bkrepo.auth.model.TAccount
import com.tencent.bkrepo.auth.pojo.account.Account
import com.tencent.bkrepo.auth.pojo.account.CreateAccountRequest
import com.tencent.bkrepo.auth.pojo.enums.CredentialStatus
import com.tencent.bkrepo.auth.pojo.token.CredentialSet
import com.tencent.bkrepo.auth.repository.AccountRepository
import com.tencent.bkrepo.auth.service.AccountService
import com.tencent.bkrepo.auth.util.IDUtil
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import org.apache.commons.lang.RandomStringUtils
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import java.time.LocalDateTime

class AccountServiceImpl constructor(
    private val accountRepository: AccountRepository,
    private val mongoTemplate: MongoTemplate
) : AccountService {

    override fun createAccount(request: CreateAccountRequest): Account? {
        logger.info("create  account  request : [$request]")
        val account = accountRepository.findOneByAppId(request.appId)
        account?.let {
            logger.warn("create account [${request.appId}]  is exist.")
            throw ErrorCodeException(AuthMessageCode.AUTH_DUP_APPID)
        }

        val accessKey = IDUtil.genRandomId()
        val secretKey = RandomStringUtils.randomAlphanumeric(RANDOM_KEY_LENGTH)
        val credentials = CredentialSet(
            accessKey = accessKey,
            secretKey = secretKey,
            createdAt = LocalDateTime.now(),
            status = CredentialStatus.ENABLE
        )
        accountRepository.insert(
            TAccount(
                appId = request.appId,
                locked = request.locked,
                credentials = listOf(credentials)
            )
        )
        val result = accountRepository.findOneByAppId(request.appId) ?: return null
        return transfer(result)
    }

    override fun listAccount(): List<Account> {
        logger.debug("list  account ")
        return accountRepository.findAllBy().map { transfer(it) }
    }

    override fun deleteAccount(appId: String): Boolean {
        logger.info("delete  account appId : {}", appId)
        val result = accountRepository.deleteByAppId(appId)
        if (result == 0L) {
            logger.warn("delete account [$appId]  not exist.")
            throw ErrorCodeException(AuthMessageCode.AUTH_APPID_NOT_EXIST)
        }
        return true
    }

    override fun updateAccountStatus(appId: String, locked: Boolean): Boolean {
        logger.info("update  account appId : [$appId], locked : [$locked]")
        accountRepository.findOneByAppId(appId) ?: run {
            logger.warn("update account [$appId]  not exist.")
            throw ErrorCodeException(AuthMessageCode.AUTH_APPID_NOT_EXIST)
        }

        val query = Query()
        query.addCriteria(Criteria.where(TAccount::appId.name).`is`(appId))
        val update = Update()
        update.set("locked", locked)
        val result = mongoTemplate.updateFirst(query, update, TAccount::class.java)
        if (result.modifiedCount == 1L) return true
        return false
    }

    override fun createCredential(appId: String): List<CredentialSet> {
        logger.info("create  credential appId : {} ", appId)
        accountRepository.findOneByAppId(appId) ?: run {
            logger.warn("account [$appId]  not exist.")
            throw ErrorCodeException(AuthMessageCode.AUTH_APPID_NOT_EXIST)
        }

        val query = Query.query(Criteria.where(TAccount::appId.name).`is`(appId))
        val update = Update()
        val accessKey = IDUtil.genRandomId()
        val secretKey = RandomStringUtils.randomAlphanumeric(RANDOM_KEY_LENGTH)
        val credentials = CredentialSet(
            accessKey = accessKey,
            secretKey = secretKey,
            createdAt = LocalDateTime.now(),
            status = CredentialStatus.ENABLE
        )
        update.addToSet("credentials", credentials)
        mongoTemplate.upsert(query, update, TAccount::class.java)
        val result = accountRepository.findOneByAppId(appId) ?: return emptyList()
        return result.credentials
    }

    override fun listCredentials(appId: String): List<CredentialSet> {
        logger.debug("list  credential appId : {} ", appId)
        val account = accountRepository.findOneByAppId(appId) ?: run {
            logger.warn("update account [$appId]  not exist.")
            throw ErrorCodeException(AuthMessageCode.AUTH_APPID_NOT_EXIST)
        }
        return account.credentials
    }

    override fun deleteCredential(appId: String, accessKey: String): List<CredentialSet> {
        logger.info("delete  credential appId : [$appId] , accessKey: [$accessKey]")
        accountRepository.findOneByAppId(appId) ?: run {
            logger.warn("appId [$appId]  not exist.")
            throw ErrorCodeException(AuthMessageCode.AUTH_USER_NOT_EXIST)
        }
        val query = Query.query(Criteria.where(TAccount::appId.name).`is`(appId))
        val s = BasicDBObject()
        s["accessKey"] = accessKey
        val update = Update()
        update.pull("credentials", s)
        mongoTemplate.updateFirst(query, update, TAccount::class.java)
        val result = accountRepository.findOneByAppId(appId) ?: return emptyList()
        return result.credentials
    }

    override fun updateCredentialStatus(appId: String, accessKey: String, status: CredentialStatus): Boolean {
        logger.info("update  credential status appId : [$appId] , accessKey: [$accessKey],status :[$status]")
        accountRepository.findOneByAppId(appId) ?: run {
            logger.warn("update account status  [$appId]  not exist.")
            throw ErrorCodeException(AuthMessageCode.AUTH_APPID_NOT_EXIST)
        }
        val accountQuery = Query.query(
            Criteria.where(TAccount::appId.name).`is`(appId)
                .and("credentials.accessKey").`is`(accessKey)
        )
        val accountResult = mongoTemplate.findOne(accountQuery, TAccount::class.java)
        accountResult?.let {
            val query = Query.query(
                Criteria.where(TAccount::appId.name).`is`(appId)
                    .and("credentials.accessKey").`is`(accessKey)
            )
            val update = Update()
            update.set("credentials.$.status", status.toString())
            val result = mongoTemplate.updateFirst(query, update, TAccount::class.java)
            if (result.modifiedCount == 1L) return true
        }
        return false
    }

    override fun checkCredential(accessKey: String, secretKey: String): String? {
        logger.debug("check  credential  accessKey : [$accessKey] , secretKey: []")
        val query = Query.query(
            Criteria.where("credentials.secretKey").`is`(secretKey)
                .and("credentials.accessKey").`is`(accessKey)
        )
        val result = mongoTemplate.findOne(query, TAccount::class.java) ?: return null
        return result.appId
    }

    private fun transfer(tAccount: TAccount): Account {
        return Account(
            appId = tAccount.appId,
            locked = tAccount.locked,
            credentials = tAccount.credentials
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AccountServiceImpl::class.java)
    }
}
