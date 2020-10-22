package com.tencent.bk.codecc.quartz.job

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.bk.codecc.quartz.pojo.QuartzJobContext
import com.tencent.devops.common.api.pojo.Response
import com.tencent.devops.common.api.pojo.TofDeptInfo
import com.tencent.devops.common.constant.ComConstants
import com.tencent.devops.common.constant.RedisKeyConstants
import com.tencent.devops.common.util.OkhttpUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.RedisTemplate


class RefreshAllOrgScheduleTask @Autowired constructor(
        private val objectMapper: ObjectMapper,
        private val redisTemplate: RedisTemplate<String, String>
) : IScheduleTask {

    companion object {
        private val logger = LoggerFactory.getLogger(RefreshAllOrgScheduleTask::class.java)
    }


    /**
     * 定时刷新公司组织架构信息
     */
    override fun executeTask(quartzJobContext: QuartzJobContext) {

        val jobCustomParam = quartzJobContext.jobCustomParam
        if (null == jobCustomParam) {
            logger.info("refresh org job custom param is null!")
            return
        }

        val rootPath = jobCustomParam["rootPath"] as String
        val appCode = jobCustomParam["appCode"] as String
        val appSecret = jobCustomParam["appSecret"] as String
        val operator = jobCustomParam["operator"] as String

        val url = "$rootPath/component/compapi/tof/get_child_dept_infos"
        val requestBody = mapOf(
                "app_code" to appCode,
                "app_secret" to appSecret,
                "operator" to operator,
                "parent_dept_id" to 0,
                "level" to 10
        )

        val result = OkhttpUtils.doHttpPost(url, objectMapper.writeValueAsString(requestBody))
        val organizeResult: Response<List<TofDeptInfo>> =
                objectMapper.readValue(result, object : TypeReference<Response<List<TofDeptInfo>>>() {})

        val organizationList = organizeResult.data
        if (organizationList.isNullOrEmpty()) {
            logger.error("GetAllOrgUnit fail!\n>>>code:{}\n>>>msg:{}", organizeResult.code, organizeResult.message)
            return
        }

        val deptTreeMap = hashMapOf<String, MutableList<String>>()
        val deptInfoMap = hashMapOf<String, String>()
        organizationList.forEach {
            val deptId = it.ID.toString()
            val deptName = it.Name
            val parentDeptId = it.ParentId

            deptInfoMap[deptId] = deptName

            /*var childDeptList = deptTreeMap[parentDeptId]
            if (null == childDeptList)
            {
                childDeptList = mutableListOf()
                deptTreeMap[parentDeptId] = childDeptList
            }
            childDeptList.add(deptId)*/
            deptTreeMap.computeIfAbsent(parentDeptId) { mutableListOf() }.add(deptId)
        }

        redisTemplate.opsForHash<String, String>().putAll(RedisKeyConstants.KEY_DEPT_INFOS, deptInfoMap)
        logger.info("refresh organization info success: {}", deptInfoMap.size)

        val deptTreeDbMap = hashMapOf<String, String>()
        deptTreeMap.entries.forEach {
            val deptId = it.key
            val childDeptList = it.value
            if (childDeptList.isNotEmpty()) {
                val sb = StringBuffer()
                childDeptList.forEach { childDeptId ->
                    sb.append(childDeptId).append(ComConstants.SEMICOLON)
                }

                deptTreeDbMap[deptId] = sb.toString()
            }
        }

        redisTemplate.opsForHash<String, String>().putAll(RedisKeyConstants.KEY_DEPT_TREE, deptTreeDbMap)
        logger.info("refresh organization tree success: {}", deptTreeDbMap.size)
    }
}