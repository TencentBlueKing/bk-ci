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

package com.tencent.devops.store.devx.service

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.constant.APPROVE
import com.tencent.devops.common.api.constant.BEGIN
import com.tencent.devops.common.api.constant.BUILD
import com.tencent.devops.common.api.constant.COMMIT
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.constant.DOING
import com.tencent.devops.common.api.constant.EDIT
import com.tencent.devops.common.api.constant.END
import com.tencent.devops.common.api.constant.FAIL
import com.tencent.devops.common.api.constant.KEY_PROJECT_ID
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
import com.tencent.devops.common.api.enums.OSType
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.OS
import com.tencent.devops.common.api.util.JsonSchemaUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.ReflectUtil
import com.tencent.devops.common.api.util.YamlUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.model.store.tables.records.TStoreBaseEnvRecord
import com.tencent.devops.repository.api.ServiceGitRepositoryResource
import com.tencent.devops.repository.constant.RepositoryConstants
import com.tencent.devops.repository.pojo.enums.TokenTypeEnum
import com.tencent.devops.repository.pojo.enums.VisibilityLevelEnum
import com.tencent.devops.scm.api.ServiceGitResource
import com.tencent.devops.store.common.configuration.StoreInnerPipelineConfig
import com.tencent.devops.store.common.dao.StoreBaseEnvExtQueryDao
import com.tencent.devops.store.common.dao.StoreBaseEnvQueryDao
import com.tencent.devops.store.common.dao.StoreBaseFeatureExtQueryDao
import com.tencent.devops.store.common.dao.StoreBaseQueryDao
import com.tencent.devops.store.common.dao.StoreBuildInfoDao
import com.tencent.devops.store.common.service.StoreArchiveService
import com.tencent.devops.store.common.service.StoreCommonService
import com.tencent.devops.store.common.service.StoreReleaseSpecBusService
import com.tencent.devops.store.common.service.TxStoreBelongDeptService
import com.tencent.devops.store.constant.StoreConstants.KEY_FRAMEWORK_CODE
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.pojo.common.CONFIG_YML_NAME
import com.tencent.devops.store.pojo.common.KEY_REPOSITORY_AUTHORIZER
import com.tencent.devops.store.pojo.common.KEY_STORE_CODE
import com.tencent.devops.store.pojo.common.KEY_STORE_TYPE
import com.tencent.devops.store.pojo.common.QueryComponentPkgEnvInfoParam
import com.tencent.devops.store.pojo.common.enums.FrameworkCodeEnum
import com.tencent.devops.store.pojo.common.enums.ReleaseTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreStatusEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.common.publication.ReleaseProcessItem
import com.tencent.devops.store.pojo.common.publication.StoreCreateRequest
import com.tencent.devops.store.pojo.common.publication.StorePkgEnvInfo
import com.tencent.devops.store.pojo.common.publication.StorePkgInfoUpdateRequest
import com.tencent.devops.store.pojo.common.publication.StoreRunPipelineParam
import com.tencent.devops.store.pojo.common.publication.StoreUpdateRequest
import com.tencent.devops.store.pojo.devx.BkConfigInfo
import com.tencent.devops.store.pojo.devx.OsConfigInfo
import com.tencent.devops.store.pojo.devx.SignatureConfigInfo
import com.tencent.devops.store.pojo.devx.constants.KEY_BUILD_DIR
import com.tencent.devops.store.pojo.devx.constants.KEY_MAX_PEAK_BAND_WIDTH
import com.tencent.devops.store.pojo.devx.constants.KEY_MIN_PEAK_BAND_WIDTH
import com.tencent.devops.store.pojo.devx.constants.KEY_NEED_VISITED_SITE_INFOS
import com.tencent.devops.store.pojo.devx.constants.KEY_NET_POLICY_INFO
import com.tencent.devops.store.pojo.devx.constants.KEY_REPOSITORY_HTTP_URL
import com.tencent.devops.store.pojo.devx.constants.KEY_REPOSITORY_ID
import com.tencent.devops.store.pojo.devx.constants.KEY_SOURCE_TYPE
import com.tencent.devops.store.pojo.devx.enums.SourceCodeEnum
import java.io.File
import org.apache.commons.codec.digest.DigestUtils
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service

