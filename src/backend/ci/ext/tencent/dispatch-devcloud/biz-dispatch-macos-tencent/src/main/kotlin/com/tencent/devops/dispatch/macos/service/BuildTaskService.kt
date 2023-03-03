package com.tencent.devops.dispatch.macos.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.dispatcher.macos.dao.BuildTaskDao
import com.tencent.devops.dispatcher.macos.dao.MacHostMachineDao
import com.tencent.devops.dispatcher.macos.dao.VirtualMachineDao
import com.tencent.devops.dispatcher.macos.dao.VirtualMachineTypeDao
import com.tencent.devops.model.dispatcher.macos.tables.records.TBuildTaskRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
open class BuildTaskService @Autowired constructor(
    private val dslContext: DSLContext,
    private val redisOperation: RedisOperation,
    private val objectMapper: ObjectMapper,
    private val virtualMachineDao: VirtualMachineDao,
    private val macHostMachineDao: MacHostMachineDao,
    private val virtualMachineTypeDao: VirtualMachineTypeDao,
    private val buildTaskDao: BuildTaskDao,
    private val passwordHelper: PasswordHelper
) {

    fun getByBuildIdAndVmSeqId(
        buildId: String,
        vmSeqId: String?,
        executeCount: Int?
    ): Result<TBuildTaskRecord> {
        var buildRecord = buildTaskDao.listByBuildIdAndVmSeqId(dslContext, buildId, vmSeqId, executeCount)
        // 如果构建记录为空，可能是因为取消时分配构建IP接口还未完成，等待30s
        if (buildRecord.isEmpty()) {
            Thread.sleep(30000)
            buildRecord = buildTaskDao.listByBuildIdAndVmSeqId(dslContext, buildId, vmSeqId, executeCount)
        }

        return buildRecord
    }

    fun deleteById(
        buildTaskId: Long
    ): Boolean {
        return buildTaskDao.deleteById(dslContext, buildTaskId)
    }
}
