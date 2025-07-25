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

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.constant.ARTIFACT
import com.tencent.devops.common.api.constant.COMPONENT
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.constant.GOLANG
import com.tencent.devops.common.api.constant.INIT_VERSION
import com.tencent.devops.common.api.constant.JAVA
import com.tencent.devops.common.api.constant.KEY_OS
import com.tencent.devops.common.api.constant.KEY_OS_ARCH
import com.tencent.devops.common.api.constant.KEY_OS_NAME
import com.tencent.devops.common.api.constant.LABEL
import com.tencent.devops.common.api.constant.NAME
import com.tencent.devops.common.api.constant.NODEJS
import com.tencent.devops.common.api.constant.PYTHON
import com.tencent.devops.common.api.constant.REPORT
import com.tencent.devops.common.api.constant.REQUIRED
import com.tencent.devops.common.api.constant.STRING
import com.tencent.devops.common.api.constant.TYPE
import com.tencent.devops.common.api.enums.FrontendTypeEnum
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildLessAtomElement
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.model.store.tables.records.TAtomRecord
import com.tencent.devops.store.atom.dao.AtomDao
import com.tencent.devops.store.atom.dao.MarketAtomDao
import com.tencent.devops.store.atom.dao.MarketAtomEnvInfoDao
import com.tencent.devops.store.atom.dao.MarketAtomVersionLogDao
import com.tencent.devops.store.atom.factory.AtomBusHandleFactory
import com.tencent.devops.store.atom.service.MarketAtomCommonService
import com.tencent.devops.store.common.dao.StoreProjectRelDao
import com.tencent.devops.store.common.service.StoreCommonService
import com.tencent.devops.store.common.utils.BkInitProjectCacheUtil
import com.tencent.devops.store.common.utils.StoreUtils
import com.tencent.devops.store.common.utils.VersionUtils
import com.tencent.devops.store.constant.StoreConstants.BK_DEFAULT_FAIL_POLICY
import com.tencent.devops.store.constant.StoreConstants.BK_DEFAULT_RETRY_POLICY
import com.tencent.devops.store.constant.StoreConstants.BK_DEFAULT_TIMEOUT
import com.tencent.devops.store.constant.StoreConstants.BK_RETRY_TIMES
import com.tencent.devops.store.constant.StoreConstants.DEFAULT_PARAM_FIELD_IS_INVALID
import com.tencent.devops.store.constant.StoreConstants.TASK_JSON_CONFIG_POLICY_FIELD_IS_INVALID
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.pojo.atom.AtomEnvRequest
import com.tencent.devops.store.pojo.atom.AtomPostInfo
import com.tencent.devops.store.pojo.atom.AtomRunInfo
import com.tencent.devops.store.pojo.atom.GetAtomConfigResult
import com.tencent.devops.store.pojo.atom.enums.AtomFailPolicyEnum
import com.tencent.devops.store.pojo.atom.enums.AtomRetryPolicyEnum
import com.tencent.devops.store.pojo.atom.enums.AtomStatusEnum
import com.tencent.devops.store.pojo.atom.enums.JobTypeEnum
import com.tencent.devops.store.pojo.common.ATOM_INPUT
import com.tencent.devops.store.pojo.common.ATOM_POST
import com.tencent.devops.store.pojo.common.ATOM_POST_CONDITION
import com.tencent.devops.store.pojo.common.ATOM_POST_ENTRY_PARAM
import com.tencent.devops.store.pojo.common.ATOM_POST_FLAG
import com.tencent.devops.store.pojo.common.ATOM_POST_NORMAL_PROJECT_FLAG_KEY_PREFIX
import com.tencent.devops.store.pojo.common.ATOM_POST_VERSION_TEST_FLAG_KEY_PREFIX
import com.tencent.devops.store.pojo.common.IS_EXPANDED
import com.tencent.devops.store.pojo.common.KEY_ATOM_CODE
import com.tencent.devops.store.pojo.common.KEY_CONFIG
import com.tencent.devops.store.pojo.common.KEY_DEFAULT
import com.tencent.devops.store.pojo.common.KEY_DEFAULT_FLAG
import com.tencent.devops.store.pojo.common.KEY_DEMANDS
import com.tencent.devops.store.pojo.common.KEY_EXECUTION
import com.tencent.devops.store.pojo.common.KEY_FINISH_KILL_FLAG
import com.tencent.devops.store.pojo.common.KEY_INPUT
import com.tencent.devops.store.pojo.common.KEY_INPUT_GROUPS
import com.tencent.devops.store.pojo.common.KEY_LANGUAGE
import com.tencent.devops.store.pojo.common.KEY_MINIMUM_VERSION
import com.tencent.devops.store.pojo.common.KEY_OUTPUT
import com.tencent.devops.store.pojo.common.KEY_PACKAGE_PATH
import com.tencent.devops.store.pojo.common.KEY_RUNTIME_VERSION
import com.tencent.devops.store.pojo.common.KEY_TARGET
import com.tencent.devops.store.pojo.common.KEY_TYPE
import com.tencent.devops.store.pojo.common.TASK_JSON_NAME
import com.tencent.devops.store.pojo.common.enums.ReleaseTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import jakarta.ws.rs.core.Response
import kotlin.reflect.KClass
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Suppress("ALL")
@Service
class MarketAtomCommonServiceImpl : MarketAtomCommonService {

    @Autowired
    private lateinit var dslContext: DSLContext