@Primary
@Service("DEVX_RELEASE_SPEC_BUS_SERVICE")
class DevxReleaseSpecBusServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val storeBaseQueryDao: StoreBaseQueryDao,
    private val storeBaseEnvQueryDao: StoreBaseEnvQueryDao,
    private val storeBaseEnvExtQueryDao: StoreBaseEnvExtQueryDao,
    private val storeBaseFeatureExtQueryDao: StoreBaseFeatureExtQueryDao,
    private val storeBuildInfoDao: StoreBuildInfoDao,
    private val storeCommonService: StoreCommonService,
    private val storeArchiveService: StoreArchiveService,
    private val storeInnerPipelineConfig: StoreInnerPipelineConfig,
    private val client: Client,
    private val txStoreBelongDeptService: TxStoreBelongDeptService
) : StoreReleaseSpecBusService {

    companion object {
        private const val KEY_WINDOWS_RUN_INFO = "windowsRunInfo"
        private const val KEY_LINUX_RUN_INFO = "linuxRunInfo"
        private const val KEY_DARWIN_RUN_INFO = "darwinRunInfo"
        private const val KEY_WINDOWS_DEFAULT_SCRIPT = "windowsDefaultScript"
        private const val KEY_LINUX_DEFAULT_SCRIPT = "linuxDefaultScript"
        private const val KEY_DARWIN_DEFAULT_SCRIPT = "darwinDefaultScript"
        private const val KEY_STORE_WINDOWS_RUN_CUSTOM_VAR = "storeWindowsRunCustomVar"
        private const val KEY_STORE_LINUX_RUN_CUSTOM_VAR = "storeLinuxRunCustomVar"
        private const val KEY_STORE_DARWIN_RUN_CUSTOM_VAR = "storeDarwinRunCustomVar"
    }

    @Value("\${store.devx.sign.windows.supportFileTypes:exe}")
    private val windowsSupportFileTypes: String = "exe"

    @Value("\${git.devx.nameSpaceId:}")
    private val devxNameSpaceId: String = ""

    @Value("\${git.devopsPrivateToken:}")
    private val devopsPrivateToken: String = ""

    override fun doStoreCreatePreBus(storeCreateRequest: StoreCreateRequest) {
        val storeBaseCreateRequest = storeCreateRequest.baseInfo
        val storeCode = storeBaseCreateRequest.storeCode
        val baseFeatureInfo = storeBaseCreateRequest.baseFeatureInfo
        val extBaseFeatureInfo = baseFeatureInfo?.extBaseFeatureInfo
        val frameworkCode = extBaseFeatureInfo?.get(KEY_FRAMEWORK_CODE)?.toString()
        val bkStoreContext = storeCreateRequest.bkStoreContext
        val userId = bkStoreContext[AUTH_HEADER_USER_ID]?.toString() ?: throw ErrorCodeException(
            errorCode = CommonMessageCode.PARAMETER_IS_NULL,
            params = arrayOf(AUTH_HEADER_USER_ID)
        )
        if (!frameworkCode.isNullOrBlank() && frameworkCode != FrameworkCodeEnum.CUSTOM_FRAMEWORK.name) {
            // 如果用户选择的开发模板不是自定义开发框架则平台自动给应用创建带脚手架的代码库
            val createGitRepositoryResult = client.getScm(ServiceGitResource::class).createGitCodeRepository(
                userId = userId,
                token = devopsPrivateToken,
                repositoryName = storeCode,
                sampleProjectPath = storeBuildInfoDao.getStoreBuildInfoByLanguage(
                    dslContext,
                    frameworkCode,
                    StoreTypeEnum.DEVX
                )?.sampleProjectPath,
                namespaceId = devxNameSpaceId.toInt(),
                visibilityLevel = VisibilityLevelEnum.LOGIN_PUBLIC,
                tokenType = TokenTypeEnum.PRIVATE_KEY,
                frontendType = null
            )
            if (createGitRepositoryResult.isNotOk()) {
                throw ErrorCodeException(
                    errorCode = createGitRepositoryResult.status.toString(),
                    defaultMessage = createGitRepositoryResult.message
                )
            }
            val repositoryInfo = createGitRepositoryResult.data
            repositoryInfo?.let {
                extBaseFeatureInfo[KEY_REPOSITORY_ID] = repositoryInfo.id
                extBaseFeatureInfo[KEY_REPOSITORY_HTTP_URL] = repositoryInfo.repositoryUrl
            }
        }
        val sourceType = extBaseFeatureInfo?.get(KEY_SOURCE_TYPE)?.toString()
        if (sourceType == SourceCodeEnum.OFFICIAL_HOSTING.name) {
            extBaseFeatureInfo[KEY_REPOSITORY_AUTHORIZER] = userId
        }
    }

    override fun doStoreCreatePostBus(
        userId: String,
        storeCode: String,
        storeType: StoreTypeEnum
    ) {
        txStoreBelongDeptService.initStoreBelongDept(
            userId = userId,
            storeCode = storeCode,
            storeType = storeType
        )
    }

    override fun doStoreUpdatePreBus(storeUpdateRequest: StoreUpdateRequest) {
        val storeBaseCreateRequest = storeUpdateRequest.baseInfo
        val storeCode = storeBaseCreateRequest.storeCode
        val storeType = storeBaseCreateRequest.storeType
        val sourceType = storeBaseFeatureExtQueryDao.getStoreBaseFeatureExt(
            dslContext = dslContext,
            storeCode = storeCode,
            storeType = storeType,
            fieldName = KEY_SOURCE_TYPE
        )?.fieldValue
        if (sourceType == SourceCodeEnum.OFFICIAL_HOSTING.name) {
            val bkStoreContext = storeUpdateRequest.bkStoreContext
            val userId = bkStoreContext[AUTH_HEADER_USER_ID]?.toString() ?: AUTH_HEADER_USER_ID_DEFAULT_VALUE
            val versionInfo = storeBaseCreateRequest.versionInfo
            val version = versionInfo.version
            doStoreEnvBus(
                storeCode = storeCode,
                storeType = storeType,
                version = version,
                userId = userId,
                releaseType = versionInfo.releaseType
            )
        }
    }

    override fun doStoreEnvBus(
        storeCode: String,
        storeType: StoreTypeEnum,
        version: String,
        userId: String,
        releaseType: ReleaseTypeEnum?
    ) {
        // 获取组件配置文件
        val remoteRepoId = storeBaseFeatureExtQueryDao.getStoreBaseFeatureExt(
            dslContext = dslContext,
            storeCode = storeCode,
            storeType = storeType,
            fieldName = RepositoryConstants.KEY_REPOSITORY_ID
        )?.fieldValue ?: return
        val configFileContent = client.get(ServiceGitRepositoryResource::class).getFileContent(
            remoteRepoId = remoteRepoId, filePath = CONFIG_YML_NAME, branch = MASTER
        ).data
        if (configFileContent.isNullOrBlank()) {
            throw ErrorCodeException(
                errorCode = CommonMessageCode.PARAMETER_IS_NULL, params = arrayOf(CONFIG_YML_NAME)
            )
        }
        // 解析配置文件中的环境信息
        val storePkgEnvInfos = generateStorePkgEnvInfos(storeCode, version, configFileContent)
        val storePkgInfoUpdateRequest = StorePkgInfoUpdateRequest(
            storeType = storeType,
            storeCode = storeCode,
            version = version,
            storePkgEnvInfos = storePkgEnvInfos,
            releaseType = releaseType
        )
        storeArchiveService.updateComponentPkgInfo(userId, storePkgInfoUpdateRequest)
    }

    override fun doStoreI18nConversionSpecBus(storeUpdateRequest: StoreUpdateRequest) {
        // 云开发暂无需做国际化转换
    }

    @Suppress("UNCHECKED_CAST")
    override fun doCheckStoreUpdateParamSpecBus(storeUpdateRequest: StoreUpdateRequest) {
        val storeBaseUpdateRequest = storeUpdateRequest.baseInfo
        val extBaseInfo = storeBaseUpdateRequest.extBaseInfo
        val netPolicyInfo = extBaseInfo?.get(KEY_NET_POLICY_INFO) as? Map<String, Any>
            ?: throw ErrorCodeException(
                errorCode = CommonMessageCode.PARAMETER_IS_NULL,
                params = arrayOf(KEY_NET_POLICY_INFO)
            )
        val maxPeakBandwidth = netPolicyInfo[KEY_MAX_PEAK_BAND_WIDTH] ?: throw ErrorCodeException(
            errorCode = CommonMessageCode.PARAMETER_IS_NULL,
            params = arrayOf(KEY_MAX_PEAK_BAND_WIDTH)
        )
        val minPeakBandwidth = netPolicyInfo[KEY_MIN_PEAK_BAND_WIDTH] ?: throw ErrorCodeException(
            errorCode = CommonMessageCode.PARAMETER_IS_NULL,
            params = arrayOf(KEY_MIN_PEAK_BAND_WIDTH)
        )
        if (!ReflectUtil.isNativeType(maxPeakBandwidth)) {
            throw ErrorCodeException(
                errorCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf(KEY_MAX_PEAK_BAND_WIDTH)
            )
        }
        if (!ReflectUtil.isNativeType(minPeakBandwidth)) {
            throw ErrorCodeException(
                errorCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf(KEY_MIN_PEAK_BAND_WIDTH)
            )
        }
        netPolicyInfo[KEY_NEED_VISITED_SITE_INFOS] ?: throw ErrorCodeException(
            errorCode = CommonMessageCode.PARAMETER_IS_NULL,
            params = arrayOf(KEY_NEED_VISITED_SITE_INFOS)
        )
        // 检验云开发的包是否有上传
        val storeCode = storeBaseUpdateRequest.storeCode
        val version = storeBaseUpdateRequest.versionInfo.version
        val storeType = StoreTypeEnum.DEVX
        val releaseType = storeBaseUpdateRequest.versionInfo.releaseType
        val storeId = when (releaseType) {
            ReleaseTypeEnum.NEW -> {
                val baseRecord = storeBaseQueryDao.getFirstComponent(
                    dslContext = dslContext,
                    storeCode = storeBaseUpdateRequest.storeCode,
                    storeType = StoreTypeEnum.DEVX
                ) ?: throw ErrorCodeException(errorCode = CommonMessageCode.ERROR_CLIENT_REST_ERROR)
                baseRecord.id
            }
            ReleaseTypeEnum.CANCEL_RE_RELEASE -> {
                val baseRecord = storeBaseQueryDao.getComponent(
                    dslContext = dslContext,
                    storeCode = storeBaseUpdateRequest.storeCode,
                    storeType = StoreTypeEnum.DEVX,
                    version = version
                ) ?: throw ErrorCodeException(errorCode = CommonMessageCode.ERROR_CLIENT_REST_ERROR)
                baseRecord.id
            }
            else -> {
                // 普通发布类型会重新生成一条版本记录
                DigestUtils.md5Hex("$storeType-$storeCode-$version")
            }
        }
        val baseEnvRecords = storeBaseEnvQueryDao.getBaseEnvsByStoreId(dslContext, storeId) ?: throw ErrorCodeException(
            errorCode = StoreMessageCode.USER_UPLOAD_PACKAGE_INVALID
        )
        baseEnvRecords.forEach { baseEnvRecord ->
            if (baseEnvRecord.pkgPath.isNullOrBlank()) {
                throw ErrorCodeException(
                    errorCode = StoreMessageCode.USER_UPLOAD_PACKAGE_INVALID
                )
            }
        }
    }

    override fun getStoreUpdateStatus(): StoreStatusEnum {
        return StoreStatusEnum.COMMITTING
    }

    override fun getStoreRunPipelineStartParams(
        storeRunPipelineParam: StoreRunPipelineParam
    ): MutableMap<String, String> {
        val storeId = storeRunPipelineParam.storeId
        val baseRecord = storeBaseQueryDao.getComponentById(dslContext, storeId) ?: throw ErrorCodeException(
            errorCode = CommonMessageCode.PARAMETER_IS_INVALID,
            params = arrayOf(storeId)
        )
        val baseEnvRecords = storeBaseEnvQueryDao.getBaseEnvsByStoreId(
            dslContext = dslContext,
            storeId = storeId
        )

        var queryDefaultScriptFlag = false
        val windowsRunInfos = mutableSetOf<String>()
        val linuxRunInfos = mutableSetOf<String>()
        val darwinRunInfos = mutableSetOf<String>()

        val storeCode = baseRecord.storeCode
        val storeType = StoreTypeEnum.getStoreTypeObj(baseRecord.storeType.toInt())

        baseEnvRecords?.forEach { baseEnvRecord ->
            val osName = baseEnvRecord.osName.orEmpty()
            val osArch = baseEnvRecord.osArch ?: " "
            val pkgRepoPath = baseEnvRecord.pkgPath.orEmpty()
            val signatureCertFileKey = getSignatureCertFileKey()
            val signatureOriginFileKey = getSignatureOriginFileKey()

            val extEnvs = storeBaseEnvExtQueryDao.getBaseExtEnvsByEnvId(
                dslContext = dslContext,
                envId = baseEnvRecord.id,
                fieldNames = arrayOf(
                    signatureCertFileKey,
                    signatureOriginFileKey,
                    OsConfigInfo::packAppPackageScriptPath.name,
                    OsConfigInfo::appPackagePath.name,
                    OsConfigInfo::packScriptPath.name,
                    OsConfigInfo::packagePath.name
                )
            )

            var certSignFilePaths = extEnvs?.find { it.fieldName == signatureCertFileKey }?.fieldValue ?: "[]"
            val originFilePaths = extEnvs?.find { it.fieldName == signatureOriginFileKey }?.fieldValue ?: "[]"
            val packScriptPath = extEnvs?.find { it.fieldName == OsConfigInfo::packScriptPath.name }?.fieldValue ?: " "
            var packAppPackageScriptPath =
                extEnvs?.find { it.fieldName == OsConfigInfo::packAppPackageScriptPath.name }?.fieldValue ?: " "

            if (packScriptPath.isBlank()) queryDefaultScriptFlag = true

            val packagePath = extEnvs?.find { it.fieldName == OsConfigInfo::packagePath.name }?.fieldValue ?: " "
            var appPackagePath = extEnvs?.find { it.fieldName == OsConfigInfo::appPackagePath.name }?.fieldValue ?: " "

            if (appPackagePath.isBlank() && storeBaseFeatureExtQueryDao.getStoreBaseFeatureExt(
                    dslContext = dslContext,
                    storeCode = storeCode,
                    storeType = storeType,
                    fieldName = KEY_FRAMEWORK_CODE
                )?.fieldValue == FrameworkCodeEnum.NODEJS_FRAMEWORK.name
            ) {
                // 兼容nodejs框架没有配置应用程序路径的情况
                val fileExtension = packagePath.substringAfterLast('.', "")
                val fileName = packagePath.substringAfterLast(File.separator, "")
                appPackagePath = packagePath.replace(fileName, "$storeCode.$fileExtension")
                if (packAppPackageScriptPath.isBlank()) {
                    packAppPackageScriptPath = packScriptPath
                }
            }

            if (appPackagePath.isNotBlank()) {
                val certSignFilePathSet =
                    JsonUtil.to(certSignFilePaths, object : TypeReference<MutableSet<String>>() {})
                certSignFilePathSet.add(appPackagePath)
                certSignFilePaths = JsonUtil.toJson(certSignFilePathSet)
            }

            val runInfo = listOf(
                osName, osArch, pkgRepoPath, certSignFilePaths, originFilePaths,
                when {
                    osName.contains("win") -> windowsSupportFileTypes.split(",")
                        .contains(pkgRepoPath.substringAfterLast("."))

                    osName.contains("darwin") -> true
                    else -> false
                }.toString(),
                packScriptPath, packagePath, packAppPackageScriptPath, appPackagePath
            ).joinToString(":")

            when {
                osName.contains("win") -> windowsRunInfos.add(runInfo)
                osName.contains("darwin") -> darwinRunInfos.add(runInfo)
                else -> linuxRunInfos.add(runInfo)
            }
        }

        val extFeatures = storeBaseFeatureExtQueryDao.queryStoreBaseFeatureExt(
            dslContext = dslContext,
            storeCode = storeCode,
            storeType = storeType,
            fieldNames = setOf(KEY_BUILD_DIR, KEY_REPOSITORY_HTTP_URL)
        )

        val buildDir = extFeatures.find { it.fieldName == KEY_BUILD_DIR }?.fieldValue ?: ""
        val repositoryHttpUrl = extFeatures.find { it.fieldName == KEY_REPOSITORY_HTTP_URL }?.fieldValue ?: ""
        val storeRunCustomVarSuffix = if (repositoryHttpUrl.isBlank()) "-pkg" else "-repo"

        val startParamMap = mutableMapOf(
            KEY_STORE_CODE to storeCode,
            KEY_STORE_TYPE to storeType.name,
            KEY_VERSION to baseRecord.version,
            KEY_PROJECT_ID to storeInnerPipelineConfig.innerPipelineProject,
            KEY_WINDOWS_RUN_INFO to JsonUtil.toJson(windowsRunInfos),
            KEY_LINUX_RUN_INFO to JsonUtil.toJson(linuxRunInfos),
            KEY_DARWIN_RUN_INFO to JsonUtil.toJson(darwinRunInfos),
            KEY_BUILD_DIR to buildDir,
            KEY_REPOSITORY_HTTP_URL to repositoryHttpUrl
        )
        listOf(
            Triple(windowsRunInfos, KEY_STORE_WINDOWS_RUN_CUSTOM_VAR, "windows"),
            Triple(linuxRunInfos, KEY_STORE_LINUX_RUN_CUSTOM_VAR, "linux"),
            Triple(darwinRunInfos, KEY_STORE_DARWIN_RUN_CUSTOM_VAR, "darwin")
        ).forEach { (infos, key, os) ->
            if (infos.isNotEmpty()) {
                startParamMap[key] = "$os$storeRunCustomVarSuffix"
            }
        }

        if (queryDefaultScriptFlag) {
            storeBaseFeatureExtQueryDao.getStoreBaseFeatureExt(
                dslContext = dslContext,
                storeCode = storeCode,
                storeType = storeType,
                fieldName = KEY_FRAMEWORK_CODE
            )?.fieldValue
                ?.let { frameworkCode ->
                    storeBuildInfoDao.getStoreBuildInfoByLanguage(
                        dslContext = dslContext,
                        language = frameworkCode,
                        storeType = storeType
                    )?.script
                        ?.takeIf { it.isNotBlank() }
                }
                ?.apply {
                    processScriptContent(this, startParamMap)
                }
        }
        return startParamMap
    }

    private fun processScriptContent(script: String, startParamMap: MutableMap<String, String>) {
        if (JsonSchemaUtil.validateJson(script)) {
            JsonUtil.toMap(script).forEach { (os, value) ->
                val osKey = when (os.lowercase()) {
                    OS.WINDOWS.name.lowercase() -> KEY_WINDOWS_DEFAULT_SCRIPT
                    OS.LINUX.name.lowercase() -> KEY_LINUX_DEFAULT_SCRIPT
                    OS.MACOS.name.lowercase() -> KEY_DARWIN_DEFAULT_SCRIPT
                    else -> null
                }
                osKey?.let { startParamMap[it] = value.toString() }
            }
        } else {
            listOf(KEY_WINDOWS_DEFAULT_SCRIPT, KEY_LINUX_DEFAULT_SCRIPT, KEY_DARWIN_DEFAULT_SCRIPT)
                .forEach { startParamMap[it] = script }
        }
    }

    override fun getStoreRunPipelineStatus(buildId: String?, startFlag: Boolean): StoreStatusEnum? {
        return if (!buildId.isNullOrBlank() || !startFlag) {
            StoreStatusEnum.BUILDING
        } else {
            StoreStatusEnum.BUILD_FAIL
        }
    }

    override fun getComponentPkgEnvInfo(
        userId: String,
        storeType: StoreTypeEnum,
        storeCode: String,
        version: String,
        osName: String?,
        osArch: String?
    ): List<StorePkgEnvInfo> {
        val storePkgEnvInfos = mutableListOf<StorePkgEnvInfo>()
        val baseRecord = storeBaseQueryDao.getComponent(
            dslContext = dslContext, storeCode = storeCode, version = version, storeType = storeType
        )
        val baseEnvRecords = if (baseRecord != null) {
            storeBaseEnvQueryDao.getBaseEnvsByStoreId(
                dslContext = dslContext, storeId = baseRecord.id, osName = osName, osArch = osArch
            )
        } else {
            null
        }
        if (!baseEnvRecords.isNullOrEmpty()) {
            baseEnvRecords.forEach { baseEnvRecord ->
                storePkgEnvInfos.add(createStorePkgEnvInfo(baseEnvRecord))
            }
        }
        return storePkgEnvInfos
    }

    override fun getComponentPkgEnvInfo(
        userId: String,
        storeType: StoreTypeEnum,
        storeCode: String,
        version: String,
        queryComponentPkgEnvInfoParam: QueryComponentPkgEnvInfoParam
    ): List<StorePkgEnvInfo> {
        val storePkgEnvInfos = mutableListOf<StorePkgEnvInfo>()
        val configFileContent = queryComponentPkgEnvInfoParam.configFileContent
        if (configFileContent.isBlank()) {
            storePkgEnvInfos.add(StorePkgEnvInfo(osName = OSType.WINDOWS.name.lowercase(), defaultFlag = true))
            return storePkgEnvInfos
        }
        return generateStorePkgEnvInfos(storeCode, version, configFileContent)
    }

    private fun generateStorePkgEnvInfos(
        storeCode: String,
        version: String,
        configFileContent: String
    ): MutableList<StorePkgEnvInfo> {
        val bkConfigInfo = YamlUtil.to(configFileContent, object : TypeReference<BkConfigInfo>() {})
        val storePkgEnvInfos = mutableListOf<StorePkgEnvInfo>()
        val osDefaultEnvNumMap = mutableMapOf<String, Int>()
        bkConfigInfo.os.forEach { osConfigInfo ->
            storePkgEnvInfos.add(createStorePkgEnvInfoFromConfig(storeCode, version, osConfigInfo))
            // 统计每种操作系统默认环境配置数量
            val defaultFlag = osConfigInfo.defaultFlag
            val configOsName = osConfigInfo.osName
            val increaseDefaultEnvNum = if (defaultFlag) 1 else 0
            if (osDefaultEnvNumMap.containsKey(configOsName)) {
                osDefaultEnvNumMap[configOsName] = osDefaultEnvNumMap[configOsName]!! + increaseDefaultEnvNum
            } else {
                osDefaultEnvNumMap[configOsName] = increaseDefaultEnvNum
            }
        }
        osDefaultEnvNumMap.forEach { (osName, defaultEnvNum) ->
            // 判断每种操作系统默认环境配置是否有且只有1个
            if (defaultEnvNum != 1) {
                throw ErrorCodeException(
                    errorCode = StoreMessageCode.USER_REPOSITORY_TASK_JSON_OS_DEFAULT_ENV_IS_INVALID,
                    params = arrayOf(CONFIG_YML_NAME, osName, defaultEnvNum.toString())
                )
            }
        }
        return storePkgEnvInfos
    }

    private fun createStorePkgEnvInfo(baseEnvRecord: TStoreBaseEnvRecord): StorePkgEnvInfo {
        val baseEnvExtRecords = storeBaseEnvExtQueryDao.getBaseExtEnvsByEnvId(dslContext, baseEnvRecord.id)
        val extEnvInfo = if (baseEnvExtRecords.isNullOrEmpty()) {
            null
        } else {
            mutableMapOf<String, Any>().apply {
                baseEnvExtRecords.forEach { baseEnvExtRecord ->
                    set(baseEnvExtRecord.fieldName, baseEnvExtRecord.fieldValue)
                }
            }
        }
        return StorePkgEnvInfo(
            pkgName = baseEnvRecord.pkgName,
            pkgRepoPath = baseEnvRecord.pkgPath,
            language = baseEnvRecord.language,
            minVersion = baseEnvRecord.minVersion,
            target = baseEnvRecord.target,
            preCmd = baseEnvRecord.preCmd,
            osName = baseEnvRecord.osName,
            osArch = baseEnvRecord.osArch,
            runtimeVersion = baseEnvRecord.runtimeVersion,
            defaultFlag = baseEnvRecord.defaultFlag,
            extEnvInfo = extEnvInfo
        )
    }

    private fun createStorePkgEnvInfoFromConfig(
        storeCode: String,
        version: String,
        osConfigInfo: OsConfigInfo
    ): StorePkgEnvInfo {
        val configOsName = osConfigInfo.osName
        val configOsArch = osConfigInfo.osArch

        val extEnvInfo = mutableMapOf<String, Any>().apply {
            // 统一处理需要添加到extEnvInfo的字段
            listOfNotNull(
                osConfigInfo.signature?.certSignFilePaths?.let {
                    getSignatureCertFileKey() to JsonUtil.toJson(it)
                },
                osConfigInfo.signature?.originFilePaths?.let {
                    getSignatureOriginFileKey() to JsonUtil.toJson(it)
                },
                osConfigInfo.packAppPackageScriptPath?.let {
                    OsConfigInfo::packAppPackageScriptPath.name to it
                },
                osConfigInfo.appPackagePath?.let {
                    OsConfigInfo::appPackagePath.name to it
                },
                osConfigInfo.packScriptPath?.let {
                    OsConfigInfo::packScriptPath.name to it
                }
            ).forEach { (key, value) -> put(key, value) }

            // 处理必填字段packagePath
            put(OsConfigInfo::packagePath.name, osConfigInfo.packagePath)
        }.takeIf { it.isNotEmpty() }

        val pkgLocalPath = osConfigInfo.packagePath
        val pkgName = File(pkgLocalPath).name

        // 生成包在仓库中的路径
        val pkgRepoPath = listOfNotNull(
            "$storeCode/$version",
            configOsName.takeUnless { it.isBlank() },
            configOsArch.takeUnless { it.isNullOrBlank() },
            pkgName
        ).filterNot { it.isBlank() }.joinToString("/")

        return StorePkgEnvInfo(
            pkgName = pkgName,
            pkgLocalPath = pkgLocalPath,
            pkgRepoPath = pkgRepoPath,
            osName = configOsName,
            osArch = configOsArch,
            defaultFlag = osConfigInfo.defaultFlag,
            extEnvInfo = extEnvInfo
        )
    }

    private fun getSignatureCertFileKey(): String {
        return "${OsConfigInfo::signature.name}_${SignatureConfigInfo::certSignFilePaths.name}"
    }

    private fun getSignatureOriginFileKey(): String {
        return "${OsConfigInfo::signature.name}_${SignatureConfigInfo::originFilePaths.name}"
    }

    override fun getReleaseProcessItems(
        userId: String,
        isNormalUpgrade: Boolean,
        status: StoreStatusEnum
    ): List<ReleaseProcessItem> {
        val processInfo = mutableListOf<ReleaseProcessItem>()
        processInfo.add(ReleaseProcessItem(I18nUtil.getCodeLanMessage(messageCode = BEGIN), BEGIN, NUM_ONE, SUCCESS))
        processInfo.add(ReleaseProcessItem(I18nUtil.getCodeLanMessage(messageCode = COMMIT), COMMIT, NUM_TWO, UNDO))
        processInfo.add(ReleaseProcessItem(I18nUtil.getCodeLanMessage(messageCode = BUILD), BUILD, NUM_THREE, UNDO))
        processInfo.add(ReleaseProcessItem(I18nUtil.getCodeLanMessage(messageCode = TEST), TEST, NUM_FOUR, UNDO))
        processInfo.add(ReleaseProcessItem(I18nUtil.getCodeLanMessage(messageCode = EDIT), EDIT, NUM_FIVE, UNDO))
        if (isNormalUpgrade) {
            processInfo.add(ReleaseProcessItem(I18nUtil.getCodeLanMessage(messageCode = END), END, NUM_SIX, UNDO))
        } else {
            processInfo.add(
                ReleaseProcessItem(I18nUtil.getCodeLanMessage(messageCode = APPROVE), APPROVE, NUM_SIX, UNDO)
            )
            processInfo.add(ReleaseProcessItem(I18nUtil.getCodeLanMessage(messageCode = END), END, NUM_SEVEN, UNDO))
        }
        val totalStep = if (isNormalUpgrade) NUM_SIX else NUM_SEVEN
        when (status) {
            StoreStatusEnum.INIT, StoreStatusEnum.COMMITTING -> {
                storeCommonService.setProcessInfo(processInfo, totalStep, NUM_TWO, DOING)
            }

            StoreStatusEnum.BUILDING -> {
                storeCommonService.setProcessInfo(processInfo, totalStep, NUM_THREE, DOING)
            }

            StoreStatusEnum.BUILD_FAIL -> {
                storeCommonService.setProcessInfo(processInfo, totalStep, NUM_THREE, FAIL)
            }

            StoreStatusEnum.TESTING -> {
                storeCommonService.setProcessInfo(processInfo, totalStep, NUM_FOUR, DOING)
            }

            StoreStatusEnum.EDITING -> {
                storeCommonService.setProcessInfo(processInfo, totalStep, NUM_FIVE, DOING)
            }

            StoreStatusEnum.AUDITING -> {
                storeCommonService.setProcessInfo(processInfo, totalStep, NUM_SIX, DOING)
            }

            StoreStatusEnum.AUDIT_REJECT -> {
                storeCommonService.setProcessInfo(processInfo, totalStep, NUM_SIX, FAIL)
            }

            StoreStatusEnum.RELEASED -> {
                val currStep = if (isNormalUpgrade) NUM_SIX else NUM_SEVEN
                storeCommonService.setProcessInfo(processInfo, totalStep, currStep, SUCCESS)
            }

            else -> {}
        }
        return processInfo
    }
}
