package com.tencent.devops.store.common.crypto

import com.tencent.devops.common.security.crypto.CryptoKeyRefreshRow
import com.tencent.devops.common.security.crypto.CryptoKeyRefreshWriter
import com.tencent.devops.model.store.tables.TStoreEnvVar
import org.jooq.DSLContext
import org.jooq.Record
import org.springframework.stereotype.Service

@Service
class StoreEnvVarCryptoKeyRefreshWriter(
    private val dslContext: DSLContext,
    private val storeCryptoHelper: StoreCryptoHelper
) : CryptoKeyRefreshWriter {
    override val name = "store-env-var"

    private val currentKeySha = storeCryptoHelper.currentKeySha()

    override fun fetchBatch(limit: Int): List<CryptoKeyRefreshRow> {
        return with(TStoreEnvVar.T_STORE_ENV_VAR) {
            dslContext.select(ID, VAR_VALUE, AES_KEY_SHA)
                .from(this)
                .where(ENCRYPT_FLAG.eq(true))
                .and(AES_KEY_SHA.isNull.or(AES_KEY_SHA.ne(currentKeySha)))
                .limit(limit)
                .fetch()
                .map(::toRow)
        }
    }

    override fun updateRow(row: CryptoKeyRefreshRow) {
        val envVarRow = row as StoreEnvVarCryptoKeyRefreshRow
        with(TStoreEnvVar.T_STORE_ENV_VAR) {
            dslContext.update(this)
                .set(VAR_VALUE, storeCryptoHelper.refreshAes(envVarRow.varValue))
                .set(AES_KEY_SHA, currentKeySha)
                .where(ID.eq(envVarRow.id))
                .execute()
        }
    }

    private fun toRow(record: Record): StoreEnvVarCryptoKeyRefreshRow {
        return with(TStoreEnvVar.T_STORE_ENV_VAR) {
            StoreEnvVarCryptoKeyRefreshRow(
                id = record.get(ID),
                varValue = record.get(VAR_VALUE),
                aesKeySha = record.get(AES_KEY_SHA)
            )
        }
    }
}

data class StoreEnvVarCryptoKeyRefreshRow(
    val id: String,
    val varValue: String,
    val aesKeySha: String?
) : CryptoKeyRefreshRow {
    override fun rowKey(): String = "store-env-var:$id"

    override fun keySha(): String? = aesKeySha
}
