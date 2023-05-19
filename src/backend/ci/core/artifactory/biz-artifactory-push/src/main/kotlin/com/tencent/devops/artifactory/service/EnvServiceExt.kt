/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.artifactory.service

import com.tencent.devops.artifactory.constant.ArtifactoryMessageCode.ENV_MACHINE_NOT_AUTH
import com.tencent.devops.artifactory.constant.ArtifactoryMessageCode.ENV_NAME_MACHINE_NOT_EXITS
import com.tencent.devops.artifactory.constant.ArtifactoryMessageCode.FUSH_FILE_REMOTE_MACHINE_EMPTY
import com.tencent.devops.artifactory.constant.ArtifactoryMessageCode.NODE_NAME_MACHINE_NOT_EXITS
import com.tencent.devops.artifactory.pojo.EnvSet
import com.tencent.devops.artifactory.pojo.PushTypeEnum
import com.tencent.devops.artifactory.pojo.RemoteResourceInfo
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.utils.CommonUtils
import com.tencent.devops.environment.api.ServiceEnvironmentResource
import com.tencent.devops.environment.api.ServiceNodeResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
@Suppress("ALL")
class EnvServiceExt @Autowired constructor(
    private val client: Client
) {
    fun parsingAndValidateEnv(remoteResourceInfo: RemoteResourceInfo, userId: String, projectId: String): EnvSet {
        val pushType = remoteResourceInfo.pushType.name
        val targetMachine = remoteResourceInfo.targetMachine
        logger.info("PushJobFile|pushType[$pushType]targetMachine[$targetMachine]userId[$userId]projectId[$projectId]")
        val envSet = when (pushType) {
            PushTypeEnum.ENVId.name -> getRemoteInfoByEnvId(targetMachine)
            PushTypeEnum.NodeId.name -> getRemoteInfoByNodeId(targetMachine)
            PushTypeEnum.ENVName.name -> getRemoteInfoByEnvName(targetMachine, userId, projectId)
            else -> throw IllegalArgumentException("Bad type: $pushType")
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
            throw ErrorCodeException(
                errorCode = ENV_NAME_MACHINE_NOT_EXITS,
                params = arrayOf(noExistsEnvNames.toString())
            )
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
            throw ErrorCodeException(
                errorCode = ENV_MACHINE_NOT_AUTH,
                params = arrayOf(noAuthEnvIds.toString())
            )
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
                throw ErrorCodeException(
                    errorCode = ENV_NAME_MACHINE_NOT_EXITS,
                    params = arrayOf(noExistsEnvIds.toString())
                )
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
                throw ErrorCodeException(
                    errorCode = NODE_NAME_MACHINE_NOT_EXITS,
                    params = arrayOf(noExistsNodeIds.toString())
                )
            }
        }
    }

    fun buildIpDto(): List<EnvSet.IpDto> {
        val ip = EnvSet.IpDto(CommonUtils.getInnerIP())
        return listOf(ip)
    }

    private fun checkParams(str: String?): Boolean {
        if (str == null || str.isEmpty()) {
            throw ErrorCodeException(errorCode = FUSH_FILE_REMOTE_MACHINE_EMPTY)
        }
        return true
    }

    companion object {
        private val logger = LoggerFactory.getLogger(EnvServiceExt::class.java)
    }
}
