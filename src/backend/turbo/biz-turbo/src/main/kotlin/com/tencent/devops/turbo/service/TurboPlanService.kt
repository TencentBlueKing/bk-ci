package com.tencent.devops.turbo.service

import com.tencent.devops.common.api.exception.TurboException
import com.tencent.devops.common.api.exception.code.TURBO_NO_DATA_FOUND
import com.tencent.devops.common.api.exception.code.TURBO_PARAM_INVALID
import com.tencent.devops.common.api.exception.code.TURBO_THIRDPARTY_SYSTEM_FAIL
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.db.PageUtils
import com.tencent.devops.common.util.JsonUtil
import com.tencent.devops.common.util.MathUtil
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.turbo.dao.mongotemplate.TurboPlanDao
import com.tencent.devops.turbo.dao.repository.TurboPlanRepository
import com.tencent.devops.turbo.dto.DistccRequestBody
import com.tencent.devops.turbo.dto.WhiteListDto
import com.tencent.devops.turbo.enums.EnumDistccTaskStatus
import com.tencent.devops.turbo.model.TTurboEngineConfigEntity
import com.tencent.devops.turbo.model.TTurboPlanEntity
import com.tencent.devops.turbo.pojo.TurboDaySummaryOverviewModel
import com.tencent.devops.turbo.pojo.TurboPlanInstanceModel
import com.tencent.devops.turbo.pojo.TurboPlanModel
import com.tencent.devops.turbo.sdk.TBSSdkApi
import com.tencent.devops.turbo.vo.TurboMigratedPlanVO
import com.tencent.devops.turbo.vo.TurboPlanDetailVO
import com.tencent.devops.turbo.vo.TurboPlanPageVO
import com.tencent.devops.turbo.vo.TurboPlanStatRowVO
import org.slf4j.LoggerFactory
import org.springframework.beans.BeanUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

