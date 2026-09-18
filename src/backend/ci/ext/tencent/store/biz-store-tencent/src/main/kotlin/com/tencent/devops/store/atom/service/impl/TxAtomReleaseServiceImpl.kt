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

package com.tencent.devops.store.atom.service.impl

import com.tencent.devops.common.api.constant.APPROVE
import com.tencent.devops.common.api.constant.BEGIN
import com.tencent.devops.common.api.constant.BUILD
import com.tencent.devops.common.api.constant.CODECC
import com.tencent.devops.common.api.constant.COMMIT
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.constant.CommonMessageCode.PARAMETER_IS_NULL
import com.tencent.devops.common.api.constant.DOING
import com.tencent.devops.common.api.constant.END
import com.tencent.devops.common.api.constant.FAIL
import com.tencent.devops.common.api.constant.HTTP_404
import com.tencent.devops.common.api.constant.JS
import com.tencent.devops.common.api.constant.KEY_BRANCH
import com.tencent.devops.common.api.constant.KEY_COMMIT_ID
import com.tencent.devops.common.api.constant.KEY_INVALID_OS_INFO
import com.tencent.devops.common.api.constant.KEY_OS_ARCH
import com.tencent.devops.common.api.constant.KEY_OS_NAME
import com.tencent.devops.common.api.constant.KEY_REPOSITORY_HASH_ID
import com.tencent.devops.common.api.constant.KEY_REPOSITORY_PATH
import com.tencent.devops.common.api.constant.KEY_SCRIPT
import com.tencent.devops.common.api.constant.KEY_VALID_OS_ARCH_FLAG
import com.tencent.devops.common.api.constant.KEY_VALID_OS_NAME_FLAG
import com.tencent.devops.common.api.constant.KEY_VERSION
import com.tencent.devops.common.api.constant.MASTER
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
import com.tencent.devops.common.api.enums.OSType
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.plugin.codecc.CodeccApi
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.repository.api.ServiceGitRepositoryResource
import com.tencent.devops.repository.pojo.RepositoryInfo
import com.tencent.devops.repository.pojo.enums.TokenTypeEnum
import com.tencent.devops.repository.pojo.enums.VisibilityLevelEnum
import com.tencent.devops.store.atom.dao.MarketAtomBuildInfoDao
import com.tencent.devops.model.store.tables.records.TAtomRecord
import com.tencent.devops.store.atom.service.TxAtomReleaseService
import com.tencent.devops.store.common.configuration.StoreInnerPipelineConfig
import com.tencent.devops.store.common.dao.StoreBuildInfoDao
import com.tencent.devops.store.common.dao.StorePipelineBuildRelDao
import com.tencent.devops.store.common.dao.StorePipelineRelDao
import com.tencent.devops.store.common.service.StorePipelineService
import com.tencent.devops.store.common.service.TxStoreBelongDeptService
import com.tencent.devops.store.common.service.TxStoreCodeccService
import com.tencent.devops.store.common.utils.StoreUtils
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.constant.StoreMessageCode.NO_COMPONENT_ADMIN_AND_CREATETOR_PERMISSION
import com.tencent.devops.store.constant.StoreMessageCode.NO_COMPONENT_ADMIN_PERMISSION
import com.tencent.devops.store.constant.StoreMessageCode.STORE_ATOM_BUILD_START_FAIL
import com.tencent.devops.store.constant.StoreMessageCode.STORE_ATOM_FORMAL_VERSION_NOT_FOUND
import com.tencent.devops.store.constant.StoreMessageCode.STORE_ATOM_INIT_PROJECT_NOT_FOUND
import com.tencent.devops.store.constant.StoreMessageCode.STORE_ATOM_OPERATE_CONCURRENT
import com.tencent.devops.store.constant.StoreMessageCode.STORE_BRANCH_NO_NEW_COMMIT
import com.tencent.devops.store.constant.StoreMessageCode.STORE_BRANCH_TEST_CREATE_FAIL
import com.tencent.devops.store.constant.StoreMessageCode.STORE_BRANCH_TEST_END_STATUS_INVALID
import com.tencent.devops.store.constant.StoreMessageCode.STORE_BRANCH_TEST_REPO_REQUIRED
import com.tencent.devops.store.constant.StoreMessageCode.USER_NOT_IS_STORE_MEMBER
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.store.pojo.atom.AtomRebuildRequest
import com.tencent.devops.store.pojo.atom.AtomReleaseRequest
import com.tencent.devops.store.pojo.atom.MarketAtomCreateRequest
import com.tencent.devops.store.pojo.atom.MarketAtomUpdateRequest
import com.tencent.devops.store.pojo.atom.UpdateAtomPackageInfo
import com.tencent.devops.store.pojo.atom.enums.AtomStatusEnum
import com.tencent.devops.store.pojo.common.BK_FRONTEND_DIR_NAME
import com.tencent.devops.store.pojo.common.KEY_ATOM_CODE
import com.tencent.devops.store.pojo.common.KEY_CODE_SRC
import com.tencent.devops.store.pojo.common.KEY_CONFIG
import com.tencent.devops.store.pojo.common.KEY_EXECUTION
import com.tencent.devops.store.pojo.common.KEY_INPUT
import com.tencent.devops.store.pojo.common.KEY_INPUT_GROUPS
import com.tencent.devops.store.pojo.common.KEY_LANGUAGE
import com.tencent.devops.store.pojo.common.KEY_OUTPUT
import com.tencent.devops.store.pojo.common.KEY_PACKAGE_PATH
import com.tencent.devops.store.pojo.common.KEY_RUNTIME_VERSION
import com.tencent.devops.store.pojo.common.STORE_REPO_CODECC_BUILD_KEY_PREFIX
import com.tencent.devops.store.pojo.common.STORE_REPO_COMMIT_KEY_PREFIX
import com.tencent.devops.store.pojo.common.StoreI18nConfig
import com.tencent.devops.store.pojo.common.enums.PackageSourceTypeEnum
import com.tencent.devops.store.pojo.common.enums.ReleaseTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.common.publication.ReleaseProcessItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.Executors
import kotlin.collections.forEach
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.context.config.annotation.RefreshScope
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service

