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
import com.tencent.devops.common.api.constant.CODECC
import com.tencent.devops.common.api.constant.COMMIT
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.constant.DOING
import com.tencent.devops.common.api.constant.END
import com.tencent.devops.common.api.constant.FAIL
import com.tencent.devops.common.api.constant.JS
import com.tencent.devops.common.api.constant.NUM_FIVE
import com.tencent.devops.common.api.constant.NUM_FOUR
import com.tencent.devops.common.api.constant.NUM_ONE
import com.tencent.devops.common.api.constant.NUM_SEVEN
import com.tencent.devops.common.api.constant.NUM_SIX
import com.tencent.devops.common.api.constant.NUM_THREE
import com.tencent.devops.common.api.constant.NUM_TWO
import com.tencent.devops.common.api.constant.SUCCESS
import com.tencent.devops.common.api.constant.TEST
import com.tencent.devops.common.api.constant.UNDO
import com.tencent.devops.common.api.enums.FrontendTypeEnum
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.pojo.AtomBaseInfo
import com.tencent.devops.common.pipeline.pojo.AtomMarketInitPipelineReq
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.model.store.tables.records.TAtomRecord
import com.tencent.devops.plugin.api.ServiceCodeccResource
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.api.service.ServicePipelineInitResource
import com.tencent.devops.repository.api.ServiceGitRepositoryResource
import com.tencent.devops.repository.pojo.RepositoryInfo
import com.tencent.devops.repository.pojo.enums.TokenTypeEnum
import com.tencent.devops.repository.pojo.enums.VisibilityLevelEnum
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.dao.atom.MarketAtomBuildInfoDao
import com.tencent.devops.store.dao.common.BusinessConfigDao
import com.tencent.devops.store.dao.common.StoreBuildInfoDao
import com.tencent.devops.store.dao.common.StorePipelineBuildRelDao
import com.tencent.devops.store.dao.common.StorePipelineRelDao
import com.tencent.devops.store.pojo.atom.MarketAtomCreateRequest
import com.tencent.devops.store.pojo.atom.MarketAtomUpdateRequest
import com.tencent.devops.store.pojo.atom.enums.AtomPackageSourceTypeEnum
import com.tencent.devops.store.pojo.atom.enums.AtomStatusEnum
import com.tencent.devops.store.pojo.common.BK_FRONTEND_DIR_NAME
import com.tencent.devops.store.pojo.common.ReleaseProcessItem
import com.tencent.devops.store.pojo.common.STORE_REPO_CODECC_BUILD_KEY_PREFIX
import com.tencent.devops.store.pojo.common.STORE_REPO_COMMIT_KEY_PREFIX
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.atom.TxAtomReleaseService
import com.tencent.devops.store.service.common.TxStoreCodeccService
import org.apache.commons.lang.StringEscapeUtils
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.context.config.annotation.RefreshScope
import org.springframework.stereotype.Service
import java.util.concurrent.Executors

@Service
@RefreshScope
class TxAtomReleaseServiceImpl : TxAtomReleaseService, AtomReleaseServiceImpl() {

    @Autowired
    lateinit var marketAtomBuildInfoDao: MarketAtomBuildInfoDao

    @Autowired
    lateinit var storePipelineRelDao: StorePipelineRelDao

    @Autowired
    lateinit var storeBuildInfoDao: StoreBuildInfoDao

    @Autowired
    lateinit var storePipelineBuildRelDao: StorePipelineBuildRelDao

    @Autowired
    lateinit var businessConfigDao: BusinessConfigDao

    @Autowired
    lateinit var txStoreCodeccService: TxStoreCodeccService

    @Value("\${git.plugin.nameSpaceId}")
    private lateinit var pluginNameSpaceId: String

    @Value("\${git.plugin.nameSpaceName}")
    private lateinit var pluginNameSpaceName: String

    @Value("\${store.codecc:timeout:10}")
    private lateinit var codeccTimeout: String

