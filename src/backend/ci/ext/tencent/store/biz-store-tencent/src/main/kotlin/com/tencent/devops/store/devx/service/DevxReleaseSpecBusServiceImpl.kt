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
import com.tencent.devops.common.api.constant.END
import com.tencent.devops.common.api.constant.FAIL
import com.tencent.devops.common.api.constant.KEY_PROJECT_ID
import com.tencent.devops.common.api.constant.KEY_VERSION
import com.tencent.devops.common.api.constant.MASTER
import com.tencent.devops.common.api.constant.NUM_FIVE
import com.tencent.devops.common.api.constant.NUM_FOUR
import com.tencent.devops.common.api.constant.NUM_ONE
import com.tencent.devops.common.api.constant.NUM_SIX
import com.tencent.devops.common.api.constant.NUM_THREE
import com.tencent.devops.common.api.constant.NUM_TWO
import com.tencent.devops.common.api.constant.SUCCESS
import com.tencent.devops.common.api.constant.TEST
import com.tencent.devops.common.api.constant.UNDO
import com.tencent.devops.common.api.enums.OSType
import com.tencent.devops.common.api.enums.RepositoryType
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
import com.tencent.devops.repository.pojo.enums.TokenTypeEnum
import com.tencent.devops.repository.pojo.enums.VisibilityLevelEnum
import com.tencent.devops.store.common.configuration.StoreInnerPipelineConfig
import com.tencent.devops.store.common.dao.StoreBaseEnvExtQueryDao
import com.tencent.devops.store.common.dao.StoreBaseEnvQueryDao
import com.tencent.devops.store.common.dao.StoreBaseFeatureExtQueryDao
import com.tencent.devops.store.common.dao.StoreBaseFeatureQueryDao
import com.tencent.devops.store.common.dao.StoreBaseQueryDao
import com.tencent.devops.store.common.dao.StoreBuildInfoDao
import com.tencent.devops.store.common.service.StoreArchiveService
import com.tencent.devops.store.common.service.StoreCommonService
import com.tencent.devops.store.common.service.StoreReleaseSpecBusService
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.pojo.common.CONFIG_YML_NAME
import com.tencent.devops.store.pojo.common.KEY_STORE_CODE
import com.tencent.devops.store.pojo.common.KEY_STORE_TYPE
import com.tencent.devops.store.pojo.common.QueryComponentPkgEnvInfoParam
import com.tencent.devops.store.pojo.common.enums.RdTypeEnum
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
import com.tencent.devops.store.pojo.devx.constants.KEY_FRAMEWORK_CODE
import com.tencent.devops.store.pojo.devx.constants.KEY_MAX_PEAK_BAND_WIDTH
import com.tencent.devops.store.pojo.devx.constants.KEY_MIN_PEAK_BAND_WIDTH
import com.tencent.devops.store.pojo.devx.constants.KEY_NEED_VISITED_SITE_INFOS
import com.tencent.devops.store.pojo.devx.constants.KEY_NET_POLICY_INFO
import com.tencent.devops.store.pojo.devx.constants.KEY_REPOSITORY_HTTP_URL
import com.tencent.devops.store.pojo.devx.enums.FrameworkCodeEnum
import org.apache.commons.codec.digest.DigestUtils
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import java.io.File