@Suppress("MaxLineLength", "ComplexMethod", "NestedBlockDepth", "SpringJavaInjectionPointsAutowiringInspection")
@Service
class TurboPlanService @Autowired constructor(
    private val turboPlanDao: TurboPlanDao,
    private val turboPlanRepository: TurboPlanRepository,
    private val turboPlanInstanceService: TurboPlanInstanceService,
    private val turboEngineConfigService: TurboEngineConfigService,
    private val serviceProjectResource: ServiceProjectResource
) {
    companion object {
        private val logger = LoggerFactory.getLogger(TurboPlanService::class.java)
    }

    /**
     * 通过主键id查找编译加速方案
     */
    fun findTurboPlanById(turboPlanId: String): TTurboPlanEntity? {
        return turboPlanRepository.findByIdOrNull(turboPlanId)
    }

    /**
     * 根据id增加实例和执行次数
     */
    fun addInstanceAndCountById(
        turboPlanInstanceModel: TurboPlanInstanceModel,
        user: String
    ): String {
        // 1.先通过方案表查询出方案
        val turboPlanEntity = turboPlanDao.incInstanceAndCountById(turboPlanInstanceModel.turboPlanId!!)
        if (turboPlanEntity?.openStatus == null || !turboPlanEntity.openStatus!!) {
            logger.info("no turbo plan found with id: ${turboPlanInstanceModel.turboPlanId}")
            throw TurboException(errorCode = TURBO_NO_DATA_FOUND, errorMessage = "no turbo plan found")
        }
        return with(turboPlanInstanceModel) {
            turboPlanInstanceService.upsertInstanceByPlanIdAndPipelineInfo(
                turboPlanId = turboPlanId!!,
                engineCode = turboPlanEntity.engineCode,
                projectId = turboPlanEntity.projectId,
                pipelineId = pipelineId!!,
                pipelineElementId = pipelineElementId!!,
                pipelineName = pipelineName,
                devopsBuildId = buildId!!,
                userName = user
            )
        }
    }

    /**
     * 关联加速实例清单加1
     */
    fun addTurboPlanInstance(turboPlanId: String) {
        turboPlanDao.incrInstanceNum(turboPlanId)
    }

    /**
     * 更新统计信息
     */
    fun updateStatInfo(
        turboPlanId: String,
        estimateTimeHour: Double?,
        executeTimeHour: Double?,
        status: String,
        createFlag: Boolean
    ) {
        if (status != EnumDistccTaskStatus.FINISH.getTBSStatus() && !createFlag) {
            logger.info("no need to update plan instance stat info!")
            return
        }
        turboPlanDao.updateTurboPlan(
            turboPlanId = turboPlanId,
            estimateTimeHour = estimateTimeHour,
            executeTimeHour = executeTimeHour,
            createFlag = createFlag
        )
    }

    /**
     * 新增加速方案
     */
    @Transactional
    fun addNewTurboPlan(turboPlanModel: TurboPlanModel, user: String): String? {
        logger.info("add turbo plan, engine code: ${turboPlanModel.engineCode}, plan name: ${turboPlanModel.planName}")
        var turboPlanEntity: TTurboPlanEntity? = null
        with(turboPlanModel) {
            val turboEngineConfigEntity = turboEngineConfigService.findEngineConfigByEngineCode(engineCode!!)
            if (configParam.isNullOrEmpty() || null == turboEngineConfigEntity) {
                logger.info("config param or turbo engine config is emtpy!")
                throw TurboException(errorCode = TURBO_PARAM_INVALID, errorMessage = "turbo param is empty!")
            }
            // 1. 判断是否存在有同名的编译加速方案
            if (turboPlanRepository.existsByProjectIdAndPlanName(projectId!!, planName!!)) {
                logger.info("same plan name already exists, project id: $projectId, plan name: $planName")
                throw TurboException(
                    errorCode = TURBO_PARAM_INVALID,
                    errorMessage = "plan name already exists"
                )
            }

            // 2. 通过projectId获取组织架构信息
            val projectVO = try {
                if (!turboPlanModel.projectId.isNullOrBlank()) {
                    val projectResult = serviceProjectResource.get(turboPlanModel.projectId!!)
                    if (projectResult.isOk() && projectResult.data != null) {
                        projectResult.data
                    } else {
                        null
                    }
                } else {
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                logger.info("get project info failed, message: ${e.message}")
                null
            }

            // 2. 保存记录

            turboPlanEntity = TTurboPlanEntity(
                projectId = projectId ?: "",
                planName = planName ?: "",
                engineCode = engineCode ?: "",
                engineName = turboEngineConfigEntity.engineName,
                desc = desc ?: "",
                configParam = configParam,
                whiteList = "0.0.0.0",
                openStatus = openStatus,
                bgId = projectVO?.bgId,
                bgName = projectVO?.bgName,
                deptId = projectVO?.deptId,
                deptName = projectVO?.deptName,
                centerId = projectVO?.centerId,
                centerName = projectVO?.centerName,
                updatedBy = user,
                updatedDate = LocalDateTime.now(),
                createdBy = user,
                createdDate = LocalDateTime.now()
            )
            turboPlanEntity = turboPlanRepository.save(turboPlanEntity!!)
            // 3. 调用api,同步信息
            updateConfigParamByApi(
                turboEngineConfigEntity = turboEngineConfigEntity,
                turboPlanEntity = turboPlanEntity!!,
                configParam = configParam,
                user = user,
                updateWhiteList = true
            )
        }
        logger.info("add turbo plan successfully!")
        return turboPlanEntity?.id
    }

    /**
     * op系统更新编译加速方案信息
     */
    fun updateTurboPlanInfo(turboPlanModel: TurboPlanModel, planId: String, user: String): Boolean {
        logger.info("update turbo plan info by op system, plan id: $planId")
        with(turboPlanModel) {
            val newTurboPlanEntity = turboPlanDao.updateTurboPlanConfig(
                turboPlanId = planId,
                planName = planName!!,
                desc = desc,
                configParam = configParam,
                openStatus = openStatus,
                user = user
            )
            if (null == newTurboPlanEntity) {
                logger.info("no turbo plan entity updated with id $planId")
                return false
            }
            // 调用api，同步信息
            val turboEngineConfigEntity = turboEngineConfigService.findEngineConfigByEngineCode(engineCode!!)
            if (configParam.isNullOrEmpty() || null == turboEngineConfigEntity) {
                logger.info("config param or turbo engine config is emtpy!")
                return false
            }
            updateConfigParamByApi(
                turboEngineConfigEntity = turboEngineConfigEntity,
                turboPlanEntity = newTurboPlanEntity,
                configParam = configParam,
                user = user,
                updateWhiteList = false
            )
        }
        logger.info("update turbo plan successfully!")
        return true
    }

    /**
     * 将配置通过api同步至加速后端
     */
    private fun updateConfigParamByApi(
        turboEngineConfigEntity: TTurboEngineConfigEntity,
        turboPlanEntity: TTurboPlanEntity,
        configParam: Map<String, Any>?,
        user: String,
        updateWhiteList: Boolean = true
    ) {

        val tbsJsonMap = mutableMapOf<String, Any?>()
        tbsJsonMap["project_name"] = turboPlanEntity.planName
        tbsJsonMap["ban_all_booster"] = !(turboPlanEntity.openStatus ?: true)
        val paramConfigMap = turboEngineConfigEntity.paramConfig?.associateBy { it.paramKey }
        if (!paramConfigMap.isNullOrEmpty()) {
            paramConfigMap.forEach { (t, u) ->
                // 1. 如果配置不显示，且有默认值的，则加上默认值
                if (!u.displayed && null != u.defaultValue) {
                    tbsJsonMap[t] = u.defaultValue
                }
                /**
                 * 2. 如果配置显示的
                 *    如果参数是必传的，则需要进行判断
                 *    如果参数是非必传的，则有值才进行赋值
                 *    对于现实的参数，不从默认值中取
                 */
                if (u.displayed) {
                    if (null != u.required && u.required!!) {
                        if (configParam.isNullOrEmpty()) {
                            logger.info("config param is empty while param $t is required")
                            throw TurboException(errorCode = TURBO_PARAM_INVALID, errorMessage = "param $t is required")
                        }
                        val configParamValue = configParam[t]
                        if (null == configParamValue) {
                            logger.info("param value is empty while param $t is required")
                            throw TurboException(errorCode = TURBO_PARAM_INVALID, errorMessage = "param $t is required")
                        }
                        tbsJsonMap[t] = u.paramType.convertParamValue(configParamValue)
                    } else {
                        if (!configParam.isNullOrEmpty()) {
                            val configParamValue = configParam[t]
                            if (null != configParamValue) {
                                tbsJsonMap[t] = u.paramType.convertParamValue(configParamValue)
                            }
                        }
                    }
                }

                if (u.displayed && !configParam.isNullOrEmpty()) {
                    val configParamValue = configParam[t]
                    if (null != configParamValue) {
                        tbsJsonMap[t] = u.paramType.convertParamValue(configParamValue)
                    }
                }
            }
        }

        logger.info("assembled config param: $configParam, assembled result: $tbsJsonMap")
        try {
            // 更新项目信息
            TBSSdkApi.updateTurboProjectInfo(
                engineCode = turboEngineConfigEntity.engineCode,
                projectId = turboPlanEntity.id!!,
                jsonBody = JsonUtil.toJson(DistccRequestBody(user, tbsJsonMap))
            )
            // 更新白名单
            if (updateWhiteList) {
                TBSSdkApi.upsertTurboWhiteList(
                    engineCode = turboPlanEntity.engineCode,
                    user = user,
                    whiteListInfo = listOf(
                        WhiteListDto(
                            ip = "0.0.0.0",
                            projectId = turboPlanEntity.id!!
                        )
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            logger.info("submit tbs data fail! error message: ${e.message}")
            throw TurboException(errorCode = TURBO_THIRDPARTY_SYSTEM_FAIL, errorMessage = "同步数据至加速后端失败")
        }
    }

    /**
     * 根据planId获取加速方案详情页信息
     */
    fun getTurboPlanDetailByPlanId(planId: String): TurboPlanDetailVO {
        val turboPlanEntity = findTurboPlanById(planId)
        val turboPlanDetailVO = TurboPlanDetailVO()
        if (turboPlanEntity != null) {
            BeanUtils.copyProperties(turboPlanEntity, turboPlanDetailVO)
            turboPlanDetailVO.planId = planId
        }
        return turboPlanDetailVO
    }

    /**
     * 编辑加速方案名称和开启状态
     */
    fun putTurboPlanDetailNameAndOpenStatus(turboPlanModel: TurboPlanModel, planId: String, user: String): Boolean {
        with(turboPlanModel) {
            logger.info("update plan name and open status, plan id: $planId, plan name: $planName, status: $openStatus")
            // 1. 判断是否存在有同名的编译加速方案
            if (turboPlanRepository.existsByProjectIdAndPlanNameAndIdNot(projectId!!, planName!!, planId)) {
                logger.info("same plan name already exists, project id: $projectId, plan name: $planName")
                throw TurboException(
                    errorCode = TURBO_PARAM_INVALID,
                    errorMessage = "plan name already exists"
                )
            }
            // 2. 更新表信息
            val turboPlanEntity = turboPlanDao.updateTurboPlanDetailNameAndOpenStatus(
                planId = planId,
                planName = planName,
                openStatus = openStatus,
                desc = desc,
                user = user
            )
            if (null == turboPlanEntity) {
                logger.info("no turbo plan entity updated, id: $planId")
                return false
            }
            // 3. 同步至后端编译加速服务
            val tbsJsonMap = mapOf(
                "project_name" to planName,
                "ban_all_booster" to !(openStatus?:true)
            )
            try {
                // 创建项目信息
                TBSSdkApi.updateTurboProjectInfo(
                    engineCode = turboPlanEntity.engineCode,
                    projectId = planId,
                    jsonBody = JsonUtil.toJson(DistccRequestBody(user, tbsJsonMap))
                )
            } catch (e: Exception) {
                throw TurboException(errorCode = TURBO_THIRDPARTY_SYSTEM_FAIL, errorMessage = "同步数据至加速后端失败")
            }
            return true
        }
    }

    /**
     * 编辑配置参数
     */
    fun putTurboPlanConfigParam(turboPlanModel: TurboPlanModel, planId: String, user: String): Boolean {
        with(turboPlanModel) {
            logger.info("update turbo plan config param, plan id: $planId, config param: $configParam")
            // 1. 更新编译加速方案表信息
            val turboPlanEntity = turboPlanDao.updateTurboPlanConfigParam(planId, configParam, user)
            if (null == turboPlanEntity) {
                logger.info("no turbo plan entity updated, id: $planId")
                return false
            }
            // 2. 同步至后端平台
            val tbsJsonMap = mutableMapOf<String, Any?>()
            val turboEngineConfigEntity = turboEngineConfigService.findEngineConfigByEngineCode(turboPlanEntity.engineCode)
            if (null == turboEngineConfigEntity) {
                logger.info("no turbo engine config found with engine code: ${turboPlanEntity.engineCode}")
                return false
            }
            val paramConfigMap = turboEngineConfigEntity.paramConfig?.associate { it.paramKey to it.paramType }
            if (!configParam.isNullOrEmpty()) {
                configParam!!.forEach { (t, u) ->
                    tbsJsonMap[t] =
                        if (!paramConfigMap.isNullOrEmpty()) paramConfigMap[t]?.convertParamValue(u) else null
                }
            }
            logger.info("assembled json map is: $tbsJsonMap, plan id: $planId")
            try {
                TBSSdkApi.updateTurboProjectInfo(
                    engineCode = turboPlanEntity.engineCode,
                    projectId = planId,
                    jsonBody = JsonUtil.toJson(DistccRequestBody(user, tbsJsonMap))
                )
            } catch (e: Exception) {
                throw TurboException(errorCode = TURBO_THIRDPARTY_SYSTEM_FAIL, errorMessage = "同步数据至加速后端失败")
            }

            return true
        }
    }

    /**
     * 编辑IP白名单
     */
    @Deprecated(message = "no more white list should be set")
    fun putTurboPlanWhiteList(turboPlanModel: TurboPlanModel, planId: String, user: String): Boolean {
        logger.info("update turbo plan white list, plan id: $planId")
        return with(turboPlanModel) {
            turboPlanDao.updateTurboPlanWhiteList(planId, whiteList, user)
            try {
                TBSSdkApi.upsertTurboWhiteList(
                    engineCode = engineCode!!,
                    user = user,
                    whiteListInfo = listOf(
                        WhiteListDto(
                            ip = "0.0.0.0",
                            projectId = planId
                        )
                    )
                )
            } catch (e: Exception) {
                throw TurboException(errorCode = TURBO_THIRDPARTY_SYSTEM_FAIL, errorMessage = "同步数据至加速后端失败")
            }
            true
        }
    }

    /**
     * 获取 加速方案-列表页 方案清单数据
     */
    fun getTurboPlanStatRowData(projectId: String, pageNum: Int?, pageSize: Int?): TurboPlanPageVO {
        val pageable = PageUtils.convertPageWithMultiFields(pageNum, pageSize, arrayOf("open_status", "top_status", "updated_date"), "DESC")
        val turboPlanList = turboPlanDao.getTurboPlanStatRowData(projectId, pageable).map {
            TurboPlanStatRowVO(
                planId = it.id,
                planName = it.planName,
                engineCode = it.engineCode,
                engineName = it.engineName,
                instanceNum = it.instanceNum,
                executeCount = it.executeCount,
                estimateTimeHour = if (it.executeCount <= 0) "0.0" else MathUtil.roundToTwoDigits(it.estimateTimeHour / it.executeCount),
                executeTimeHour = if (it.executeCount <= 0) "0.0" else MathUtil.roundToTwoDigits(it.executeTimeHour / it.executeCount),
                topStatus = it.topStatus,
                turboRatio = if (it.estimateTimeHour <= 0.0) "--" else "${MathUtil.roundToTwoDigits(
                    (
                        it.estimateTimeHour -
                            it.executeTimeHour
                        ) * 100 / it.estimateTimeHour
                )}%",
                openStatus = it.openStatus
            )
        }
        val turboPlanCount = turboPlanDao.getTurboPlanCount(projectId)
        return TurboPlanPageVO(turboPlanList, turboPlanCount)
    }

    /**
     * 获取实例数和执行数
     */
    fun getInstanceNumAndExecuteCount(projectId: String): List<TurboDaySummaryOverviewModel> {
        return turboPlanDao.getInstanceNumAndExecuteCount(projectId)
    }

    /**
     * 编辑置顶状态
     */
    fun putTurboPlanTopStatus(planId: String, topStatus: String, user: String): Boolean {
        logger.info("update top status, plan id: $planId, top status: $topStatus")
        return try {
            turboPlanDao.updateTurboPlanTopStatus(planId, topStatus, user)
            true
        } catch (e: Exception) {
            logger.info("edit TopStatus error!")
            false
        }
    }

    /**
     * 获取方案id及方案名
     */
    fun getByProjectId(projectId: String): List<TTurboPlanEntity> {
        return turboPlanRepository.findByProjectId(projectId) ?: listOf()
    }

    /**
     * 获取可用的编译加速方案清单
     */
    fun getAvailableProjectIdList(projectId: String, pageNum: Int?, pageSize: Int?): Page<TurboPlanDetailVO> {
        val pageable = PageUtils.convertPageWithMultiFields(pageNum, pageSize, arrayOf("top_status", "updated_date"), "DESC")
        val turboPlanPageList = turboPlanRepository.findByProjectIdAndOpenStatusAndMigratedIn(projectId, true, listOf(false, null), pageable)
        if (null == turboPlanPageList || turboPlanPageList.content.isNullOrEmpty()) {
            return Page(0, 0, 0, listOf())
        }
        val turboPlanVOList = turboPlanPageList.content.map {
            TurboPlanDetailVO(
                planId = it.id!!,
                planName = it.planName,
                projectId = it.projectId,
                engineCode = it.engineCode,
                desc = it.desc ?: "",
                configParam = it.configParam,
                createdBy = it.createdBy,
                createdDate = it.createdDate,
                updatedBy = it.updatedBy,
                updatedDate = it.updatedDate
            )
        }
        return Page(pageable.pageNumber + 1, pageable.pageSize, 0, turboPlanVOList)
    }

    /**
     * 获取所有编译加速方案清单
     */
    fun getAllTurboPlanList(turboPlanId: String?, planName: String?, projectId: String?, pageNum: Int?, pageSize: Int?): Page<TurboPlanDetailVO> {
        val pageable = PageUtils.convertPageSizeToPageable(pageNum, pageSize, "created_date", "DESC")
        val turboPlanPage = turboPlanDao.getAllTurboPlanList(turboPlanId, planName, projectId, pageable)
        if (turboPlanPage.records.isNullOrEmpty()) {
            return Page(0, 0, 0, listOf())
        }

        val turboPlanList = turboPlanPage.records.map {
            TurboPlanDetailVO(
                planId = it.id!!,
                planName = it.planName,
                projectId = it.projectId,
                desc = it.desc,
                engineCode = it.engineCode,
                configParam = it.configParam,
                openStatus = it.openStatus ?: true,
                createdBy = it.createdBy,
                createdDate = it.createdDate,
                updatedBy = it.updatedBy,
                updatedDate = it.updatedDate
            )
        }
        return Page(
            turboPlanPage.count, turboPlanPage.page, turboPlanPage.pageSize, turboPlanPage.totalPages,
            turboPlanList
        )
    }

    /**
     * 通过流水线信息查找
     */
    fun findMigratedTurboPlanByPipelineInfo(projectId: String, pipelineId: String, pipelineElementId: String): TurboMigratedPlanVO? {
        val turboPlanInstanceEntity = turboPlanInstanceService.findByProjectIdAndPipelineInfo(projectId, pipelineId, pipelineElementId)
        if (null == turboPlanInstanceEntity || turboPlanInstanceEntity.id.isNullOrBlank()) {
            logger.info("no turbo plan instance found with project id: $projectId, pipeline id: $pipelineId and pipeline element id: $pipelineElementId")
            return null
        }
        val turboPlanEntity = turboPlanRepository.findByIdOrNull(turboPlanInstanceEntity.turboPlanId)
        if (turboPlanEntity?.migrated == null || !turboPlanEntity.migrated!!) {
            logger.info("no migrated turbo plan found with id: ${turboPlanInstanceEntity.turboPlanId}")
            return null
        }
        return TurboMigratedPlanVO(
            taskId = turboPlanEntity.id,
            taskName = turboPlanEntity.planName,
            projLang = turboPlanEntity.configParam?.get("proj_lang").toString(),
            banDistcc = turboPlanEntity.configParam?.get("ban_distcc").toString(),
            ccacheEnabled = turboPlanEntity.configParam?.get("ccache_enabled").toString(),
            gccVersion = turboPlanEntity.configParam?.get("gcc_version").toString(),
            toolType = turboPlanEntity.configParam?.get("tool_type").toString(),
            banAllBooster = turboPlanEntity.configParam?.get("ban_all_booster").toString()
        )
    }

    /**
     * 更新评估时间
     */
    fun updateEstimateTime(
        turboPlanId: String,
        estimateTime: Long
    ) {
        turboPlanDao.updateEstimateTime(turboPlanId, estimateTime)
    }

    /**
     * 通过蓝盾项目id查询
     */
    fun findTurboListByProjectId(
        projectId: String
    ): List<TTurboPlanEntity>? {
        return turboPlanRepository.findByProjectId(projectId)
    }

    /**
     * openApi 获取方案列表
     */
    fun getTurboPlanByProjectIdAndCreatedDate(projectId: String, startTime: LocalDate?, endTime: LocalDate?, pageNum: Int?, pageSize: Int?): Page<TurboPlanStatRowVO> {
        val pageable = PageUtils.convertPageWithMultiFields(pageNum, pageSize, arrayOf("open_status", "top_status", "updated_date"), "DESC")

        val turboPlanResult = turboPlanDao.getTurboPlanByProjectIdAndCreatedDate(projectId, startTime, endTime, pageable)
        val turboPlanList = turboPlanResult.records

        if (turboPlanList.isEmpty()) {
            return Page(0, 0, 0, listOf())
        }

        val turboPlanVOList = turboPlanList.map {
            TurboPlanStatRowVO(
                    planId = it.id,
                    planName = it.planName,
                    engineCode = it.engineCode,
                    engineName = it.engineName,
                    instanceNum = it.instanceNum,
                    executeCount = it.executeCount,
                    estimateTimeHour = if (it.executeCount <= 0) "0.0" else MathUtil.roundToTwoDigits(it.estimateTimeHour / it.executeCount),
                    executeTimeHour = if (it.executeCount <= 0) "0.0" else MathUtil.roundToTwoDigits(it.executeTimeHour / it.executeCount),
                    topStatus = it.topStatus,
                    turboRatio = if (it.estimateTimeHour <= 0.0) "--" else "${MathUtil.roundToTwoDigits(
                            (
                                    it.estimateTimeHour -
                                            it.executeTimeHour
                                    ) * 100 / it.estimateTimeHour
                    )}%",
                    openStatus = it.openStatus
            )
        }

        return Page(
            turboPlanResult.count, turboPlanResult.page, turboPlanResult.pageSize, turboPlanResult.totalPages,
            turboPlanVOList
        )
    }
}
