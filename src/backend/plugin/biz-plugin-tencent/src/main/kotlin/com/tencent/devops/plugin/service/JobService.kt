package com.tencent.devops.plugin.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.environment.api.ServiceEnvironmentResource
import com.tencent.devops.environment.api.ServiceNodeResource
import com.tencent.devops.environment.pojo.EnvWithPermission
import com.tencent.devops.environment.pojo.NodeBaseInfo
import com.tencent.devops.process.api.builds.BuildHistoryBuildResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * @Description 为插件【作业平台-执行脚本】提供build接口的实现
 * @Author Jsonwan
 * @Date 2019/8/19
 * @Version 1.0
 */
@Service
class JobService @Autowired constructor(
    val client: Client
) {
    companion object {
        private val logger = LoggerFactory.getLogger(JobService::class.java)
    }
    // 根据buildId查出构建启动人
    private fun getUserId(buildId: String): String? {
        logger.info("getUserId(buildId=$buildId)=")
        val historyBuildResult = client.get(BuildHistoryBuildResource::class).getSingleHistoryByBuildId(buildId)
        logger.info("userId=${historyBuildResult.data?.userId}|====end==getUserId====(buildId=$buildId)")
        return historyBuildResult.data?.userId
    }

    fun listUsableServerEnvs(projectId: String, buildId: String): Result<List<EnvWithPermission>> {
        logger.info("listUsableServerEnvs(projectId=$projectId,buildId=$buildId)=")
        val userId = getUserId(buildId) ?: return Result(500, "服务端内部异常，buildId=${buildId}的构建未查到")
        // 以启动人的身份调用service接口获取信息
        val result = client.get(ServiceEnvironmentResource::class).listUsableServerEnvs(userId, projectId)
        logger.info("listUsableServerEnvs==Return===\n${jacksonObjectMapper().writeValueAsString(result)}")
        return result
    }

    fun listRawByEnvHashIds(projectId: String, buildId: String, envHashIds: List<String>): Result<List<EnvWithPermission>> {
        logger.info("listRawByEnvHashIds(projectId=$projectId,buildId=$buildId,envHashIds=${envHashIds.reduce{s1,s2 -> "[$s1,$s2]"}})=")
        val userId = getUserId(buildId) ?: return Result(500, "服务端内部异常，buildId=${buildId}的构建未查到")
        val result = client.get(ServiceEnvironmentResource::class).listRawByEnvHashIds(userId, projectId, envHashIds)
        logger.info("listRawByEnvHashIds==Return===\n${jacksonObjectMapper().writeValueAsString(result)}")
        return result
    }

    fun listRawByEnvNames(projectId: String, buildId: String, envNames: List<String>): Result<List<EnvWithPermission>> {
        logger.info("listRawByEnvNames(projectId=$projectId,buildId=$buildId,envNames=${envNames.reduce{s1,s2 -> "[$s1,$s2]"}})=")
        val userId = getUserId(buildId) ?: return Result(500, "服务端内部异常，buildId=${buildId}的构建未查到")
        val result = client.get(ServiceEnvironmentResource::class).listRawByEnvNames(userId, projectId, envNames)
        logger.info("listRawByEnvNames==Return===\n${jacksonObjectMapper().writeValueAsString(result)}")
        return result
    }

    fun listRawNodesByHashIds(projectId: String, buildId: String, nodeHashIds: List<String>): Result<List<NodeBaseInfo>> {
        logger.info("listRawNodesByHashIds(projectId=$projectId,buildId=$buildId,nodeHashIds=${nodeHashIds.reduce{s1,s2 -> "[$s1,$s2]"}})=")
        val userId = getUserId(buildId) ?: return Result(500, "服务端内部异常，buildId=${buildId}的构建未查到")
        val result = client.get(ServiceNodeResource::class).listRawByHashIds(userId, projectId, nodeHashIds)
        logger.info("listRawNodesByHashIds==Return===\n${jacksonObjectMapper().writeValueAsString(result)}")
        return result
    }
}