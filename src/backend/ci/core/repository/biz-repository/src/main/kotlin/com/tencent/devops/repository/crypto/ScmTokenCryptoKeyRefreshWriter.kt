package com.tencent.devops.repository.crypto

import com.tencent.devops.common.security.crypto.CryptoKeyRefreshRow
import com.tencent.devops.common.security.crypto.CryptoKeyRefreshWriter
import com.tencent.devops.model.repository.tables.TRepositoryScmToken
import org.jooq.DSLContext
import org.jooq.Record
import org.springframework.stereotype.Service

@Service
class ScmTokenCryptoKeyRefreshWriter(
    private val dslContext: DSLContext,
    private val gitTokenCryptoHelper: GitTokenCryptoHelper
) : CryptoKeyRefreshWriter {
    override val name = "repository-scm-token"

    override fun fetchBatch(limit: Int): List<CryptoKeyRefreshRow> {
        return with(TRepositoryScmToken.T_REPOSITORY_SCM_TOKEN) {
            dslContext.select(USER_ID, SCM_CODE, APP_TYPE, ACCESS_TOKEN, REFRESH_TOKEN, AES_KEY_SHA)
                .from(this)
                .where(AES_KEY_SHA.isNull.or(AES_KEY_SHA.ne(gitTokenCryptoHelper.currentKeySha())))
                .limit(limit)
                .fetch()
                .map(::toRow)
        }
    }

    override fun updateRow(row: CryptoKeyRefreshRow) {
        val scmTokenRow = row as ScmTokenCryptoKeyRefreshRow
        with(TRepositoryScmToken.T_REPOSITORY_SCM_TOKEN) {
            dslContext.update(this)
                .set(ACCESS_TOKEN, scmTokenRow.accessToken?.let(gitTokenCryptoHelper::refreshSm4OrAes))
                .set(REFRESH_TOKEN, scmTokenRow.refreshToken?.let(gitTokenCryptoHelper::refreshSm4OrAes))
                .set(AES_KEY_SHA, gitTokenCryptoHelper.currentKeySha())
                .where(USER_ID.eq(scmTokenRow.userId))
                .and(SCM_CODE.eq(scmTokenRow.scmCode))
                .and(APP_TYPE.eq(scmTokenRow.appType))
                .execute()
        }
    }

    override fun fetchMissingKeyShaBatch(limit: Int): List<CryptoKeyRefreshRow> {
        return with(TRepositoryScmToken.T_REPOSITORY_SCM_TOKEN) {
            dslContext.select(USER_ID, SCM_CODE, APP_TYPE, ACCESS_TOKEN, REFRESH_TOKEN, AES_KEY_SHA)
                .from(this)
                .where(AES_KEY_SHA.isNull)
                .limit(limit)
                .fetch()
                .map(::toRow)
        }
    }

    override fun updateAesKeySha(row: CryptoKeyRefreshRow) {
        val scmTokenRow = row as ScmTokenCryptoKeyRefreshRow
        with(TRepositoryScmToken.T_REPOSITORY_SCM_TOKEN) {
            dslContext.update(this)
                .set(AES_KEY_SHA, gitTokenCryptoHelper.currentKeySha())
                .where(USER_ID.eq(scmTokenRow.userId))
                .and(SCM_CODE.eq(scmTokenRow.scmCode))
                .and(APP_TYPE.eq(scmTokenRow.appType))
                .execute()
        }
    }

    private fun toRow(record: Record): ScmTokenCryptoKeyRefreshRow {
        return with(TRepositoryScmToken.T_REPOSITORY_SCM_TOKEN) {
            ScmTokenCryptoKeyRefreshRow(
                userId = record.get(USER_ID),
                scmCode = record.get(SCM_CODE),
                appType = record.get(APP_TYPE),
                accessToken = record.get(ACCESS_TOKEN),
                refreshToken = record.get(REFRESH_TOKEN),
                aesKeySha = record.get(AES_KEY_SHA)
            )
        }
    }
}

data class ScmTokenCryptoKeyRefreshRow(
    val userId: String,
    val scmCode: String,
    val appType: String,
    val accessToken: String?,
    val refreshToken: String?,
    val aesKeySha: String?
) : CryptoKeyRefreshRow {
    override fun rowKey(): String = "repository-scm-token:$userId:$scmCode:$appType"

    override fun keySha(): String? = aesKeySha
}
