package com.tencent.devops.ticket.crypto

import com.tencent.devops.common.security.crypto.CryptoKeyRefreshWriter
import com.tencent.devops.common.security.crypto.CryptoKeyRefreshRow
import com.tencent.devops.model.ticket.tables.TCredential
import com.tencent.devops.ticket.service.CredentialHelper
import org.jooq.DSLContext
import org.jooq.Record
import org.springframework.stereotype.Service

@Service
class CredentialCryptoKeyRefreshWriter(
    private val dslContext: DSLContext,
    private val credentialHelper: CredentialHelper
) : CryptoKeyRefreshWriter {
    override val name = "credential"

    private val currentKeySha = credentialHelper.currentKeySha()

    override fun fetchBatch(limit: Int): List<CryptoKeyRefreshRow> {
        return with(TCredential.T_CREDENTIAL) {
            dslContext.select(
                PROJECT_ID,
                CREDENTIAL_ID,
                CREDENTIAL_V1,
                CREDENTIAL_V2,
                CREDENTIAL_V3,
                CREDENTIAL_V4,
                AES_KEY_SHA
            ).from(this)
                .where(AES_KEY_SHA.isNull.or(AES_KEY_SHA.ne(currentKeySha)))
                .limit(limit)
                .fetch()
                .map(::toRow)
        }
    }

    override fun updateRow(row: CryptoKeyRefreshRow) {
        val credentialRow = row as CredentialCryptoKeyRefreshRow
        with(TCredential.T_CREDENTIAL) {
            dslContext.update(this)
                .set(CREDENTIAL_V1, credentialHelper.refreshCredential(credentialRow.credentialV1)!!)
                .set(CREDENTIAL_V2, credentialHelper.refreshCredential(credentialRow.credentialV2))
                .set(CREDENTIAL_V3, credentialHelper.refreshCredential(credentialRow.credentialV3))
                .set(CREDENTIAL_V4, credentialHelper.refreshCredential(credentialRow.credentialV4))
                .set(AES_KEY_SHA, currentKeySha)
                .where(PROJECT_ID.eq(credentialRow.projectId))
                .and(CREDENTIAL_ID.eq(credentialRow.credentialId))
                .execute()
        }
    }

    private fun toRow(record: Record): CredentialCryptoKeyRefreshRow {
        return with(TCredential.T_CREDENTIAL) {
            CredentialCryptoKeyRefreshRow(
                projectId = record.get(PROJECT_ID),
                credentialId = record.get(CREDENTIAL_ID),
                credentialV1 = record.get(CREDENTIAL_V1),
                credentialV2 = record.get(CREDENTIAL_V2),
                credentialV3 = record.get(CREDENTIAL_V3),
                credentialV4 = record.get(CREDENTIAL_V4),
                aesKeySha = record.get(AES_KEY_SHA)
            )
        }
    }
}

data class CredentialCryptoKeyRefreshRow(
    val projectId: String,
    val credentialId: String,
    val credentialV1: String,
    val credentialV2: String?,
    val credentialV3: String?,
    val credentialV4: String?,
    val aesKeySha: String?
) : CryptoKeyRefreshRow {
    override fun rowKey(): String = "credential:$projectId:$credentialId"

    override fun keySha(): String? = aesKeySha
}
