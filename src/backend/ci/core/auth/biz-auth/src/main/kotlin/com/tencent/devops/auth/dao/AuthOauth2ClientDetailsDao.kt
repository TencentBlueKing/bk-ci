package com.tencent.devops.auth.dao

import com.tencent.devops.auth.pojo.dto.ClientDetailsDTO
import com.tencent.devops.model.auth.tables.TAuthOauth2ClientDetails
import com.tencent.devops.model.auth.tables.records.TAuthOauth2ClientDetailsRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class AuthOauth2ClientDetailsDao {
    fun get(
        dslContext: DSLContext,
        clientId: String
    ): TAuthOauth2ClientDetailsRecord? {
        return with(TAuthOauth2ClientDetails.T_AUTH_OAUTH2_CLIENT_DETAILS) {
            dslContext.selectFrom(this).where(
                CLIENT_ID.eq(clientId)
            )
        }.fetchOne()
    }

    fun create(
        dslContext: DSLContext,
        clientDetailsDTO: ClientDetailsDTO
    ) {
        with(TAuthOauth2ClientDetails.T_AUTH_OAUTH2_CLIENT_DETAILS) {
            dslContext.insertInto(
                this,
                CLIENT_ID,
                CLIENT_SECRET,
                CLIENT_NAME,
                SCOPE,
                ICON,
                AUTHORIZED_GRANT_TYPES,
                WEB_SERVER_REDIRECT_URI,
                ACCESS_TOKEN_VALIDITY,
                REFRESH_TOKEN_VALIDITY,
                CREATE_USER
            ).values(
                clientDetailsDTO.clientId,
                clientDetailsDTO.clientSecret,
                clientDetailsDTO.clientName,
                clientDetailsDTO.scope,
                clientDetailsDTO.icon,
                clientDetailsDTO.authorizedGrantTypes.map { it.grantType }.joinToString { "," },
                clientDetailsDTO.webServerRedirectUri,
                clientDetailsDTO.accessTokenValidity,
                clientDetailsDTO.refreshTokenValidity,
                clientDetailsDTO.createUser
            ).execute()
        }
    }

    fun delete(
        dslContext: DSLContext,
        clientId: String
    ) {
        with(TAuthOauth2ClientDetails.T_AUTH_OAUTH2_CLIENT_DETAILS) {
            dslContext.deleteFrom(this).where(CLIENT_ID.eq(clientId)).execute()
        }
    }
}
