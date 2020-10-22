package com.tencent.bk.codecc.task.component

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
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
import java.util.*
import kotlin.collections.LinkedHashMap

@Component
class GongfengProjectChecker @Autowired constructor(
    private val openSourceTaskService: OpenSourceTaskService,
    private val gongfengPublicProjService: GongfengPublicProjService,
    private val taskDao: TaskDao,
    private val objectMapper: ObjectMapper
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
    fun check(checkType: TaskConstants.GongfengCheckType, gongfengPublicProjEntity: GongfengPublicProjEntity?, gongfengStatProjEntity: GongfengStatProjEntity?, taskInfo: TaskInfoEntity?): Int? {
        val conditionMap = LinkedHashMap<Int,Boolean>()
        // 任意方式都需要有统计信息
        conditionMap[ComConstants.OpenSourceDisableReason.NOGONGFENGSTAT.code] = null == gongfengStatProjEntity || gongfengStatProjEntity.id == null

        // 定时任务或api触发的开源项目
        // 检查是否可clone、项目存在且公开、未归档、非空项目
        if (null != gongfengPublicProjEntity){
            conditionMap[ComConstants.OpenSourceDisableReason.NOCLONE.code] = gongfengStatProjEntity?.publicVisibility != 100
            conditionMap[ComConstants.OpenSourceDisableReason.DELETEORPRIVATE.code] = deleteOrPrivate(gongfengPublicProjEntity)
            conditionMap[ComConstants.OpenSourceDisableReason.ARCHIVED.code] = null != gongfengPublicProjEntity.archived && gongfengPublicProjEntity.archived
            conditionMap[ComConstants.OpenSourceDisableReason.NOCOMMIT.code] = judgeEmptyProject(gongfengPublicProjEntity)
        }

        // api触发的闭源项目
        // 检查是否可clone、未归档
        if (checkType == TaskConstants.GongfengCheckType.CUSTOM_TRIGGER_TASK && null == gongfengPublicProjEntity){
            conditionMap[ComConstants.OpenSourceDisableReason.NOCLONE.code] = gongfengStatProjEntity?.publicVisibility != 100
            conditionMap[ComConstants.OpenSourceDisableReason.ARCHIVED.code] = null != gongfengStatProjEntity && gongfengStatProjEntity.archived
        }

        for ((disableReasonCode,conditionValue) in conditionMap) {
            if (conditionValue) {
                stopTask(taskInfo, disableReasonCode)
                return disableReasonCode
            }
        }

        //如果鉴权都通过，但是任务被停用了，则要重新启用
        if(null != taskInfo && taskInfo.status == TaskConstants.TaskStatus.DISABLE.value()){
            logger.info("task need to recover")
            startTask(taskInfo)
        }

        return null
    }

    private fun deleteOrPrivate(gongfengPublicProjEntity: GongfengPublicProjEntity): Boolean {
        try{
            val url = "$gitCodePath/api/v3/projects/${gongfengPublicProjEntity.id}"
            val result = OkhttpUtils.doGet(url, mapOf("PRIVATE-TOKEN" to gitPrivateToken!!))
            val newGongfengPublicProjEntity = JsonUtil.to(result, GongfengPublicProjEntity::class.java)
            if(gongfengPublicProjEntity.httpUrlToRepo != newGongfengPublicProjEntity.httpUrlToRepo){
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
        } catch (e : Exception){
            logger.info("no permission to this project, need to stop, error msg : ${e.message}")
            return true
        }
        return false
    }

    private fun stopTask(taskInfo: TaskInfoEntity?, disableReasonCode: Int) {
        val disableReason = when(disableReasonCode) {
            ComConstants.OpenSourceDisableReason.NOGONGFENGSTAT.code -> "no gongfeng stat"
            ComConstants.OpenSourceDisableReason.NOCLONE.code -> "no clone"
            ComConstants.OpenSourceDisableReason.DELETEORPRIVATE.code -> "delete or private"
            ComConstants.OpenSourceDisableReason.ARCHIVED.code -> "archived"
            ComConstants.OpenSourceDisableReason.NOCOMMIT.code -> "no commit"
            ComConstants.OpenSourceDisableReason.OWNERPROBLEM.code -> "owner problem"
            else -> "other reason"
        }
        if(null != taskInfo && null == taskInfo.opensourceDisableReason){
            logger.info("task {} disable reason is {}", taskInfo.taskId, disableReason)
            taskDao.updateOpenSourceDisableReason(disableReasonCode, taskInfo.taskId)
        }
        if(null != taskInfo && taskInfo.status == TaskConstants.TaskStatus.ENABLE.value()){
            openSourceTaskService.stopTask(taskInfo.taskId, disableReason, taskInfo.taskOwner[0]?:"codecc-admin")
        }
    }

    private fun startTask(taskInfo: TaskInfoEntity) {
        logger.info("task {} need to recover", taskInfo.taskId)
        if(null != taskInfo.opensourceDisableReason){
            taskInfo.opensourceDisableReason = null
            taskDao.updateOpenSourceDisableReason(null, taskInfo.taskId)
        }
        //因偶现的recover失败的场景，将主线程休眠一段时间确认更新
        Thread.sleep(1000L)
        openSourceTaskService.startTask(taskInfo.taskId, taskInfo.taskOwner[0]?:"codecc-admin")
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
                    objectMapper.readValue(result, object : TypeReference<List<CommitListInfo>>() {})
            return commitList.isNullOrEmpty()
        } catch (e: Exception) {
            logger.error("judge empty projects fail! gongfeng id: ${gongfengPublicProjEntity.id}", e)
            true
        }
    }
}