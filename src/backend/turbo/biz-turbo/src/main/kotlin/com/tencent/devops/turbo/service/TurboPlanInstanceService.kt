package com.tencent.devops.turbo.service

import com.google.common.base.CaseFormat
import com.tencent.devops.common.api.exception.TurboException
import com.tencent.devops.common.api.exception.code.TURBO_NO_DATA_FOUND
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.db.PageUtils
import com.tencent.devops.common.util.MathUtil
import com.tencent.devops.common.util.UUIDUtil
import com.tencent.devops.common.util.constants.codeccAdmin
import com.tencent.devops.turbo.dao.mongotemplate.TurboPlanInstanceDao
import com.tencent.devops.turbo.dao.repository.TurboPlanInstanceRepository
import com.tencent.devops.turbo.enums.EnumDistccTaskStatus
import com.tencent.devops.turbo.model.TTurboPlanInstanceEntity
import com.tencent.devops.turbo.vo.TurboPlanInstanceVO
import org.slf4j.LoggerFactory
import org.springframework.beans.BeanUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Suppress("NAME_SHADOWING", "MaxLineLength")
@Service
class TurboPlanInstanceService @Autowired constructor(
    private val turboPlanInstanceRepository: TurboPlanInstanceRepository,
    private val turboPlanInstanceDao: TurboPlanInstanceDao,
    private val turboRecordService: TurboRecordService,
    private val turboEngineConfigService: TurboEngineConfigService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(TurboPlanInstanceService::class.java)
    }

    /**
     * 通过id查询实例
     */
    fun findInstanceById(
        turboPlanInstanceId: String
    ): TTurboPlanInstanceEntity? {
        return turboPlanInstanceRepository.findByIdOrNull(turboPlanInstanceId)
    }

    /**
     * 根据方案id和客户端ip查找，如果没有则新增，如果有则返回
     */
    fun upsertInstanceByPlanIdAndIp(
        turboPlanId: String,
        projectId: String,
        clientIp: String
    ): Pair<TTurboPlanInstanceEntity?, Boolean> {
        var turboPlanInstanceEntity =
            turboPlanInstanceRepository.findFirstByTurboPlanIdAndClientIp(turboPlanId, clientIp)
        return if (null != turboPlanInstanceEntity && !turboPlanInstanceEntity.id.isNullOrBlank()) {
            Pair(turboPlanInstanceEntity, false)
        } else {
            turboPlanInstanceEntity = TTurboPlanInstanceEntity(
                turboPlanId = turboPlanId,
                projectId = projectId,
                clientIp = clientIp,
                updatedBy = codeccAdmin,
                updatedDate = LocalDateTime.now(),
                createdBy = codeccAdmin,
                createdDate = LocalDateTime.now()
            )
            val newTurboPlanInstanceEntity = turboPlanInstanceRepository.save(turboPlanInstanceEntity)
            Pair(newTurboPlanInstanceEntity, true)
        }
    }

    /**
     * 运行流水线时插入更新编译加速方案实例记录
     */
    fun upsertInstanceByPlanIdAndPipelineInfo(
        turboPlanId: String,
        engineCode: String,
        projectId: String,
        pipelineId: String,
        pipelineElementId: String,
        pipelineName: String?,
        devopsBuildId: String,
        userName: String
    ): String {
        logger.info("upsert plan instance, turbo plan id: $turboPlanId, pipeline id: $pipelineId, pipeline element id: $pipelineElementId")
        // 查询并插入编译加速实例
        var turboPlanInstanceEntity = turboPlanInstanceRepository.findByTurboPlanIdAndPipelineIdAndPipelineElementId(turboPlanId, pipelineId, pipelineElementId)
        if (null == turboPlanInstanceEntity || turboPlanInstanceEntity.id.isNullOrBlank()) {
            logger.info("need to insert new turbo plan instance entity, turbo plan id: $turboPlanId, pipeline id: $pipelineId, pipeline element id: $pipelineElementId")
            turboPlanInstanceEntity = TTurboPlanInstanceEntity(
                turboPlanId = turboPlanId,
                projectId = projectId,
                pipelineId = pipelineId,
                pipelineElementId = pipelineElementId,
                pipelineName = pipelineName,
                executeCount = 1,
                createdDate = LocalDateTime.now(),
                createdBy = userName,
                updatedDate = LocalDateTime.now(),
                updatedBy = userName
            )
            turboPlanInstanceEntity = turboPlanInstanceRepository.save(turboPlanInstanceEntity)
        } else {
            turboPlanInstanceDao.updateTurboPlanInstance(
                turboPlanInstanceId = turboPlanInstanceEntity.id!!,
                totalExecuteTime = null,
                totalEstimateTime = null,
                createFlag = true,
                startTime = LocalDateTime.now(),
                status = EnumDistccTaskStatus.STAGING.getTBSStatus()
            )
        }
        // 生成build_id，并插入编译加速记录
        val buildId = UUIDUtil.generate()
        val turboEngineConfigEntity = turboEngineConfigService.findEngineConfigByEngineCode(engineCode)
        if (null == turboEngineConfigEntity || turboEngineConfigEntity.id.isNullOrBlank()) {
            logger.info("no turbo engine config found with engine code: $engineCode")
            throw TurboException(errorCode = TURBO_NO_DATA_FOUND, errorMessage = "no turbo engine found")
        }
        turboRecordService.insertTurboReport(
            turboDataMap = mapOf(
                "build_id" to buildId,
                "start_time" to (System.currentTimeMillis() / 1000L).toInt(),
                "status" to EnumDistccTaskStatus.STAGING.getTBSStatus()
            ),
            engineCode = engineCode,
            turboPlanId = turboPlanId,
            projectId = projectId,
            turboPlanInstanceId = turboPlanInstanceEntity.id!!,
            spelExpression = turboEngineConfigEntity.spelExpression,
            spelParamMap = setOf(),
            pipelineId = pipelineId,
            pipelineElementId = pipelineElementId,
            pipelineName = pipelineName,
            devopsBuildId = devopsBuildId
        )
        logger.info("finish upsert turbo plan instance")
        return buildId
    }

    /**
     * 更新编译加速实例统计信息
     */
    fun updateStatInfo(
        turboPlanInstanceId: String,
        estimateTimeSecond: Long?,
        executeTimeSecond: Long?,
        startTime: LocalDateTime,
        status: String,
        createFlag: Boolean
    ) {
        if (status != EnumDistccTaskStatus.FINISH.getTBSStatus() && !createFlag) {
            logger.info("no need to update plan instance stat info!")
            return
        }

        turboPlanInstanceDao.updateTurboPlanInstance(
            turboPlanInstanceId = turboPlanInstanceId,
            totalEstimateTime = estimateTimeSecond,
            totalExecuteTime = executeTimeSecond,
            startTime = startTime,
            status = status,
            createFlag = createFlag
        )
    }

    /**
     * 获取 加速方案-列表页 表格数据
     */
    fun getTurboPlanInstanceList(turboPlanId: String, pageNum: Int?, pageSize: Int?, sortField: String?, sortType: String?): Page<TurboPlanInstanceVO> {
        val sortFieldInDb = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, sortField ?: "latestStartTime")
        val pageable = PageUtils.convertPageSizeToPageable(pageNum, pageSize, sortFieldInDb, sortType ?: "DESC")

        val turboPlanInstanceList = turboPlanInstanceDao.getTurboPlanInstanceList(turboPlanId, pageable)
        val planInstanceStatModelList = turboPlanInstanceList.records

        if (planInstanceStatModelList.isEmpty()) {
            return Page(0, 0, 0, listOf())
        }

        val dataList: List<TurboPlanInstanceVO> = planInstanceStatModelList.map {
            val turboPlanInstanceVO = TurboPlanInstanceVO()
            BeanUtils.copyProperties(it, turboPlanInstanceVO)
            turboPlanInstanceVO.averageExecuteTimeSecond = if (it.executeCount <= 0) 0L else (it.totalExecuteTimeSecond / it.executeCount)
            turboPlanInstanceVO.averageExecuteTimeValue = "${turboPlanInstanceVO.averageExecuteTimeSecond / 3600L}h " +
                "${(turboPlanInstanceVO.averageExecuteTimeSecond % 3600L) / 60L}m ${turboPlanInstanceVO.averageExecuteTimeSecond % 60L}s"
            turboPlanInstanceVO.averageEstimateTimeSecond = if (it.executeCount <= 0) 0L else (it.totalEstimateTimeSecond / it.executeCount)
            turboPlanInstanceVO.turboRatio = if (it.totalEstimateTimeSecond <= 0L) "--" else "${MathUtil.roundToTwoDigits(
                (
                    it.totalEstimateTimeSecond -
                        it.totalExecuteTimeSecond
                    ).toDouble() * 100 / it.totalEstimateTimeSecond.toDouble()
            )}%"
            turboPlanInstanceVO
        }

        return Page(
            turboPlanInstanceList.count, turboPlanInstanceList.page, turboPlanInstanceList.pageSize, turboPlanInstanceList.totalPages,
            dataList
        )
    }

    /**
     * 查询编译加速方案唯一对应的实例id
     */
    fun findFirstByTurboPlanId(turboPlanId: String): Pair<TTurboPlanInstanceEntity?, Boolean> {
        val turboPlanInstanceList = turboPlanInstanceRepository.findByTurboPlanId(turboPlanId)
        if (turboPlanInstanceList.isNullOrEmpty()) {
            return Pair(null, false)
        }
        if (turboPlanInstanceList.size > 1) {
            turboPlanInstanceRepository.deleteAll(turboPlanInstanceList.subList(1, turboPlanInstanceList.size - 1))
        }
        return Pair(turboPlanInstanceList[0], false)
    }

    /**
     * 查询条件拉取框流水线信息
     */
    fun findPipelineInfoByProjectId(projectId: String): List<TTurboPlanInstanceEntity> {
        return turboPlanInstanceRepository.findByProjectId(projectId)
    }

    /**
     * 通过项目id和流水线信息查找
     */
    fun findByProjectIdAndPipelineInfo(projectId: String, pipelineId: String, pipelineElementId: String): TTurboPlanInstanceEntity? {
        return turboPlanInstanceRepository.findByProjectIdAndPipelineIdAndPipelineElementId(projectId, pipelineId, pipelineElementId)
    }

    // ///////////////////////////以下逻辑为数据刷新逻辑，刷新后需要去除////////////////////////////////
    /**
     * 通过编译加速方案id查询编译加速实例
     */
    fun findByTurboPlanId(turboPlanId: String): List<TTurboPlanInstanceEntity> {
        return turboPlanInstanceRepository.findByTurboPlanId(turboPlanId)
    }

    /**
     * 更新评估时间
     */
    fun updateEstimateTime(
        turboPlanInstanceId: String,
        estimateTime: Long
    ) {
        turboPlanInstanceDao.updateEstimateTime(turboPlanInstanceId, estimateTime)
    }
}
