package com.tencent.devops.auth.service.oauth2

import com.tencent.devops.auth.constant.AuthMessageCode
import com.tencent.devops.auth.dao.AuthOauth2CodeDao
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.auth.utils.AuthUtils
import com.tencent.devops.model.auth.tables.records.TAuthOauth2CodeRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Service

@Service
class Oauth2CodeService constructor(
    private val authOauth2CodeDao: AuthOauth2CodeDao,
    private val dslContext: DSLContext
) {
    fun get(
        code: String?
    ): TAuthOauth2CodeRecord {
        if (code == null) {
            throw ErrorCodeException(
                errorCode = AuthMessageCode.INVALID_AUTHORIZATION_CODE,
                defaultMessage = "The authorization code must be provided"
            )
        }
        return authOauth2CodeDao.get(
            dslContext = dslContext,
            code = code
        ) ?: throw ErrorCodeException(
            errorCode = AuthMessageCode.INVALID_AUTHORIZATION_CODE,
            defaultMessage = "The authorization code invalid"
        )
    }

    fun consume(code: String) {
        authOauth2CodeDao.delete(
            dslContext = dslContext,
            code = code
        )
    }

    fun create(
        userId: String,
        code: String,
        clientId: String,
        scopeId: Int,
        codeValiditySeconds: Long
    ) {
        authOauth2CodeDao.create(
            dslContext = dslContext,
            code = code,
            userId = userId,
            clientId = clientId,
            scopeId = scopeId,
            expiredTime = DateTimeUtil.getFutureTimestamp(codeValiditySeconds)
        )
    }

    fun verifyCode(
        clientId: String,
        codeDetails: TAuthOauth2CodeRecord
    ): Boolean {
        if (codeDetails.clientId != clientId) {
            throw ErrorCodeException(
                errorCode = AuthMessageCode.INVALID_AUTHORIZATION_CODE,
                defaultMessage = "The authorization code does not belong to the client($clientId)"
            )
        }
        if (AuthUtils.isExpired(codeDetails.expiredTime)) {
            throw ErrorCodeException(
                errorCode = AuthMessageCode.INVALID_AUTHORIZATION_EXPIRED,
                defaultMessage = "The authorization code expired"
            )
        }
        return true
    }
}
