package com.tencent.devops.environment.service

import com.tencent.devops.common.api.pojo.OS
import com.tencent.devops.environment.pojo.EnvCreateInfo
import com.tencent.devops.environment.pojo.EnvUpdateInfo
import com.tencent.devops.environment.pojo.EnvWithNodeCount
import com.tencent.devops.environment.pojo.EnvWithPermission
import com.tencent.devops.environment.pojo.EnvironmentId
import com.tencent.devops.environment.pojo.NodeBaseInfo
import com.tencent.devops.environment.pojo.enums.EnvType

interface IEnvService {
    fun checkName(projectId: String, envId: Long?, envName: String)
    fun createEnvironment(userId: String, projectId: String, envCreateInfo: EnvCreateInfo): EnvironmentId
    fun updateEnvironment(userId: String, projectId: String, envHashId: String, envUpdateInfo: EnvUpdateInfo)
    fun listEnvironment(userId: String, projectId: String): List<EnvWithPermission>
    fun listUsableServerEnvs(userId: String, projectId: String): List<EnvWithPermission>
    fun listEnvironmentByType(userId: String, projectId: String, envType: EnvType): List<EnvWithNodeCount>
    fun listBuildEnvs(userId: String, projectId: String, os: OS): List<EnvWithNodeCount>
    fun getEnvironment(userId: String, projectId: String, envHashId: String): EnvWithPermission
    fun listRawEnvByHashIds(userId: String, projectId: String, envHashIds: List<String>): List<EnvWithPermission>
    fun listRawEnvByEnvNames(userId: String, projectId: String, envNames: List<String>): List<EnvWithPermission>
    fun deleteEnvironment(userId: String, projectId: String, envHashId: String)
    fun listRawServerNodeByEnvHashIds(
        userId: String,
        projectId: String,
        envHashIds: List<String>
    ): Map<String, List<NodeBaseInfo>>

    fun listAllEnvNodes(userId: String, projectId: String, envHashIds: List<String>): List<NodeBaseInfo>
    fun addEnvNodes(userId: String, projectId: String, envHashId: String, nodeHashIds: List<String>)
    fun deleteEnvNodes(userId: String, projectId: String, envHashId: String, nodeHashIds: List<String>)
}