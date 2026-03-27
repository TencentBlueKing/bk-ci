package com.tencent.devops.store.common.service.impl

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.artifactory.api.ServiceArchiveComponentPkgResource
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.store.common.dao.StoreVersionLogDao
import com.tencent.devops.store.common.service.AbstractStoreComponentPkgSizeHandleService
import com.tencent.devops.store.constant.StoreConstants.MB_UNIT
import com.tencent.devops.store.pojo.common.StorePackageInfoReq
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.common.version.StoreVersionSizeInfo
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * DEVX类型组件包大小处理服务实现类
 * 负责批量更新和查询DEVX类型组件的版本包大小信息
 */
@Service("DEVX_PKG_SIZE_HANDLE_SERVICE")
class StoreDevxPkgSizeHandleServiceImpl : AbstractStoreComponentPkgSizeHandleService() {
    @Autowired
    lateinit var dslContext: DSLContext

    @Autowired
    lateinit var client: Client

    @Autowired
    lateinit var redisOperation: RedisOperation

    @Autowired
    lateinit var storeVersionLogDao: StoreVersionLogDao

    /**
     * 批量更新所有DEVX组件的版本包大小信息
     * 采用分页方式处理，避免一次性加载过多数据
     */
    override fun batchUpdateComponentsVersionSize() {
        // 获取DEVX类型组件的总数量
        val count = storeVersionLogDao.countComponent(dslContext, StoreTypeEnum.DEVX.type.toByte())
        val batchSize = 100L

        // 使用generateSequence生成分页偏移量序列：[0, 100, 200, ...]
        // 只在需要时才计算下一个偏移量
        generateSequence(0L) { offset ->
            (offset + batchSize).takeIf { it < count }
        }.forEach { offset ->
            // 按偏移量分批处理组件数据
            processBatchByOffset(offset = offset, batchSize = batchSize)
        }
    }

    /**
     * 按偏移量处理一批组件数据
     * @param offset 数据偏移量（起始位置）
     * @param batchSize 批次大小（每次处理的数量）
     */
    private fun processBatchByOffset(offset: Long, batchSize: Long) {
        // 查询当前批次的组件ID列表
        val storeIds = storeVersionLogDao.selectComponentIds(
            dslContext = dslContext,
            offset = offset,
            batchSize = batchSize
        )

        if (storeIds.isNullOrEmpty()) return
        // 根据组件ID列表查询组件的环境信息（包含包路径、操作系统、架构等）
        val atomEnvInfos = storeVersionLogDao.selectComponentEnvInfoByStoreIds(dslContext, storeIds)
        if (atomEnvInfos.isNullOrEmpty()) return

        // 按storeId分组，每个组件可能有多个环境配置（不同OS、架构）
        atomEnvInfos.groupBy { it.get("STORE_ID").toString() }
            .forEach { (storeId, records) ->
                updateStorePackageSizeForStore(storeId = storeId, records = records)
            }
    }

    /**
     * 更新指定组件的包大小信息
     * @param storeId 组件ID
     * @param records 组件的环境信息记录列表
     */
    private fun updateStorePackageSizeForStore(storeId: String, records: List<org.jooq.Record>) {
        // 将每条记录转换为包信息请求对象，过滤掉转换失败的记录
        val storePackageInfoReqs = records.mapNotNull { record ->
            buildPackageInfoFromRecord(record)
        }

        if (storePackageInfoReqs.isEmpty()) return

        // 将包信息列表序列化为JSON并更新到数据库
        storeVersionLogDao.updateComponentVersionInfo(
            dslContext = dslContext,
            storeId = storeId,
            pkgSize = JsonUtil.toJson(storePackageInfoReqs)
        )
    }

    /**
     * 从数据库记录构建包信息请求对象
     * @param record 数据库记录，包含包路径、操作系统、架构等信息
     * @return 包信息请求对象，如果获取文件大小失败则返回null
     */
    private fun buildPackageInfoFromRecord(record: org.jooq.Record): StorePackageInfoReq? {
        val pkgPath = record.get("PKG_PATH").toString()
        // 远程调用归档服务获取文件实际大小
        val nodeSize = client.get(ServiceArchiveComponentPkgResource::class)
            .getFileSize(StoreTypeEnum.DEVX, pkgPath).data
            ?: return null

        return StorePackageInfoReq(
            storeType = StoreTypeEnum.DEVX,
            osName = record.get("OS_NAME").toString(),
            arch = record.get("OS_ARCH").toString(),
            size = nodeSize
        )
    }

    /**
     * 更新指定组件的版本包大小信息（增量更新）
     * 使用Redis分布式锁保证并发安全
     * @param storeId 组件ID
     * @param storePackageInfoReqs 新增的包信息列表
     * @param storeType 组件类型
     * @return 更新是否成功
     */
    override fun updateComponentVersionSize(
        storeId: String,
        storePackageInfoReqs: List<StorePackageInfoReq>,
        storeType: StoreTypeEnum
    ): Boolean {
        val redisLock = RedisLock(
            redisOperation = redisOperation,
            lockKey = "store:$storeId:${storeType.name}",
            expiredTimeInSeconds = 10
        )
        try {
            redisLock.lock()
            // 获取已存在的包大小信息
            val size = storeVersionLogDao.getComponentVersionSizeInfo(dslContext, storeId)
            if (size.isNullOrBlank()) {
                // 如果不存在，直接插入新数据
                storeVersionLogDao.updateComponentVersionInfo(
                    dslContext = dslContext,
                    storeId = storeId,
                    pkgSize = JsonUtil.toJson(storePackageInfoReqs)
                )
            } else {
                // 如果已存在，追加新的包信息到现有列表
                val atomPackageInfoList = JsonUtil.to(size, object : TypeReference<List<StorePackageInfoReq>>() {})
                val mutableList = atomPackageInfoList.toMutableList()
                mutableList.addAll(storePackageInfoReqs)
                storeVersionLogDao.updateComponentVersionInfo(
                    dslContext = dslContext,
                    storeId = storeId,
                    pkgSize = JsonUtil.toJson(mutableList)
                )
            }
        } finally {
            redisLock.unlock()
        }

        return true
    }

    /**
     * 获取指定版本组件的包大小信息
     * @param version 组件版本号
     * @param storeCode 组件代码
     * @param osName 操作系统名称（可选，用于筛选特定OS的包大小）
     * @param osArch 操作系统架构（可选，用于筛选特定架构的包大小）
     * @return 组件版本包大小信息
     */
    override fun getComponentVersionSize(
        version: String,
        storeCode: String,
        osName: String?,
        osArch: String?
    ): StoreVersionSizeInfo {
        // 从数据库查询包大小信息，并根据OS和架构进行过滤解析
        val size = storeVersionLogDao.getComponentSizeByVersionAndCode(
            dslContext = dslContext,
            storeCode = storeCode,
            version = version,
            storeType = StoreTypeEnum.DEVX.type.toByte()
        ).takeIf { !it.isNullOrBlank() }
            ?.let {
                // 解析JSON并根据OS和架构筛选匹配的包大小
                parseComponentPackageSize(
                    size = it,
                    osName = osName,
                    osArch = osArch
                )
            }
        return StoreVersionSizeInfo(
            storeCode = storeCode,
            storeType = StoreTypeEnum.DEVX.name,
            version = version,
            packageSize = size,
            unit = MB_UNIT
        )
    }
}