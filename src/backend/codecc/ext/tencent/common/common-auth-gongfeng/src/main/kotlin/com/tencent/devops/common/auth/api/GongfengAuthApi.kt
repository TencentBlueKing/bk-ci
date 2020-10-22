package com.tencent.devops.common.auth.api

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.auth.api.pojo.external.OwnerInfo
import com.tencent.devops.common.constant.RedisKeyConstants
import com.tencent.devops.common.util.JsonUtil
import com.tencent.devops.common.util.OkhttpUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.RedisTemplate

class GongfengAuthApi @Autowired constructor(
    private val redisTemplate: RedisTemplate<String, String>
){

    @Value("\${gongfeng.code.path:#{null}}")
    private val gitCodePath: String? = null

    @Value("\${gongfeng.code.privateToken:#{null}}")
    private val gitPrivateToken: String? = null

    companion object {
        private val logger = LoggerFactory.getLogger(GongfengAuthApi::class.java)
    }

    /**
     * 通过用户id和项目验证
     */
    fun authByUserIdAndProject(userId: String, projectId: Int): Boolean {
        var page = 1
        var dataSize: Int
        do {
            val url = "$gitCodePath/api/v3/projects/${projectId}/members/all?page=$page&per_page=1000"
            try {
                val result = OkhttpUtils.doGet(url, mapOf("PRIVATE-TOKEN" to gitPrivateToken!!))
                val memberList: List<OwnerInfo> =
                        JsonUtil.getObjectMapper().readValue(result, object : TypeReference<List<OwnerInfo>>() {})
                if (memberList.isNullOrEmpty()) {
                    return false
                }
                dataSize = memberList.size
                if (null != memberList.find { it.userName == userId }) {
                    return true
                }
                page++
            } catch (e: Exception) {
                logger.error("get project member info fail! user id: $userId, gongfeng id: $projectId")
                return false
            }
        } while (dataSize >= 100)
        return false
    }

    /**
     * bg维度判断管理员
     */
    fun authByBgLevelAdmin(userId: String, taskId : Long) : Boolean{
        try{
            logger.info("start to auth bg level admin! user id : $userId, task id : $taskId")
            val bgId = redisTemplate.opsForHash<String, String>().get(RedisKeyConstants.KEY_TASK_BG_MAPPING, taskId.toString())
            if(bgId.isNullOrBlank())
                return false
            val userList = redisTemplate.opsForHash<String, String>().get(RedisKeyConstants.KEY_USER_BG_ADMIN, bgId)?.split(";") ?: emptyList()
            return userList.contains(userId)
        } catch (e : Exception){
            logger.error("gongfeng scan bg admin authorization fail! user: $userId, task id : $taskId")
            return false
        }
    }

    /**
     * 根据项目成员清单设置owner信息
     */
    private fun gongfengAuthByProject(userId: String, projectId: Int): Boolean {
        var page = 1
        var dataSize: Int
        do {
            val url = "$gitCodePath/api/v3/projects/$projectId/members?page=$page&per_page=100"
            try {
                val result = OkhttpUtils.doGet(url, mapOf("PRIVATE-TOKEN" to gitPrivateToken!!))

                val ownerList: List<OwnerInfo> =
                    JsonUtil.getObjectMapper().readValue(result, object : TypeReference<List<OwnerInfo>>() {})
                if (ownerList.isNullOrEmpty()) {
                    break
                }
                dataSize = ownerList.size
                ownerList.forEach {
                    if (userId == it.userName && it.accessLevel!! >= 10)
                        return true
                }
                page++
            } catch (e: Exception) {
                logger.error("get project member list fail! project id: $projectId")
                return false
            }
        } while (dataSize >= 100)
        return false
    }

    /**
     * 根据项目组成员清单设置owner信息
     */
    private fun gongfengAuthByGroup(userId: String, groupId: Int): Boolean {
        var page = 1
        var dataSize: Int
        //1. 查项目owner
        do {
            try {
                val url = "$gitCodePath/api/v3/groups/$groupId/members?page=$page&per_page=100"
                val result = OkhttpUtils.doGet(url, mapOf("PRIVATE-TOKEN" to gitPrivateToken!!))
                val ownerList: List<OwnerInfo> =
                    JsonUtil.getObjectMapper().readValue(result, object : TypeReference<List<OwnerInfo>>() {})
                if (ownerList.isNullOrEmpty()) {
                    break
                }
                dataSize = ownerList.size
                ownerList.forEach {
                    if (userId == it.userName && it.accessLevel!! >= 10)
                        return true
                }
                page++
            } catch (e: Exception) {
                logger.error("get group member info fail! group id: $groupId")
                break
            }
        } while (dataSize >= 100)
        return false
    }
}