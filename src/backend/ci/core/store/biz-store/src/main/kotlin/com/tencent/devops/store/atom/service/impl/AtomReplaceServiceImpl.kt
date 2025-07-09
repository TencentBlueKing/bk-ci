/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.store.atom.service.impl

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.enums.FrontendTypeEnum
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.model.store.tables.records.TAtomRecord
import com.tencent.devops.process.api.service.ServicePipelineAtomResource
import com.tencent.devops.store.atom.dao.AtomDao
import com.tencent.devops.store.atom.service.AtomReplaceService
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.pojo.atom.AtomParamReplaceInfo
import com.tencent.devops.store.pojo.atom.AtomReplaceRequest
import com.tencent.devops.store.pojo.atom.AtomReplaceRollBack
import com.tencent.devops.store.pojo.atom.AtomVersionReplaceInfo
import com.tencent.devops.store.pojo.atom.enums.AtomStatusEnum
import com.tencent.devops.store.pojo.common.ATOM_INPUT
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Suppress("ALL")
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
    ): Result<String> {
        logger.info("replacePipelineAtom userId:$userId,projectId:$projectId,atomReplaceRequest:$atomReplaceRequest")
        val fromAtomCode = atomReplaceRequest.fromAtomCode
        val toAtomCode = atomReplaceRequest.toAtomCode
        val versionInfoList = atomReplaceRequest.versionInfoList
        val pipelineIdList = atomReplaceRequest.pipelineIdList
        if (pipelineIdList != null && pipelineIdList.size > 100) {
            throw ErrorCodeException(
                errorCode = CommonMessageCode.ERROR_NEED_PARAM_,
                params = arrayOf("the number of pipelines is greater than 100")
            )
        }
        versionInfoList.forEach { versionInfo ->
            // 根据插件的atomCode和version查出输入输出参数json串
            val fromAtomVersion = versionInfo.fromAtomVersion
            // 校验插件信息是否合法
            val (fromAtomRecord, toAtomRecord) = validateAtomInfo(
                fromAtomCode = fromAtomCode,
                fromAtomVersion = fromAtomVersion,
                versionInfo = versionInfo,
                toAtomCode = toAtomCode
            )
            val fromAtomHtmlVersion = fromAtomRecord.htmlTemplateVersion
            val toAtomHtmlVersion = toAtomRecord.htmlTemplateVersion
            // 替换的插件必须是新插件的校验
            if (toAtomHtmlVersion == FrontendTypeEnum.HISTORY.typeVersion) {
                throw ErrorCodeException(errorCode = StoreMessageCode.USER_TO_ATOM_IS_NOT_BE_HIS_ATOM)
            }
            val paramInfoList = versionInfo.paramReplaceInfoList
            // 校验插件参数合法性
            validateAtomParam(
                fromAtomRecord = fromAtomRecord,
                toAtomRecord = toAtomRecord,
                fromAtomHtmlVersion = fromAtomHtmlVersion,
                toAtomHtmlVersion = toAtomHtmlVersion,
                paramInfoList = paramInfoList,
                fromAtomVersion = fromAtomVersion
            )
        }
        return client.get(ServicePipelineAtomResource::class).createReplaceAtomInfo(
            userId = userId,
            projectId = projectId,
            atomReplaceRequest = atomReplaceRequest
        )
    }

    private fun validateAtomParam(
        fromAtomRecord: TAtomRecord,
        toAtomRecord: TAtomRecord,
        fromAtomHtmlVersion: String?,
        toAtomHtmlVersion: String,
        paramInfoList: List<AtomParamReplaceInfo>?,
        fromAtomVersion: String
    ) {
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

    private fun validateAtomInfo(
        fromAtomCode: String,
        fromAtomVersion: String,
        versionInfo: AtomVersionReplaceInfo,
        toAtomCode: String
    ): Pair<TAtomRecord, TAtomRecord> {
        val atomStatusList = listOf(
            AtomStatusEnum.RELEASED.status.toByte(),
            AtomStatusEnum.UNDERCARRIAGING.status.toByte(),
            AtomStatusEnum.UNDERCARRIAGED.status.toByte()
        )
        val fromAtomRecord = atomDao.getPipelineAtom(
            dslContext = dslContext,
            atomCode = fromAtomCode,
            version = fromAtomVersion,
            atomStatusList = atomStatusList
        ) ?: throw ErrorCodeException(
            errorCode = CommonMessageCode.PARAMETER_IS_INVALID,
            params = arrayOf("$fromAtomCode:$fromAtomVersion")
        )
        val toAtomVersion = versionInfo.toAtomVersion
        val toAtomRecord = atomDao.getPipelineAtom(
            dslContext = dslContext,
            atomCode = toAtomCode,
            version = toAtomVersion,
            atomStatusList = listOf(AtomStatusEnum.RELEASED.status.toByte())
        ) ?: throw ErrorCodeException(
            errorCode = CommonMessageCode.PARAMETER_IS_INVALID,
            params = arrayOf("$toAtomCode:$toAtomVersion")
        )
        return Pair(fromAtomRecord, toAtomRecord)
    }

    override fun atomReplaceRollBack(userId: String, atomReplaceRollBack: AtomReplaceRollBack): Result<Boolean> {
        return client.get(ServicePipelineAtomResource::class).atomReplaceRollBack(
            userId = userId,
            atomReplaceRollBack = atomReplaceRollBack
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun generateInputParamNameList(
        atomHtmlVersion: String?,
        atomPropMap: Map<String, Any>
    ): List<String>? {
        return if (atomHtmlVersion == FrontendTypeEnum.HISTORY.typeVersion) {
            atomPropMap.map { it.key }
        } else {
            val inputParamMap = atomPropMap[ATOM_INPUT] as? Map<String, Any>
            inputParamMap?.map { it.key }
        }
    }
}
