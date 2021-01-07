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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.store.service.atom.impl

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.enums.FrontendTypeEnum
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.process.api.service.ServicePipelineAtomResource
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.dao.atom.AtomDao
import com.tencent.devops.store.pojo.atom.AtomReplaceRequest
import com.tencent.devops.store.pojo.atom.enums.AtomStatusEnum
import com.tencent.devops.store.service.atom.AtomReplaceService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class AtomReplaceServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val atomDao: AtomDao,
    private val client: Client
) : AtomReplaceService {

    private val logger = LoggerFactory.getLogger(AtomReplaceServiceImpl::class.java)

    override fun replacePipelineAtom(
        userId: String,
        projectId: String?,
        atomReplaceRequest: AtomReplaceRequest
    ): Result<Boolean> {
        logger.info("replacePipelineAtom userId:$userId,projectId:$projectId,atomReplaceRequest:$atomReplaceRequest")
        val fromAtomCode = atomReplaceRequest.fromAtomCode
        val toAtomCode = atomReplaceRequest.toAtomCode
        val versionInfoList = atomReplaceRequest.versionInfoList
        versionInfoList.forEach { versionInfo ->
            // 根据插件的atomCode和version查出输入输出参数json串
            val fromAtomVersion = versionInfo.fromAtomVersion
            val atomStatusList = listOf(
                AtomStatusEnum.RELEASED.status.toByte(),
                AtomStatusEnum.UNDERCARRIAGING.status.toByte(),
                AtomStatusEnum.UNDERCARRIAGED.status.toByte()
            )
            val fromAtomRecord = atomDao.getPipelineAtom(
                dslContext = dslContext,
                atomCode = fromAtomCode,
                version = fromAtomVersion.replace("*", ""),
                atomStatusList = atomStatusList
            ) ?: throw ErrorCodeException(
                errorCode = CommonMessageCode.PARAMETER_IS_EXIST,
                params = arrayOf("$fromAtomCode:$fromAtomVersion")
            )
            val toAtomVersion = versionInfo.toAtomVersion
            val toAtomRecord = atomDao.getPipelineAtom(
                dslContext = dslContext,
                atomCode = toAtomCode,
                version = toAtomVersion.replace("*", ""),
                atomStatusList = listOf(AtomStatusEnum.RELEASED.status.toByte())
            ) ?: throw ErrorCodeException(
                errorCode = CommonMessageCode.PARAMETER_IS_EXIST,
                params = arrayOf("$fromAtomCode:$fromAtomVersion")
            )
            val fromAtomHtmlVersion = fromAtomRecord.htmlTemplateVersion
            val toAtomHtmlVersion = toAtomRecord.htmlTemplateVersion
            val paramInfoList = versionInfo.paramReplaceInfoList
            val fromAtomPropMap = JsonUtil.toMap(fromAtomRecord.props)
            val toAtomPropMap = JsonUtil.toMap(toAtomRecord.props)
            // 解析出插件的参数列表
            val fromAtomInputParamNameList = generateInputParamNameList(fromAtomHtmlVersion, fromAtomPropMap)
            val toAtomInputParamNameList = generateInputParamNameList(toAtomHtmlVersion, toAtomPropMap)
            val invalidParamNameList = mutableListOf<String>()
            toAtomInputParamNameList?.forEach toAtomLoop@{ toAtomParamName ->
                var validFlag = false
                paramInfoList?.forEach { paramReplaceInfo ->
                    if (paramReplaceInfo.toParamName == toAtomParamName) {
                        validFlag = true
                        return@toAtomLoop
                    }
                }
                fromAtomInputParamNameList?.forEach { fromAtomParamName ->
                    if (fromAtomParamName == toAtomParamName) {
                        validFlag = true
                        return@toAtomLoop
                    }
                }
                if (!validFlag) {
                    invalidParamNameList.add(toAtomParamName)
                }
            }
            if (invalidParamNameList.isNotEmpty()) {
                throw ErrorCodeException(
                    errorCode = StoreMessageCode.USER_ATOM_IS_NOT_ALLOW_REPLACE,
                    params = arrayOf(
                        fromAtomRecord.name,
                        fromAtomVersion,
                        toAtomRecord.name,
                        toAtomHtmlVersion,
                        JsonUtil.toJson(invalidParamNameList)
                    )
                )
            }
        }
        return client.get(ServicePipelineAtomResource::class).createReplaceAtomInfo(
            userId = userId,
            projectId = projectId,
            atomReplaceRequest = atomReplaceRequest
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun generateInputParamNameList(
        atomHtmlVersion: String?,
        atomPropMap: Map<String, Any>
    ) : List<String>? {
        return if (atomHtmlVersion == FrontendTypeEnum.HISTORY.typeVersion) {
            atomPropMap.map { it.key }
        } else {
            val inputParamMap = atomPropMap["input"] as? Map<String, Any>
            inputParamMap?.map { it.key }
        }
    }
}
