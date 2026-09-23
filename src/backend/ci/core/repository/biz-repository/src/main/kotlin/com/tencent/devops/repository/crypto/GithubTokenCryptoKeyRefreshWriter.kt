package com.tencent.devops.repository.crypto

import com.tencent.devops.common.security.crypto.CryptoKeyRefreshRow
import com.tencent.devops.common.security.crypto.CryptoKeyRefreshWriter
import com.tencent.devops.model.repository.tables.TRepositoryGithubToken
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record
import org.springframework.stereotype.Service

@Service
class GithubTokenCryptoKeyRefreshWriter(
    private val dslContext: DSLContext,
    private val githubTokenCryptoHelper: GithubTokenCryptoHelper
) : CryptoKeyRefreshWriter {
    override val name = "repository-github-token"

    override fun fetchBatch(limit: Int, filters: Map<String, String>): List<CryptoKeyRefreshRow> {
        return with(TRepositoryGithubToken.T_REPOSITORY_GITHUB_TOKEN) {
            dslContext.select(USER_ID, TYPE, ACCESS_TOKEN, AES_KEY_SHA)
                .from(this)
                .where(refreshCondition(filters))
                .limit(limit)
                .fetch()
                .map(::toRow)
        }
    }

    private fun TRepositoryGithubToken.refreshCondition(filters: Map<String, String>): List<Condition> {
        val conditions = mutableListOf(
            AES_KEY_SHA.isNull.or(AES_KEY_SHA.ne(githubTokenCryptoHelper.currentKeySha()))
        )
        filters[GithubTokenCryptoKeyRefreshRow::userId.name]
            ?.takeIf { it.isNotBlank() }
            ?.let { conditions.add(USER_ID.eq(it)) }
        filters[GithubTokenCryptoKeyRefreshRow::type.name]
            ?.takeIf { it.isNotBlank() }
            ?.let { conditions.add(TYPE.eq(it)) }
        return conditions
    }

    override fun updateRow(row: CryptoKeyRefreshRow) {
        val githubTokenRow = row as GithubTokenCryptoKeyRefreshRow
        with(TRepositoryGithubToken.T_REPOSITORY_GITHUB_TOKEN) {
            dslContext.update(this)
                .set(ACCESS_TOKEN, githubTokenCryptoHelper.refreshSm4OrAes(githubTokenRow.accessToken))
                .set(AES_KEY_SHA, githubTokenCryptoHelper.currentKeySha())
                .where(USER_ID.eq(githubTokenRow.userId))
                .and(githubTokenRow.type?.let { TYPE.eq(it) } ?: TYPE.isNull)
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
    val type: String?,
    val accessToken: String,
    val aesKeySha: String?
) : CryptoKeyRefreshRow {
    override fun rowKey(): String = "repository-github-token:$userId:$type"

    override fun keySha(): String? = aesKeySha
}
