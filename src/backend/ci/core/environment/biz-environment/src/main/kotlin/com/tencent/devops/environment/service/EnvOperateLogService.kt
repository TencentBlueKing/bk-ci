package com.tencent.devops.environment.service

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.environment.dao.EnvOperateLogDao
import com.tencent.devops.environment.pojo.envOperate.EnvOperateContent
import com.tencent.devops.environment.pojo.envOperate.EnvOperateLog
import com.tencent.devops.environment.pojo.envOperate.EnvOperateName
import com.tencent.devops.environment.pojo.envOperate.EnvOperateOrigin
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class EnvOperateLogService @Autowired constructor(
    private val dslContext: DSLContext,
    private val envOperateLogDao: EnvOperateLogDao
) {
    fun addOperateLog(
        projectId: String,
        envId: Long,
        operateOrigin: EnvOperateOrigin,
        operateName: EnvOperateName,
        operateContent: EnvOperateContent?,
        operator: String
    ) {
        envOperateLogDao.addOperateLog(
            dslContext = dslContext,
            projectId = projectId,
            envId = envId,
            operateOrigin = operateOrigin,
            operateName = operateName,
            operateContent = operateContent,
            operator = operator
        )
    }

    fun fetchOperateLog(
        projectId: String,
        envId: Long,
        operator: String?,
        page: Int,
        pageSize: Int
    ): Page<EnvOperateLog> {
        val count = envOperateLogDao.countOperateLog(
            dslContext = dslContext,
            projectId = projectId,
            envId = envId,
            operator = operator
        )
        val (offset, limit) = PageUtil.convertPageSizeToSQLLimit(page = page, pageSize = pageSize)
        return Page(
            page = page,
            pageSize = pageSize,
            count = count,
            records = envOperateLogDao.fetchOperateLog(
                dslContext = dslContext,
                projectId = projectId,
                envId = envId,
                operator = operator,
                limit = limit,
                offset = offset
            )
        )
    }

    @Scheduled(cron = "0 5 2 * * ?")
    fun cleanOperateLog() {
        val now = java.time.LocalDateTime.now()
        val threeMonthsAgo = now.minusMonths(3)

        // 删除超过 3 个月的记录
        val oldLogIds = envOperateLogDao.getOldLogIds(dslContext, threeMonthsAgo)
        if (oldLogIds.isNotEmpty()) {
            envOperateLogDao.deleteByIds(dslContext, oldLogIds)
        }

        // 每个 envId 下保留最新 1000 条，删除超出部分
        val envIds = envOperateLogDao.getAllEnvIds(dslContext)
        envIds.forEach { envId ->
            val excessIds = envOperateLogDao.getExcessLogIds(dslContext, envId, 1000)
            if (excessIds.isNotEmpty()) {
                envOperateLogDao.deleteByIds(dslContext, excessIds)
            }
        }
    }
}