package com.tencent.devops.common.security.crypto

/**
 * 加密密钥刷新任务的数据读写器。
 */
interface CryptoKeyRefreshWriter {
    /**
     * 刷新器名称，用于日志和异常定位。
     */
    val name: String

    /**
     * 拉取一批需要刷新密钥的数据。
     *
     * @param limit 本批最多拉取的数据行数。
     * @return 待刷新数据行列表。
     */
    fun fetchBatch(limit: Int): List<CryptoKeyRefreshRow>

    /**
     * 刷新并回写单行数据（重加密密文，并写入当前密钥指纹）。
     *
     * @param row 待刷新的数据行。
     */
    fun updateRow(row: CryptoKeyRefreshRow)

    /**
     * 拉取一批 `AES_KEY_SHA` 为空的存量行，供 OP 补指纹。不要带密文重加密条件。
     *
     * @param limit 本批最多拉取的数据行数。
     */
    fun fetchMissingKeyShaBatch(limit: Int): List<CryptoKeyRefreshRow>

    /**
     * 只回写当前密钥指纹，不改密文。用于新列上线后给存量数据补 `AES_KEY_SHA`。
     *
     * @param row 待补指纹的数据行。
     */
    fun updateAesKeySha(row: CryptoKeyRefreshRow)
}