@Primary
@Service("DEVX_RELEASE_SPEC_BUS_SERVICE")
class DevxReleaseSpecBusServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val storeBaseQueryDao: StoreBaseQueryDao,
    private val storeBaseFeatureQueryDao: StoreBaseFeatureQueryDao,
    private val storeBaseEnvQueryDao: StoreBaseEnvQueryDao,
    private val storeBaseEnvExtQueryDao: StoreBaseEnvExtQueryDao,
    private val storeBaseFeatureExtQueryDao: StoreBaseFeatureExtQueryDao,
    private val storeBuildInfoDao: StoreBuildInfoDao,
    private val storeCommonService: StoreCommonService,
    private val storeArchiveService: StoreArchiveService,
    private val storeInnerPipelineConfig: StoreInnerPipelineConfig,
    private val client: Client
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

    override fun doStoreCreatePreBus(storeCreateRequest: StoreCreateRequest) {
        val storeBaseCreateRequest = storeCreateRequest.baseInfo
        val storeCode = storeBaseCreateRequest.storeCode
        val baseFeatureInfo = storeBaseCreateRequest.baseFeatureInfo
        val extBaseFeatureInfo = baseFeatureInfo?.extBaseFeatureInfo
        val frameworkCode = extBaseFeatureInfo?.get(KEY_FRAMEWORK_CODE)?.toString()
        if (!frameworkCode.isNullOrBlank() && frameworkCode != FrameworkCodeEnum.CUSTOM_FRAMEWORK.name) {
            // 如果用户选择的开发模板不是自定义开发框架则平台自动给应用创建带脚手架的代码库
            val createGitRepositoryResult = client.get(ServiceGitRepositoryResource::class).createGitCodeRepository(
                userId = storeInnerPipelineConfig.innerPipelineUser,
                projectCode = storeInnerPipelineConfig.innerPipelineProject,
                repositoryName = storeCode,
                sampleProjectPath = storeBuildInfoDao.getStoreBuildInfoByLanguage(
                    dslContext,
                    frameworkCode,
                    StoreTypeEnum.DEVX
                )?.sampleProjectPath,
                namespaceId = devxNameSpaceId.toInt(),
                visibilityLevel = VisibilityLevelEnum.LOGIN_PUBLIC,
                tokenType = TokenTypeEnum.PRIVATE_KEY
            )
            if (createGitRepositoryResult.isNotOk()) {
                throw ErrorCodeException(
                    errorCode = createGitRepositoryResult.status.toString(),
                    defaultMessage = createGitRepositoryResult.message
                )
            }
            val repositoryInfo = createGitRepositoryResult.data
            repositoryInfo?.let {
                extBaseFeatureInfo[KEY_REPOSITORY_HTTP_URL] = repositoryInfo.url
            }
        }
    }

    override fun doStoreUpdatePreBus(storeUpdateRequest: StoreUpdateRequest) {
        val storeBaseCreateRequest = storeUpdateRequest.baseInfo
        val storeCode = storeBaseCreateRequest.storeCode
        val storeType = storeBaseCreateRequest.storeType
        val rdType = storeBaseFeatureQueryDao.getBaseFeatureByCode(dslContext, storeCode, storeType)?.rdType
        if (rdType == RdTypeEnum.SELF_DEVELOPED.name) {
            val frameworkCode = storeBaseFeatureExtQueryDao.getStoreBaseFeatureExt(
                dslContext = dslContext, storeCode = storeCode, storeType = storeType, fieldName = KEY_FRAMEWORK_CODE
            )?.fieldValue
            if (frameworkCode != FrameworkCodeEnum.CUSTOM_FRAMEWORK.name) {
                // 获取组件配置文件
                val repositoryNameWithNamespace = storeBaseFeatureExtQueryDao.getStoreBaseFeatureExt(
                    dslContext = dslContext,
                    storeCode = storeCode,
                    storeType = storeType,
                    fieldName = KEY_REPOSITORY_HTTP_URL
                )?.fieldValue ?: ""
                val configFileContent = client.get(ServiceGitRepositoryResource::class).getFileContent(
                    repoId = repositoryNameWithNamespace,
                    filePath = CONFIG_YML_NAME,
                    reversion = null,
                    branch = MASTER,
                    repositoryType = RepositoryType.NAME,
                    projectId = storeInnerPipelineConfig.innerPipelineProject
                ).data
                if (configFileContent.isNullOrBlank()) {
                    throw ErrorCodeException(
                        errorCode = CommonMessageCode.PARAMETER_IS_NULL,
                        params = arrayOf(CONFIG_YML_NAME)
                    )
                }
                val versionInfo = storeBaseCreateRequest.versionInfo
                val version = versionInfo.version
                // 解析配置文件中的环境信息
                val storePkgEnvInfos = generateStorePkgEnvInfos(storeCode, version, configFileContent)
                val storePkgInfoUpdateRequest = StorePkgInfoUpdateRequest(
                    storeType = storeType,
                    storeCode = storeCode,
                    version = version,
                    storePkgEnvInfos = storePkgEnvInfos
                )
                val bkStoreContext = storeUpdateRequest.bkStoreContext
                val userId = bkStoreContext[AUTH_HEADER_USER_ID]?.toString() ?: AUTH_HEADER_USER_ID_DEFAULT_VALUE
                storeArchiveService.updateComponentPkgInfo(userId, storePkgInfoUpdateRequest)
            }
        }
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
        if (maxPeakBandwidth.toString().toDouble() - minPeakBandwidth.toString().toDouble() < 0) {
            throw ErrorCodeException(
                errorCode = CommonMessageCode.ERROR_INVALID_PARAM_,
                params = arrayOf("$KEY_MIN_PEAK_BAND_WIDTH,$KEY_MAX_PEAK_BAND_WIDTH")
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
        val storeId = if (releaseType == ReleaseTypeEnum.NEW ||
            releaseType == ReleaseTypeEnum.CANCEL_RE_RELEASE
        ) {
            val baseRecord = storeBaseQueryDao.getComponent(
                dslContext = dslContext,
                storeCode = storeBaseUpdateRequest.storeCode,
                version = storeBaseUpdateRequest.versionInfo.version,
                storeType = StoreTypeEnum.DEVX
            ) ?: throw ErrorCodeException(errorCode = CommonMessageCode.ERROR_CLIENT_REST_ERROR)
            baseRecord.id
        } else {
            // 普通发布类型会重新生成一条版本记录
            DigestUtils.md5Hex("$storeType-$storeCode-$version")
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
        var windowsRunInfos: MutableSet<String>? = null
        var linuxRunInfos: MutableSet<String>? = null
        var darwinRunInfos: MutableSet<String>? = null
        baseEnvRecords?.forEach { baseEnvRecord ->
            val osName = baseEnvRecord.osName
            val osArch = if (!baseEnvRecord.osArch.isNullOrBlank()) baseEnvRecord.osArch else " "
            val pkgRepoPath = baseEnvRecord.pkgPath
            val signatureFileKey = getSignatureFileKey()
            val extEnvs = storeBaseEnvExtQueryDao.getBaseExtEnvsByEnvId(
                dslContext = dslContext,
                envId = baseEnvRecord.id,
                fieldNames = arrayOf(
                    signatureFileKey,
                    OsConfigInfo::packScriptPath.name,
                    OsConfigInfo::packagePath.name
                )
            )
            val signFilePaths = extEnvs?.filter { it.fieldName == signatureFileKey }?.getOrNull(0)?.fieldValue ?: "[]"
            val packScriptPath =
                extEnvs?.filter { it.fieldName == OsConfigInfo::packScriptPath.name }?.getOrNull(0)?.fieldValue ?: " "
            if (packScriptPath.isBlank()) {
                // 用户没有配置脚本，则需查出平台默认的打包脚本
                queryDefaultScriptFlag = true
            }
            val pkgLocalPath =
                extEnvs?.filter { it.fieldName == OsConfigInfo::packagePath.name }?.getOrNull(0)?.fieldValue ?: " "
            val fileType = pkgRepoPath.substringAfterLast(".")
            // 暂时只支持windows操作系统的exe软件包签名
            if (osName.contains("win")) {
                val signFlag = windowsSupportFileTypes.split(",").contains(fileType)
                if (windowsRunInfos == null) {
                    windowsRunInfos = mutableSetOf()
                }
                windowsRunInfos?.add("$osName:$osArch:$pkgRepoPath:$signFilePaths:$signFlag:$packScriptPath:$pkgLocalPath")
            } else if (osName.contains("darwin")) {
                if (darwinRunInfos == null) {
                    darwinRunInfos = mutableSetOf()
                }
                darwinRunInfos?.add("$osName:$osArch:$pkgRepoPath:$signFilePaths:false:$packScriptPath:$pkgLocalPath")
            } else {
                if (linuxRunInfos == null) {
                    linuxRunInfos = mutableSetOf()
                }
                linuxRunInfos?.add("$osName:$osArch:$pkgRepoPath:$signFilePaths:false:$packScriptPath:$pkgLocalPath")
            }
        }
        val storeCode = baseRecord.storeCode
        val storeType = StoreTypeEnum.getStoreTypeObj(baseRecord.storeType.toInt())
        val extFeatures = storeBaseFeatureExtQueryDao.queryStoreBaseFeatureExt(
            dslContext = dslContext,
            storeCode = storeCode,
            storeType = storeType,
            fieldNames = setOf(KEY_BUILD_DIR, KEY_REPOSITORY_HTTP_URL)
        )
        val buildDir = extFeatures.filter { it.fieldName == KEY_BUILD_DIR }.getOrNull(0)?.fieldValue ?: ""
        val repositoryHttpUrl =
            extFeatures.filter { it.fieldName == KEY_REPOSITORY_HTTP_URL }.getOrNull(0)?.fieldValue ?: ""
        val storeRunCustomVarSuffix = if (repositoryHttpUrl.isBlank()) {
            "-pkg"
        } else {
            "-repo"
        }
        val startParamMap = mutableMapOf(
            KEY_STORE_CODE to storeCode,
            KEY_STORE_TYPE to storeType.name,
            KEY_VERSION to baseRecord.version,
            KEY_PROJECT_ID to storeInnerPipelineConfig.innerPipelineProject,
            KEY_WINDOWS_RUN_INFO to if (!windowsRunInfos.isNullOrEmpty()) JsonUtil.toJson(windowsRunInfos!!) else "[]",
            KEY_LINUX_RUN_INFO to if (!linuxRunInfos.isNullOrEmpty()) JsonUtil.toJson(linuxRunInfos!!) else "[]",
            KEY_DARWIN_RUN_INFO to if (!darwinRunInfos.isNullOrEmpty()) JsonUtil.toJson(darwinRunInfos!!) else "[]",
            KEY_BUILD_DIR to buildDir,
            KEY_REPOSITORY_HTTP_URL to repositoryHttpUrl,
            KEY_STORE_WINDOWS_RUN_CUSTOM_VAR to "windows$storeRunCustomVarSuffix",
            KEY_STORE_LINUX_RUN_CUSTOM_VAR to "linux$storeRunCustomVarSuffix",
            KEY_STORE_DARWIN_RUN_CUSTOM_VAR to "darwin$storeRunCustomVarSuffix"
        )
        if (queryDefaultScriptFlag) {
            val frameworkCode = storeBaseFeatureExtQueryDao.getStoreBaseFeatureExt(
                dslContext = dslContext, storeCode = storeCode, storeType = storeType, fieldName = KEY_FRAMEWORK_CODE
            )?.fieldValue ?: ""
            val buildInfoRecord = storeBuildInfoDao.getStoreBuildInfoByLanguage(dslContext, frameworkCode, storeType)
            val script = buildInfoRecord?.script
            if (script.isNullOrBlank()) {
                return startParamMap
            }
            if (JsonSchemaUtil.validateJson(script)) {
                val scriptMap = JsonUtil.toMap(script)
                scriptMap[OS.WINDOWS.name.lowercase()]?.let {
                    startParamMap[KEY_WINDOWS_DEFAULT_SCRIPT] = it.toString()
                }
                scriptMap[OS.LINUX.name.lowercase()]?.let {
                    startParamMap[KEY_LINUX_DEFAULT_SCRIPT] = it.toString()
                }
                scriptMap[OS.MACOS.name.lowercase()]?.let {
                    startParamMap[KEY_DARWIN_DEFAULT_SCRIPT] = it.toString()
                }
            } else {
                startParamMap[KEY_WINDOWS_DEFAULT_SCRIPT] = script
                startParamMap[KEY_LINUX_DEFAULT_SCRIPT] = script
                startParamMap[KEY_DARWIN_DEFAULT_SCRIPT] = script
            }
        }
        return startParamMap
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
        } else {
            storePkgEnvInfos.add(StorePkgEnvInfo(osName = OSType.WINDOWS.name.lowercase(), defaultFlag = true))
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
        val bkConfigInfo = YamlUtil.to(configFileContent, object : TypeReference<BkConfigInfo>() {})
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
        var extEnvInfo: MutableMap<String, Any>? = null
        osConfigInfo.signature?.originFilePaths?.let {
            val signatureFileKey = getSignatureFileKey()
            extEnvInfo = mutableMapOf(signatureFileKey to JsonUtil.toJson(it))
        }
        osConfigInfo.packScriptPath?.let {
            if (extEnvInfo != null) {
                extEnvInfo!![OsConfigInfo::packScriptPath.name] = it
            } else {
                extEnvInfo = mutableMapOf(OsConfigInfo::packScriptPath.name to it)
            }
        }
        val pkgLocalPath = osConfigInfo.packagePath
        if (extEnvInfo != null) {
            extEnvInfo!![OsConfigInfo::packagePath.name] = pkgLocalPath
        } else {
            extEnvInfo = mutableMapOf(OsConfigInfo::packagePath.name to pkgLocalPath)
        }
        val pkgName = File(pkgLocalPath).name
        val pkgRepoPathSb = StringBuilder("$storeCode/$version/")
        if (configOsName.isNotBlank()) {
            pkgRepoPathSb.append(configOsName).append("/")
        }
        if (!configOsArch.isNullOrBlank()) {
            pkgRepoPathSb.append(configOsArch).append("/")
        }
        pkgRepoPathSb.append(pkgName)
        return StorePkgEnvInfo(
            pkgName = pkgName,
            pkgLocalPath = pkgLocalPath,
            pkgRepoPath = pkgRepoPathSb.toString(),
            osName = configOsName,
            osArch = configOsArch,
            defaultFlag = osConfigInfo.defaultFlag,
            extEnvInfo = extEnvInfo
        )
    }

    private fun getSignatureFileKey(): String {
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
        if (isNormalUpgrade) {
            processInfo.add(ReleaseProcessItem(I18nUtil.getCodeLanMessage(messageCode = END), END, NUM_FIVE, UNDO))
        } else {
            processInfo.add(
                ReleaseProcessItem(I18nUtil.getCodeLanMessage(messageCode = APPROVE), APPROVE, NUM_FIVE, UNDO)
            )
            processInfo.add(ReleaseProcessItem(I18nUtil.getCodeLanMessage(messageCode = END), END, NUM_SIX, UNDO))
        }
        val totalStep = if (isNormalUpgrade) NUM_FIVE else NUM_SIX
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

            StoreStatusEnum.AUDITING -> {
                storeCommonService.setProcessInfo(processInfo, totalStep, NUM_FIVE, DOING)
            }

            StoreStatusEnum.AUDIT_REJECT -> {
                storeCommonService.setProcessInfo(processInfo, totalStep, NUM_FIVE, FAIL)
            }

            StoreStatusEnum.RELEASED -> {
                val currStep = if (isNormalUpgrade) NUM_FIVE else NUM_SIX
                storeCommonService.setProcessInfo(processInfo, totalStep, currStep, SUCCESS)
            }

            else -> {}
        }
        return processInfo
    }
}
