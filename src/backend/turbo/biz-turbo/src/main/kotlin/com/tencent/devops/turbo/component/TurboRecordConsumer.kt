package com.tencent.devops.turbo.component

import com.github.benmanes.caffeine.cache.Caffeine
import com.tencent.devops.common.util.JsonUtil
import com.tencent.devops.common.util.constants.codeccAdmin
import com.tencent.devops.turbo.dto.TurboRecordCreateDto
import com.tencent.devops.turbo.dto.TurboRecordPluginUpdateDto
import com.tencent.devops.turbo.dto.TurboRecordUpdateDto
import com.tencent.devops.turbo.enums.EnumDistccTaskStatus
import com.tencent.devops.turbo.enums.EnumEngineScene
import com.tencent.devops.turbo.model.TTurboEngineConfigEntity
import com.tencent.devops.turbo.sdk.TBSSdkApi
import com.tencent.devops.turbo.service.TurboDataSyncService
import com.tencent.devops.turbo.service.TurboEngineConfigService
import com.tencent.devops.turbo.service.TurboPlanService
import com.tencent.devops.turbo.service.TurboRecordService
import com.tencent.devops.turbo.service.TurboWorkStatService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Suppress("MaxLineLength")
@Component
class TurboRecordConsumer @Autowired constructor(
    private val turboEngineConfigService: TurboEngineConfigService,
    private val turboDataSyncService: TurboDataSyncService,
    private val turboPlanService: TurboPlanService,
    private val turboRecordService: TurboRecordService,
    private val turboWorkDataService: TurboWorkStatService
) {

    companion object {
        private val logger = LoggerFactory.getLogger(TurboRecordConsumer::class.java)
    }

    private val turboEngineConfigCache = Caffeine.newBuilder()
        .maximumSize(10000)
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build<String, TTurboEngineConfigEntity?> { engineCode ->
            turboEngineConfigService.findEngineConfigByEngineCode(
                engineCode
            )
        }

    /**
     * 创建记录消费逻辑
     */
    fun createSingleTurboRecord(turboRecordCreateDto: TurboRecordCreateDto) {
        val turboPlanId = (turboRecordCreateDto.dataMap["project_id"] as String?)?.substringBefore("_")
        try {
            val turboEngineConfig = turboEngineConfigCache[turboRecordCreateDto.engineCode]
            if (null == turboEngineConfig) {
                logger.info("[create turbo job|${turboRecordCreateDto.engineCode}|$turboPlanId] turbo engine config not found with code ${turboRecordCreateDto.engineCode}")
                return
            }
            logger.info("[create turbo job|${turboRecordCreateDto.engineCode}|$turboPlanId] create single turbo record!")
            turboDataSyncService.createTurboRecord(
                turboRecordCreateDto, turboEngineConfig
            )
            logger.info("[create turbo job|${turboRecordCreateDto.engineCode}|$turboPlanId] create turbo record and update stats finished!")
        } catch (e: Exception) {
            logger.info("[create turbo job|${turboRecordCreateDto.engineCode}|$turboPlanId] create turbo record fail! message: ${e.message}")
        }
    }

    /**
     * 更新记录消费逻辑
     */
    fun updateSingleTurboRecord(turboRecordUpdateDto: TurboRecordUpdateDto) {
        try {
            logger.info("[update turbo job|${turboRecordUpdateDto.engineCode}|${turboRecordUpdateDto.turboPlanId}] update single turbo record!")
            val newTurboRecordList = TBSSdkApi.queryTurboRecordInfo(
                engineCode = turboRecordUpdateDto.engineCode,
                queryParam = if (!turboRecordUpdateDto.tbsTurboRecordId.isNullOrBlank()) mapOf(
                    "task_id" to turboRecordUpdateDto.tbsTurboRecordId
                ) else if (!turboRecordUpdateDto.buildId.isNullOrBlank()) mapOf(
                    "build_id" to turboRecordUpdateDto.buildId
                ) else return
            )
            logger.info("[update turbo job|${turboRecordUpdateDto.engineCode}|${turboRecordUpdateDto.turboPlanId}] update single turbo record! new turbo record list size: ${newTurboRecordList.size}")
            val turboEngineConfig = turboEngineConfigCache[turboRecordUpdateDto.engineCode]
            if (null == turboEngineConfig) {
                logger.info("[update turbo job|${turboRecordUpdateDto.engineCode}|${turboRecordUpdateDto.turboPlanId}] turbo engine config not found")
                return
            }
            if (!newTurboRecordList.isNullOrEmpty()) {
                val newTurboData = JsonUtil.toMap(newTurboRecordList[0])
                turboDataSyncService.updateTurboRecord(
                    turboDataMap = newTurboData,
                    turboEngineConfig = turboEngineConfig,
                    turboRecordUpdateDto = turboRecordUpdateDto
                )
            } else {
                turboRecordService.updateRecordStatus(
                    tbsRecordId = turboRecordUpdateDto.tbsTurboRecordId,
                    buildId = turboRecordUpdateDto.buildId,
                    status = EnumDistccTaskStatus.FAILED.getTBSStatus(),
                    user = codeccAdmin
                )
            }
            logger.info("[update turbo job|${turboRecordUpdateDto.engineCode}|${turboRecordUpdateDto.turboPlanId}] update turbo record and update stats finished!")
        } catch (e: Exception) {
            e.printStackTrace()
            logger.info("[update turbo job|${turboRecordUpdateDto.engineCode}|${turboRecordUpdateDto.turboPlanId}] update turbo record fail! message: ${e.message}")
            turboRecordService.updateRecordStatus(
                turboRecordUpdateDto.tbsTurboRecordId, turboRecordUpdateDto.buildId,
                EnumDistccTaskStatus.FAILED.getTBSStatus(), codeccAdmin
            )
        }
    }

    /**
     * 刷新数据逻辑
     */
    fun updateTurboDateForRefresh(turboPlanId: String) {
        val turboPlanEntity = turboPlanService.findTurboPlanById(turboPlanId)
        if (null == turboPlanEntity || turboPlanEntity.id.isNullOrBlank()) {
            logger.info("no turbo plan found with id: $turboPlanId")
            return
        }
        val turboEngineConfig = turboEngineConfigCache[turboPlanEntity.engineCode]
        if (null == turboEngineConfig) {
            logger.info("no engine info found with code ${turboPlanEntity.engineCode}")
        }
        turboDataSyncService.refreshTurboRecord(turboPlanId, turboEngineConfig!!)
    }

    /**
     * 更新插件输出记录状态
     */
    fun updateSingleRecordForPlugin(turboRecordPluginUpdateDto: TurboRecordPluginUpdateDto) {
        logger.info("update single record for plugin, build id: ${turboRecordPluginUpdateDto.buildId}")
        turboRecordService.updateRecordStatusForPlugin(
            buildId = turboRecordPluginUpdateDto.buildId,
            status = EnumDistccTaskStatus.FAILED.getTBSStatus(),
            user = turboRecordPluginUpdateDto.user
        )
    }

    /**
     * 同步TBS编译加速统计数据
     */
    fun syncTbsWorkStatData(turboRecordId: String) {
        logger.info("sync TBS work stats data, turbo record id: $turboRecordId")
        try {
            // distcc无法同步WorkStat数据，更新场景后退出
            if (turboRecordId.startsWith(EnumEngineScene.DISTCC.regexStr())) {
                turboRecordService.updateRecordScene(turboRecordId, EnumEngineScene.DISTCC)
                return
            }

            turboWorkDataService.syncTbsWorkStatData(
                turboRecordId = turboRecordId
            )
        } catch (e: Exception) {
            logger.error("sync TBS work stats data failed! msg: ${e.message}", e)
        }
    }
}
