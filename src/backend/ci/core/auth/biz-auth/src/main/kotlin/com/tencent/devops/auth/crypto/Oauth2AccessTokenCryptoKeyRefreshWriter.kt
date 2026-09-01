package com.tencent.devops.auth.crypto

import com.tencent.devops.common.security.crypto.CryptoKeyRefreshRow
import com.tencent.devops.common.security.crypto.CryptoKeyRefreshWriter
import com.tencent.devops.model.auth.tables.TAuthOauth2AccessToken
import org.jooq.DSLContext
import org.jooq.Record
import org.springframework.stereotype.Service

@Service
class Oauth2AccessTokenCryptoKeyRefreshWriter(
    private val dslContext: DSLContext,
    private val oauth2AccessTokenCryptoHelper: Oauth2AccessTokenCryptoHelper
) : CryptoKeyRefreshWriter {
    override val name = "oauth2-access-token"

    private val currentKeySha = oauth2AccessTokenCryptoHelper.currentKeySha()

    override fun fetchBatch(limit: Int): List<CryptoKeyRefreshRow> {
        return with(TAuthOauth2AccessToken.T_AUTH_OAUTH2_ACCESS_TOKEN) {
            dslContext.select(ACCESS_TOKEN, PASS_WORD, AES_KEY_SHA)
                .from(this)
                .where(PASS_WORD.isNotNull)
                .and(AES_KEY_SHA.isNull.or(AES_KEY_SHA.ne(currentKeySha)))
                .limit(limit)
                .fetch()
                .map(::toRow)
        }
    }

    override fun updateRow(row: CryptoKeyRefreshRow) {
        val tokenRow = row as Oauth2AccessTokenCryptoKeyRefreshRow
        with(TAuthOauth2AccessToken.T_AUTH_OAUTH2_ACCESS_TOKEN) {
            dslContext.update(this)
                .set(
                    PASS_WORD,
                    tokenRow.passWord?.let(oauth2AccessTokenCryptoHelper::refreshSm4OrAes)
                )
                .set(AES_KEY_SHA, currentKeySha)
                .where(ACCESS_TOKEN.eq(tokenRow.accessToken))
                .execute()
        }
    }

    override fun fetchMissingKeyShaBatch(limit: Int): List<CryptoKeyRefreshRow> {
        return with(TAuthOauth2AccessToken.T_AUTH_OAUTH2_ACCESS_TOKEN) {
            dslContext.select(ACCESS_TOKEN, PASS_WORD, AES_KEY_SHA)
                .from(this)
                .where(PASS_WORD.isNotNull)
                .and(AES_KEY_SHA.isNull)
                .limit(limit)
                .fetch()
                .map(::toRow)
        }
    }

    override fun updateAesKeySha(row: CryptoKeyRefreshRow) {
        val tokenRow = row as Oauth2AccessTokenCryptoKeyRefreshRow
        with(TAuthOauth2AccessToken.T_AUTH_OAUTH2_ACCESS_TOKEN) {
            dslContext.update(this)
                .set(AES_KEY_SHA, currentKeySha)
                .where(ACCESS_TOKEN.eq(tokenRow.accessToken))
                .execute()
        }
    }

    private fun toRow(record: Record): Oauth2AccessTokenCryptoKeyRefreshRow {
        return with(TAuthOauth2AccessToken.T_AUTH_OAUTH2_ACCESS_TOKEN) {
            Oauth2AccessTokenCryptoKeyRefreshRow(
                accessToken = record.get(ACCESS_TOKEN),
                passWord = record.get(PASS_WORD),
                aesKeySha = record.get(AES_KEY_SHA)
            )
        }
    }
}

data class Oauth2AccessTokenCryptoKeyRefreshRow(
    val accessToken: String,
    val passWord: String?,
    val aesKeySha: String?
) : CryptoKeyRefreshRow {
    override fun rowKey(): String = "oauth2-access-token:$accessToken"

    override fun keySha(): String? = aesKeySha
}
