package com.tencent.devops.ticket.crypto

import com.tencent.devops.common.security.crypto.CryptoKeyRefreshWriter
import com.tencent.devops.common.security.crypto.CryptoKeyRefreshRow
import com.tencent.devops.model.ticket.tables.TCertTls
import com.tencent.devops.ticket.service.CertHelper
import org.jooq.DSLContext
import org.jooq.Record
import org.springframework.stereotype.Service

@Service
class CertTlsCryptoKeyRefreshWriter(
    private val dslContext: DSLContext,
    private val certHelper: CertHelper
) : CryptoKeyRefreshWriter {
    override val name = "cert-tls"

    private val currentKeySha = certHelper.currentKeySha()

    override fun fetchBatch(limit: Int): List<CryptoKeyRefreshRow> {
        return with(TCertTls.T_CERT_TLS) {
            dslContext.select(
                PROJECT_ID,
                CERT_ID,
                CERT_SERVER_CRT_FILE,
                CERT_SERVER_KEY_FILE,
                CERT_CLIENT_CRT_FILE,
                CERT_CLIENT_KEY_FILE,
                AES_KEY_SHA
            ).from(this)
                .where(AES_KEY_SHA.isNull.or(AES_KEY_SHA.ne(currentKeySha)))
                .limit(limit)
                .fetch()
                .map(::toRow)
        }
    }

    override fun updateRow(row: CryptoKeyRefreshRow) {
        val certRow = row as CertTlsCryptoKeyRefreshRow
        with(TCertTls.T_CERT_TLS) {
            dslContext.update(this)
                .set(CERT_SERVER_CRT_FILE, certHelper.refreshBytes(certRow.serverCrtFile)!!)
                .set(CERT_SERVER_KEY_FILE, certHelper.refreshBytes(certRow.serverKeyFile)!!)
                .set(CERT_CLIENT_CRT_FILE, certHelper.refreshBytes(certRow.clientCrtFile))
                .set(CERT_CLIENT_KEY_FILE, certHelper.refreshBytes(certRow.clientKeyFile))
                .set(AES_KEY_SHA, currentKeySha)
                .where(PROJECT_ID.eq(certRow.projectId))
                .and(CERT_ID.eq(certRow.certId))
                .execute()
        }
    }

    private fun toRow(record: Record): CertTlsCryptoKeyRefreshRow {
        return with(TCertTls.T_CERT_TLS) {
            CertTlsCryptoKeyRefreshRow(
                projectId = record.get(PROJECT_ID),
                certId = record.get(CERT_ID),
                serverCrtFile = record.get(CERT_SERVER_CRT_FILE),
                serverKeyFile = record.get(CERT_SERVER_KEY_FILE),
                clientCrtFile = record.get(CERT_CLIENT_CRT_FILE),
                clientKeyFile = record.get(CERT_CLIENT_KEY_FILE),
                aesKeySha = record.get(AES_KEY_SHA)
            )
        }
    }
}

data class CertTlsCryptoKeyRefreshRow(
    val projectId: String,
    val certId: String,
    val serverCrtFile: ByteArray,
    val serverKeyFile: ByteArray,
    val clientCrtFile: ByteArray?,
    val clientKeyFile: ByteArray?,
    val aesKeySha: String?
) : CryptoKeyRefreshRow {
    override fun rowKey(): String = "cert-tls:$projectId:$certId"

    override fun keySha(): String? = aesKeySha
}
