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
     * 是否支持按项目过滤。无 `PROJECT_ID` 的表保持默认 false。
     */
    fun supportsProjectFilter(): Boolean = false

    /**
     * 拉取一批需要刷新密钥的数据。
     *
     * @param limit 本批最多拉取的数据行数。
     * @param projectId 按项目过滤；为空则全量。不支持项目过滤的 Writer 应忽略该参数。
     * @return 待刷新数据行列表。
     */
    fun fetchBatch(limit: Int, projectId: String? = null): List<CryptoKeyRefreshRow>

    /**
     * 刷新并回写单行数据（重加密密文，并写入当前密钥指纹）。
     *
     * @param row 待刷新的数据行。
     */
    fun updateRow(row: CryptoKeyRefreshRow)
}