    private val executorService = Executors.newFixedThreadPool(10)

    private val logger = LoggerFactory.getLogger(TxAtomReleaseServiceImpl::class.java)

    override fun handleAtomPackage(
        marketAtomCreateRequest: MarketAtomCreateRequest,
        userId: String,
        atomCode: String
    ): Result<Map<String, String>?> {
        logger.info("handleAtomPackage marketAtomCreateRequest is:$marketAtomCreateRequest,atomCode is:$atomCode,userId is:$userId")
        marketAtomCreateRequest.authType ?: return MessageCodeUtil.generateResponseDataObject(
            CommonMessageCode.PARAMETER_IS_NULL,
            arrayOf("authType"),
            null
        )
        marketAtomCreateRequest.visibilityLevel ?: return MessageCodeUtil.generateResponseDataObject(
            CommonMessageCode.PARAMETER_IS_NULL,
            arrayOf("visibilityLevel"),
            null
        )
        val repositoryInfo: RepositoryInfo?
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
        val frontendType = marketAtomCreateRequest.frontendType
        try {
            val createGitRepositoryResult = client.get(ServiceGitRepositoryResource::class).createGitCodeRepository(
                userId = userId,
                projectCode = marketAtomCreateRequest.projectCode,
                repositoryName = atomCode,
                sampleProjectPath = storeBuildInfoDao.getStoreBuildInfoByLanguage(
                    dslContext,
                    marketAtomCreateRequest.language,
                    StoreTypeEnum.ATOM
                ).sampleProjectPath,
                namespaceId = pluginNameSpaceId.toInt(),
                visibilityLevel = marketAtomCreateRequest.visibilityLevel,
                tokenType = TokenTypeEnum.PRIVATE_KEY,
                frontendType = frontendType
            )
            logger.info("the createGitRepositoryResult is :$createGitRepositoryResult")
            if (createGitRepositoryResult.isOk()) {
                repositoryInfo = createGitRepositoryResult.data
            } else {
                return Result(createGitRepositoryResult.status, createGitRepositoryResult.message, null)
            }
        } catch (e: Exception) {
            logger.warn("createGitCodeRepository error  is :$e", e)
            return MessageCodeUtil.generateResponseDataObject(StoreMessageCode.USER_CREATE_REPOSITORY_FAIL)
        }
        if (null == repositoryInfo) {
            return MessageCodeUtil.generateResponseDataObject(StoreMessageCode.USER_CREATE_REPOSITORY_FAIL)
        }
        // 创建codecc扫描流水线
        executorService.submit<Unit> {
            val language = marketAtomCreateRequest.language
            val codeccLanguage = txStoreCodeccService.getCodeccLanguage(language)
            val codeccLanguages = if (frontendType == FrontendTypeEnum.SPECIAL) {
                listOf(codeccLanguage, txStoreCodeccService.getCodeccLanguage(JS))
            } else {
                listOf(codeccLanguage)
            }
            val createCodeccPipelineResult =
                client.get(ServiceCodeccResource::class).createCodeccPipeline(repositoryInfo.aliasName, codeccLanguages)
            logger.info("createCodeccPipelineResult is :$createCodeccPipelineResult")
        }
        return Result(mapOf("repositoryHashId" to repositoryInfo.repositoryHashId!!, "codeSrc" to repositoryInfo.url))
    }

