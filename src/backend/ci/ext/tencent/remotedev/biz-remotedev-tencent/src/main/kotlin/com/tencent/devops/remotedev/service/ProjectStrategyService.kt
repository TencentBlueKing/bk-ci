package com.tencent.devops.remotedev.service

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.remotedev.common.exception.ErrorCodeEnum
import com.tencent.devops.remotedev.dao.ProjectStrategyDao
import com.tencent.devops.remotedev.pojo.strategy.LockScreenSaverStrategy
import com.tencent.devops.remotedev.pojo.strategy.PersonalDesktopLockScreenStrategy
import com.tencent.devops.remotedev.pojo.strategy.ProjectStrategyFetchInfo
import com.tencent.devops.remotedev.pojo.strategy.ProjectStrategyInfo
import com.tencent.devops.remotedev.pojo.strategy.ProjectStrategyResp
import com.tencent.devops.remotedev.pojo.strategy.PublicDesktopLockScreenStrategy
import com.tencent.devops.remotedev.pojo.strategy.QuitOnExitStrategy
import com.tencent.devops.remotedev.pojo.strategy.SilentUpgradeStrategy
import com.tencent.devops.remotedev.pojo.strategy.StrategyType
import com.tencent.devops.remotedev.service.redis.ConfigCacheService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ProjectStrategyService @Autowired constructor(
    private val dslContext: DSLContext,
    private val projectStrategyDao: ProjectStrategyDao,
    private val permissionService: PermissionService,
    private val cosLogUploadService: CosLogUploadService,
    private val configCacheService: ConfigCacheService
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
                        (info.personalDesktopLockScreenStrategy ?: PersonalDesktopLockScreenStrategy.default())
                            .copy(globalLockScreenMin = null),
                        false
                    )
                )

                StrategyType.PUBLIC_DESKTOP_LOCK_SCREEN -> projectStrategyDao.createOrUpdateStrategy(
                    dslContext = dslContext,
                    projectId = info.projectId,
                    zoneType = info.zoneType,
                    strategyType = StrategyType.PUBLIC_DESKTOP_LOCK_SCREEN,
                    content = JsonUtil.toJson(
                        (info.publicDesktopLockScreenStrategy ?: PublicDesktopLockScreenStrategy.default())
                            .copy(globalLockScreenMin = null),
                        false
                    )
                )

                StrategyType.SILENT_UPGRADE -> projectStrategyDao.createOrUpdateStrategy(
                    dslContext = dslContext,
                    projectId = info.projectId,
                    zoneType = info.zoneType,
                    strategyType = StrategyType.SILENT_UPGRADE,
                    content = JsonUtil.toJson(
                        info.silentUpgradeStrategy ?: SilentUpgradeStrategy.default(), false
                    )
                )

                StrategyType.QUIT_ON_EXIT -> projectStrategyDao.createOrUpdateStrategy(
                    dslContext = dslContext,
                    projectId = info.projectId,
                    zoneType = info.zoneType,
                    strategyType = StrategyType.QUIT_ON_EXIT,
                    content = JsonUtil.toJson(
                        info.quitOnExitStrategy ?: QuitOnExitStrategy.default(), false
                    )
                )

                StrategyType.LOCK_SCREEN_SAVER -> {
                    val strategy = info.lockScreenSaverStrategy ?: LockScreenSaverStrategy.default()
                    if (strategy.enabled && strategy.path.isNullOrBlank()) {
                        throw ErrorCodeException(
                            errorCode = ErrorCodeEnum.BASE_ERROR.errorCode,
                            params = arrayOf("lockScreenSaverStrategy.path can not be empty when enabled")
                        )
                    }
                    projectStrategyDao.createOrUpdateStrategy(
                        dslContext = dslContext,
                        projectId = info.projectId,
                        zoneType = info.zoneType,
                        strategyType = StrategyType.LOCK_SCREEN_SAVER,
                        content = JsonUtil.toJson(strategy.copy(url = null), false)
                    )
                }
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

                StrategyType.SILENT_UPGRADE -> {
                    val content = JsonUtil.to(
                        record.strategyContent.data(),
                        object : TypeReference<SilentUpgradeStrategy>() {}
                    )
                    resp.silentUpgradeStrategy = content
                }

                StrategyType.QUIT_ON_EXIT -> {
                    val content = JsonUtil.to(
                        record.strategyContent.data(),
                        object : TypeReference<QuitOnExitStrategy>() {}
                    )
                    resp.quitOnExitStrategy = content
                }

                StrategyType.LOCK_SCREEN_SAVER -> {
                    val content = JsonUtil.to(
                        record.strategyContent.data(),
                        object : TypeReference<LockScreenSaverStrategy>() {}
                    )
                    val path = content.path
                    resp.lockScreenSaverStrategy = if (path.isNullOrBlank()) {
                        content
                    } else {
                        content.copy(url = cosLogUploadService.generateDownloadUrl(path))
                    }
                }
            }
        }
        fillGlobalLockScreenMin(info, resp)
        return resp
    }

    private fun fillGlobalLockScreenMin(info: ProjectStrategyFetchInfo, resp: ProjectStrategyResp) {
        val requested = info.typeList.filter { it in LOCK_SCREEN_STRATEGY_TYPES }
        if (requested.isEmpty()) {
            return
        }
        val globalLockScreenMin = readGlobalLockScreenMin()
        if (StrategyType.PERSONAL_DESKTOP_LOCK_SCREEN in requested) {
            val current = resp.personalDesktopLockScreenStrategy ?: PersonalDesktopLockScreenStrategy.default()
            resp.personalDesktopLockScreenStrategy = current.copy(globalLockScreenMin = globalLockScreenMin)
        }
        if (StrategyType.PUBLIC_DESKTOP_LOCK_SCREEN in requested) {
            val current = resp.publicDesktopLockScreenStrategy ?: PublicDesktopLockScreenStrategy.default()
            resp.publicDesktopLockScreenStrategy = current.copy(globalLockScreenMin = globalLockScreenMin)
        }
    }

    private fun readGlobalLockScreenMin(): Long? {
        val raw = configCacheService.get(GLOBAL_LOCK_SCREEN_MIN_KEY)?.trim()
        if (raw.isNullOrBlank()) {
            return null
        }
        val minutes = raw.toLongOrNull()
        if (minutes == null) {
            logger.warn("invalid global lock screen config|$GLOBAL_LOCK_SCREEN_MIN_KEY|$raw")
        }
        return minutes
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ProjectStrategyService::class.java)
        private const val GLOBAL_LOCK_SCREEN_MIN_KEY = "remotedev:globalLockScreenMin"
        private val LOCK_SCREEN_STRATEGY_TYPES = setOf(
            StrategyType.PERSONAL_DESKTOP_LOCK_SCREEN,
            StrategyType.PUBLIC_DESKTOP_LOCK_SCREEN
        )
    }
}