package com.tencent.devops.store.common.service.impl

import com.tencent.devops.artifactory.api.ServiceArchiveComponentPkgResource
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.api.util.ShaUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.model.store.tables.records.TStoreBaseEnvRecord
import com.tencent.devops.model.store.tables.records.TStoreBaseRecord
import com.tencent.devops.store.common.dao.StoreBaseEnvManageDao
import com.tencent.devops.store.common.dao.StoreBaseManageDao
import com.tencent.devops.store.common.service.TxStorePkgService
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import java.io.File
import java.util.UUID


@RestResource
class TxStorePkgServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val client: Client,
    private val redisOperation: RedisOperation,
    private val storeBaseManageDao: StoreBaseManageDao,
    private val storeBaseEnvManageDao: StoreBaseEnvManageDao
) : TxStorePkgService {


    companion object {
        private val logger = LoggerFactory.getLogger(TxStorePkgServiceImpl::class.java)
        private const val TEMP_DIR = "/tmp/bk-ci-sha256-migration"
        private const val LOCK_EXPIRED_TIME_IN_SECONDS = 540L
    }

    override fun updatePackageSha256(
        userId: String,
        storeType: StoreTypeEnum,
        pageSize: Int
    ) {
        // 1. 获取分布式锁
        val redisLock = RedisLock(
            redisOperation = redisOperation,
            lockKey = "OP_STORE_PKG_SHA256_UPDATE_LOCK",
            expiredTimeInSeconds = LOCK_EXPIRED_TIME_IN_SECONDS
        )
        if (!redisLock.tryLock()) {
            logger.warn("SHA256 update task is already running")
            return
        }

        return try {
            logger.info("Start running SHA256 update task , storeType: $storeType")
            // 2. 创建临时目录
            val tempDir = createTempDir()
            // 3. 分页处理所有记录
            processAllRecords(storeType, pageSize, tempDir, userId)
            logger.info("SHA256 update task running end ")
        } finally {
            // 4. 清理资源
            cleanupResources()
            redisLock.unlock()
        }
    }

    /**
     * 创建临时目录
     */
    private fun createTempDir(): File {
        val tempDir = File(TEMP_DIR)
        if (!tempDir.exists() && !tempDir.mkdirs()) {
            logger.error("Failed to create temp directory: $TEMP_DIR")
            throw RuntimeException("Cannot create temp directory: $TEMP_DIR")
        }
        return tempDir
    }

    /**
     * 分页处理所有记录
     */
    private fun processAllRecords(
        storeType: StoreTypeEnum,
        pageSize: Int,
        tempDir: File,
        userId: String
    ) {
        var offset = 0
        while (true) {
            // 查询单批记录
            val storeBaseRecords = queryStoreBaseRecords(storeType, offset, pageSize)
            if (storeBaseRecords.isNullOrEmpty()) {
                logger.info("No more records to process")
                break
            }
            // 处理单批记录
            processBatchRecords(storeBaseRecords, tempDir, userId)
            offset += pageSize
        }
    }

    /**
     * 处理单批记录
     */
    private fun processBatchRecords(
        storeBaseRecords: List<TStoreBaseRecord>,
        tempDir: File,
        userId: String
    ) {
        for (storeBaseRecord in storeBaseRecords) {
            try {
                processStorePackage(storeBaseRecord, tempDir, userId)
            } catch (e: Exception) {
                logger.error("Failed to process store: ${storeBaseRecord.storeCode}", e)
            }
        }
    }

    /**
     * 处理单个组件的所有版本包文件
     */
    private fun processStorePackage(
        storeBaseRecord: TStoreBaseRecord,
        tempDir: File,
        userId: String
    ) {
        val storeId = storeBaseRecord.id
        val storeCode = storeBaseRecord.storeCode
        logger.info("Processing store: $storeCode, storeId: $storeId")

        val envRecords = queryStoreEnvRecords(storeId)
        if (envRecords.isNullOrEmpty()) {
            logger.info("No env records found for store: $storeCode, storeId: $storeId")
            return
        }
        for (envRecord in envRecords) {
            try {
                processEnvPackage(envRecord, storeBaseRecord, tempDir, userId)
            } catch (e: Exception) {
                logger.error("Failed to process env record: ${envRecord.id}", e)
            }
        }
    }

    /**
     * 清理资源（临时目录）
     */
    private fun cleanupResources() {
        val tempDir = File(TEMP_DIR)
        if (tempDir.exists() && !tempDir.deleteRecursively()) {
            logger.warn("Failed to delete temp directory: $TEMP_DIR")
        }
    }

    private fun queryStoreBaseRecords(
        storeType: StoreTypeEnum,
        offset: Int,
        limit: Int
    ): List<TStoreBaseRecord>? {
        return storeBaseManageDao.getStoreBaseRecordsByStoreType(
            dslContext = dslContext,
            storeType = storeType,
            offset = offset,
            limit = limit
        )
    }

    private fun queryStoreEnvRecords(storeId: String): List<TStoreBaseEnvRecord>? {
        return storeBaseEnvManageDao.getStoreEnvRecordsByStoreId(
            dslContext = dslContext,
            storeId = storeId
        )
    }

    private fun processEnvPackage(
        envRecord: TStoreBaseEnvRecord,
        storeBaseRecord: TStoreBaseRecord,
        tempDir: File,
        userId: String
    ) {
        val storeCode = storeBaseRecord.storeCode
        val version = storeBaseRecord.version
        val storeType = StoreTypeEnum.getStoreTypeObj(storeBaseRecord.storeType.toInt())
        logger.info("Processing package: $storeCode-$version")

        val downloadUrl = getPackageDownloadUrl(
            userId = userId,
            storeType = storeType,
            storeCode = storeCode,
            version = version,
            osName = envRecord.osName,
            osArch = envRecord.osArch
        ) ?: run {
            logger.warn("Failed to get download url for $storeCode-$version")
            return
        }

        val localFile = File(tempDir, "${storeCode}_${version}_${UUID.randomUUID()}.tmp")
        try {
            downloadFileToLocal(downloadUrl, localFile)
            val sha256Value = calculateSha256(localFile)
            logger.info("Calculated SHA256 for $storeCode-$version: $sha256Value")
            updateSha256ToDatabase(envRecord.id, sha256Value)
        } finally {
            if (localFile.exists()) {
                localFile.delete()
            }
        }
    }

    /**
     * 获取组件包下载地址
     */
    private fun getPackageDownloadUrl(
        userId: String,
        storeType: StoreTypeEnum,
        storeCode: String,
        version: String,
        osName: String?,
        osArch: String?
    ): String? {
        return try {
            val result = client.get(ServiceArchiveComponentPkgResource::class)
                .getComponentPkgDownloadUrl(
                    userId = userId,
                    projectId = " ",
                    storeType = storeType,
                    storeCode = storeCode,
                    version = version,
                    instanceId = null,
                    osName = osName,
                    osArch = osArch,
                    checkPermissionFlag = false
                )
            result.data
        } catch (e: Exception) {
            logger.error("Failed to get download url", e)
            null
        }
    }

    /**
     * 下载文件到本地
     */
    private fun downloadFileToLocal(url: String, destFile: File) {
        OkhttpUtils.downloadFile(url, destFile)
    }

    /**
     * 计算文件的 SHA256 值
     */
    private fun calculateSha256(file: File): String {
        return ShaUtils.sha256InputStream(file.inputStream())
    }

    /**
     * 更新 SHA256 值到数据库
     */
    private fun updateSha256ToDatabase(envId: String, sha256Value: String) {
        storeBaseEnvManageDao.updateSha256(
            dslContext = dslContext,
            envId = envId,
            sha256Value = sha256Value
        )
        logger.info("Updated SHA256 for env record: $envId")
    }
}