    @Autowired
    private lateinit var redisOperation: RedisOperation

    @Autowired
    private lateinit var atomDao: AtomDao

    @Autowired
    private lateinit var marketAtomDao: MarketAtomDao

    @Autowired
    private lateinit var marketAtomEnvInfoDao: MarketAtomEnvInfoDao

    @Autowired
    private lateinit var marketAtomVersionLogDao: MarketAtomVersionLogDao

    @Autowired
    private lateinit var storeProjectRelDao: StoreProjectRelDao

    @Autowired
    private lateinit var storeCommonService: StoreCommonService

    @Value("\${pipeline.setting.common.stage.job.task.maxInputNum:100}")
    private val maxInputNum: Int = 100

    @Value("\${pipeline.setting.common.stage.job.task.maxOutputNum:100}")
    private val maxOutputNum: Int = 100

    @Value("\${store.atom.maxTimeout:10080}")
    private val maxAtomTimeout: Int = 10080

    @Value("\${store.atom.defaultTimeout:900}")
    private val defaultAtomTimeout: Int = 900

    @Value("\${store.atom.minTimeout:1}")
    private val minAtomTimeout: Int = 1

    @Value("\${store.atom.maxRetryNum:5}")
    private val maxAtomRetryTimes: Int = 5

    @Value("\${store.atom.minRetryNum:1}")
    private val minAtomRetryTimes: Int = 1

    private val logger = LoggerFactory.getLogger(MarketAtomCommonServiceImpl::class.java)

    @Suppress("UNCHECKED_CAST")
    override fun validateAtomVersion(
        atomRecord: TAtomRecord,
        releaseType: ReleaseTypeEnum,
        osList: ArrayList<String>,
        version: String
    ): Result<Boolean> {
        val dbVersion = atomRecord.version
        val atomStatus = atomRecord.atomStatus
        // 判断插件首个版本对应的请求是否合法
        if (releaseType == ReleaseTypeEnum.NEW && dbVersion == INIT_VERSION &&
            atomStatus != AtomStatusEnum.INIT.status.toByte()) {
            throw ErrorCodeException(errorCode = CommonMessageCode.ERROR_REST_EXCEPTION_COMMON_TIP)
        }
        val dbOsList = if (!atomRecord.os.isNullOrBlank()) JsonUtil.getObjectMapper().readValue(
            atomRecord.os,
            List::class.java
        ) as List<String> else null
        // 支持的操作系统减少必须采用大版本升级方案
        val requireReleaseType =
            if (null != dbOsList && !osList.containsAll(dbOsList)) {
                ReleaseTypeEnum.INCOMPATIBILITY_UPGRADE // 最近的版本处于上架中止状态，重新升级版本号不变
            } else releaseType
        val cancelFlag = atomStatus == AtomStatusEnum.GROUNDING_SUSPENSION.status.toByte()
        val requireVersionList =
            if (cancelFlag && releaseType == ReleaseTypeEnum.CANCEL_RE_RELEASE) {
                listOf(dbVersion)
            } else {
                // 历史大版本下的小版本更新模式需获取要更新大版本下的最新版本
                val reqVersion = if (releaseType == ReleaseTypeEnum.HIS_VERSION_UPGRADE) {
                    atomDao.getPipelineAtom(
                        dslContext = dslContext,
                        atomCode = atomRecord.atomCode,
                        version = VersionUtils.convertLatestVersion(version)
                    )?.version
                } else {
                    null
                }
                storeCommonService.getRequireVersion(
                    reqVersion = reqVersion,
                    dbVersion = dbVersion,
                    releaseType = requireReleaseType
                )
            }
        if (!requireVersionList.contains(version)) {
            return I18nUtil.generateResponseDataObject(
                messageCode = StoreMessageCode.USER_ATOM_VERSION_IS_INVALID,
                params = arrayOf(version, requireVersionList.toString()),
                language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
            )
        }
        if (dbVersion.isNotBlank() && releaseType != ReleaseTypeEnum.NEW) {
            // 判断最近一个插件版本的状态，只有处于审核驳回、已发布、上架中止和已下架的状态才允许添加新的版本
            val atomFinalStatusList = listOf(
                AtomStatusEnum.AUDIT_REJECT.status.toByte(),
                AtomStatusEnum.RELEASED.status.toByte(),
                AtomStatusEnum.GROUNDING_SUSPENSION.status.toByte(),
                AtomStatusEnum.UNDERCARRIAGED.status.toByte()
            )
            if (!atomFinalStatusList.contains(atomStatus)) {
                return I18nUtil.generateResponseDataObject(
                    messageCode = StoreMessageCode.USER_ATOM_VERSION_IS_NOT_FINISH,
                    params = arrayOf(atomRecord.name, atomRecord.version),
                    language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
                )
            }
        }
        return Result(true)
    }

