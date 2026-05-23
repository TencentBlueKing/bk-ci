package com.tencent.devops.repository.crypto

import com.tencent.devops.common.security.crypto.CryptoKeyRefreshRow
import com.tencent.devops.common.security.crypto.CryptoKeyRefreshWriter
import com.tencent.devops.model.repository.tables.TRepositoryTgitToken
import org.jooq.DSLContext
import org.jooq.Record
import org.springframework.stereotype.Service

@Service
class TGitTokenCryptoKeyRefreshWriter(
    private val dslContext: DSLContext,
    private val gitTokenCryptoHelper: GitTokenCryptoHelper
) : CryptoKeyRefreshWriter {
    override val name = "repository-tgit-token"

    override fun fetchBatch(limit: Int): List<CryptoKeyRefreshRow> {
        return with(TRepositoryTgitToken.T_REPOSITORY_TGIT_TOKEN) {
            dslContext.select(USER_ID, ACCESS_TOKEN, REFRESH_TOKEN, AES_KEY_SHA)
                .from(this)
                .where(AES_KEY_SHA.isNull.or(AES_KEY_SHA.ne(gitTokenCryptoHelper.currentKeySha())))
                .limit(limit)
                .fetch()
                .map(::toRow)
        }
    }

    override fun updateRow(row: CryptoKeyRefreshRow) {
        val tGitTokenRow = row as TGitTokenCryptoKeyRefreshRow
        with(TRepositoryTgitToken.T_REPOSITORY_TGIT_TOKEN) {
            dslContext.update(this)
                .set(ACCESS_TOKEN, tGitTokenRow.accessToken?.let(gitTokenCryptoHelper::refreshSm4OrAes))
                .set(REFRESH_TOKEN, tGitTokenRow.refreshToken?.let(gitTokenCryptoHelper::refreshSm4OrAes))
                .set(AES_KEY_SHA, gitTokenCryptoHelper.currentKeySha())
                .where(USER_ID.eq(tGitTokenRow.userId))
                .execute()
        }
    }

    private fun toRow(record: Record): TGitTokenCryptoKeyRefreshRow {
        return with(TRepositoryTgitToken.T_REPOSITORY_TGIT_TOKEN) {
            TGitTokenCryptoKeyRefreshRow(
                userId = record.get(USER_ID),
                accessToken = record.get(ACCESS_TOKEN),
                refreshToken = record.get(REFRESH_TOKEN),
                aesKeySha = record.get(AES_KEY_SHA)
            )
        }
    }
}

data class TGitTokenCryptoKeyRefreshRow(
    val userId: String,
    val accessToken: String?,
    val refreshToken: String?,
    val aesKeySha: String?
) : CryptoKeyRefreshRow {
    override fun rowKey(): String = "repository-tgit-token:$userId"

    override fun keySha(): String? = aesKeySha
}
