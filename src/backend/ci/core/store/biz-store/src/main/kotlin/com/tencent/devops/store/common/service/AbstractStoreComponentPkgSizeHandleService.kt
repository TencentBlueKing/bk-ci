package com.tencent.devops.store.common.service

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.store.pojo.common.StorePackageInfoReq
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import java.math.BigDecimal
import java.math.RoundingMode

abstract class AbstractStoreComponentPkgSizeHandleService {

    /**
     * 处理商城组件包大小
     */
    abstract fun batchUpdateComponentsVersionSize()

    abstract fun updateComponentVersionSize(
        storeId: String,
        storePackageInfoReqs: List<StorePackageInfoReq>,
        storeType: StoreTypeEnum
    ): Boolean

    abstract fun getComponentVersionSize(
        version: String,
        storeCode: String
    ): BigDecimal?

    fun parseComponentPackageSize(size: String): BigDecimal? {
        val atomPackageInfo = JsonUtil.to(size, object : TypeReference<List<StorePackageInfoReq>>() {})
        if (atomPackageInfo.isEmpty()) return null

        val totalBytes = atomPackageInfo.fold(BigDecimal.ZERO) { acc, info ->
            acc + BigDecimal(info.size)
        }
        val totalMB = formatSizeInMB(totalBytes)
        val packageCount = BigDecimal(atomPackageInfo.size)
        return totalMB.divide(packageCount, DIV_SCALE, ROUNDING_MODE)
    }

    fun formatSizeInMB(sizeInBytes: BigDecimal): BigDecimal {
        return sizeInBytes.divide(MB_DIVISOR, DIV_SCALE, ROUNDING_MODE)
    }

    companion object {
        private val MB_DIVISOR = BigDecimal(1024).pow(2)
        private const val DIV_SCALE = 2
        private val ROUNDING_MODE = RoundingMode.HALF_UP
    }
}