    @Suppress("UNCHECKED_CAST")
    override fun validateReleaseType(
        atomId: String,
        atomCode: String,
        version: String,
        releaseType: ReleaseTypeEnum,
        taskDataMap: Map<String, Any>,
        fieldCheckConfirmFlag: Boolean?
    ) {
        val validateReleaseTypeList = listOf(ReleaseTypeEnum.COMPATIBILITY_FIX, ReleaseTypeEnum.COMPATIBILITY_UPGRADE)
        val validateFlag = releaseType in validateReleaseTypeList
        val dbAtomProps = marketAtomDao.getLatestAtomByCode(dslContext, atomCode)?.props
        if (dbAtomProps != null && (validateFlag || getCancelValidateFlag(
                atomId = atomId,
                releaseType = releaseType,
                validateReleaseTypeList = validateReleaseTypeList
            ))
        ) {
            val dbAtomPropMap = JsonUtil.toMap(dbAtomProps)
            val dbAtomInputMap = dbAtomPropMap[KEY_INPUT] as? Map<String, Any>
            val dbAtomOutputMap = dbAtomPropMap[KEY_OUTPUT] as? Map<String, Any>
            val currentAtomInputMap = taskDataMap[KEY_INPUT] as? Map<String, Any>
            val currentAtomOutputMap = taskDataMap[KEY_OUTPUT] as? Map<String, Any>
            val dbAtomInputNames = dbAtomInputMap?.keys
            val dbAtomOutputNames = dbAtomOutputMap?.keys?.toMutableSet()
            val currentAtomInputNames = currentAtomInputMap?.keys?.toMutableSet()
            val currentAtomOutputNames = currentAtomOutputMap?.keys
            // 判断插件是否有新增输入参数
            if (dbAtomInputNames?.isNotEmpty() == true) {
                currentAtomInputNames?.removeAll(dbAtomInputNames)
                if (currentAtomInputNames?.isNotEmpty() == true) {
                    validateAtomAddInputField(
                        atomAddInputNames = currentAtomInputNames,
                        atomInputMap = currentAtomInputMap,
                        atomCode = atomCode,
                        version = version,
                        fieldCheckConfirmFlag = fieldCheckConfirmFlag
                    )
                }
            } else if (dbAtomInputNames?.isEmpty() == true && currentAtomInputNames?.isNotEmpty() == true) {
                // 当前版本的插件有输入参数且上一个版本有输入参数也需要校验
                validateAtomAddInputField(
                    atomAddInputNames = currentAtomInputNames,
                    atomInputMap = currentAtomInputMap,
                    atomCode = atomCode,
                    version = version,
                    fieldCheckConfirmFlag = fieldCheckConfirmFlag
                )
            }
            // 判断插件是否有减少的输出参数
            handleAtomDecreaseField(currentAtomOutputNames, dbAtomOutputNames, fieldCheckConfirmFlag)
        }
    }

    private fun getCancelValidateFlag(
        atomId: String,
        releaseType: ReleaseTypeEnum,
        validateReleaseTypeList: List<ReleaseTypeEnum>
    ): Boolean {
        var cancelValidateFlag = false
        if (releaseType != ReleaseTypeEnum.CANCEL_RE_RELEASE) {
            return cancelValidateFlag
        }
        val atomVersionRecord = marketAtomVersionLogDao.getAtomVersion(dslContext, atomId)
        val dbReleaseType = ReleaseTypeEnum.getReleaseTypeObj(atomVersionRecord.releaseType.toInt())!!
        if (dbReleaseType in validateReleaseTypeList) {
            cancelValidateFlag = true
        }
        return cancelValidateFlag
    }

