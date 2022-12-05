package com.tencent.bkrepo.scanner.component.manager

import com.tencent.bkrepo.common.scanner.pojo.scanner.ScanExecutorResult
import com.tencent.bkrepo.common.scanner.pojo.scanner.Scanner
import com.tencent.bkrepo.scanner.pojo.request.LoadResultArguments
import com.tencent.bkrepo.scanner.pojo.request.SaveResultArguments

/**
 * 详细扫描结果管理
 */
interface ScanExecutorResultManager {
    /**
     * 保存扫描结果详情
     *
     * @param credentialsKey 被扫描文件所在存储， 为null时表示在默认存储
     * @param sha256 被扫描文件sha256
     * @param scanner 使用的扫描器
     * @param result 扫描结果详情
     * @param arguments 参数
     *
     */
    fun save(
        credentialsKey: String?,
        sha256: String,
        scanner: Scanner,
        result: ScanExecutorResult,
        arguments: SaveResultArguments? = null
    )

    /**
     * 分页获取指定类型的扫描结果详情
     *
     * @param credentialsKey 被扫描文件所在存储， 为null时表示在默认存储
     * @param sha256 被扫描文件sha256
     * @param scanner 使用的扫描器
     * @param arguments 参数
     *
     * @return 扫描结果详情
     */
    fun load(
        credentialsKey: String?,
        sha256: String,
        scanner: Scanner,
        arguments: LoadResultArguments?
    ): Any?
}
