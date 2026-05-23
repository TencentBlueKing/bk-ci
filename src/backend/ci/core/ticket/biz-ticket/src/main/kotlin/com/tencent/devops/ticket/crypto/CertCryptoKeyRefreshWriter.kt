package com.tencent.devops.ticket.crypto

import com.tencent.devops.common.security.crypto.CryptoKeyRefreshWriter
import com.tencent.devops.common.security.crypto.CryptoKeyRefreshRow
import com.tencent.devops.model.ticket.tables.TCert
import com.tencent.devops.ticket.service.CertHelper
import org.jooq.DSLContext
import org.jooq.Record
import org.springframework.stereotype.Service

@Service
class CertCryptoKeyRefreshWriter(
    private val dslContext: DSLContext,
    private val certHelper: CertHelper
) : CryptoKeyRefreshWriter {
    override val name = "cert"

    private val currentKeySha = certHelper.currentKeySha()

    override fun fetchBatch(limit: Int): List<CryptoKeyRefreshRow> {
        return with(TCert.T_CERT) {
            dslContext.select(
                PROJECT_ID,
                CERT_ID,
                CERT_P12_FILE_CONTENT,
                CERT_MP_FILE_CONTENT,
                CERT_JKS_FILE_CONTENT,
                AES_KEY_SHA
            ).from(this)
                .where(AES_KEY_SHA.isNull.or(AES_KEY_SHA.ne(currentKeySha)))
                .limit(limit)
                .fetch()
                .map(::toRow)
        }
    }

    override fun updateRow(row: CryptoKeyRefreshRow) {
        val certRow = row as CertCryptoKeyRefreshRow
        with(TCert.T_CERT) {
            dslContext.update(this)
                .set(CERT_P12_FILE_CONTENT, certHelper.refreshBytes(certRow.certP12FileContent)!!)
                .set(CERT_MP_FILE_CONTENT, certHelper.refreshBytes(certRow.certMpFileContent)!!)
                .set(CERT_JKS_FILE_CONTENT, certHelper.refreshBytes(certRow.certJksFileContent)!!)
                .set(AES_KEY_SHA, currentKeySha)
                .where(PROJECT_ID.eq(certRow.projectId))
                .and(CERT_ID.eq(certRow.certId))
                .execute()
        }
    }

    private fun toRow(record: Record): CertCryptoKeyRefreshRow {
        return with(TCert.T_CERT) {
            CertCryptoKeyRefreshRow(
                projectId = record.get(PROJECT_ID),
                certId = record.get(CERT_ID),
                certP12FileContent = record.get(CERT_P12_FILE_CONTENT),
                certMpFileContent = record.get(CERT_MP_FILE_CONTENT),
                certJksFileContent = record.get(CERT_JKS_FILE_CONTENT),
                aesKeySha = record.get(AES_KEY_SHA)
            )
        }
    }
}

data class CertCryptoKeyRefreshRow(
    val projectId: String,
    val certId: String,
    val certP12FileContent: ByteArray,
    val certMpFileContent: ByteArray,
    val certJksFileContent: ByteArray,
    val aesKeySha: String?
) : CryptoKeyRefreshRow {
    override fun rowKey(): String = "cert:$projectId:$certId"

    override fun keySha(): String? = aesKeySha
}