    override fun getAtomPackageSourceType(atomCode: String): AtomPackageSourceTypeEnum {
        // 内部版暂时只支持代码库打包的方式，后续支持用户传可执行包的方式
        return AtomPackageSourceTypeEnum.REPO
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
        val atomPackageSourceType = getAtomPackageSourceType(atomCode)
        val fileStr = if (atomPackageSourceType == AtomPackageSourceTypeEnum.REPO) {
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

    override fun validateUpdateMarketAtomReq(
        userId: String,
        marketAtomUpdateRequest: MarketAtomUpdateRequest,
        atomRecord: TAtomRecord
    ): Result<Boolean> {
        logger.info("validateUpdateMarketAtomReq userId is:$userId,marketAtomUpdateRequest is:$marketAtomUpdateRequest")
        val frontendType = marketAtomUpdateRequest.frontendType
        if (frontendType == FrontendTypeEnum.SPECIAL) {
            val repositoryTreeInfoResult = client.get(ServiceGitRepositoryResource::class).getGitRepositoryTreeInfo(
                userId = userId,
                repoId = atomRecord.repositoryHashId,
                refName = null,
                path = null,
                tokenType = TokenTypeEnum.PRIVATE_KEY
            )
            logger.info("the repositoryTreeInfoResult is :$repositoryTreeInfoResult")
            if (repositoryTreeInfoResult.isNotOk()) {
                return Result(repositoryTreeInfoResult.status, repositoryTreeInfoResult.message, false)
            }
            val repositoryTreeInfoList = repositoryTreeInfoResult.data
            var flag = false
            run outside@{
                repositoryTreeInfoList?.forEach {
                    if (it.name == BK_FRONTEND_DIR_NAME && it.type == "tree") {
                        flag = true
                        return@outside
                    }
                }
            }
            if (!flag) {
                return MessageCodeUtil.generateResponseDataObject(
                    StoreMessageCode.USER_REPOSITORY_BK_FRONTEND_DIR_IS_NULL,
                    arrayOf(BK_FRONTEND_DIR_NAME),
                    false
                )
            }
        }
        return Result(true)
    }

    override fun handleProcessInfo(isNormalUpgrade: Boolean, status: Int): List<ReleaseProcessItem> {
        val codeccFlag = getCodeccFlag(StoreTypeEnum.ATOM.name)
        val processInfo = initProcessInfo(isNormalUpgrade, codeccFlag)
        val flag = codeccFlag == null || !codeccFlag
        val totalStep = if (isNormalUpgrade) {
            if (flag) NUM_FIVE else NUM_SIX
        } else {
            if (flag) NUM_SIX else NUM_SEVEN
        }
        val currAuditStep = if (flag) NUM_FIVE else NUM_SIX
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
            AtomStatusEnum.CODECCING.status -> {
                storeCommonService.setProcessInfo(processInfo, totalStep, NUM_FIVE, DOING)
            }
            AtomStatusEnum.CODECC_FAIL.status -> {
                storeCommonService.setProcessInfo(processInfo, totalStep, NUM_FIVE, FAIL)
            }
            AtomStatusEnum.AUDITING.status -> {
                storeCommonService.setProcessInfo(processInfo, totalStep, currAuditStep, DOING)
            }
            AtomStatusEnum.AUDIT_REJECT.status -> {
                storeCommonService.setProcessInfo(processInfo, totalStep, currAuditStep, FAIL)
            }
            AtomStatusEnum.RELEASED.status -> {
                storeCommonService.setProcessInfo(processInfo, totalStep, totalStep, SUCCESS)
            }
        }
        return processInfo
    }

    override fun getPreValidatePassTestStatus(): Byte {
        return AtomStatusEnum.CODECCING.status.toByte()
    }

    override fun doPassTestPreOperation(atomId: String, atomStatus: Byte, userId: String) {
        marketAtomDao.setAtomStatusById(dslContext, atomId, atomStatus, userId, "")
        storeWebsocketService.sendWebsocketMessage(userId, atomId)
    }

    override fun getAfterValidatePassTestStatus(
        atomId: String,
        atomCode: String,
        validateFlag: Boolean,
        isNormalUpgrade: Boolean
    ): Byte {
        return if (!validateFlag) {
            AtomStatusEnum.CODECC_FAIL.status.toByte()
        } else {
            val storeType = StoreTypeEnum.ATOM.name
            redisOperation.delete("$STORE_REPO_COMMIT_KEY_PREFIX:$storeType:$atomCode:$atomId")
            redisOperation.delete("$STORE_REPO_CODECC_BUILD_KEY_PREFIX:$storeType:$atomCode:$atomId")
            if (isNormalUpgrade) AtomStatusEnum.RELEASED.status.toByte() else AtomStatusEnum.AUDITING.status.toByte()
        }
    }

    override fun validateAtomPassTestCondition(userId: String, atomCode: String, atomId: String): Boolean {
        val storeType = StoreTypeEnum.ATOM.name
        // 判断codecc校验开关是否打开
        val codeccFlag = getCodeccFlag(storeType)
        if (codeccFlag != null && !codeccFlag) {
            return true
        }
        // 获取当次构建对应的buildId
        val buildId = redisOperation.get("$STORE_REPO_CODECC_BUILD_KEY_PREFIX:$storeType:$atomCode:$atomId")
        val atomRecord = atomDao.getPipelineAtom(dslContext, atomId)!!
        val repoId = "$pluginNameSpaceName/${atomRecord.atomCode}"
        return handleAtomCodeccValidateStatus(repoId, buildId)
    }

    private fun getCodeccFlag(storeType: String): Boolean? {
        val codeccFlagConfig = businessConfigDao.get(
            dslContext = dslContext,
            business = storeType,
            feature = "codeccFlag",
            businessValue = storeType
        )
        return codeccFlagConfig?.configValue?.toBoolean()
    }

    private fun handleAtomCodeccValidateStatus(repoId: String, buildId: String?): Boolean {
        val startTime = System.currentTimeMillis()
        var validateFlag = false
        loop@ while (true) {
            // 睡眠3秒再轮询去查扫描结果信息
            Thread.sleep(3000)
            val codeccMeasureInfoResult = client.get(ServiceCodeccResource::class).getCodeccMeasureInfo(
                repoId = repoId,
                buildId = buildId
            )
            logger.info("handleAtomCodeccValidateStatus codeccMeasureInfoResult: $codeccMeasureInfoResult")
            val codeccMeasureInfo = codeccMeasureInfoResult.data
            val status = codeccMeasureInfo?.status
            if (codeccMeasureInfoResult.isNotOk() || codeccMeasureInfo == null || status == null) break@loop
            if (status != 3) {
                if (status == 0) {
                    val codeStyleScore = codeccMeasureInfo.codeStyleScore
                    val codeSecurityScore = codeccMeasureInfo.codeSecurityScore
                    val codeMeasureScore = codeccMeasureInfo.codeMeasureScore
                    if (codeStyleScore != null && codeSecurityScore != null && codeMeasureScore != null) {
                        val storeType = StoreTypeEnum.ATOM.name
                        val codeStyleQualifiedScore = txStoreCodeccService.getQualifiedScore(storeType, "codeStyle")
                        val codeSecurityQualifiedScore =
                            txStoreCodeccService.getQualifiedScore(storeType, "codeSecurity")
                        val codeMeasureQualifiedScore = txStoreCodeccService.getQualifiedScore(storeType, "codeMeasure")
                        // 判断插件代码库的扫描分数是否合格
                        if (codeStyleScore >= codeStyleQualifiedScore && codeSecurityScore >= codeSecurityQualifiedScore && codeMeasureScore >= codeMeasureQualifiedScore)
                            validateFlag = true
                    }
                }
                break@loop
            } else {
                // 轮询超时则直接返回校验失败
                if ((System.currentTimeMillis() - startTime) > codeccTimeout.toInt() * 60 * 1000) {
                    break@loop
                }
            }
        }
        return validateFlag
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
        val atomPackageSourceType =
            if (repoId.isBlank()) AtomPackageSourceTypeEnum.UPLOAD else AtomPackageSourceTypeEnum.REPO
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

    /**
     * 初始化插件版本进度
     */
    private fun initProcessInfo(isNormalUpgrade: Boolean, codeccFlag: Boolean?): List<ReleaseProcessItem> {
        val processInfo = mutableListOf<ReleaseProcessItem>()
        processInfo.add(ReleaseProcessItem(MessageCodeUtil.getCodeLanMessage(BEGIN), BEGIN, NUM_ONE, SUCCESS))
        processInfo.add(ReleaseProcessItem(MessageCodeUtil.getCodeLanMessage(COMMIT), COMMIT, NUM_TWO, UNDO))
        processInfo.add(ReleaseProcessItem(MessageCodeUtil.getCodeLanMessage(BUILD), BUILD, NUM_THREE, UNDO))
        processInfo.add(ReleaseProcessItem(MessageCodeUtil.getCodeLanMessage(TEST), TEST, NUM_FOUR, UNDO))
        val flag = codeccFlag == null || !codeccFlag
        if (!flag) {
            processInfo.add(ReleaseProcessItem(MessageCodeUtil.getCodeLanMessage(CODECC), CODECC, NUM_FIVE, UNDO))
        }
        if (isNormalUpgrade) {
            val endStep = if (flag) NUM_FIVE else NUM_SIX
            processInfo.add(ReleaseProcessItem(MessageCodeUtil.getCodeLanMessage(END), END, endStep, UNDO))
        } else {
            val approveStep = if (flag) NUM_FIVE else NUM_SIX
            val endStep = if (flag) NUM_SIX else NUM_SEVEN
            processInfo.add(ReleaseProcessItem(MessageCodeUtil.getCodeLanMessage(APPROVE), APPROVE, approveStep, UNDO))
            processInfo.add(ReleaseProcessItem(MessageCodeUtil.getCodeLanMessage(END), END, endStep, UNDO))
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
        )!! // 查找新增插件时关联的项目
        val repositoryHashId = atomRecord.repositoryHashId
        val commitId = handleCodeccTask(
            userId = userId,
            repositoryHashId = repositoryHashId,
            atomCode = atomCode,
            atomId = atomId
        )
        val buildInfo = marketAtomBuildInfoDao.getAtomBuildInfo(context, atomId)
        logger.info("the buildInfo is:$buildInfo")
        val script = buildInfo.value1()
        val language = buildInfo.value3()
        if (null == atomPipelineRelRecord) {
            // 为用户初始化构建流水线并触发执行
            val version = atomRecord.version
            val atomBaseInfo = AtomBaseInfo(
                atomId = atomId,
                atomCode = atomCode,
                version = atomRecord.version,
                language = language,
                commitId = commitId
            )
            val pipelineModelConfig = businessConfigDao.get(context, StoreTypeEnum.ATOM.name, "initBuildPipeline", "PIPELINE_MODEL")
            var pipelineModel = pipelineModelConfig!!.configValue
            var pipelineName = "am-$projectCode-$atomCode-${System.currentTimeMillis()}"
            if (pipelineName.toCharArray().size > 128) {
                pipelineName = "am-$atomCode-${UUIDUtil.generate()}"
            }
            val paramMap = mapOf(
                "pipelineName" to pipelineName,
                "storeCode" to atomCode,
                "version" to version,
                "language" to language,
                "script" to StringEscapeUtils.escapeJava(script),
                "repositoryHashId" to atomRecord.repositoryHashId,
                "repositoryPath" to (buildInfo.value2() ?: "")
            )
            // 将流水线模型中的变量替换成具体的值
            paramMap.forEach { (key, value) ->
                pipelineModel = pipelineModel.replace("#{$key}", value)
            }
            val atomMarketInitPipelineReq = AtomMarketInitPipelineReq(
                pipelineModel = pipelineModel,
                script = script,
                atomBaseInfo = atomBaseInfo
            )
            val atomMarketInitPipelineResp = client.get(ServicePipelineInitResource::class)
                .initAtomMarketPipeline(userId, projectCode, atomMarketInitPipelineReq).data
            logger.info("the atomMarketInitPipelineResp is:$atomMarketInitPipelineResp")
            if (null != atomMarketInitPipelineResp) {
                storePipelineRelDao.add(context, atomCode, StoreTypeEnum.ATOM, atomMarketInitPipelineResp.pipelineId)
                marketAtomDao.setAtomStatusById(
                    dslContext = context,
                    atomId = atomId,
                    atomStatus = atomMarketInitPipelineResp.atomBuildStatus.status.toByte(),
                    userId = userId,
                    msg = null
                )
                val buildId = atomMarketInitPipelineResp.buildId
                if (null != buildId) {
                    storePipelineBuildRelDao.add(context, atomId, atomMarketInitPipelineResp.pipelineId, buildId)
                }
                // 通过websocket推送状态变更消息
                storeWebsocketService.sendWebsocketMessage(userId, atomId)
            }
        } else {
            // 触发执行流水线
            val startParams = mutableMapOf<String, String>() // 启动参数
            startParams["atomCode"] = atomCode
            startParams["version"] = atomRecord.version
            startParams["language"] = language
            startParams["script"] = script
            startParams["commitId"] = commitId
            val buildIdObj = client.get(ServiceBuildResource::class).manualStartup(
                userId, projectCode, atomPipelineRelRecord.pipelineId, startParams,
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
            storeWebsocketService.sendWebsocketMessage(userId, atomId)
        }
        return true
    }

    private fun handleCodeccTask(
        userId: String,
        repositoryHashId: String,
        atomCode: String,
        atomId: String
    ): String {
        // 获取插件代码库最新提交记录
        val getRepoRecentCommitInfoResult = client.get(ServiceGitRepositoryResource::class).getRepoRecentCommitInfo(
            userId = userId,
            repoId = repositoryHashId,
            sha = "master",
            tokenType = TokenTypeEnum.PRIVATE_KEY
        )
        logger.info("handleCodeccTask  atomId:$atomId,getRepoRecentCommitInfoResult: $getRepoRecentCommitInfoResult")
        if (getRepoRecentCommitInfoResult.isNotOk()) {
            throw ErrorCodeException(
                errorCode = getRepoRecentCommitInfoResult.status.toString(),
                defaultMessage = getRepoRecentCommitInfoResult.message
            )
        }
        val gitCommit = getRepoRecentCommitInfoResult.data!!
        val commitId = gitCommit.id
        // 把代码提交ID存入redis
        redisOperation.set(
            key = "$STORE_REPO_COMMIT_KEY_PREFIX:${StoreTypeEnum.ATOM.name}:$atomCode:$atomId",
            value = commitId
        )
        executorService.submit<Unit> {
            val repoId = "$pluginNameSpaceName/$atomCode"
            // 如果代码扫描任务没有被触发或者失败则调接口触发
            val startCodeccTaskResult = client.get(ServiceCodeccResource::class).startCodeccTask(
                repoId = repoId,
                commitId = commitId
            )
            logger.info("handleCodeccTask  atomId:$atomId,startCodeccTaskResult: $startCodeccTaskResult")
            val buildId = startCodeccTaskResult.data
            if (startCodeccTaskResult.isNotOk() || buildId.isNullOrBlank()) {
                throw ErrorCodeException(
                    errorCode = startCodeccTaskResult.status.toString(),
                    defaultMessage = startCodeccTaskResult.message
                )
            }
            // 把代码扫描构建ID存入redis
            redisOperation.set(
                key = "$STORE_REPO_CODECC_BUILD_KEY_PREFIX:${StoreTypeEnum.ATOM.name}:$atomCode:$atomId",
                value = buildId!!
            )
        }
        return commitId
    }

    /**
     * 检查版本发布过程中的操作权限
     */
    override fun checkAtomVersionOptRight(
        userId: String,
        atomId: String,
        status: Byte,
        isNormalUpgrade: Boolean?
    ): Pair<Boolean, String> {
        logger.info("checkAtomVersionOptRight, userId=$userId, atomId=$atomId, status=$status, isNormalUpgrade=$isNormalUpgrade")
        val record =
            marketAtomDao.getAtomRecordById(dslContext, atomId) ?: return Pair(
                false,
                CommonMessageCode.PARAMETER_IS_INVALID
            )
        val atomCode = record.atomCode
        val creator = record.creator
        val recordStatus = record.atomStatus

        // 判断用户是否有权限(当前版本的创建者和管理员可以操作)
        if (!(storeMemberDao.isStoreAdmin(
                dslContext,
                userId,
                atomCode,
                StoreTypeEnum.ATOM.type.toByte()
            ) || creator == userId)
        ) {
            return Pair(false, CommonMessageCode.PERMISSION_DENIED)
        }
        logger.info("record status=$recordStatus, status=$status")
        val allowReleaseStatus = if (isNormalUpgrade != null && isNormalUpgrade) AtomStatusEnum.TESTING
        else AtomStatusEnum.AUDITING
        var validateFlag = true
        if (status == AtomStatusEnum.COMMITTING.status.toByte() &&
            recordStatus != AtomStatusEnum.INIT.status.toByte()
        ) {
            validateFlag = false
        } else if (status == AtomStatusEnum.BUILDING.status.toByte() &&
            recordStatus !in (
                listOf(
                    AtomStatusEnum.COMMITTING.status.toByte(),
                    AtomStatusEnum.BUILD_FAIL.status.toByte(),
                    AtomStatusEnum.TESTING.status.toByte()
                ))
        ) {
            validateFlag = false
        } else if (status == AtomStatusEnum.BUILD_FAIL.status.toByte() &&
            recordStatus !in (
                listOf(
                    AtomStatusEnum.COMMITTING.status.toByte(),
                    AtomStatusEnum.BUILDING.status.toByte(),
                    AtomStatusEnum.BUILD_FAIL.status.toByte(),
                    AtomStatusEnum.TESTING.status.toByte()
                ))
        ) {
            validateFlag = false
        } else if (status == AtomStatusEnum.TESTING.status.toByte() &&
            recordStatus != AtomStatusEnum.BUILDING.status.toByte()
        ) {
            validateFlag = false
        } else if (status == AtomStatusEnum.CODECCING.status.toByte() &&
            recordStatus != AtomStatusEnum.TESTING.status.toByte()
        ) {
            validateFlag = false
        } else if (status == AtomStatusEnum.AUDITING.status.toByte() &&
            recordStatus != AtomStatusEnum.CODECCING.status.toByte()
        ) {
            validateFlag = false
        } else if (status == AtomStatusEnum.AUDIT_REJECT.status.toByte() &&
            recordStatus != AtomStatusEnum.AUDITING.status.toByte()
        ) {
            validateFlag = false
        } else if (status == AtomStatusEnum.RELEASED.status.toByte() &&
            recordStatus != allowReleaseStatus.status.toByte()
        ) {
            validateFlag = false
        } else if (status == AtomStatusEnum.GROUNDING_SUSPENSION.status.toByte() &&
            recordStatus == AtomStatusEnum.RELEASED.status.toByte()
        ) {
            validateFlag = false
        } else if (status == AtomStatusEnum.UNDERCARRIAGING.status.toByte() &&
            recordStatus == AtomStatusEnum.RELEASED.status.toByte()
        ) {
            validateFlag = false
        } else if (status == AtomStatusEnum.UNDERCARRIAGED.status.toByte() &&
            recordStatus !in (
                listOf(
                    AtomStatusEnum.UNDERCARRIAGING.status.toByte(),
                    AtomStatusEnum.RELEASED.status.toByte()
                ))
        ) {
            validateFlag = false
        }

        return if (validateFlag) Pair(true, "") else Pair(false, StoreMessageCode.USER_ATOM_RELEASE_STEPS_ERROR)
    }
}
