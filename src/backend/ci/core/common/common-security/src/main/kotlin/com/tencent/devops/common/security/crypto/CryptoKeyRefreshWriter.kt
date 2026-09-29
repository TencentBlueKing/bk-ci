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
     * @param filters OP 传入的过滤条件。字段值非空时，Writer 追加对应等值条件。
     * @return 待刷新数据行列表。
     */
    fun fetchBatch(limit: Int, filters: Map<String, String> = emptyMap()): List<CryptoKeyRefreshRow>

    /**
     * 刷新并回写单行数据（重加密密文，并写入当前密钥指纹）。
     *
     * @param row 待刷新的数据行。
     */
    fun updateRow(row: CryptoKeyRefreshRow)
}
