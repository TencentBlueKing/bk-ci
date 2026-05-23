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
     * 刷新并回写单行数据。
     *
     * @param row 待刷新的数据行。
     */
    fun updateRow(row: CryptoKeyRefreshRow)
}
