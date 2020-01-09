package com.tencent.devops.store.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.store.dao.ExtServiceVersionLogDao
import com.tencent.devops.store.pojo.VersionLog
import com.tencent.devops.store.pojo.vo.VersionLogVO
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class ExtServiceVersionLogService @Autowired constructor() {
    @Autowired
    lateinit var extServiceVersionDao: ExtServiceVersionLogDao
    @Autowired
    lateinit var dslContext: DSLContext

    fun listVersionLog(
        serviceId: String
    ): Result<VersionLogVO?> {
        val logRecords = extServiceVersionDao.listVersionLogByServiceId(dslContext, serviceId)
        val count = extServiceVersionDao.countVersionLogByServiceId(dslContext, serviceId)
        val logList = mutableListOf<VersionLog>()
        if (logRecords != null) {
            for (logRecord in logRecords) {
                logList.add(
                    VersionLog(
                        logId = logRecord.id,
                        serviceId = logRecord.serviceId,
                        releaseType = logRecord.releaseType.toString(),
                        content = logRecord.content,
                        createTime = DateTimeUtil.toDateTime(logRecord.createTime as LocalDateTime),
                        createUser = logRecord.creator
                    )
                )
            }
        }

        val result = VersionLogVO(count, logList)

        return Result(result)
    }

    fun getVersionLog(
        logId: String
    ): Result<VersionLog>{
        val logRecord = extServiceVersionDao.getVersionLogById(
            dslContext, logId
        )
        val result = VersionLog(
            logId = logRecord.id,
            serviceId = logRecord.serviceId,
            releaseType = logRecord.releaseType.toString(),
            content = logRecord.content,
            createTime = DateTimeUtil.toDateTime(logRecord.createTime as LocalDateTime),
            createUser = logRecord.creator
        )
        return Result(result)
    }
}