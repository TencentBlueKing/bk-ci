package com.tencent.devops.ticket.crypto

import com.tencent.devops.common.security.crypto.CryptoKeyRefreshWriter
import com.tencent.devops.common.security.crypto.CryptoKeyRefreshRow
import com.tencent.devops.model.ticket.tables.TCertEnterprise
import com.tencent.devops.ticket.service.CertHelper
import org.jooq.DSLContext
import org.jooq.Record
import org.springframework.stereotype.Service

@Service
class CertEnterpriseCryptoKeyRefreshWriter(
    private val dslContext: DSLContext,
    private val certHelper: CertHelper
) : CryptoKeyRefreshWriter {
    override val name = "cert-enterprise"

    private val currentKeySha = certHelper.currentKeySha()

    override fun fetchBatch(limit: Int): List<CryptoKeyRefreshRow> {
        return with(TCertEnterprise.T_CERT_ENTERPRISE) {
            dslContext.select(
                PROJECT_ID,
                CERT_ID,
                CERT_MP_FILE_CONTENT,
                AES_KEY_SHA
            ).from(this)
                .where(AES_KEY_SHA.isNull.or(AES_KEY_SHA.ne(currentKeySha)))
                .limit(limit)
                .fetch()
                .map(::toRow)
        }
    }

    override fun updateRow(row: CryptoKeyRefreshRow) {
        val certRow = row as CertEnterpriseCryptoKeyRefreshRow
        with(TCertEnterprise.T_CERT_ENTERPRISE) {
            dslContext.update(this)
                .set(CERT_MP_FILE_CONTENT, certHelper.refreshBytes(certRow.certMpFileContent)!!)
                .set(AES_KEY_SHA, currentKeySha)
                .where(PROJECT_ID.eq(certRow.projectId))
                .and(CERT_ID.eq(certRow.certId))
                .execute()
        }
    }

    private fun toRow(record: Record): CertEnterpriseCryptoKeyRefreshRow {
        return with(TCertEnterprise.T_CERT_ENTERPRISE) {
            CertEnterpriseCryptoKeyRefreshRow(
                projectId = record.get(PROJECT_ID),
                certId = record.get(CERT_ID),
                certMpFileContent = record.get(CERT_MP_FILE_CONTENT),
                aesKeySha = record.get(AES_KEY_SHA)
            )
        }
    }
}

data class CertEnterpriseCryptoKeyRefreshRow(
    val projectId: String,
    val certId: String,
    val certMpFileContent: ByteArray,
    val aesKeySha: String?
) : CryptoKeyRefreshRow {
    override fun rowKey(): String = "cert-enterprise:$projectId:$certId"

    override fun keySha(): String? = aesKeySha
}
