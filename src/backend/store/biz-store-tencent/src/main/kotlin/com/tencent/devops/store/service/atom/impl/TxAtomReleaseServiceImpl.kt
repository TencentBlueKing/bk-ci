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

import com.tencent.devops.common.api.constant.APPROVE
import com.tencent.devops.common.api.constant.BEGIN
import com.tencent.devops.common.api.constant.BUILD
import com.tencent.devops.common.api.constant.COMMIT
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.constant.DOING
import com.tencent.devops.common.api.constant.END
import com.tencent.devops.common.api.constant.FAIL
import com.tencent.devops.common.api.constant.NUM_FIVE
import com.tencent.devops.common.api.constant.NUM_FOUR
import com.tencent.devops.common.api.constant.NUM_ONE
import com.tencent.devops.common.api.constant.NUM_SIX
import com.tencent.devops.common.api.constant.NUM_THREE
import com.tencent.devops.common.api.constant.NUM_TWO
import com.tencent.devops.common.api.constant.SUCCESS
import com.tencent.devops.common.api.constant.TEST
import com.tencent.devops.common.api.constant.UNDO
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.pojo.AtomBaseInfo
import com.tencent.devops.common.pipeline.pojo.AtomMarketInitPipelineReq
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.api.service.ServicePipelineResource
import com.tencent.devops.repository.api.ServiceGitRepositoryResource
import com.tencent.devops.repository.pojo.RepositoryInfo
import com.tencent.devops.repository.pojo.enums.TokenTypeEnum
import com.tencent.devops.repository.pojo.enums.VisibilityLevelEnum
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.pojo.atom.MarketAtomCreateRequest
import com.tencent.devops.store.pojo.atom.enums.AtomPackageSourceTypeEnum
import com.tencent.devops.store.pojo.atom.enums.AtomStatusEnum
import com.tencent.devops.store.pojo.common.ReleaseProcessItem
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.atom.TxAtomReleaseService
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class TxAtomReleaseServiceImpl : TxAtomReleaseService, AtomReleaseServiceImpl() {

    @Value("\${git.plugin.nameSpaceId}")
    private lateinit var pluginNameSpaceId: String

    private val logger = LoggerFactory.getLogger(TxAtomReleaseServiceImpl::class.java)

    override fun handleAtomPackage(
        atomPackageSourceType: AtomPackageSourceTypeEnum,
        marketAtomCreateRequest: MarketAtomCreateRequest,
        userId: String,
        atomCode: String
    ): Result<Map<String, String>?> {
        var repositoryInfo: RepositoryInfo? = null
        if (atomPackageSourceType == AtomPackageSourceTypeEnum.REPO) {
            if (marketAtomCreateRequest.visibilityLevel == VisibilityLevelEnum.PRIVATE) {
                if (marketAtomCreateRequest.privateReason.isNullOrBlank()) {
                    return MessageCodeUtil.generateResponseDataObject(
                        CommonMessageCode.PARAMETER_IS_NULL,
                        arrayOf("privateReason"),
                        null
                    )
                }
            }
            // 远程调工蜂接口创建代码库
            try {
                val createGitRepositoryResult = client.get(ServiceGitRepositoryResource::class).createGitCodeRepository(
                    userId,
                    marketAtomCreateRequest.projectCode,
                    atomCode,
                    marketAtomBuildInfoDao.getAtomBuildInfoByLanguage(
                        dslContext,
                        marketAtomCreateRequest.language
                    ).sampleProjectPath,
                    pluginNameSpaceId.toInt(),
                    marketAtomCreateRequest.visibilityLevel,
                    TokenTypeEnum.PRIVATE_KEY
                )
                logger.info("the createGitRepositoryResult is :$createGitRepositoryResult")
                if (createGitRepositoryResult.isOk()) {
                    repositoryInfo = createGitRepositoryResult.data
                } else {
                    return Result(createGitRepositoryResult.status, createGitRepositoryResult.message, null)
                }
            } catch (e: Exception) {
                logger.info("createGitCodeRepository error  is :$e", e)
                return MessageCodeUtil.generateResponseDataObject(StoreMessageCode.USER_CREATE_REPOSITORY_FAIL)
            }
        }
        if (null == repositoryInfo) {
            return MessageCodeUtil.generateResponseDataObject(StoreMessageCode.USER_CREATE_REPOSITORY_FAIL)
        }
        return Result(mapOf("repositoryHashId" to repositoryInfo.repositoryHashId!!, "codeSrc" to repositoryInfo.url))
    }

    override fun getFileStr(
        projectCode: String,
        atomCode: String,
        atomVersion: String,
        repositoryHashId: String,
        fileName: String
    ): String? {
        logger.info("getFileStr projectCode is:$projectCode,atomCode is:$atomCode,atomVersion is:$atomVersion")
        logger.info("getFileStr repositoryHashId is:$repositoryHashId,fileName is:$fileName")
        val fileStr = if (repositoryHashId.isNotBlank()) {
            // 从工蜂拉取文件
            client.get(ServiceGitRepositoryResource::class).getFileContent(
                repositoryHashId,
                fileName, null, null, null
            ).data
        } else {
            // 直接从仓库拉取文件
            marketAtomArchiveService.getFileStr(projectCode, atomCode, atomVersion, fileName)
        }
        logger.info("getFileStr fileStr is:$fileStr")
        return fileStr
    }

    override fun asyncHandleUpdateAtom(context: DSLContext, atomId: String, userId: String) {
        // 执行构建流水线
        runPipeline(context, atomId, userId)
    }

    override fun handleProcessInfo(isNormalUpgrade: Boolean, status: Int): List<ReleaseProcessItem> {
        val processInfo = initProcessInfo(isNormalUpgrade)
        val totalStep = if (isNormalUpgrade) NUM_FIVE else NUM_SIX
        when (status) {
            AtomStatusEnum.INIT.status, AtomStatusEnum.COMMITTING.status -> {
                storeCommonService.setProcessInfo(processInfo, totalStep, NUM_TWO, DOING)
            }
            AtomStatusEnum.BUILDING.status -> {
                storeCommonService.setProcessInfo(processInfo, totalStep, NUM_THREE, DOING)
            }
            AtomStatusEnum.BUILD_FAIL.status -> {
                storeCommonService.setProcessInfo(processInfo, totalStep, NUM_THREE, FAIL)
            }
            AtomStatusEnum.TESTING.status -> {
                storeCommonService.setProcessInfo(processInfo, totalStep, NUM_FOUR, DOING)
            }
            AtomStatusEnum.AUDITING.status -> {
                storeCommonService.setProcessInfo(processInfo, totalStep, NUM_FIVE, DOING)
            }
            AtomStatusEnum.AUDIT_REJECT.status -> {
                storeCommonService.setProcessInfo(processInfo, totalStep, NUM_FIVE, FAIL)
            }
            AtomStatusEnum.RELEASED.status -> {
                val currStep = if (isNormalUpgrade) NUM_FIVE else NUM_SIX
                storeCommonService.setProcessInfo(processInfo, totalStep, currStep, SUCCESS)
            }
        }
        return processInfo
    }

    override fun rebuild(projectCode: String, userId: String, atomId: String): Result<Boolean> {
        logger.info("rebuild, projectCode=$projectCode, userId=$userId, atomId=$atomId")
        // 判断是否可以启动构建
        val status = AtomStatusEnum.BUILDING.status.toByte()
        val (checkResult, code) = checkAtomVersionOptRight(userId, atomId, status)
        if (!checkResult) {
            return MessageCodeUtil.generateResponseDataObject(code)
        }
        // 拉取task.json，检查格式，更新入库
        val atomRecord = marketAtomDao.getAtomRecordById(dslContext, atomId) ?: return Result(false)
        val atomCode = atomRecord.atomCode
        val atomName = atomRecord.name
        val atomVersion = atomRecord.version
        val repoId = atomRecord.repositoryHashId
        val atomPackageSourceType = if (repoId.isBlank()) AtomPackageSourceTypeEnum.UPLOAD else AtomPackageSourceTypeEnum.REPO
        val getAtomConfResult = getAtomConfig(
            atomPackageSourceType = atomPackageSourceType,
            projectCode = projectCode,
            atomCode = atomCode,
            atomVersion = atomVersion,
            repositoryHashId = repoId,
            userId = userId
        )
        logger.info("rebuild, getAtomConfResult: $getAtomConfResult")
        if (getAtomConfResult.errorCode != "0") {
            return MessageCodeUtil.generateResponseDataObject(
                getAtomConfResult.errorCode,
                getAtomConfResult.errorParams
            )
        }
        val taskDataMap = getAtomConfResult.taskDataMap
        val atomEnvRequest = getAtomConfResult.atomEnvRequest ?: return MessageCodeUtil.generateResponseDataObject(
            StoreMessageCode.USER_REPOSITORY_TASK_JSON_FIELD_IS_NULL, arrayOf("execution")
        )
        // 解析quality.json
        val getAtomQualityResult = getAtomQualityConfig(
            projectCode = projectCode,
            atomCode = atomCode,
            atomName = atomName,
            atomVersion = atomVersion,
            repositoryHashId = atomRecord.repositoryHashId,
            userId = userId
        )
        logger.info("rebuild, getAtomQualityResult: $getAtomQualityResult")
        if (getAtomQualityResult.errorCode == StoreMessageCode.USER_REPOSITORY_PULL_QUALITY_JSON_FILE_FAIL) {
            logger.info("quality.json not found , skip...")
        } else if (getAtomQualityResult.errorCode != "0") {
            return MessageCodeUtil.generateResponseDataObject(
                getAtomQualityResult.errorCode,
                getAtomQualityResult.errorParams
            )
        }
        val propsMap = mutableMapOf<String, Any?>()
        propsMap["inputGroups"] = taskDataMap?.get("inputGroups")
        propsMap["input"] = taskDataMap?.get("input")
        propsMap["output"] = taskDataMap?.get("output")
        dslContext.transaction { t ->
            val context = DSL.using(t)
            val props = JsonUtil.toJson(propsMap)
            marketAtomDao.updateMarketAtomProps(context, atomId, props, userId)
            marketAtomEnvInfoDao.updateMarketAtomEnvInfo(context, atomId, atomEnvRequest)
            // 执行构建流水线
            runPipeline(context, atomId, userId)
        }
        return Result(true)
    }

    override fun getPassTestStatus(isNormalUpgrade: Boolean): Byte {
        return if (isNormalUpgrade) AtomStatusEnum.RELEASED.status.toByte() else AtomStatusEnum.AUDITING.status.toByte()
    }

    /**
     * 初始化插件版本进度
     */
    private fun initProcessInfo(isNormalUpgrade: Boolean): List<ReleaseProcessItem> {
        val processInfo = mutableListOf<ReleaseProcessItem>()
        processInfo.add(ReleaseProcessItem(MessageCodeUtil.getCodeLanMessage(BEGIN), BEGIN, NUM_ONE, SUCCESS))
        processInfo.add(ReleaseProcessItem(MessageCodeUtil.getCodeLanMessage(COMMIT), COMMIT, NUM_TWO, UNDO))
        processInfo.add(ReleaseProcessItem(MessageCodeUtil.getCodeLanMessage(BUILD), BUILD, NUM_THREE, UNDO))
        processInfo.add(ReleaseProcessItem(MessageCodeUtil.getCodeLanMessage(TEST), TEST, NUM_FOUR, UNDO))
        if (isNormalUpgrade) {
            processInfo.add(ReleaseProcessItem(MessageCodeUtil.getCodeLanMessage(END), END, NUM_FIVE, UNDO))
        } else {
            processInfo.add(ReleaseProcessItem(MessageCodeUtil.getCodeLanMessage(APPROVE), APPROVE, NUM_FIVE, UNDO))
            processInfo.add(ReleaseProcessItem(MessageCodeUtil.getCodeLanMessage(END), END, NUM_SIX, UNDO))
        }
        return processInfo
    }

    private fun runPipeline(context: DSLContext, atomId: String, userId: String): Boolean {
        val atomRecord = marketAtomDao.getAtomRecordById(context, atomId) ?: return false
        val atomCode = atomRecord.atomCode
        val atomPipelineRelRecord = storePipelineRelDao.getStorePipelineRel(context, atomCode, StoreTypeEnum.ATOM)
        val projectCode = storeProjectRelDao.getInitProjectCodeByStoreCode(
            context,
            atomCode,
            StoreTypeEnum.ATOM.type.toByte()
        ) // 查找新增插件时关联的项目
        val buildInfo = marketAtomBuildInfoDao.getAtomBuildInfo(context, atomId)
        logger.info("the buildInfo is:$buildInfo")
        val script = buildInfo.value1()
        if (null == atomPipelineRelRecord) {
            // 为用户初始化构建流水线并触发执行
            val atomBaseInfo = AtomBaseInfo(atomId, atomCode, atomRecord.version)
            val atomBuildAppInfoRecords = marketAtomBuildAppRelDao.getMarketAtomBuildAppInfo(context, atomId)
            val buildEnv = mutableMapOf<String, String>()
            atomBuildAppInfoRecords?.forEach {
                buildEnv[it["appName"] as String] = it["appVersion"] as String
            }
            val atomMarketInitPipelineReq = AtomMarketInitPipelineReq(
                atomRecord.repositoryHashId,
                buildInfo.value2(),
                script,
                atomBaseInfo,
                buildEnv
            )
            val atomMarketInitPipelineResp = client.get(ServicePipelineResource::class)
                .initAtomMarketPipeline(userId, projectCode!!, atomMarketInitPipelineReq).data
            logger.info("the atomMarketInitPipelineResp is:$atomMarketInitPipelineResp")
            if (null != atomMarketInitPipelineResp) {
                storePipelineRelDao.add(context, atomCode, StoreTypeEnum.ATOM, atomMarketInitPipelineResp.pipelineId)
                marketAtomDao.setAtomStatusById(
                    context,
                    atomId,
                    atomMarketInitPipelineResp.atomBuildStatus.status.toByte(),
                    userId,
                    null
                )
                val buildId = atomMarketInitPipelineResp.buildId
                if (null != buildId) {
                    storePipelineBuildRelDao.add(context, atomId, atomMarketInitPipelineResp.pipelineId, buildId)
                }
                // 通过websocket推送状态变更消息
                // websocketService.sendWebsocketMessage(userId, atomId)
            }
        } else {
            // 触发执行流水线
            val startParams = mutableMapOf<String, String>() // 启动参数
            startParams["atomCode"] = atomCode
            startParams["version"] = atomRecord.version
            startParams["script"] = script
            val buildIdObj = client.get(ServiceBuildResource::class).manualStartup(
                userId, projectCode!!, atomPipelineRelRecord.pipelineId, startParams,
                ChannelCode.AM
            ).data
            logger.info("the buildIdObj is:$buildIdObj")
            if (null != buildIdObj) {
                storePipelineBuildRelDao.add(context, atomId, atomPipelineRelRecord.pipelineId, buildIdObj.id)
                marketAtomDao.setAtomStatusById(
                    context,
                    atomId,
                    AtomStatusEnum.BUILDING.status.toByte(),
                    userId,
                    null
                ) // 构建中
            } else {
                marketAtomDao.setAtomStatusById(
                    context,
                    atomId,
                    AtomStatusEnum.BUILD_FAIL.status.toByte(),
                    userId,
                    null
                ) // 构建失败
            }
            // 通过websocket推送状态变更消息
            // websocketService.sendWebsocketMessage(userId, atomId)
        }
        return true
    }
}