@Primary
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
    lateinit var codeccApi: CodeccApi

    @Autowired
    lateinit var txStoreCodeccService: TxStoreCodeccService

    @Autowired
    lateinit var storeInnerPipelineConfig: StoreInnerPipelineConfig

    @Autowired
    lateinit var storePipelineService: StorePipelineService

    @Autowired
    private lateinit var txStoreBelongDeptService: TxStoreBelongDeptService

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
        if (marketAtomCreateRequest.packageSourceType == PackageSourceTypeEnum.UPLOAD) {
            return Result(data = null)
        }
        marketAtomCreateRequest.authType ?: return I18nUtil.generateResponseDataObject(
            messageCode = CommonMessageCode.PARAMETER_IS_NULL,
            params = arrayOf("authType"),
            data = null,
            language = I18nUtil.getLanguage(userId)
        )
        marketAtomCreateRequest.visibilityLevel ?: return I18nUtil.generateResponseDataObject(
            messageCode = CommonMessageCode.PARAMETER_IS_NULL,
            params = arrayOf("visibilityLevel"),
            data = null,
            language = I18nUtil.getLanguage(userId)
        )
        val repositoryInfo: RepositoryInfo?
        if (marketAtomCreateRequest.visibilityLevel == VisibilityLevelEnum.PRIVATE) {
            if (marketAtomCreateRequest.privateReason.isNullOrBlank()) {
                return I18nUtil.generateResponseDataObject(
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
                )?.sampleProjectPath,
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
            return I18nUtil.generateResponseDataObject(
                messageCode = StoreMessageCode.USER_CREATE_REPOSITORY_FAIL,
                language = I18nUtil.getLanguage(userId)
            )
        }
        if (null == repositoryInfo) {
            return I18nUtil.generateResponseDataObject(
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
            val createCodeccPipelineResult = codeccApi.createCodeccPipeline(
                repoId = repositoryInfo.aliasName,
                languages = codeccLanguages
            )
            logger.info("createCodeccPipelineResult is :$createCodeccPipelineResult")
        }
        return Result(mapOf("repositoryHashId" to repositoryInfo.repositoryHashId!!, "codeSrc" to repositoryInfo.url))
    }

    override fun handleAtomExtend(marketAtomCreateRequest: MarketAtomCreateRequest, userId: String, atomCode: String) {
        txStoreBelongDeptService.initStoreBelongDept(
            userId = userId,
            storeCode = atomCode,
            storeType = StoreTypeEnum.ATOM
        )
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
        return if (!repositoryHashId.isNullOrBlank()) {
            // 从工蜂拉取文件
            try {
                client.get(ServiceGitRepositoryResource::class).getFileContent(
                    repoId = repositoryHashId,
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
        repositoryHashId: String?
    ): Result<Boolean> {
        logger.info("validateUpdateMarketAtomReq userId is:$userId,marketAtomUpdateRequest is:$marketAtomUpdateRequest")
        val frontendType = marketAtomUpdateRequest.frontendType
        val branch = marketAtomUpdateRequest.branch
        if (frontendType != FrontendTypeEnum.SPECIAL || repositoryHashId.isNullOrBlank()) {
            return Result(true)
        }
        val repositoryTreeInfoResult = client.get(ServiceGitRepositoryResource::class).getGitRepositoryTreeInfo(
            userId = userId,
            repoId = repositoryHashId,
            refName = branch,
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
            return I18nUtil.generateResponseDataObject(
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
        status: Int,
        branchTestFlag: Boolean
    ): List<ReleaseProcessItem> {
        // 分支测试版本：仅展示到测试环节，不展示测试之后的发布流程
        if (branchTestFlag) {
            return handleBranchTestProcessInfo(status)
        }
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

    /**
     * 分支测试版本进度：仅包含提交、构建、测试环节，不展示测试之后的发布流程
     */
    private fun handleBranchTestProcessInfo(status: Int): List<ReleaseProcessItem> {
        val processInfo = mutableListOf<ReleaseProcessItem>()
        processInfo.add(ReleaseProcessItem(I18nUtil.getCodeLanMessage(messageCode = BEGIN), BEGIN, NUM_ONE, SUCCESS))
        processInfo.add(ReleaseProcessItem(I18nUtil.getCodeLanMessage(messageCode = COMMIT), COMMIT, NUM_TWO, UNDO))
        processInfo.add(ReleaseProcessItem(I18nUtil.getCodeLanMessage(messageCode = BUILD), BUILD, NUM_THREE, UNDO))
        processInfo.add(ReleaseProcessItem(I18nUtil.getCodeLanMessage(messageCode = TEST), TEST, NUM_FOUR, UNDO))
        when (status) {
            AtomStatusEnum.INIT.status, AtomStatusEnum.COMMITTING.status -> {
                storeCommonService.setProcessInfo(processInfo, NUM_FOUR, NUM_TWO, DOING)
            }
            AtomStatusEnum.BUILDING.status, AtomStatusEnum.CODECCING.status -> {
                storeCommonService.setProcessInfo(processInfo, NUM_FOUR, NUM_THREE, DOING)
            }
            AtomStatusEnum.BUILD_FAIL.status, AtomStatusEnum.CODECC_FAIL.status -> {
                storeCommonService.setProcessInfo(processInfo, NUM_FOUR, NUM_THREE, FAIL)
            }
            AtomStatusEnum.TESTING.status -> {
                // 测试进行中：先标记前序步骤完成，再手动将测试步骤置为进行中（避免末步被置为完成）
                storeCommonService.setProcessInfo(processInfo, NUM_FOUR, NUM_THREE, SUCCESS)
                processInfo.firstOrNull { it.step == NUM_FOUR }?.status = DOING
            }
            AtomStatusEnum.TESTED.status, AtomStatusEnum.GROUNDING_SUSPENSION.status -> {
                // 测试完成（含取消发布）：测试步骤标记完成
                storeCommonService.setProcessInfo(processInfo, NUM_FOUR, NUM_FOUR, SUCCESS)
            }
            else -> {
                // 其余状态（AUDITING/AUDIT_REJECT/RELEASED/下架等）不改动默认进度
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
        val repositoryHashId = atomReleaseRequest.repositoryHashId
        if (repositoryHashId.isNullOrBlank()) {
            return
        }
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
        val (checkResult, code, params) = checkAtomVersionOptRight(userId, atomId, status)
        if (!checkResult) {
            return I18nUtil.generateResponseDataObject(
                messageCode = code,
                language = I18nUtil.getLanguage(userId),
                params = params
            )
        }
        // 拉取task.json，检查格式，更新入库
        val atomRecord = marketAtomDao.getAtomRecordById(dslContext, atomId) ?: return Result(false)
        val atomCode = atomRecord.atomCode
        val atomName = atomRecord.name
        val atomVersion = atomRecord.version
        storeFileService.cleanStoreVersionReferenceFile(atomCode, atomVersion)
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
            return I18nUtil.generateResponseDataObject(
                messageCode = getAtomConfResult.errorCode,
                params = getAtomConfResult.errorParams,
                language = I18nUtil.getLanguage(userId)
            )
        }
        val taskJsonMap = getAtomConfResult.taskDataMap
        val executionInfoMap = taskJsonMap[KEY_EXECUTION] as Map<String, Any>
        val atomLanguage = executionInfoMap[KEY_LANGUAGE].toString()
        val i18nDir = StoreUtils.getStoreI18nDir(atomLanguage, getAtomPackageSourceType(repoId))
        val taskDataMap = storeI18nMessageService.parseJsonMapI18nInfo(
            userId = userId,
            jsonMap = taskJsonMap.toMutableMap(),
            storeI18nConfig = StoreI18nConfig(
                projectCode = projectCode,
                storeCode = atomCode,
                fileDir = "$atomCode/$atomVersion",
                i18nDir = i18nDir,
                dbKeyPrefix = StoreUtils.getStoreFieldKeyPrefix(StoreTypeEnum.ATOM, atomCode, atomVersion),
                repositoryHashId = repoId,
                branch = branch
            ),
            version = atomVersion
        )
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
        val atomEnvRequests = getAtomConfResult.atomEnvRequests ?: return I18nUtil.generateResponseDataObject(
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
            i18nDir = i18nDir,
            branch = branch
        )
        logger.info("rebuild, getAtomQualityResult: $getAtomQualityResult")
        if (getAtomQualityResult.errorCode == StoreMessageCode.USER_REPOSITORY_PULL_QUALITY_JSON_FILE_FAIL) {
            logger.info("quality.json not found , skip...")
        } else if (getAtomQualityResult.errorCode != "0") {
            return I18nUtil.generateResponseDataObject(
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
        processInfo.add(ReleaseProcessItem(I18nUtil.getCodeLanMessage(messageCode = BEGIN), BEGIN, NUM_ONE, SUCCESS))
        processInfo.add(ReleaseProcessItem(I18nUtil.getCodeLanMessage(messageCode = COMMIT), COMMIT, NUM_TWO, UNDO))
        processInfo.add(ReleaseProcessItem(I18nUtil.getCodeLanMessage(messageCode = BUILD), BUILD, NUM_THREE, UNDO))
        processInfo.add(ReleaseProcessItem(I18nUtil.getCodeLanMessage(messageCode = TEST), TEST, NUM_FOUR, UNDO))
        val flag = codeccFlag == null || !codeccFlag
        if (!flag) {
            processInfo.add(
                ReleaseProcessItem(
                    name = I18nUtil.getCodeLanMessage(messageCode = CODECC),
                    code = CODECC,
                    step = NUM_FIVE,
                    status = UNDO
                )
            )
        }
        if (isNormalUpgrade) {
            val endStep = if (flag) NUM_FIVE else NUM_SIX
            processInfo.add(ReleaseProcessItem(I18nUtil.getCodeLanMessage(messageCode = END), END, endStep, UNDO))
        } else {
            val approveStep = if (flag) NUM_FIVE else NUM_SIX
            val endStep = if (flag) NUM_SIX else NUM_SEVEN
            processInfo.add(
                ReleaseProcessItem(I18nUtil.getCodeLanMessage(messageCode = APPROVE), APPROVE, approveStep, UNDO)
            )
            processInfo.add(ReleaseProcessItem(I18nUtil.getCodeLanMessage(messageCode = END), END, endStep, UNDO))
        }
        return processInfo
    }

    @SuppressWarnings("ComplexMethod", "LongMethod")
    private fun runPipeline(
        context: DSLContext,
        atomId: String,
        userId: String,
        branch: String? = null,
        validOsNameFlag: Boolean? = null,
        validOsArchFlag: Boolean? = null
    ): Boolean {
        val atomRecord = marketAtomDao.getAtomRecordById(context, atomId) ?: return false
        val repositoryHashId = atomRecord.repositoryHashId
        if (repositoryHashId.isNullOrBlank()) {
            return false
        }
        val atomCode = atomRecord.atomCode
        val atomPipelineRelRecord = storePipelineRelDao.getStorePipelineRel(context, atomCode, StoreTypeEnum.ATOM)
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
            osNames.add(OSType.LINUX.name.lowercase())
        }
        if (osArchs.isEmpty()) {
            osArchs.add("amd64")
        }
        val startParams = mutableMapOf<String, String>() // 启动参数
        startParams[KEY_ATOM_CODE] = atomCode
        startParams[KEY_VERSION] = atomRecord.version
        startParams[KEY_COMMIT_ID] = commitId
        startParams[KEY_BRANCH] = branch ?: MASTER
        startParams[KEY_OS_NAME] = JsonUtil.toJson(osNames)
        startParams[KEY_OS_ARCH] = JsonUtil.toJson(osArchs)
        startParams[KEY_INVALID_OS_INFO] = JsonUtil.toJson(invalidOsInfos)
        startParams[KEY_CODE_SRC] = atomRecord.codeSrc
        startParams[KEY_REPOSITORY_PATH] = (buildInfo.value2() ?: "")
        startParams[KEY_REPOSITORY_HASH_ID] = atomRecord.repositoryHashId
        startParams[KEY_LANGUAGE] = language
        startParams[KEY_SCRIPT] = buildInfo.value1()
        runtimeVersion?.let { startParams[KEY_RUNTIME_VERSION] = it }
        validOsNameFlag?.let {
            startParams[KEY_VALID_OS_NAME_FLAG] = it.toString()
        }
        validOsArchFlag?.let {
            startParams[KEY_VALID_OS_ARCH_FLAG] = it.toString()
        }
        val innerPipelineProject = storeInnerPipelineConfig.innerPipelineProject
        val innerPipelineUser = storeInnerPipelineConfig.innerPipelineUser
        val pipelineId: String?
        var projectCode = innerPipelineProject
        var pipelineUser = innerPipelineUser
        if (atomPipelineRelRecord == null) {
            pipelineId = storePipelineService.creatStorePipelineByStoreCode(storeType = StoreTypeEnum.ATOM.name)
        } else {
            pipelineId = atomPipelineRelRecord.pipelineId
            if (atomPipelineRelRecord.projectCode != innerPipelineProject) {
                projectCode = atomPipelineRelRecord.projectCode
                pipelineUser = userId
            }
            val buildInfoRecord = storePipelineBuildRelDao.getStorePipelineBuildRel(dslContext, atomId)
            // 判断插件版本最近一次的构建是否完成
            val buildResult = if (buildInfoRecord != null) {
                client.get(ServiceBuildResource::class).getBuildStatusWithoutPermission(
                    userId = pipelineUser,
                    projectId = projectCode,
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
        }
        // 触发执行流水线
        val buildIdObj = client.get(ServiceBuildResource::class).manualStartupNew(
            userId = pipelineUser,
            projectId = projectCode,
            pipelineId = pipelineId!!,
            values = startParams,
            channelCode = ChannelCode.AM,
            startType = StartType.SERVICE
        ).data
        logger.info("the buildIdObj is:$buildIdObj")

        if (null != buildIdObj) {
            storePipelineBuildRelDao.add(context, atomId, pipelineId, buildIdObj.id)
            if (atomPipelineRelRecord == null) {
                storePipelineRelDao.add(
                    dslContext = context,
                    storeCode = atomCode,
                    storeType = StoreTypeEnum.ATOM,
                    pipelineId = pipelineId,
                    projectCode = storeInnerPipelineConfig.innerPipelineProject
                )
            }
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
            val startCodeccTaskResult = codeccApi.startCodeccTask(
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
    ): Triple<Boolean, String, Array<String>?> {
        logger.info("checkAtomVersionOptRight params[$userId|$atomId|$status|$isNormalUpgrade]")
        val record =
            marketAtomDao.getAtomRecordById(dslContext, atomId) ?: return Triple(
                false,
                CommonMessageCode.PARAMETER_IS_INVALID,
                null
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
            return Triple(false, NO_COMPONENT_ADMIN_PERMISSION, arrayOf(atomCode))
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

        return if (validateFlag) Triple(true, "", null)
        else Triple(false, StoreMessageCode.USER_ATOM_RELEASE_STEPS_ERROR, null)
    }

    @Suppress("ComplexMethod")
    override fun creatAtomBranchTestVersion(
        userId: String,
        marketAtomUpdateRequest: MarketAtomUpdateRequest
    ): Result<String> {
        val atomCode = marketAtomUpdateRequest.atomCode
        val branch = marketAtomUpdateRequest.branch ?: throw ErrorCodeException(
            errorCode = PARAMETER_IS_NULL,
            params = arrayOf("branch")
        )
        // 判断用户是否有权限(插件成员可以操作)
        if (!(storeMemberDao.isStoreMember(
                dslContext = dslContext,
                userId = userId,
                storeCode = atomCode,
                storeType = StoreTypeEnum.ATOM.type.toByte()
            ))
        ) {
            throw ErrorCodeException(
                errorCode = USER_NOT_IS_STORE_MEMBER,
                params = arrayOf(userId)
            )
        }
        // 仅代码库来源的插件支持分支测试版本
        val baseAtomRecord = atomDao.getMaxVersionAtomByCode(dslContext, atomCode)
            ?: throw ErrorCodeException(
                errorCode = STORE_ATOM_FORMAL_VERSION_NOT_FOUND,
                params = arrayOf(atomCode)
            )
        if (baseAtomRecord.repositoryHashId.isNullOrBlank()) {
            throw ErrorCodeException(
                errorCode = STORE_BRANCH_TEST_REPO_REQUIRED,
                params = arrayOf(atomCode)
            )
        }
        return try {
            doCreateBranchTestVersion(userId, marketAtomUpdateRequest, atomCode, branch)
        } catch (e: ErrorCodeException) {
            throw e
        } catch (e: Throwable) {
            logger.warn("creatAtomBranchTestVersion error, atomCode=$atomCode, branch=$branch", e)
            throw ErrorCodeException(errorCode = STORE_BRANCH_TEST_CREATE_FAIL)
        }
    }

    /**
     * 创建分支测试版本：分支提交时间拉取、配置解析与构建启动在锁外，落点判定与写库在分支锁内
     */
    private fun doCreateBranchTestVersion(
        userId: String,
        marketAtomUpdateRequest: MarketAtomUpdateRequest,
        atomCode: String,
        branch: String
    ): Result<String> {
        val projectCode = storeProjectRelDao.getInitProjectCodeByStoreCode(
            dslContext = dslContext,
            storeCode = atomCode,
            storeType = StoreTypeEnum.ATOM.type.toByte()
        ) ?: throw ErrorCodeException(
            errorCode = STORE_ATOM_INIT_PROJECT_NOT_FOUND
        )
        // 读取当前分支测试版本；非构建失败态时要求分支有新提交，并保留提交时间供锁内复用
        val versionRecord = marketAtomDao.getLatestBranchTestVersion(dslContext, atomCode, branch)
        val recentCommitMillis = if (versionRecord != null && !isBranchTestBuildFailed(versionRecord.atomStatus)) {
            val commitMillis = fetchBranchRecentCommitMillis(versionRecord, userId, branch)
            if (commitMillis <= versionRecord.updateTime.timestampmilli()) {
                throw ErrorCodeException(errorCode = STORE_BRANCH_NO_NEW_COMMIT)
            }
            commitMillis
        } else {
            null
        }
        val newVersionFlag = isNeedNewVersion(versionRecord, recentCommitMillis)
        // 复用原版本时必然存在历史版本记录
        val reuseRecord = if (newVersionFlag) null else checkNotNull(versionRecord)
        val version = reuseRecord?.version ?: generateBranchTestVersion(branch)
        val atomId = reuseRecord?.id ?: UUIDUtil.generate()
        marketAtomUpdateRequest.version = version

        val atomRecord = versionRecord
            ?: atomDao.getMaxVersionAtomByCode(dslContext, atomCode)
            ?: throw ErrorCodeException(
                errorCode = STORE_ATOM_FORMAL_VERSION_NOT_FOUND,
                params = arrayOf(atomCode)
            )
        val atomPackageSourceType = getAtomPackageSourceType(atomRecord.repositoryHashId)

        val getAtomConfResult = getAtomConfig(
            projectCode = projectCode,
            atomCode = atomCode,
            atomVersion = version,
            userId = userId,
            repositoryHashId = atomRecord.repositoryHashId,
            branch = branch
        )
        if (getAtomConfResult.errorCode != "0") {
            return I18nUtil.generateResponseDataObject(
                messageCode = getAtomConfResult.errorCode,
                params = getAtomConfResult.errorParams,
                language = I18nUtil.getLanguage(userId)
            )
        }
        marketAtomUpdateRequest.branchTestFlag = true
        val taskJsonMap = getAtomConfResult.taskDataMap
        val executionInfoMap = taskJsonMap[KEY_EXECUTION] as Map<String, Any>
        val atomLanguage = executionInfoMap[KEY_LANGUAGE].toString()
        val i18nDir = StoreUtils.getStoreI18nDir(atomLanguage, atomPackageSourceType)

        val preparedResult = prepareAtomVersionInfo(
            userId = userId,
            projectCode = projectCode,
            newVersionFlag = newVersionFlag,
            updateAtomPackageInfo = UpdateAtomPackageInfo(
                atomId = atomId,
                i18nDir = i18nDir,
                packagePath = executionInfoMap[KEY_PACKAGE_PATH] as? String,
                atomPackageSourceType = atomPackageSourceType
            ),
            convertUpdateRequest = marketAtomUpdateRequest,
            getAtomConfResult = getAtomConfResult
        )
        val prepared = preparedResult.data
            ?: return Result(preparedResult.status, preparedResult.message, null)

        // 并发控制：同一插件同一分支的创建操作串行化
        RedisLock(
            redisOperation,
            branchTestLockKey(atomCode, branch),
            60L
        ).use { redisLock ->
            if (!redisLock.tryLock()) {
                throw ErrorCodeException(errorCode = STORE_ATOM_OPERATE_CONCURRENT)
            }
            val versionRecordInLock = marketAtomDao.getLatestBranchTestVersion(dslContext, atomCode, branch)
            // 锁外准备期间落点发生变化即报冲突，避免用错版本的准备数据写库
            if (versionRecordInLock?.id != versionRecord?.id ||
                isNeedNewVersion(versionRecordInLock, recentCommitMillis) != newVersionFlag
            ) {
                throw ErrorCodeException(errorCode = STORE_ATOM_OPERATE_CONCURRENT)
            }
            commitAtomVersionInfo(prepared)
        }
        // 构建启动在锁外执行
        val result = startAtomBuild(prepared)
        // 构建未启动时返回失败
        val createdAtomId = result.data
        if (createdAtomId != null && marketAtomDao.getAtomRecordById(dslContext, createdAtomId)
                ?.atomStatus == AtomStatusEnum.BUILD_FAIL.status.toByte()
        ) {
            return I18nUtil.generateResponseDataObject(
                messageCode = STORE_ATOM_BUILD_START_FAIL,
                language = I18nUtil.getLanguage(userId)
            )
        }
        return result
    }

    /**
     * 分支测试版本是否处于构建失败态（构建失败或代码检查失败）
     */
    private fun isBranchTestBuildFailed(atomStatus: Byte?): Boolean =
        atomStatus == AtomStatusEnum.BUILD_FAIL.status.toByte() ||
            atomStatus == AtomStatusEnum.CODECC_FAIL.status.toByte()

    /**
     * 是否需要新建分支测试版本
     * @param recentCommitMillis 分支最新提交时间（毫秒时间戳），为空表示无历史版本或处于构建失败态
     */
    private fun isNeedNewVersion(versionRecord: TAtomRecord?, recentCommitMillis: Long?): Boolean {
        if (versionRecord == null) {
            return true
        }
        if (isBranchTestBuildFailed(versionRecord.atomStatus)) {
            return false
        }
        if (recentCommitMillis == null || recentCommitMillis <= versionRecord.updateTime.timestampmilli()) {
            return false
        }
        return versionRecord.atomStatus != AtomStatusEnum.TESTING.status.toByte() &&
            versionRecord.atomStatus != AtomStatusEnum.BUILDING.status.toByte() &&
            versionRecord.atomStatus != AtomStatusEnum.BUILD_FAIL.status.toByte()
    }

    /**
     * 拉取分支最新提交时间（毫秒时间戳）
     */
    private fun fetchBranchRecentCommitMillis(versionRecord: TAtomRecord, userId: String, branch: String): Long {
        val recentCommitInfo = client.get(ServiceGitRepositoryResource::class).getRepoRecentCommitInfo(
            userId = userId,
            repoId = versionRecord.repositoryHashId,
            sha = branch,
            tokenType = TokenTypeEnum.OAUTH
        ).data ?: throw ErrorCodeException(errorCode = StoreMessageCode.GET_BRANCH_COMMIT_INFO_ERROR)
        return DateTimeUtil.zoneDateToTimestamp(recentCommitInfo.committed_date)
    }

    /**
     * 生成分支测试版本号：test-{branch}-{yyyyMMddHHmmss}，秒级唯一
     */
    private fun generateBranchTestVersion(branch: String): String {
        val dateFormat = SimpleDateFormat("yyyyMMddHHmmss")
        return "test-$branch-${dateFormat.format(Date())}"
    }

    override fun endBranchVersionTest(userId: String, atomCode: String, branch: String): Result<Boolean> {
        logger.info("endBranchVersionTest,userId:$userId | atomCode:$atomCode | branch=$branch")
        // 并发控制：同一插件同一分支的结束操作串行化
        val finishedRecord = RedisLock(
            redisOperation,
            branchTestLockKey(atomCode, branch),
            60L
        ).use { redisLock ->
            if (!redisLock.tryLock()) {
                throw ErrorCodeException(errorCode = STORE_ATOM_OPERATE_CONCURRENT)
            }
            val latestRecord = marketAtomDao.getLatestBranchTestVersion(
                dslContext = dslContext,
                atomCode = atomCode,
                branch = branch
            ) ?: throw ErrorCodeException(
                errorCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf("$atomCode:$branch")
            )
            // 仅测试中的分支测试版本可结束测试
            if (latestRecord.atomStatus != AtomStatusEnum.TESTING.status.toByte()) {
                throw ErrorCodeException(errorCode = STORE_BRANCH_TEST_END_STATUS_INVALID)
            }
            // 判断用户是否有权限(当前版本的创建者和管理员可以操作)
            if (!(storeMemberDao.isStoreAdmin(
                    dslContext = dslContext,
                    userId = userId,
                    storeCode = atomCode,
                    storeType = StoreTypeEnum.ATOM.type.toByte()
                ) || latestRecord.creator == userId)
            ) {
                throw ErrorCodeException(
                    errorCode = NO_COMPONENT_ADMIN_AND_CREATETOR_PERMISSION,
                    params = arrayOf(atomCode)
                )
            }
            finishBranchVersionTest(userId, atomCode, latestRecord)
            latestRecord
        }
        // 通过websocket推送状态变更消息
        storeWebsocketService.sendWebsocketMessage(userId, finishedRecord.id)
        // 取消发布总线产物、删除质量红线数据在锁外执行
        afterBranchVersionTestEnd(userId, atomCode, finishedRecord)
        return Result(true)
    }
}
