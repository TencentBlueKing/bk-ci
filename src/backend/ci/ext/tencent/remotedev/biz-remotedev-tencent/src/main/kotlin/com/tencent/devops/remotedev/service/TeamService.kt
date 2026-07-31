package com.tencent.devops.remotedev.service

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.remotedev.common.exception.ErrorCodeEnum
import com.tencent.devops.remotedev.dao.WorkspaceDao
import com.tencent.devops.remotedev.dispatch.kubernetes.dao.DispatchWorkspaceDao
import com.tencent.devops.remotedev.pojo.TeamInfo
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

// 开黑相关的逻辑
@Service
class TeamService @Autowired constructor(
    private val dslContext: DSLContext,
    private val redisOperation: RedisOperation,
    private val workspaceDao: WorkspaceDao,
    private val dispatchWorkspaceDao: DispatchWorkspaceDao
) {
    fun hasEnableTeam(workspaceName: String): Boolean {
        return redisOperation.hasKey(genTeamInfoRedisKey(workspaceName))
    }

    fun getTeamInfo(userId: String, projectId: String, workspaceName: String?, envId: String?): TeamInfo? {
        // TODO: 待确认补充鉴权
        if (workspaceName == null && envId == null) {
            return null
        }
        val workspaceRealName =
            workspaceName ?: dispatchWorkspaceDao.getWorkspaceNameByEnvId(envId!!, dslContext) ?: return null
        return getTeamInfo(workspaceRealName)
    }

    fun getTeamInfo(workspaceName: String?, envId: String?): TeamInfo? {
        if (workspaceName == null && envId == null) {
            return null
        }
        val workspaceRealName =
            workspaceName ?: dispatchWorkspaceDao.getWorkspaceNameByEnvId(envId!!, dslContext) ?: return null
        return getTeamInfo(workspaceRealName)
    }

    fun getTeamInfo(workspaceName: String): TeamInfo? {
        workspaceDao.fetchAnyWorkspace(dslContext, workspaceName = workspaceName) ?: throw ErrorCodeException(
            errorCode = ErrorCodeEnum.WORKSPACE_NOT_FIND.errorCode,
            params = arrayOf(workspaceName)
        )
        val infoRecord = redisOperation.get(genTeamInfoRedisKey(workspaceName)) ?: return null
        return JsonUtil.to(infoRecord, object : TypeReference<TeamInfo>() {})
    }

    fun enableTeam(
        userId: String,
        projectId: String,
        workspaceName: String?,
        envId: String?,
        enable: Boolean
    ): TeamInfo? {
        // TODO: 待确认补充鉴权
        if (workspaceName == null && envId == null) {
            return null
        }
        val workspaceRealName =
            workspaceName ?: dispatchWorkspaceDao.getWorkspaceNameByEnvId(envId!!, dslContext) ?: return null
        return enableTeam(workspaceRealName, enable)
    }

    fun enableTeam(workspaceName: String?, envId: String?, enable: Boolean): TeamInfo? {
        if (workspaceName == null && envId == null) {
            return null
        }
        val workspaceRealName =
            workspaceName ?: dispatchWorkspaceDao.getWorkspaceNameByEnvId(envId!!, dslContext) ?: return null
        return enableTeam(workspaceRealName, enable)
    }

    fun enableTeam(workspaceName: String, enable: Boolean): TeamInfo? {
        if (enable) {
            workspaceDao.fetchAnyWorkspace(dslContext, workspaceName = workspaceName) ?: throw ErrorCodeException(
                errorCode = ErrorCodeEnum.WORKSPACE_NOT_FIND.errorCode,
                params = arrayOf(workspaceName)
            )
            val infoRecord = redisOperation.get(genTeamInfoRedisKey(workspaceName))
            if (infoRecord != null) {
                return JsonUtil.to(infoRecord, object : TypeReference<TeamInfo>() {})
            }
            val info = TeamInfo(
                roomId = UUIDUtil.generate()
            )
            redisOperation.set(genTeamInfoRedisKey(workspaceName), JsonUtil.toJson(info))
            return info
        } else {
            redisOperation.delete(genTeamInfoRedisKey(workspaceName))
            return null
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TeamService::class.java)
        private const val TEAM_INFO_REDIS_KEY = "remotedev:team:info"
        private fun genTeamInfoRedisKey(workspaceName: String) = "$TEAM_INFO_REDIS_KEY:$workspaceName"
    }
}