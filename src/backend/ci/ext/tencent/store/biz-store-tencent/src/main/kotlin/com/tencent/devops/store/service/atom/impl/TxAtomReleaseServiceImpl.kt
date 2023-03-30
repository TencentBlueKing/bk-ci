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

package com.tencent.devops.store.service.atom.impl

import com.tencent.devops.common.api.constant.*
import com.tencent.devops.common.api.enums.FrontendTypeEnum
import com.tencent.devops.common.api.enums.OSType
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.pojo.AtomBaseInfo
import com.tencent.devops.common.pipeline.pojo.AtomMarketInitPipelineReq
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.model.store.tables.records.TAtomRecord
import com.tencent.devops.plugin.api.ServiceCodeccResource
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.api.service.ServicePipelineInitResource
import com.tencent.devops.process.utils.KEY_PIPELINE_NAME
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
import com.tencent.devops.store.pojo.atom.AtomRebuildRequest
import com.tencent.devops.store.pojo.atom.AtomReleaseRequest
import com.tencent.devops.store.pojo.atom.MarketAtomCreateRequest
import com.tencent.devops.store.pojo.atom.MarketAtomUpdateRequest
import com.tencent.devops.store.pojo.atom.enums.AtomPackageSourceTypeEnum
import com.tencent.devops.store.pojo.atom.enums.AtomStatusEnum
import com.tencent.devops.store.pojo.common.BK_FRONTEND_DIR_NAME
import com.tencent.devops.store.pojo.common.KEY_ATOM_CODE
import com.tencent.devops.store.pojo.common.KEY_CONFIG
import com.tencent.devops.store.pojo.common.KEY_EXECUTION
import com.tencent.devops.store.pojo.common.KEY_INPUT
import com.tencent.devops.store.pojo.common.KEY_INPUT_GROUPS
import com.tencent.devops.store.pojo.common.KEY_LANGUAGE
import com.tencent.devops.store.pojo.common.KEY_OUTPUT
import com.tencent.devops.store.pojo.common.KEY_RUNTIME_VERSION
import com.tencent.devops.store.pojo.common.KEY_STORE_CODE
import com.tencent.devops.store.pojo.common.ReleaseProcessItem
import com.tencent.devops.store.pojo.common.STORE_REPO_CODECC_BUILD_KEY_PREFIX
import com.tencent.devops.store.pojo.common.STORE_REPO_COMMIT_KEY_PREFIX
import com.tencent.devops.store.pojo.common.enums.ReleaseTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.atom.TxAtomReleaseService
import com.tencent.devops.store.service.common.TxStoreCodeccService
import org.apache.commons.lang3.StringEscapeUtils
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.context.config.annotation.RefreshScope
import org.springframework.stereotype.Service
import java.util.*
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

    private val executorService = Executors.newFixedThreadPool(10)

    private val logger = LoggerFactory.getLogger(TxAtomReleaseServiceImpl::class.java)

    override fun handleAtomPackage(
        marketAtomCreateRequest: MarketAtomCreateRequest,
        userId: String,
        atomCode: String
    ): Result<Map<String, String>?> {
        logger.info("handleAtomPackage params:[$marketAtomCreateRequest|$atomCode|$userId]")
        marketAtomCreateRequest.authType ?: return MessageUtil.generateResponseDataObject(
            messageCode = CommonMessageCode.PARAMETER_IS_NULL,
            params = arrayOf("authType"),
            data = null,
            language = I18nUtil.getLanguage(userId)
        )
        marketAtomCreateRequest.visibilityLevel ?: return MessageUtil.generateResponseDataObject(
            messageCode = CommonMessageCode.PARAMETER_IS_NULL,
            params = arrayOf("visibilityLevel"),
            data = null,
            language = I18nUtil.getLanguage(userId)
        )
        val repositoryInfo: RepositoryInfo?
        if (marketAtomCreateRequest.visibilityLevel == VisibilityLevelEnum.PRIVATE) {
            if (marketAtomCreateRequest.privateReason.isNullOrBlank()) {
                return MessageUtil.generateResponseDataObject(
                    messageCode = CommonMessageCode.PARAMETER_IS_NULL,
                    params = arrayOf("privateReason"),
                    data = null,
                    language = I18nUtil.getLanguage(userId)
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
        } catch (ignored: Throwable) {
            logger.warn("atom[$atomCode] createGitCodeRepository fail!", ignored)
            return MessageUtil.generateResponseDataObject(
                messageCode = StoreMessageCode.USER_CREATE_REPOSITORY_FAIL,
                language = I18nUtil.getLanguage(userId)
            )
        }
        if (null == repositoryInfo) {
            return MessageUtil.generateResponseDataObject(
                messageCode = StoreMessageCode.USER_CREATE_REPOSITORY_FAIL,
                language = I18nUtil.getLanguage(userId))
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

    override fun getAtomPackageSourceType(): AtomPackageSourceTypeEnum {
        // 内部版暂时只支持代码库打包的方式，后续支持用户传可执行包的方式
        return AtomPackageSourceTypeEnum.REPO
    }

    override fun getFileStr(
        projectCode: String,
        atomCode: String,
        atomVersion: String,
        fileName: String,
        repositoryHashId: String?,
        branch: String?
    ): String? {
        logger.info("getFileStr $projectCode|$atomCode|$atomVersion|$fileName|$repositoryHashId|$branch")
        val atomPackageSourceType = getAtomPackageSourceType()
        return if (atomPackageSourceType == AtomPackageSourceTypeEnum.REPO) {
            // 从工蜂拉取文件
            try {
                client.get(ServiceGitRepositoryResource::class).getFileContent(
                    repoId = repositoryHashId!!,
                    filePath = fileName,
                    reversion = null,
                    branch = branch,
                    repositoryType = null
                ).data
            } catch (ignored: RemoteServiceException) {
                logger.warn("getFileContent fileName:$fileName,branch:$branch error", ignored)
                if (ignored.httpStatus == HTTP_404 || ignored.errorCode == HTTP_404) {
                    ""
                } else {
                    throw ignored
                }
            }
        } else {
            // 直接从仓库拉取文件
            marketAtomArchiveService.getFileStr(projectCode, atomCode, atomVersion, fileName)
        }
    }

    override fun asyncHandleUpdateAtom(
        context: DSLContext,
        atomId: String,
        userId: String,
        branch: String?,
        validOsNameFlag: Boolean?,
        validOsArchFlag: Boolean?
    ) {
        // 执行构建流水线
        runPipeline(
            context = context,
            atomId = atomId,
            userId = userId,
            branch = branch,
            validOsNameFlag = validOsNameFlag,
            validOsArchFlag = validOsArchFlag
        )
    }

    override fun validateUpdateMarketAtomReq(
        userId: String,
        marketAtomUpdateRequest: MarketAtomUpdateRequest,
        atomRecord: TAtomRecord
    ): Result<Boolean> {
        logger.info("validateUpdateMarketAtomReq userId is:$userId,marketAtomUpdateRequest is:$marketAtomUpdateRequest")
        val frontendType = marketAtomUpdateRequest.frontendType
        if (frontendType != FrontendTypeEnum.SPECIAL) {
            return Result(true)
        }
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
            return MessageUtil.generateResponseDataObject(
                messageCode = StoreMessageCode.USER_REPOSITORY_BK_FRONTEND_DIR_IS_NULL,
                params = arrayOf(BK_FRONTEND_DIR_NAME),
                data = false,
                language = I18nUtil.getLanguage(userId)
            )
        }
        return Result(true)
    }

    @SuppressWarnings("ComplexMethod")
    override fun handleProcessInfo(
        userId: String,
        atomId: String,
        isNormalUpgrade: Boolean,
        status: Int
    ): List<ReleaseProcessItem> {
        val codeccFlag = txStoreCodeccService.getCodeccFlag(StoreTypeEnum.ATOM.name)
        val processInfo = initProcessInfo(isNormalUpgrade, codeccFlag)
        val flag = codeccFlag == null || !codeccFlag
        var conditionStatus = status
        if (flag) {
            if (status == AtomStatusEnum.CODECCING.status || status == AtomStatusEnum.CODECC_FAIL.status) {
                // 如果codecc开关关闭，将处于代码检查中和代码检查失败状态的插件置为测试中
                conditionStatus = AtomStatusEnum.TESTING.status
                marketAtomDao.setAtomStatusById(
                    dslContext = dslContext,
                    atomId = atomId,
                    atomStatus = conditionStatus.toByte(),
                    userId = userId,
                    msg = null
                )
            }
        }
        val totalStep = if (isNormalUpgrade) {
            if (flag) NUM_FIVE else NUM_SIX
        } else {
            if (flag) NUM_SIX else NUM_SEVEN
        }
        val currAuditStep = if (flag) NUM_FIVE else NUM_SIX
        when (conditionStatus) {
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

    override fun doCancelReleaseBus(userId: String, atomId: String) {
        val atomRecord = atomDao.getPipelineAtom(dslContext, atomId) ?: return
        val storeBuildInfoRecord = storePipelineBuildRelDao.getStorePipelineBuildRel(dslContext, atomId) ?: return
        // 获取插件的初始化项目
        val initProjectCode = storeProjectRelDao.getInitProjectCodeByStoreCode(
            dslContext = dslContext,
            storeCode = atomRecord.atomCode,
            storeType = StoreTypeEnum.ATOM.type.toByte()
        )!!
        // 插件打包流水线取消构建
        client.get(ServiceBuildResource::class).serviceShutdown(
            pipelineId = storeBuildInfoRecord.pipelineId,
            projectId = initProjectCode,
            buildId = storeBuildInfoRecord.buildId,
            channelCode = ChannelCode.AM
        )
    }

    override fun getPreValidatePassTestStatus(atomCode: String, atomId: String, atomStatus: Byte): Byte {
        val storeType = StoreTypeEnum.ATOM.name
        val codeccFlag = txStoreCodeccService.getCodeccFlag(StoreTypeEnum.ATOM.name)
        return if (codeccFlag != null && codeccFlag) {
            // 判断插件构建时启动扫描任务是否成功，buildId为空则说明启动扫描任务失败
            val buildId = redisOperation.get("$STORE_REPO_CODECC_BUILD_KEY_PREFIX:$storeType:$atomCode:$atomId")
            if (buildId == null) {
                AtomStatusEnum.CODECC_FAIL.status.toByte()
            } else {
                AtomStatusEnum.CODECCING.status.toByte()
            }
        } else {
            // codecc开关关闭，无需进行codecc扫描
            val isNormalUpgrade = marketAtomCommonService.getNormalUpgradeFlag(atomCode, atomStatus.toInt())
            if (isNormalUpgrade) AtomStatusEnum.RELEASED.status.toByte() else AtomStatusEnum.AUDITING.status.toByte()
        }
    }

    override fun doAtomReleaseBus(userId: String, atomReleaseRequest: AtomReleaseRequest) {
        val date = DateTimeUtil.formatDate(Date(), DateTimeUtil.YYYY_MM_DD)
        val tagName = "prod-v${atomReleaseRequest.version}-$date"
        val branch = if (atomReleaseRequest.branch.isNullOrBlank()) MASTER else atomReleaseRequest.branch
        val createGitTagResult = client.get(ServiceGitRepositoryResource::class).createGitTag(
            userId = userId,
            repoId = atomReleaseRequest.repositoryHashId!!,
            tagName = tagName,
            ref = branch!!,
            tokenType = TokenTypeEnum.PRIVATE_KEY
        )
        logger.info("createGitTagResult is :$createGitTagResult")
        if (createGitTagResult.isNotOk() || createGitTagResult.data == false) {
            throw ErrorCodeException(
                errorCode = createGitTagResult.status.toString(),
                defaultMessage = createGitTagResult.message
            )
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun rebuild(
        projectCode: String,
        userId: String,
        atomId: String,
        atomRebuildRequest: AtomRebuildRequest
    ): Result<Boolean> {
        logger.info("rebuild, projectCode=$projectCode, userId=$userId, atomId=$atomId, request=$atomRebuildRequest")
        // 判断是否可以启动构建
        val status = AtomStatusEnum.BUILDING.status.toByte()
        val (checkResult, code) = checkAtomVersionOptRight(userId, atomId, status)
        if (!checkResult) {
            return MessageUtil.generateResponseDataObject(
                messageCode = code,
                language = I18nUtil.getLanguage(userId))
        }
        // 拉取task.json，检查格式，更新入库
        val atomRecord = marketAtomDao.getAtomRecordById(dslContext, atomId) ?: return Result(false)
        val atomCode = atomRecord.atomCode
        val atomName = atomRecord.name
        val atomVersion = atomRecord.version
        val repoId = atomRecord.repositoryHashId
        val branch = if (atomRecord.branch.isNullOrBlank()) MASTER else atomRecord.branch
        val getAtomConfResult = getAtomConfig(
            projectCode = projectCode,
            atomCode = atomCode,
            atomVersion = atomVersion,
            repositoryHashId = repoId,
            userId = userId,
            branch = branch
        )
        logger.info("rebuild, getAtomConfResult: $getAtomConfResult")
        if (getAtomConfResult.errorCode != "0") {
            return MessageUtil.generateResponseDataObject(
                messageCode = getAtomConfResult.errorCode,
                params = getAtomConfResult.errorParams,
                language = I18nUtil.getLanguage(userId)
            )
        }
        val taskDataMap = getAtomConfResult.taskDataMap
        val atomVersionRecord = marketAtomVersionLogDao.getAtomVersion(dslContext, atomId)
        val releaseType = ReleaseTypeEnum.getReleaseTypeObj(atomVersionRecord.releaseType.toInt())!!
        // 校验插件发布类型
        marketAtomCommonService.validateReleaseType(
            atomId = atomId,
            atomCode = atomCode,
            version = atomVersion,
            releaseType = releaseType,
            taskDataMap = taskDataMap,
            fieldCheckConfirmFlag = atomRebuildRequest.fieldCheckConfirmFlag
        )
        val atomEnvRequests = getAtomConfResult.atomEnvRequests ?: return MessageUtil.generateResponseDataObject(
            messageCode = StoreMessageCode.USER_REPOSITORY_TASK_JSON_FIELD_IS_NULL,
            params = arrayOf(KEY_EXECUTION),
            language = I18nUtil.getLanguage(userId)

        )
        // 解析quality.json
        val getAtomQualityResult = getAtomQualityConfig(
            projectCode = projectCode,
            atomCode = atomCode,
            atomName = atomName,
            atomVersion = atomVersion,
            repositoryHashId = atomRecord.repositoryHashId,
            userId = userId,
            branch = branch
        )
        logger.info("rebuild, getAtomQualityResult: $getAtomQualityResult")
        if (getAtomQualityResult.errorCode == StoreMessageCode.USER_REPOSITORY_PULL_QUALITY_JSON_FILE_FAIL) {
            logger.info("quality.json not found , skip...")
        } else if (getAtomQualityResult.errorCode != "0") {
            return MessageUtil.generateResponseDataObject(
                messageCode = getAtomQualityResult.errorCode,
                params = getAtomQualityResult.errorParams,
                language = I18nUtil.getLanguage(userId)

            )
        }
        val propsMap = mutableMapOf<String, Any?>()
        propsMap[KEY_INPUT_GROUPS] = taskDataMap[KEY_INPUT_GROUPS]
        propsMap[KEY_INPUT] = taskDataMap[KEY_INPUT]
        propsMap[KEY_OUTPUT] = taskDataMap[KEY_OUTPUT]
        propsMap[KEY_CONFIG] = taskDataMap[KEY_CONFIG]

        // 清空redis中保存的发布过程保存的buildId和commitId
        val suffixKeyName = "${StoreTypeEnum.ATOM.name}:$atomCode:$atomId"
        redisOperation.delete("$STORE_REPO_COMMIT_KEY_PREFIX:$suffixKeyName")
        redisOperation.delete("$STORE_REPO_CODECC_BUILD_KEY_PREFIX:$suffixKeyName")
        dslContext.transaction { t ->
            val context = DSL.using(t)
            val props = JsonUtil.toJson(propsMap)
            marketAtomDao.updateMarketAtomProps(context, atomId, props, userId)
            marketAtomEnvInfoDao.deleteAtomEnvInfoById(context, atomId)
            marketAtomEnvInfoDao.addMarketAtomEnvInfo(context, atomId, atomEnvRequests)
            // 执行构建流水线
            runPipeline(
                context = context,
                atomId = atomId,
                userId = userId,
                branch = branch,
                validOsNameFlag = marketAtomCommonService.getValidOsNameFlag(atomEnvRequests),
                validOsArchFlag = marketAtomCommonService.getValidOsArchFlag(atomEnvRequests)
            )
        }
        return Result(true)
    }

    /**
     * 初始化插件版本进度
     */
    private fun initProcessInfo(isNormalUpgrade: Boolean, codeccFlag: Boolean?): List<ReleaseProcessItem> {
        val processInfo = mutableListOf<ReleaseProcessItem>()
        processInfo.add(ReleaseProcessItem(I18nUtil.getCodeLanMessage(messageCode = BEGIN,
            language = I18nUtil.getDefaultLocaleLanguage()), BEGIN, NUM_ONE, SUCCESS))
        processInfo.add(ReleaseProcessItem(I18nUtil.getCodeLanMessage(
            messageCode = COMMIT,
            language = I18nUtil.getDefaultLocaleLanguage()), COMMIT, NUM_TWO, UNDO))
        processInfo.add(ReleaseProcessItem(I18nUtil.getCodeLanMessage(
            messageCode = BUILD,
            language = I18nUtil.getDefaultLocaleLanguage()), BUILD, NUM_THREE, UNDO))
        processInfo.add(ReleaseProcessItem(I18nUtil.getCodeLanMessage(
            messageCode = TEST,
            language = I18nUtil.getDefaultLocaleLanguage()), TEST, NUM_FOUR, UNDO))
        val flag = codeccFlag == null || !codeccFlag
        if (!flag) {
            processInfo.add(ReleaseProcessItem(I18nUtil.getCodeLanMessage(
                messageCode = CODECC,
                language = I18nUtil.getDefaultLocaleLanguage()), CODECC, NUM_FIVE, UNDO))
        }
        if (isNormalUpgrade) {
            val endStep = if (flag) NUM_FIVE else NUM_SIX
            processInfo.add(ReleaseProcessItem(I18nUtil.getCodeLanMessage(
                messageCode = END,
                language = I18nUtil.getDefaultLocaleLanguage()), END, endStep, UNDO))
        } else {
            val approveStep = if (flag) NUM_FIVE else NUM_SIX
            val endStep = if (flag) NUM_SIX else NUM_SEVEN
            processInfo.add(ReleaseProcessItem(I18nUtil.getCodeLanMessage(
                messageCode = APPROVE,
                language = I18nUtil.getDefaultLocaleLanguage()), APPROVE, approveStep, UNDO))
            processInfo.add(ReleaseProcessItem(I18nUtil.getCodeLanMessage(messageCode = END,
                language = I18nUtil.getDefaultLocaleLanguage()), END, endStep, UNDO))
        }
        return processInfo
    }

    @SuppressWarnings("ComplexMethod")
    private fun runPipeline(
        context: DSLContext,
        atomId: String,
        userId: String,
        branch: String? = null,
        validOsNameFlag: Boolean? = null,
        validOsArchFlag: Boolean? = null
    ): Boolean {
        val atomRecord = marketAtomDao.getAtomRecordById(context, atomId) ?: return false
        val atomCode = atomRecord.atomCode
        val atomPipelineRelRecord = storePipelineRelDao.getStorePipelineRel(context, atomCode, StoreTypeEnum.ATOM)
        val initProjectCode = storeProjectRelDao.getInitProjectCodeByStoreCode(
            dslContext = context,
            storeCode = atomCode,
            storeType = StoreTypeEnum.ATOM.type.toByte()
        )!! // 查找新增插件时关联的项目
        val repositoryHashId = atomRecord.repositoryHashId
        // 获取插件代码库最新提交记录
        val getRepoRecentCommitInfoResult = client.get(ServiceGitRepositoryResource::class).getRepoRecentCommitInfo(
            userId = userId,
            repoId = repositoryHashId,
            sha = branch ?: MASTER,
            tokenType = TokenTypeEnum.PRIVATE_KEY
        )
        logger.info("runPipeline  atomId:$atomId,getRepoRecentCommitInfoResult:$getRepoRecentCommitInfoResult")
        if (getRepoRecentCommitInfoResult.isNotOk()) {
            throw ErrorCodeException(
                errorCode = getRepoRecentCommitInfoResult.status.toString(),
                defaultMessage = getRepoRecentCommitInfoResult.message
            )
        }
        val gitCommit = getRepoRecentCommitInfoResult.data!!
        val commitId = gitCommit.id
        val codeccFlag = txStoreCodeccService.getCodeccFlag(StoreTypeEnum.ATOM.name)
        if (codeccFlag == true) {
            handleCodeccTask(atomCode, atomId, commitId)
        }
        val buildInfo = marketAtomBuildInfoDao.getAtomBuildInfo(context, atomId)
        logger.info("atom[$atomCode] buildInfo is:$buildInfo")
        val script = buildInfo.value1()
        val language = buildInfo.value3()
        // 获取打包所需的操作系统名称和操作系统cpu架构
        val atomEnvRecords = marketAtomEnvInfoDao.getMarketAtomEnvInfosByAtomId(context, atomId)
        val osNames = mutableSetOf<String>()
        val osArchs = mutableSetOf<String>()
        var runtimeVersion: String? = null
        val validOsInfos = mutableSetOf<String>()
        atomEnvRecords?.forEach { atomEnvRecord ->
            if (runtimeVersion == null) {
                runtimeVersion = atomEnvRecord.runtimeVersion
            }
            val osName = atomEnvRecord.osName
            if (!osName.isNullOrBlank()) {
                osNames.add(osName)
            }
            val osArch = atomEnvRecord.osArch
            if (!osArch.isNullOrBlank()) {
                osArchs.add(osArch)
            }
            validOsInfos.add("$osName-$osArch")
        }
        val invalidOsInfos = getInvalidOsInfos(osNames, osArchs, validOsInfos)
        if (osNames.isEmpty()) {
            osNames.add(OSType.LINUX.name.toLowerCase())
        }
        if (osArchs.isEmpty()) {
            osArchs.add("amd64")
        }
        if (null == atomPipelineRelRecord) {
            // 为用户初始化构建流水线并触发执行
            val version = atomRecord.version
            val atomBaseInfo = AtomBaseInfo(
                atomId = atomId,
                atomCode = atomCode,
                version = atomRecord.version,
                atomStatus = AtomStatusEnum.getAtomStatus(atomRecord.atomStatus.toInt()),
                language = language,
                commitId = commitId,
                branch = branch ?: MASTER,
                osName = JsonUtil.toJson(osNames),
                osArch = JsonUtil.toJson(osArchs),
                invalidOsInfo = JsonUtil.toJson(invalidOsInfos),
                runtimeVersion = runtimeVersion
            )
            val pipelineModelConfig = businessConfigDao.get(
                dslContext = context,
                business = StoreTypeEnum.ATOM.name,
                feature = "initBuildPipeline",
                businessValue = "PIPELINE_MODEL"
            )
            var pipelineModel = pipelineModelConfig!!.configValue
            val pipelineName = "am-$atomCode-${UUIDUtil.generate()}"
            val paramMap = mapOf(
                KEY_PIPELINE_NAME to pipelineName,
                KEY_STORE_CODE to atomCode,
                KEY_VERSION to version,
                KEY_LANGUAGE to language,
                KEY_SCRIPT to StringEscapeUtils.escapeJava(script),
                KEY_REPOSITORY_HASH_ID to atomRecord.repositoryHashId,
                KEY_REPOSITORY_PATH to (buildInfo.value2() ?: ""),
                KEY_BRANCH to (branch ?: MASTER)
            )
            // 将流水线模型中的变量替换成具体的值
            paramMap.forEach { (key, value) ->
                pipelineModel = pipelineModel.replace("#{$key}", value)
            }
            val atomMarketInitPipelineReq = AtomMarketInitPipelineReq(
                pipelineModel = pipelineModel,
                script = script,
                atomBaseInfo = atomBaseInfo,
                validOsNameFlag = validOsNameFlag,
                validOsArchFlag = validOsArchFlag
            )
            val atomMarketInitPipelineResp = client.get(ServicePipelineInitResource::class)
                .initAtomMarketPipeline(userId, initProjectCode, atomMarketInitPipelineReq).data
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
            val buildInfoRecord = storePipelineBuildRelDao.getStorePipelineBuildRel(dslContext, atomId)
            // 判断插件版本最近一次的构建是否完成
            val buildResult = if (buildInfoRecord != null) {
                client.get(ServiceBuildResource::class).getBuildStatus(
                    userId = userId,
                    projectId = initProjectCode,
                    pipelineId = buildInfoRecord.pipelineId,
                    buildId = buildInfoRecord.buildId,
                    channelCode = ChannelCode.AM
                ).data
            } else {
                null
            }
            if (buildResult != null) {
                val buildStatus = BuildStatus.parse(buildResult.status)
                if (!buildStatus.isFinish()) {
                    // 最近一次构建还未完全结束，给出错误提示
                    throw ErrorCodeException(
                        errorCode = StoreMessageCode.USER_ATOM_VERSION_IS_NOT_FINISH,
                        params = arrayOf(atomRecord.name, atomRecord.version)
                    )
                }
            }
            // 触发执行流水线
            val startParams = mutableMapOf<String, String>() // 启动参数
            startParams[KEY_ATOM_CODE] = atomCode
            startParams[KEY_VERSION] = atomRecord.version
            startParams[KEY_LANGUAGE] = language
            startParams[KEY_SCRIPT] = script
            startParams[KEY_COMMIT_ID] = commitId
            startParams[KEY_BRANCH] = branch ?: MASTER
            startParams[KEY_OS_NAME] = JsonUtil.toJson(osNames)
            startParams[KEY_OS_ARCH] = JsonUtil.toJson(osArchs)
            startParams[KEY_INVALID_OS_INFO] = JsonUtil.toJson(invalidOsInfos)
            runtimeVersion?.let { startParams[KEY_RUNTIME_VERSION] = it }
            validOsNameFlag?.let {
                startParams[KEY_VALID_OS_NAME_FLAG] = it.toString()
            }
            validOsArchFlag?.let {
                startParams[KEY_VALID_OS_ARCH_FLAG] = it.toString()
            }
            val buildIdObj = client.get(ServiceBuildResource::class).manualStartup(
                userId, initProjectCode, atomPipelineRelRecord.pipelineId, startParams,
                ChannelCode.AM
            ).data
            logger.info("the buildIdObj is:$buildIdObj")
            if (null != buildIdObj) {
                storePipelineBuildRelDao.add(context, atomId, atomPipelineRelRecord.pipelineId, buildIdObj.id)
                marketAtomDao.setAtomStatusById(
                    dslContext = context,
                    atomId = atomId,
                    atomStatus = AtomStatusEnum.BUILDING.status.toByte(),
                    userId = userId,
                    msg = null
                ) // 构建中
            } else {
                marketAtomDao.setAtomStatusById(
                    dslContext = context,
                    atomId = atomId,
                    atomStatus = AtomStatusEnum.BUILD_FAIL.status.toByte(),
                    userId = userId,
                    msg = null
                ) // 构建失败
            }
            // 通过websocket推送状态变更消息
            storeWebsocketService.sendWebsocketMessage(userId, atomId)
        }
        return true
    }

    private fun getInvalidOsInfos(
        osNames: MutableSet<String>,
        osArchs: MutableSet<String>,
        validOsInfos: MutableSet<String>
    ): ArrayList<Map<String, String>> {
        val invalidOsInfos = arrayListOf<Map<String, String>>()
        if (osNames.isEmpty() && osArchs.isEmpty()) {
            return invalidOsInfos
        }
        osNames.forEach { osName ->
            osArchs.forEach { osArch ->
                if (!validOsInfos.contains("$osName-$osArch")) {
                    invalidOsInfos.add(mapOf(KEY_OS_NAME to osName, KEY_OS_ARCH to osArch))
                }
            }
        }
        return invalidOsInfos
    }

    private fun handleCodeccTask(
        atomCode: String,
        atomId: String,
        commitId: String
    ) {
        // 把代码提交ID存入redis
        redisOperation.set(
            key = "$STORE_REPO_COMMIT_KEY_PREFIX:${StoreTypeEnum.ATOM.name}:$atomCode:$atomId",
            value = commitId,
            expired = false
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
                value = buildId,
                expired = false
            )
        }
    }

    /**
     * 检查版本发布过程中的操作权限
     */
    @SuppressWarnings("ComplexMethod")
    override fun checkAtomVersionOptRight(
        userId: String,
        atomId: String,
        status: Byte,
        isNormalUpgrade: Boolean?
    ): Pair<Boolean, String> {
        logger.info("checkAtomVersionOptRight params[$userId|$atomId|$status|$isNormalUpgrade]")
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
                dslContext = dslContext,
                userId = userId,
                storeCode = atomCode,
                storeType = StoreTypeEnum.ATOM.type.toByte()
            ) || creator == userId)
        ) {
            return Pair(false, CommonMessageCode.PERMISSION_DENIED)
        }
        val allowReleaseStatus =
            if (isNormalUpgrade != null && isNormalUpgrade) AtomStatusEnum.TESTING else AtomStatusEnum.AUDITING
        val codeccFlag = txStoreCodeccService.getCodeccFlag(StoreTypeEnum.ATOM.name)
        val allowAuditStatus =
            if (codeccFlag != null && !codeccFlag) AtomStatusEnum.TESTING else AtomStatusEnum.CODECCING
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
                    AtomStatusEnum.TESTING.status.toByte(),
                    AtomStatusEnum.CODECC_FAIL.status.toByte()
                ))
        ) {
            validateFlag = false
        } else if (status == AtomStatusEnum.BUILD_FAIL.status.toByte() &&
            recordStatus != AtomStatusEnum.BUILDING.status.toByte()
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
            recordStatus != allowAuditStatus.status.toByte()
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
