package com.tencent.devops.repository.crypto

import com.tencent.devops.common.security.crypto.CryptoKeyRefreshRow
import com.tencent.devops.common.security.crypto.CryptoKeyRefreshWriter
import com.tencent.devops.model.repository.tables.TRepositoryGithubToken
import org.jooq.DSLContext
import org.jooq.Record
import org.springframework.stereotype.Service

@Service
class GithubTokenCryptoKeyRefreshWriter(
    private val dslContext: DSLContext,
    private val githubTokenCryptoHelper: GithubTokenCryptoHelper
) : CryptoKeyRefreshWriter {
    override val name = "repository-github-token"

    override fun fetchBatch(limit: Int): List<CryptoKeyRefreshRow> {
        return with(TRepositoryGithubToken.T_REPOSITORY_GITHUB_TOKEN) {
            dslContext.select(USER_ID, TYPE, ACCESS_TOKEN, AES_KEY_SHA)
                .from(this)
                .where(AES_KEY_SHA.isNull.or(AES_KEY_SHA.ne(githubTokenCryptoHelper.currentKeySha())))
                .limit(limit)
                .fetch()
                .map(::toRow)
        }
    }

    override fun updateRow(row: CryptoKeyRefreshRow) {
        val githubTokenRow = row as GithubTokenCryptoKeyRefreshRow
        with(TRepositoryGithubToken.T_REPOSITORY_GITHUB_TOKEN) {
            dslContext.update(this)
                .set(ACCESS_TOKEN, githubTokenCryptoHelper.refreshSm4OrAes(githubTokenRow.accessToken))
                .set(AES_KEY_SHA, githubTokenCryptoHelper.currentKeySha())
                .where(USER_ID.eq(githubTokenRow.userId))
                .and(TYPE.eq(githubTokenRow.type))
                .execute()
        }
    }

    private fun toRow(record: Record): GithubTokenCryptoKeyRefreshRow {
        return with(TRepositoryGithubToken.T_REPOSITORY_GITHUB_TOKEN) {
            GithubTokenCryptoKeyRefreshRow(
                userId = record.get(USER_ID),
                type = record.get(TYPE),
                accessToken = record.get(ACCESS_TOKEN),
                aesKeySha = record.get(AES_KEY_SHA)
            )
        }
    }
}

data class GithubTokenCryptoKeyRefreshRow(
    val userId: String,
    val type: String,
    val accessToken: String,
    val aesKeySha: String?
) : CryptoKeyRefreshRow {
    override fun rowKey(): String = "repository-github-token:$userId:$type"

    override fun keySha(): String? = aesKeySha
}
