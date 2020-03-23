package com.tencent.devops.artifactory.service

import com.tencent.devops.artifactory.constant.PushMessageCode
import com.tencent.devops.artifactory.pojo.EnvSet
import com.tencent.devops.artifactory.pojo.PushTypeEnum
import com.tencent.devops.artifactory.pojo.RemoteResourceInfo
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.utils.CommonUtils
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.environment.api.ServiceEnvironmentResource
import com.tencent.devops.environment.api.ServiceNodeResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.ArrayList

@Service
class EnvServiceExt @Autowired constructor(
    private val client: Client
) {
    fun parsingAndValidateEnv(remoteResourceInfo: RemoteResourceInfo, userId: String, projectId: String): EnvSet {
        val pushType = remoteResourceInfo.pushType.name
        val targetMachine = remoteResourceInfo.targetMachine
        logger.info("push file by Job: pushType[$pushType] targetMachine[$targetMachine] userId[$userId] projectId[$projectId]")
        val envSet = when (pushType) {
            PushTypeEnum.ENVId.name -> getRemoteInfoByEnvId(targetMachine)
            PushTypeEnum.NodeId.name -> getRemoteInfoByNodeId(targetMachine)
            PushTypeEnum.ENVName.name -> getRemoteInfoByEnvName(targetMachine, userId, projectId)
            else -> throw RuntimeException()
        }
        checkEnvNodeExists(userId, projectId, envSet)
        logger.info("push file by Job: envSet[$envSet]")
        return envSet
    }

    private fun getRemoteInfoByEnvId(targetMachine: String): EnvSet {
        checkParams(targetMachine)
        val envHashIds = targetMachine.split(",")
        val nodeHashIds: List<String> = ArrayList()
//        val ipLists: List<EnvSet.IpDto> = buildIpDto()
        return EnvSet(envHashIds, nodeHashIds, emptyList())
    }

    private fun getRemoteInfoByEnvName(targetMachine: String, userId: String, projectId: String): EnvSet {
        checkParams(targetMachine)
        val envNameList = targetMachine.split(",")
        val envList =
            client.get(ServiceEnvironmentResource::class).listRawByEnvNames(userId, projectId, envNameList).data
        val envNameExistsList = mutableListOf<String>()
        val envIdList = mutableListOf<String>()
        envList!!.forEach {
            envNameExistsList.add(it.name)
            envIdList.add(it.envHashId)
        }
        val noExistsEnvNames = envNameList.subtract(envNameExistsList)
        if (noExistsEnvNames.isNotEmpty()) {
            logger.warn("The envNames not exists, name:$noExistsEnvNames")
            throw RuntimeException(MessageCodeUtil.getCodeMessage(PushMessageCode.ENV_NAME_MACHINE_NOT_EXITS, arrayOf(noExistsEnvNames.toString())))
        }

        // 校验权限
        val userEnvList = client.get(ServiceEnvironmentResource::class).listUsableServerEnvs(userId, projectId).data
        val userEnvIdList = mutableListOf<String>()
        userEnvList!!.forEach {
            userEnvIdList.add(it.envHashId)
        }

        val noAuthEnvIds = envIdList.subtract(userEnvIdList)
        if (noAuthEnvIds.isNotEmpty()) {
            logger.warn("User does not permit to access the env: $noAuthEnvIds")
            throw RuntimeException(MessageCodeUtil.getCodeMessage(PushMessageCode.ENV_MACHINE_NOT_AUTH, arrayOf(noAuthEnvIds.toString())))
        }
        val nodeHashIds: List<String> = ArrayList()
//        val ipLists: List<EnvSet.IpDto> = buildIpDto()
        return EnvSet(envIdList, nodeHashIds, emptyList())
    }

    private fun getRemoteInfoByNodeId(targetMachine: String): EnvSet {
        checkParams(targetMachine)

        val nodeHashIds = targetMachine.split(",")
        val envHashIds: List<String> = ArrayList()
//        val ipLists: List<EnvSet.IpDto> = buildIpDto()
        return EnvSet(envHashIds, nodeHashIds, emptyList())
    }

    private fun checkEnvNodeExists(
        operator: String,
        projectId: String,
        envSet: EnvSet
    ) {
        if (envSet.envHashIds.isNotEmpty()) {
            val envList = client.get(ServiceEnvironmentResource::class)
                .listRawByEnvHashIds(operator, projectId, envSet.envHashIds).data
            val envIdList = mutableListOf<String>()
            envList!!.forEach {
                envIdList.add(it.envHashId)
            }
            val noExistsEnvIds = envSet.envHashIds.subtract(envIdList)
            if (noExistsEnvIds.isNotEmpty()) {
                logger.warn("The envIds not exists, id:$noExistsEnvIds")
                throw RuntimeException(MessageCodeUtil.getCodeMessage(PushMessageCode.ENV_NAME_MACHINE_NOT_EXITS, arrayOf(noExistsEnvIds.toString())))
            }
        }
        if (envSet.nodeHashIds.isNotEmpty()) {
            val nodeList =
                client.get(ServiceNodeResource::class).listRawByHashIds(operator, projectId, envSet.nodeHashIds).data
            val nodeIdList = mutableListOf<String>()
            nodeList!!.forEach {
                nodeIdList.add(it.nodeHashId)
            }
            val noExistsNodeIds = envSet.nodeHashIds.subtract(nodeIdList)
            if (noExistsNodeIds.isNotEmpty()) {
                logger.warn("The nodeIds not exists, id:$noExistsNodeIds")
                throw RuntimeException(MessageCodeUtil.getCodeMessage(PushMessageCode.NODE_NAME_MACHINE_NOT_EXITS, arrayOf(noExistsNodeIds.toString())))
            }
        }
    }

    fun buildIpDto(): List<EnvSet.IpDto> {
        val ip = EnvSet.IpDto(CommonUtils.getInnerIP())
        return listOf(ip)
    }

    private fun checkParams(str: String?): Boolean {
        if (str == null || str.isEmpty()) {
            throw RuntimeException(MessageCodeUtil.getCodeMessage(PushMessageCode.FUSH_FILE_REMOTE_MACHINE_EMPTY, null))
        }
        return true
    }

    companion object {
        val logger = LoggerFactory.getLogger(this::class.java)
    }
}