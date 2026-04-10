package com.tencent.devops.remotedev.service

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.remotedev.dao.ProjectStrategyDao
import com.tencent.devops.remotedev.pojo.strategy.PersonalDesktopLockScreenStrategy
import com.tencent.devops.remotedev.pojo.strategy.ProjectStrategyFetchInfo
import com.tencent.devops.remotedev.pojo.strategy.ProjectStrategyInfo
import com.tencent.devops.remotedev.pojo.strategy.ProjectStrategyResp
import com.tencent.devops.remotedev.pojo.strategy.PublicDesktopLockScreenStrategy
import com.tencent.devops.remotedev.pojo.strategy.StrategyType
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ProjectStrategyService @Autowired constructor(
    private val dslContext: DSLContext,
    private val projectStrategyDao: ProjectStrategyDao,
    private val permissionService: PermissionService
) {
    fun createOrUpdateStrategy(
        info: ProjectStrategyInfo
    ) {
        info.typeList.forEach { type ->
            when (type) {
                StrategyType.PERSONAL_DESKTOP_LOCK_SCREEN -> projectStrategyDao.createOrUpdateStrategy(
                    dslContext = dslContext,
                    projectId = info.projectId,
                    zoneType = info.zoneType,
                    strategyType = StrategyType.PERSONAL_DESKTOP_LOCK_SCREEN,
                    content = JsonUtil.toJson(
                        info.personalDesktopLockScreenStrategy ?: PersonalDesktopLockScreenStrategy.default(), false
                    )
                )

                StrategyType.PUBLIC_DESKTOP_LOCK_SCREEN -> projectStrategyDao.createOrUpdateStrategy(
                    dslContext = dslContext,
                    projectId = info.projectId,
                    zoneType = info.zoneType,
                    strategyType = StrategyType.PUBLIC_DESKTOP_LOCK_SCREEN,
                    content = JsonUtil.toJson(
                        info.publicDesktopLockScreenStrategy ?: PublicDesktopLockScreenStrategy.default(), false
                    )
                )
            }
        }
    }

    fun getStrategy(
        info: ProjectStrategyFetchInfo
    ): ProjectStrategyResp {
        val resp = ProjectStrategyResp(null, null)
        val records = projectStrategyDao.fetchStrategyList(
            dslContext = dslContext,
            projectId = info.projectId,
            zoneType = info.zoneType,
            strategyType = info.typeList
        )
        records.forEach { record ->
            val type = StrategyType.fromName(record.strategyType) ?: return@forEach
            when (type) {
                StrategyType.PERSONAL_DESKTOP_LOCK_SCREEN -> {
                    val content = JsonUtil.to(
                        record.strategyContent.data(),
                        object : TypeReference<PersonalDesktopLockScreenStrategy>() {}
                    )
                    resp.personalDesktopLockScreenStrategy = content
                }

                StrategyType.PUBLIC_DESKTOP_LOCK_SCREEN -> {
                    val content = JsonUtil.to(
                        record.strategyContent.data(),
                        object : TypeReference<PublicDesktopLockScreenStrategy>() {}
                    )
                    resp.publicDesktopLockScreenStrategy = content
                }
            }
        }
        return resp
    }
}