package com.tencent.bk.codecc.task.component

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.bk.codecc.task.constant.TaskConstants
import com.tencent.bk.codecc.task.dao.mongotemplate.TaskDao
import com.tencent.bk.codecc.task.model.GongfengPublicProjEntity
import com.tencent.bk.codecc.task.model.GongfengStatProjEntity
import com.tencent.bk.codecc.task.model.TaskInfoEntity
import com.tencent.bk.codecc.task.pojo.CommitListInfo
import com.tencent.bk.codecc.task.service.GongfengPublicProjService
import com.tencent.bk.codecc.task.service.OpenSourceTaskService
import com.tencent.devops.common.constant.ComConstants
import com.tencent.devops.common.util.JsonUtil
import com.tencent.devops.common.util.OkhttpUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.Calendar
import java.util.Date

@Component
class GongfengProjectChecker @Autowired constructor(
    private val openSourceTaskService: OpenSourceTaskService,
    private val gongfengPublicProjService: GongfengPublicProjService,
    private val taskDao: TaskDao,
    private val checkConfigCache: GongfengCheckConfigCache
) {

    @Value("\${git.path:#{null}}")
    private val gitCodePath: String? = null

    @Value("\${codecc.privatetoken:#{null}}")
    private val gitPrivateToken: String? = null

    companion object {
        private val logger = LoggerFactory.getLogger(GongfengProjectChecker::class.java)
    }

    /**
     * 校验开源项目是否符合规范，如果符合，则返回空，如果不符合，则返回错误码
     */
    fun check(
        checkType: TaskConstants.GongfengCheckType,
        gongfengPublicProjEntity: GongfengPublicProjEntity?,
        gongfengStatProjEntity: GongfengStatProjEntity?,
        taskInfo: TaskInfoEntity?
    ): Int? {
        val conditionMap = LinkedHashMap<Int, Boolean>()
        // 任意方式都需要有统计信息
        conditionMap[ComConstants.OpenSourceDisableReason.NOGONGFENGSTAT.code] =
            null == gongfengStatProjEntity || gongfengStatProjEntity.id == null

        // 如果是oteam项目则统一不校验是否开源
        if (checkType == TaskConstants.GongfengCheckType.CUSTOM_TRIGGER_TASK) {
            conditionMap[ComConstants.OpenSourceDisableReason.NOCLONE.code] =
                gongfengStatProjEntity?.publicVisibility != 100
            conditionMap[ComConstants.OpenSourceDisableReason.ARCHIVED.code] =
                null != gongfengStatProjEntity && gongfengStatProjEntity.archived
        }

        // 如果是定时开源扫描的话则全部都要校验
        if (checkType == TaskConstants.GongfengCheckType.SCHEDULE_TASK) {
            conditionMap[ComConstants.OpenSourceDisableReason.NOCLONE.code] =
                gongfengStatProjEntity?.publicVisibility != 100
            conditionMap[ComConstants.OpenSourceDisableReason.DELETEORPRIVATE.code] =
                if (null == gongfengPublicProjEntity) true else deleteOrPrivate(gongfengPublicProjEntity)
            conditionMap[ComConstants.OpenSourceDisableReason.ARCHIVED.code] =
                (null != gongfengPublicProjEntity?.archived) && gongfengPublicProjEntity.archived
            conditionMap[ComConstants.OpenSourceDisableReason.NOCOMMIT.code] =
                if (null == gongfengPublicProjEntity) true else judgeEmptyProject(gongfengPublicProjEntity)
        }

        for ((disableReasonCode, conditionValue) in conditionMap) {
            if (conditionValue) {
                stopTask(taskInfo, disableReasonCode)
                return disableReasonCode
            }
        }

        // 如果鉴权都通过，但是任务被停用了，则要重新启用
        if (null != taskInfo && taskInfo.status == TaskConstants.TaskStatus.DISABLE.value()) {
            logger.info("task need to recover")
            startTask(taskInfo)
        }

        return null
    }

    /**
     * 工蜂扫描配置化检查，根据 BaseData 中 GONGFENG_CHECK_CONFIG 的配置跳过相应的检查流程
     * 没有配置的则继续检查是否符合开源扫描规范，如果符合，则返回空，如果不符合，则返回错误码
     *
     * @param checkType
     * @param gongfengPublicProjEntity
     * @param gongfengStatProjEntity
     * @param taskInfo
     */
    fun configurableCheck(
        checkType: TaskConstants.GongfengCheckType,
        gongfengPublicProjEntity: GongfengPublicProjEntity?,
        gongfengStatProjEntity: GongfengStatProjEntity?,
        taskInfo: TaskInfoEntity?
    ): Int? {
        val conditionMap = LinkedHashMap<Int, Boolean>()
        val configKeysMap = LinkedHashMap<String, String?>()
        // 从 projectId、代码库url、工蜂代码库数字ID、工蜂代码库namespace 四个维度配置检查
        configKeysMap["projectId"] = taskInfo?.projectId
        configKeysMap["url"] = gongfengStatProjEntity?.url ?: gongfengPublicProjEntity?.httpUrlToRepo
        configKeysMap["gongfengId"] = gongfengStatProjEntity?.id?.toString() ?: gongfengPublicProjEntity?.id?.toString()
        configKeysMap["namespace"] = gongfengStatProjEntity?.path ?: gongfengPublicProjEntity?.nameWithNameSpace

        val configValuesMap = LinkedHashMap<ComConstants.OpenSourceDisableReason, Map<String, List<String>>?>()
        configValuesMap[ComConstants.OpenSourceDisableReason.NOGONGFENGSTAT] =
            checkConfigCache.getConfigCache(ComConstants.OpenSourceDisableReason.NOGONGFENGSTAT)
        // 检查扫描任务是否有工蜂统计信息
        logger.info("check gongfeng stat info: $configValuesMap \n $configKeysMap")

        // CUSTOM 表项目检查 归档 和 clone，这两个操作都需要用到 工蜂stat 信息，所以配置跳过这两个检查时还需要跳过 工蜂stat 检查
        if (checkType == TaskConstants.GongfengCheckType.CUSTOM_TRIGGER_TASK) {
            configValuesMap[ComConstants.OpenSourceDisableReason.NOCLONE] =
                checkConfigCache.getConfigCache(ComConstants.OpenSourceDisableReason.NOCLONE)
            configValuesMap[ComConstants.OpenSourceDisableReason.ARCHIVED] =
                checkConfigCache.getConfigCache(ComConstants.OpenSourceDisableReason.ARCHIVED)
            check(
                conditionMap,
                configKeysMap,
                configValuesMap,
                gongfengPublicProjEntity,
                gongfengStatProjEntity,
                checkType
            )
        } else {
            configValuesMap[ComConstants.OpenSourceDisableReason.NOCLONE] =
                checkConfigCache.getConfigCache(ComConstants.OpenSourceDisableReason.NOCLONE)
            configValuesMap[ComConstants.OpenSourceDisableReason.ARCHIVED] =
                checkConfigCache.getConfigCache(ComConstants.OpenSourceDisableReason.ARCHIVED)
            configValuesMap[ComConstants.OpenSourceDisableReason.DELETEORPRIVATE] =
                checkConfigCache.getConfigCache(ComConstants.OpenSourceDisableReason.DELETEORPRIVATE)
            configValuesMap[ComConstants.OpenSourceDisableReason.NOCOMMIT] =
                checkConfigCache.getConfigCache(ComConstants.OpenSourceDisableReason.NOCOMMIT)

            check(
                conditionMap,
                configKeysMap,
                configValuesMap,
                gongfengPublicProjEntity,
                gongfengStatProjEntity,
                checkType
            )
        }

        for ((disableReasonCode, conditionValue) in conditionMap) {
            if (conditionValue) {
                stopTask(taskInfo, disableReasonCode)
                return disableReasonCode
            }
        }

        // 如果鉴权都通过，但是任务被停用了，则要重新启用
        if (null != taskInfo && taskInfo.status == TaskConstants.TaskStatus.DISABLE.value()) {
            logger.info("task need to recover")
            startTask(taskInfo)
        }

        return null
    }

    /**
     *
     * @param conditionMap 检查结果
     * @param configkeys 当前扫描任务的信息，用于和配置信息做匹配，匹配的话跳过检查
     * @param configValues 配置信息
     * @param reason 当前检查项
     * @param gongfengPublicProjEntity
     * @param gongfengStatProjEntity
     * @param checkType
     */
    private fun check(
        conditionMap: LinkedHashMap<Int, Boolean>,
        configkeys: Map<String, String?>,
        configValuesMap: Map<ComConstants.OpenSourceDisableReason, Map<String, List<String>>?>,
        gongfengPublicProjEntity: GongfengPublicProjEntity?,
        gongfengStatProjEntity: GongfengStatProjEntity?,
        checkType: TaskConstants.GongfengCheckType
    ) {
        logger.info("gongfeng project check: $checkType")
        configValuesMap.forEach reasonCheck@{ reason, configValues ->
            // 根据配置判断是否跳过检查
            if (!configValues.isNullOrEmpty()) {
                configkeys.forEach { (currKey, currValue) ->
                    if (null != configValues[currKey]?.find { it == currValue || currValue?.matches(Regex(it)) == true }) {
                        logger.info("gongfeng checker hit: $reason $configValues[currKey] $currValue")
                        return@reasonCheck
                    }
                }
            }

            when (reason) {
                ComConstants.OpenSourceDisableReason.NOGONGFENGSTAT -> conditionMap[ComConstants.OpenSourceDisableReason.NOGONGFENGSTAT.code] =
                    null == gongfengStatProjEntity || gongfengStatProjEntity.id == null

                ComConstants.OpenSourceDisableReason.NOCLONE ->
                    conditionMap[ComConstants.OpenSourceDisableReason.NOCLONE.code] =
                        gongfengStatProjEntity?.publicVisibility != 100

                ComConstants.OpenSourceDisableReason.DELETEORPRIVATE ->
                    conditionMap[ComConstants.OpenSourceDisableReason.DELETEORPRIVATE.code] =
                        if (null == gongfengPublicProjEntity) true else deleteOrPrivate(gongfengPublicProjEntity)

                ComConstants.OpenSourceDisableReason.ARCHIVED -> {
                    conditionMap[ComConstants.OpenSourceDisableReason.ARCHIVED.code] =
                        if (checkType == TaskConstants.GongfengCheckType.CUSTOM_TRIGGER_TASK) {
                            null != gongfengStatProjEntity && gongfengStatProjEntity.archived
                        } else {
                            (null != gongfengPublicProjEntity?.archived) && gongfengPublicProjEntity.archived
                        }
                }

                ComConstants.OpenSourceDisableReason.NOCOMMIT ->
                    conditionMap[ComConstants.OpenSourceDisableReason.NOCOMMIT.code] =
                        if (null == gongfengPublicProjEntity) true else judgeEmptyProject(gongfengPublicProjEntity)

                else -> {
                }
            }
        }
    }

    private fun deleteOrPrivate(gongfengPublicProjEntity: GongfengPublicProjEntity): Boolean {
        try {
            val url = "$gitCodePath/api/v3/projects/${gongfengPublicProjEntity.id}"
            val result = OkhttpUtils.doGet(url, mapOf("PRIVATE-TOKEN" to gitPrivateToken!!))
            val newGongfengPublicProjEntity = JsonUtil.to(result, GongfengPublicProjEntity::class.java)
            if (gongfengPublicProjEntity.httpUrlToRepo != newGongfengPublicProjEntity.httpUrlToRepo) {
                logger.info("need to update http url repo!")
                newGongfengPublicProjEntity.id = gongfengPublicProjEntity.id
                newGongfengPublicProjEntity.entityId = gongfengPublicProjEntity.entityId
                gongfengPublicProjEntity.httpUrlToRepo = newGongfengPublicProjEntity.httpUrlToRepo
                gongfengPublicProjEntity.httpsUrlToRepo = newGongfengPublicProjEntity.httpsUrlToRepo
                gongfengPublicProjEntity.webUrl = newGongfengPublicProjEntity.webUrl
                gongfengPublicProjEntity.defaultBranch = newGongfengPublicProjEntity.defaultBranch
                gongfengPublicProjEntity.archived = newGongfengPublicProjEntity.archived
                gongfengPublicProjService.saveProject(newGongfengPublicProjEntity)
            }
        } catch (e: Exception) {
            logger.info("no permission to this project, need to stop, error msg : ${e.message}")
            return true
        }
        return false
    }

    private fun stopTask(taskInfo: TaskInfoEntity?, disableReasonCode: Int) {
        val disableReason = when (disableReasonCode) {
            ComConstants.OpenSourceDisableReason.NOGONGFENGSTAT.code -> "no gongfeng stat"
            ComConstants.OpenSourceDisableReason.NOCLONE.code -> "no clone"
            ComConstants.OpenSourceDisableReason.DELETEORPRIVATE.code -> "delete or private"
            ComConstants.OpenSourceDisableReason.ARCHIVED.code -> "archived"
            ComConstants.OpenSourceDisableReason.NOCOMMIT.code -> "no commit"
            ComConstants.OpenSourceDisableReason.OWNERPROBLEM.code -> "owner problem"
            else -> "other reason"
        }
        if (null != taskInfo && (null == taskInfo.opensourceDisableReason || disableReasonCode != taskInfo.opensourceDisableReason)) {
            logger.info("task ${taskInfo.taskId} disable reason is $disableReason")
            taskDao.updateOpenSourceDisableReason(disableReasonCode, taskInfo.taskId)
        }
        if (null != taskInfo && taskInfo.status == TaskConstants.TaskStatus.ENABLE.value()) {
            openSourceTaskService.stopTask(taskInfo.taskId, disableReason, taskInfo.taskOwner[0] ?: "codecc-admin")
        }
    }

    fun startTask(taskInfo: TaskInfoEntity) {
        logger.info("task {} need to recover", taskInfo.taskId)
        if (null != taskInfo.opensourceDisableReason) {
            taskInfo.opensourceDisableReason = null
            taskDao.updateOpenSourceDisableReason(null, taskInfo.taskId)
        }
        // 因偶现的recover失败的场景，将主线程休眠一段时间确认更新
        Thread.sleep(1000L)
        openSourceTaskService.startTask(taskInfo.taskId, taskInfo.taskOwner[0] ?: "codecc-admin")
    }

    /**
     * 判断是否是空项目
     */
    private fun judgeEmptyProject(gongfengPublicProjEntity: GongfengPublicProjEntity): Boolean {
        return try {
            val monthAgoDate = Calendar.getInstance()
            monthAgoDate.time = Date()
            monthAgoDate.add(Calendar.YEAR, -20)
            val url =
                "$gitCodePath/api/v3/projects/${gongfengPublicProjEntity.id}/repository/commits?page=1&per_page=10"
            val result = OkhttpUtils.doGet(url, mapOf("PRIVATE-TOKEN" to gitPrivateToken!!))
            val commitList: List<CommitListInfo> =
                JsonUtil.getObjectMapper().readValue(result, object : TypeReference<List<CommitListInfo>>() {})
            return commitList.isNullOrEmpty()
        } catch (e: Exception) {
            logger.error("judge empty projects fail! gongfeng id: ${gongfengPublicProjEntity.id}", e)
            true
        }
    }
}