    private fun handleAtomDecreaseField(
        currentAtomOutputNames: Set<String>?,
        dbAtomOutputNames: MutableSet<String>?,
        fieldCheckConfirmFlag: Boolean? = false
    ) {
        var flag = false
        if (currentAtomOutputNames?.isNotEmpty() == true) {
            dbAtomOutputNames?.removeAll(currentAtomOutputNames)
            if (dbAtomOutputNames?.isNotEmpty() == true) {
                // 当前版本的插件有减少的输出参数，让用户确定是否继续发布
                flag = true
            }
        } else if (currentAtomOutputNames?.isEmpty() == true && dbAtomOutputNames?.isNotEmpty() == true) {
            // 当前版本的插件无输出参数且上一个版本有输出参数，让用户确定是否继续发布
            flag = true
        }
        if (flag && fieldCheckConfirmFlag != true) {
            if (dbAtomOutputNames?.isNotEmpty() == true) {
                throw ErrorCodeException(
                    statusCode = Response.Status.OK.statusCode,
                    errorCode = StoreMessageCode.USER_ATOM_COMPATIBLE_OUTPUT_FIELD_CONFIRM,
                    params = arrayOf(JsonUtil.toJson(dbAtomOutputNames))
                )
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun validateAtomAddInputField(
        atomAddInputNames: MutableSet<String>,
        atomInputMap: Map<String, Any>,
        atomCode: String,
        version: String,
        fieldCheckConfirmFlag: Boolean? = false
    ) {
        val invalidAtomInputNames = mutableSetOf<String>()
        // 判断新增的必填参数是否有默认值
        atomAddInputNames.forEach { atomInputName ->
            val atomInputField = atomInputMap[atomInputName] as? Map<String, Any>
            if (atomInputField?.get(REQUIRED) == true && atomInputField[KEY_DEFAULT] == null) {
                invalidAtomInputNames.add(atomInputName)
            }
        }
        if (invalidAtomInputNames.isNotEmpty()) {
            // 存在没有默认值的不兼容新增参数，中断发布流程
            logger.info("validateVersion $atomCode,$version,invalidAtomInputNames:$invalidAtomInputNames")
            throw ErrorCodeException(
                errorCode = StoreMessageCode.USER_ATOM_NOT_COMPATIBLE_INPUT_FIELD,
                params = arrayOf(JsonUtil.toJson(invalidAtomInputNames))
            )
        } else if (fieldCheckConfirmFlag != true) {
            // 存在有默认值的不兼容新增参数，让用户确定是否继续发布
            logger.info("validateVersion $atomCode,$version,confirmAtomInputNames:$atomAddInputNames")
            throw ErrorCodeException(
                statusCode = Response.Status.OK.statusCode,
                errorCode = StoreMessageCode.USER_ATOM_COMPATIBLE_INPUT_FIELD_CONFIRM,
                params = arrayOf(JsonUtil.toJson(atomAddInputNames))
            )
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun parseBaseTaskJson(
        taskJsonStr: String,
        projectCode: String,
        atomCode: String,
        version: String,
        userId: String
    ): GetAtomConfigResult {
        val taskDataMap = try {
            JsonUtil.toMap(taskJsonStr)
        } catch (e: Exception) {
            throw ErrorCodeException(
                errorCode = StoreMessageCode.USER_ATOM_CONF_INVALID,
                params = arrayOf(TASK_JSON_NAME)
            )
        }
        val taskAtomCode = taskDataMap[KEY_ATOM_CODE] as? String
        if (atomCode != taskAtomCode) {
            // 如果用户输入的插件代码和其代码库配置文件的不一致，则抛出错误提示给用户
            throw ErrorCodeException(
                errorCode = StoreMessageCode.USER_REPOSITORY_TASK_JSON_FIELD_IS_NOT_MATCH,
                params = arrayOf(KEY_ATOM_CODE)
            )
        }
        val executionInfoMap = taskDataMap[KEY_EXECUTION] as? Map<String, Any>
        val keyInputGroupMapList = taskDataMap[KEY_INPUT_GROUPS] as? List<Map<String, Any>>
        if (!keyInputGroupMapList.isNullOrEmpty()) {
            keyInputGroupMapList.forEach { inputGroupMap ->
                validateTaskJsonField(
                    dataMap = inputGroupMap,
                    fieldName = NAME,
                    promptName = "$KEY_INPUT_GROUPS.$NAME",
                    expectedType = String::class
                )
                validateTaskJsonField(
                    dataMap = inputGroupMap,
                    fieldName = LABEL,
                    promptName = "$KEY_INPUT_GROUPS.$LABEL",
                    expectedType = String::class
                )
                validateTaskJsonField(
                    dataMap = inputGroupMap,
                    fieldName = IS_EXPANDED,
                    promptName = "$KEY_INPUT_GROUPS.$IS_EXPANDED",
                    expectedType = Boolean::class
                )
            }
        }
        var atomPostInfo: AtomPostInfo? = null
        if (executionInfoMap == null) {
            // 抛出错误提示
            throw ErrorCodeException(
                errorCode = StoreMessageCode.USER_REPOSITORY_TASK_JSON_FIELD_IS_NULL,
                params = arrayOf(KEY_EXECUTION)
            )
        }
        // pref:完善研发商店组件配置文件参数校验 #11269
        validateTaskJsonField(
            dataMap = executionInfoMap,
            fieldName = KEY_LANGUAGE,
            expectedType = String::class,
            promptName = "$KEY_EXECUTION.$KEY_LANGUAGE",
            supportedFieldTypes = setOf(JAVA, PYTHON, GOLANG, NODEJS)
        )
        val language = executionInfoMap[KEY_LANGUAGE].toString()
        val config = taskDataMap[KEY_CONFIG] as? Map<String, Any>
        config?.let { validateConfigMap(config) }
        val atomPostMap = executionInfoMap[ATOM_POST] as? Map<String, Any>
        if (null != atomPostMap) {
            try {
                val postCondition = atomPostMap[ATOM_POST_CONDITION] as? String
                    ?: throw ErrorCodeException(
                        errorCode = StoreMessageCode.USER_REPOSITORY_TASK_JSON_FIELD_IS_INVALID,
                        params = arrayOf(ATOM_POST_CONDITION)
                    )
                val postEntryParam = atomPostMap[ATOM_POST_ENTRY_PARAM] as? String
                    ?: throw ErrorCodeException(
                        errorCode = StoreMessageCode.USER_REPOSITORY_TASK_JSON_FIELD_IS_INVALID,
                        params = arrayOf(ATOM_POST_ENTRY_PARAM)
                    )
                atomPostInfo = AtomPostInfo(
                    atomCode = atomCode,
                    version = version,
                    postEntryParam = postEntryParam,
                    postCondition = postCondition
                )
            } catch (e: Exception) {
                throw ErrorCodeException(
                    errorCode = StoreMessageCode.USER_REPOSITORY_TASK_JSON_FIELD_IS_INVALID,
                    params = arrayOf(ATOM_POST_CONDITION)
                )
            }
        }
        val atomEnvRequests = mutableListOf<AtomEnvRequest>()
        val osList = executionInfoMap[KEY_OS] as? List<Map<String, Any>>
        val finishKillFlag = executionInfoMap[KEY_FINISH_KILL_FLAG] as? Boolean
        val runtimeVersion = executionInfoMap[KEY_RUNTIME_VERSION] as? String
        val atomBusHandleService = AtomBusHandleFactory.createAtomBusHandleService(language)
        val finalRuntimeVersion = if (runtimeVersion.isNullOrBlank()) {
            atomBusHandleService.getDefaultRuntimeVersion()
        } else {
            runtimeVersion
        }
        if (!osList.isNullOrEmpty()) {
            val osDefaultEnvNumMap = mutableMapOf<String, Int>()
            osList.forEach { osExecutionInfoMap ->
                validateTaskJsonField(
                    dataMap = osExecutionInfoMap,
                    fieldName = KEY_OS_NAME,
                    promptName = "$KEY_OS.$KEY_OS_NAME",
                    expectedType = String::class
                )
                val osName = osExecutionInfoMap[KEY_OS_NAME].toString()
                validateTaskJsonField(
                    dataMap = osExecutionInfoMap,
                    fieldName = KEY_TARGET,
                    promptName = "$KEY_OS.$KEY_TARGET",
                    expectedType = String::class
                )
                val target = osExecutionInfoMap[KEY_TARGET].toString()
                val osArch = osExecutionInfoMap[KEY_OS_ARCH] as? String
                val defaultFlag = osExecutionInfoMap[KEY_DEFAULT_FLAG] as? Boolean ?: false
                // 统计每种操作系统默认环境配置数量
                val increaseDefaultEnvNum = if (defaultFlag) 1 else 0
                if (osDefaultEnvNumMap.containsKey(osName)) {
                    osDefaultEnvNumMap[osName] = osDefaultEnvNumMap[osName]!! + increaseDefaultEnvNum
                } else {
                    osDefaultEnvNumMap[osName] = increaseDefaultEnvNum
                }
                val pkgLocalPath = osExecutionInfoMap[KEY_PACKAGE_PATH] as? String ?: ""
                val atomEnvRequest = AtomEnvRequest(
                    userId = userId,
                    pkgLocalPath = pkgLocalPath,
                    pkgRepoPath = getPkgRepoPath(pkgLocalPath, projectCode, atomCode, version),
                    language = language,
                    minVersion = executionInfoMap[KEY_MINIMUM_VERSION] as? String,
                    target = target,
                    shaContent = null,
                    preCmd = JsonUtil.toJson(osExecutionInfoMap[KEY_DEMANDS] ?: ""),
                    atomPostInfo = atomPostInfo,
                    osName = osName,
                    osArch = osArch,
                    runtimeVersion = finalRuntimeVersion,
                    defaultFlag = defaultFlag,
                    finishKillFlag = finishKillFlag
                )
                atomEnvRequests.add(atomEnvRequest)
            }
            osDefaultEnvNumMap.forEach { (osName, defaultEnvNum) ->
                // 判断每种操作系统默认环境配置是否有且只有1个
                if (defaultEnvNum != 1) {
                    throw ErrorCodeException(
                        errorCode = StoreMessageCode.USER_REPOSITORY_TASK_JSON_OS_DEFAULT_ENV_IS_INVALID,
                        params = arrayOf(TASK_JSON_NAME, osName, defaultEnvNum.toString())
                    )
                }
            }
        } else {
            val target = executionInfoMap[KEY_TARGET] as? String
            val pkgLocalPath = executionInfoMap[KEY_PACKAGE_PATH] as? String ?: ""
            val atomEnvRequest = AtomEnvRequest(
                userId = userId,
                pkgLocalPath = pkgLocalPath,
                pkgRepoPath = getPkgRepoPath(pkgLocalPath, projectCode, atomCode, version),
                language = language,
                minVersion = executionInfoMap[KEY_MINIMUM_VERSION] as? String,
                target = target,
                shaContent = null,
                preCmd = JsonUtil.toJson(executionInfoMap[KEY_DEMANDS] ?: ""),
                atomPostInfo = atomPostInfo,
                runtimeVersion = finalRuntimeVersion,
                defaultFlag = true,
                finishKillFlag = finishKillFlag
            )
            atomEnvRequests.add(atomEnvRequest)
        }

        val inputDataMap = taskDataMap[KEY_INPUT] as? Map<String, Any>
        if (inputDataMap != null && inputDataMap.size > maxInputNum) {
            throw ErrorCodeException(
                errorCode = StoreMessageCode.USER_ATOM_INPUT_NUM_IS_TOO_MANY,
                params = arrayOf(maxInputNum.toString())
            )
        }
        val outputDataMap = taskDataMap[KEY_OUTPUT] as? Map<String, Any>
        if (outputDataMap != null && outputDataMap.size > maxOutputNum) {
            throw ErrorCodeException(
                errorCode = StoreMessageCode.USER_ATOM_OUTPUT_NUM_IS_TOO_MANY,
                params = arrayOf(maxOutputNum.toString())
            )
        }
        outputDataMap?.let { dataMap ->
            for (entry in dataMap) {
                val key = entry.key as? String
                val value = entry.value as? Map<String, Any>
                if (key != null && value != null) {
                    validateTaskJsonField(
                        dataMap = value,
                        fieldName = KEY_TYPE,
                        promptName = "$KEY_OUTPUT.$key.$KEY_TYPE",
                        expectedType = String::class,
                        supportedFieldTypes = setOf(STRING, ARTIFACT, REPORT)
                    )
                }
            }
        }
        return GetAtomConfigResult("0", arrayOf(""), taskDataMap, atomEnvRequests)
    }

    private fun <T : Any> validateTaskJsonField(
        dataMap: Map<String, Any>,
        fieldName: String,
        promptName: String,
        expectedType: KClass<T>,
        required: Boolean = true,
        supportedFieldTypes: Set<String>? = null
    ) {
        val fieldValue = dataMap[fieldName]
        if (required) {
            if (fieldValue == null || (fieldValue is String && fieldValue.isBlank())) {
                throw ErrorCodeException(
                    errorCode = StoreMessageCode.USER_REPOSITORY_TASK_JSON_FIELD_IS_NULL,
                    params = arrayOf(promptName)
                )
            }
        }
        if (!expectedType.isInstance(fieldValue)) {
            throw ErrorCodeException(
                errorCode = StoreMessageCode.USER_REPOSITORY_TASK_JSON_FIELD_IS_INVALID,
                params = arrayOf(promptName)
            )
        }
        if (supportedFieldTypes != null) {
            if (fieldValue !in supportedFieldTypes) {
                throw ErrorCodeException(
                    errorCode = StoreMessageCode.USER_REPOSITORY_TASK_JSON_FIELD_IS_NOT_SUPPORT,
                    params = arrayOf(promptName, supportedFieldTypes.joinToString(separator = ","))
                )
            }
        }
    }

    private fun validateConfigMap(configMap: Map<String, Any>) {
        val message: String?
        val defaultTimeout = configMap[BK_DEFAULT_TIMEOUT] as? Int ?: defaultAtomTimeout
        if (defaultTimeout !in minAtomTimeout..maxAtomTimeout) {
            message = I18nUtil.getCodeLanMessage(
                messageCode = DEFAULT_PARAM_FIELD_IS_INVALID,
                params = arrayOf("defaultTimeout", "$minAtomTimeout~$maxAtomTimeout")
            )
            throw ErrorCodeException(
                errorCode = StoreMessageCode.TASK_JSON_CONFIG_IS_INVALID,
                params = arrayOf(message)
            )
        }
        val defaultFailPolicy = configMap[BK_DEFAULT_FAIL_POLICY] as? String
        if (defaultFailPolicy !in listOf(
                AtomFailPolicyEnum.AUTO_CONTINUE.name,
                AtomFailPolicyEnum.MANUALLY_CONTINUE.name, null)
        ) {
            message = I18nUtil.getCodeLanMessage(
                messageCode = DEFAULT_PARAM_FIELD_IS_INVALID,
                params = arrayOf(
                    "defaultFailPolicy",
                    "${AtomFailPolicyEnum.AUTO_CONTINUE}/${AtomFailPolicyEnum.MANUALLY_CONTINUE}"
                )
            )
            throw ErrorCodeException(
                errorCode = StoreMessageCode.TASK_JSON_CONFIG_IS_INVALID,
                params = arrayOf(message)
            )
        }
        val defaultRetryPolicy = configMap[BK_DEFAULT_RETRY_POLICY]
        if (defaultRetryPolicy != null) {
            if (defaultRetryPolicy !is List<*>) {
                throw ErrorCodeException(
                    errorCode = StoreMessageCode.TASK_JSON_CONFIG_IS_INVALID,
                    params = arrayOf("defaultRetryPolicy not is List")
                )
            }
            val invalidValues = defaultRetryPolicy.filter {
                it !in setOf(AtomRetryPolicyEnum.AUTO_RETRY.name, AtomRetryPolicyEnum.MANUALLY_RETRY.name)
            }
            if (invalidValues.isNotEmpty()) {
                message = I18nUtil.getCodeLanMessage(
                    messageCode = DEFAULT_PARAM_FIELD_IS_INVALID,
                    params = arrayOf(
                        "defaultRetryPolicy",
                        "${AtomRetryPolicyEnum.AUTO_RETRY}/${AtomRetryPolicyEnum.MANUALLY_RETRY}"
                    )
                )
                throw ErrorCodeException(
                    errorCode = StoreMessageCode.TASK_JSON_CONFIG_IS_INVALID,
                    params = arrayOf(message)
                )
            }
            if (defaultFailPolicy == AtomFailPolicyEnum.AUTO_CONTINUE.name &&
                AtomRetryPolicyEnum.MANUALLY_RETRY.name in defaultRetryPolicy) {
                message = I18nUtil.getCodeLanMessage(messageCode = TASK_JSON_CONFIG_POLICY_FIELD_IS_INVALID)
                throw ErrorCodeException(
                    errorCode = StoreMessageCode.TASK_JSON_CONFIG_IS_INVALID,
                    params = arrayOf(message)
                )
            }
            val retryTimes = configMap[BK_RETRY_TIMES] as? Int ?: minAtomRetryTimes
            if (AtomRetryPolicyEnum.AUTO_RETRY.name in defaultRetryPolicy &&
                retryTimes !in minAtomRetryTimes..maxAtomRetryTimes) {
                message = I18nUtil.getCodeLanMessage(
                    messageCode = DEFAULT_PARAM_FIELD_IS_INVALID,
                    params = arrayOf("retryTimes", "$minAtomRetryTimes~$maxAtomRetryTimes")
                )
                throw ErrorCodeException(
                    errorCode = StoreMessageCode.TASK_JSON_CONFIG_IS_INVALID,
                    params = arrayOf(message)
                )
            }
        }
    }

    private fun getPkgRepoPath(pkgLocalPath: String, projectCode: String, atomCode: String, version: String) =
        if (pkgLocalPath.isNotBlank()) {
            "$projectCode/$atomCode/$version/$pkgLocalPath"
        } else {
            ""
        }

    override fun checkEditCondition(atomCode: String): Boolean {
        // 查询插件的最新记录
        val newestAtomRecord = atomDao.getNewestAtomByCode(dslContext, atomCode)
            ?: throw ErrorCodeException(errorCode = CommonMessageCode.PARAMETER_IS_INVALID, params = arrayOf(atomCode))
        val atomFinalStatusList = listOf(
            AtomStatusEnum.AUDIT_REJECT.status.toByte(),
            AtomStatusEnum.RELEASED.status.toByte(),
            AtomStatusEnum.GROUNDING_SUSPENSION.status.toByte(),
            AtomStatusEnum.UNDERCARRIAGED.status.toByte(),
            AtomStatusEnum.INIT.status.toByte()
        )
        // 判断最近一个插件版本的状态，只有处于审核驳回、已发布、上架中止和已下架的状态才允许修改基本信息
        return atomFinalStatusList.contains(newestAtomRecord.atomStatus)
    }

    override fun getNormalUpgradeFlag(atomCode: String, status: Int): Boolean {
        val releaseTotalNum = marketAtomDao.countReleaseAtomByCode(dslContext, atomCode)
        val currentNum = if (status == AtomStatusEnum.RELEASED.status) 1 else 0
        return releaseTotalNum > currentNum
    }

    override fun handleAtomCache(atomId: String, atomCode: String, version: String, releaseFlag: Boolean) {
        val atomEnv = marketAtomEnvInfoDao.getNewestAtomEnvInfo(dslContext, atomId)
            ?: throw ErrorCodeException(
                errorCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf(atomId)
            )
        val atom = atomDao.getPipelineAtom(dslContext, atomId) ?: throw ErrorCodeException(
            errorCode = CommonMessageCode.PARAMETER_IS_INVALID,
            params = arrayOf(atomId)
        )
        val postEntryParam = atomEnv.postEntryParam
        val postCondition = atomEnv.postCondition
        val postFlag = !postEntryParam.isNullOrBlank() && !postCondition.isNullOrBlank()
        val atomPostMap = mapOf(
            ATOM_POST_FLAG to postFlag,
            ATOM_POST_ENTRY_PARAM to postEntryParam,
            ATOM_POST_CONDITION to postCondition
        )
        val atomRunInfoKey = StoreUtils.getStoreRunInfoKey(StoreTypeEnum.ATOM.name, atomCode)
        val jobType = atom.jobType
        val initProjectCode = storeProjectRelDao.getInitProjectCodeByStoreCode(
            dslContext = dslContext,
            storeCode = atomCode,
            storeType = StoreTypeEnum.ATOM.type.toByte()
        ) ?: ""
        val props = atom.props
        val params = getAtomSensitiveParams(props)
        val atomRunInfo = AtomRunInfo(
            atomCode = atomCode,
            atomName = atom.name,
            version = atom.version,
            initProjectCode = initProjectCode,
            jobType = if (jobType == null) null else JobTypeEnum.valueOf(jobType),
            buildLessRunFlag = atom.buildLessRunFlag,
            inputTypeInfos = generateInputTypeInfos(atom.props),
            atomStatus = atom.atomStatus,
            sensitiveParams = params?.joinToString(",")
        )
        // 更新插件当前版本号的缓存信息
        redisOperation.hset(
            key = "$ATOM_POST_NORMAL_PROJECT_FLAG_KEY_PREFIX:$atomCode",
            hashKey = version,
            values = JsonUtil.toJson(atomPostMap)
        )
        redisOperation.hset(
            key = atomRunInfoKey,
            hashKey = version,
            values = JsonUtil.toJson(atomRunInfo)
        )
        // 更新插件xxx.latest这种版本号的缓存信息
        redisOperation.hset(
            key = "$ATOM_POST_NORMAL_PROJECT_FLAG_KEY_PREFIX:$atomCode",
            hashKey = VersionUtils.convertLatestVersion(version),
            values = JsonUtil.toJson(atomPostMap)
        )
        redisOperation.hset(
            key = atomRunInfoKey,
            hashKey = VersionUtils.convertLatestVersion(version),
            values = JsonUtil.toJson(atomRunInfo)
        )
        if (releaseFlag) {
            // 更新插件当前大版本内是否有测试版本标识
            redisOperation.hset(
                key = "$ATOM_POST_VERSION_TEST_FLAG_KEY_PREFIX:$atomCode",
                hashKey = VersionUtils.convertLatestVersion(version),
                values = "false"
            )
        }
    }

    override fun updateAtomRunInfoCache(
        atomId: String,
        atomName: String?,
        jobType: JobTypeEnum?,
        buildLessRunFlag: Boolean?,
        latestFlag: Boolean?,
        props: String?
    ) {
        val atomRecord = atomDao.getPipelineAtom(dslContext, atomId) ?: return
        val atomCode = atomRecord.atomCode
        val version = atomRecord.version
        val atomRunInfoKey = StoreUtils.getStoreRunInfoKey(StoreTypeEnum.ATOM.name, atomCode)
        val atomRunInfoJson = redisOperation.hget(atomRunInfoKey, version)
        if (!atomRunInfoJson.isNullOrEmpty()) {
            val atomRunInfo = JsonUtil.to(atomRunInfoJson, AtomRunInfo::class.java)
            if (atomName != null) atomRunInfo.atomName = atomName
            if (jobType != null) atomRunInfo.jobType = jobType
            if (buildLessRunFlag != null) atomRunInfo.buildLessRunFlag = buildLessRunFlag
            if (props != null) atomRunInfo.inputTypeInfos = generateInputTypeInfos(props)
            val params = getAtomSensitiveParams(props ?: atomRecord.props)
            atomRunInfo.sensitiveParams = params?.joinToString(",")
            // 更新插件当前版本号的缓存信息
            redisOperation.hset(
                key = atomRunInfoKey,
                hashKey = version,
                values = JsonUtil.toJson(atomRunInfo)
            )
            val updateLatestAtomCacheFlag = if (latestFlag == true) {
                true
            } else {
                val latestAtomRecord = marketAtomDao.getLatestAtomByCode(dslContext, atomCode)
                atomId == latestAtomRecord?.id
            }
            if (updateLatestAtomCacheFlag) {
                // 更新插件xxx.latest这种版本号的缓存信息
                redisOperation.hset(
                    key = atomRunInfoKey,
                    hashKey = VersionUtils.convertLatestVersion(version),
                    values = JsonUtil.toJson(atomRunInfo)
                )
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun generateInputTypeInfos(props: String?): Map<String, String>? {
        var inputTypeInfos: Map<String, String>? = null
        if (!props.isNullOrEmpty()) {
            val propMap = JsonUtil.toMap(props)
            inputTypeInfos = mutableMapOf()
            val inputDataMap = propMap[ATOM_INPUT] as? Map<String, Any>
            if (inputDataMap != null) {
                // 生成新插件输入参数对应的类型数据
                inputDataMap.keys.forEach { inputKey ->
                    val inputDataObj = inputDataMap[inputKey] as Map<String, Any>
                    inputTypeInfos[inputKey] = inputDataObj[TYPE].toString()
                }
            } else {
                // 生成老插件输入参数对应的类型数据
                propMap.keys.forEach { inputKey ->
                    val inputDataObj = propMap[inputKey] as Map<String, Any>
                    inputTypeInfos[inputKey] = inputDataObj[COMPONENT].toString()
                }
            }
        }
        return inputTypeInfos
    }

    override fun isPublicAtom(atomCode: String): Boolean {
        val storePublicFlagKey = StoreUtils.getStorePublicFlagKey(StoreTypeEnum.ATOM.name)
        if (!redisOperation.hasKey(storePublicFlagKey)) {
            // 从db去查查默认插件
            val defaultAtomCodeRecords = atomDao.batchGetDefaultAtomCode(dslContext)
            val defaultAtomCodeList = defaultAtomCodeRecords.map { it.value1() }
            redisOperation.sadd(storePublicFlagKey, *defaultAtomCodeList.toTypedArray())
        }
        // 判断是否是默认插件
        return redisOperation.isMember(storePublicFlagKey, atomCode)
    }

    override fun getValidOsNameFlag(atomEnvRequests: List<AtomEnvRequest>): Boolean {
        // 判断task.json的配置是否有根据操作系统名称来配
        var validOsNameFlag = false
        for (atomEnvRequest in atomEnvRequests) {
            if (!atomEnvRequest.osName.isNullOrBlank()) {
                validOsNameFlag = true
                break
            }
        }
        return validOsNameFlag
    }

    override fun getValidOsArchFlag(atomEnvRequests: List<AtomEnvRequest>): Boolean {
        // 判断task.json的配置是否有根据操作系统cpu架构名称来配
        var validOsArchFlag = false
        for (atomEnvRequest in atomEnvRequests) {
            if (!atomEnvRequest.osArch.isNullOrBlank()) {
                validOsArchFlag = true
                break
            }
        }
        return validOsArchFlag
    }

    override fun getInitProjectCode(atomCode: String, classType: String, htmlTemplateVersion: String): String? {
        return if (htmlTemplateVersion == FrontendTypeEnum.HISTORY.typeVersion ||
            (classType != MarketBuildAtomElement.classType && classType != MarketBuildLessAtomElement.classType)
        ) {
            // 内置插件没有初始化项目，故返回空
            null
        } else {
            // 先从本地缓存获取插件的初始化项目
            val localCacheKey = BkInitProjectCacheUtil.getInitProjectCacheKey(atomCode, StoreTypeEnum.ATOM)
            var initProjectCode = BkInitProjectCacheUtil.getIfPresent(localCacheKey)
            if (initProjectCode == null) {
                // 本地缓存未找到记录则从redis中找
                val redisCacheKey = BkInitProjectCacheUtil.getInitProjectCacheKeyPrefix(StoreTypeEnum.ATOM)
                initProjectCode = redisOperation.hget(redisCacheKey, atomCode)
                if (initProjectCode == null) {
                    // redis缓存未找到记录则从db中找
                    initProjectCode = storeProjectRelDao.getInitProjectCodeByStoreCode(
                        dslContext = dslContext,
                        storeCode = atomCode,
                        storeType = StoreTypeEnum.ATOM.type.toByte()
                    ) ?: ""
                    BkInitProjectCacheUtil.put(localCacheKey, initProjectCode)
                    redisOperation.hset(
                        key = redisCacheKey,
                        hashKey = atomCode,
                        values = initProjectCode
                    )
                } else {
                    BkInitProjectCacheUtil.put(localCacheKey, initProjectCode)
                }
            }
            initProjectCode
        }
    }

    override fun getAtomSensitiveParams(props: String): List<String>? {
        return try {
            val propsMap: Map<String, Any> = jacksonObjectMapper().readValue(props)
            propsMap["input"]?.let { input ->
                (input as? Map<*, *>)?.flatMap { (key, value) ->
                    when {
                        value is Map<*, *> && value["isSensitive"] as? Boolean == true ->
                            listOf(key.toString())
                        else -> emptyList()
                    }
                }
            }?.takeIf { it.isNotEmpty() }
        } catch (e: Exception) {
            logger.error("Parse atom props failed, props: $props", e)
            null
        }
    }
}
