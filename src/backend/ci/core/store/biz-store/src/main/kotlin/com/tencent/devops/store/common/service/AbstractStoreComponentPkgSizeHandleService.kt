package com.tencent.devops.store.common.service

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.store.pojo.common.StorePackageInfoReq
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.common.version.StoreVersionSizeInfo
import java.math.BigDecimal
import java.math.RoundingMode
import org.slf4j.LoggerFactory

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
        storeCode: String,
        osName: String?,
        osArch: String?
    ): StoreVersionSizeInfo

    fun parseComponentPackageSize(size: String, osName: String?, osArch: String?): BigDecimal? {
        return try {
            val atomPackageInfos = JsonUtil.to(size, object : TypeReference<List<StorePackageInfoReq>>() {})
            if (atomPackageInfos.isEmpty()) return null

            if (!osName.isNullOrBlank() && !osArch.isNullOrBlank()) {
                val matchedPackage = atomPackageInfos.firstOrNull {
                    it.osName == osName && it.arch == osArch
                }
                return matchedPackage?.let {
                    formatSizeInMB(BigDecimal(it.size))
                }
            }

            val totalBytes = atomPackageInfos.fold(BigDecimal.ZERO) { acc, info ->
                acc + BigDecimal(info.size)
            }
            val totalMB = formatSizeInMB(totalBytes)
            val packageCount = BigDecimal(atomPackageInfos.size)
            totalMB.divide(packageCount, DIV_SCALE, ROUNDING_MODE)
        } catch (e: Exception) {
            logger.warn("parseComponentPackageSize error: $e")
            null
        }
    }

    fun formatSizeInMB(sizeInBytes: BigDecimal): BigDecimal {
        return sizeInBytes.divide(MB_DIVISOR, DIV_SCALE, ROUNDING_MODE)
    }

    companion object {
        private val MB_DIVISOR = BigDecimal(1024).pow(2)
        private const val DIV_SCALE = 2
        private val ROUNDING_MODE = RoundingMode.HALF_UP
        private val logger = LoggerFactory.getLogger(AbstractStoreComponentPkgSizeHandleService::class.java)
    }
}