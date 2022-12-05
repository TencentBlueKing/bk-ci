package com.tencent.bkrepo.repository.service.packages

interface PackageRepairService {

    /**
     * 修复npm历史版本数据
     */
    fun repairHistoryVersion()
}
