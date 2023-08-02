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

import com.tencent.devops.artifactory.api.ServiceArchiveAtomResource
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.enums.FrontendTypeEnum
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.store.constant.StoreMessageCode.GET_INFO_NO_PERMISSION
import com.tencent.devops.store.dao.atom.AtomDao
import com.tencent.devops.store.dao.atom.MarketAtomDao
import com.tencent.devops.store.dao.atom.MarketAtomEnvInfoDao
import com.tencent.devops.store.dao.atom.MarketAtomVersionLogDao
import com.tencent.devops.store.dao.common.StoreMemberDao
import com.tencent.devops.store.pojo.atom.AtomConfigInfo
import com.tencent.devops.store.pojo.atom.AtomPkgInfoUpdateRequest
import com.tencent.devops.store.pojo.atom.GetAtomConfigResult
import com.tencent.devops.store.pojo.atom.ReleaseInfo
import com.tencent.devops.store.pojo.common.KEY_CONFIG
import com.tencent.devops.store.pojo.common.KEY_EXECUTION
import com.tencent.devops.store.pojo.common.KEY_INPUT
import com.tencent.devops.store.pojo.common.KEY_INPUT_GROUPS
import com.tencent.devops.store.pojo.common.KEY_OUTPUT
import com.tencent.devops.store.pojo.common.KEY_RELEASE_INFO
import com.tencent.devops.store.pojo.common.TASK_JSON_NAME
import com.tencent.devops.store.pojo.common.enums.ReleaseTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.atom.MarketAtomArchiveService
import com.tencent.devops.store.service.atom.MarketAtomCommonService
import com.tencent.devops.store.service.common.StoreI18nMessageService
import com.tencent.devops.store.utils.StoreUtils
import java.net.URLEncoder
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Suppress("ALL")
@Service
class MarketAtomArchiveServiceImpl : MarketAtomArchiveService {

    @Autowired
    lateinit var dslContext: DSLContext
    @Autowired
    lateinit var atomDao: AtomDao
    @Autowired
    lateinit var marketAtomDao: MarketAtomDao
    @Autowired
    lateinit var marketAtomEnvInfoDao: MarketAtomEnvInfoDao
    @Autowired
    lateinit var storeMemberDao: StoreMemberDao
    @Autowired
    lateinit var marketAtomVersionLogDao: MarketAtomVersionLogDao
    @Autowired
    lateinit var marketAtomCommonService: MarketAtomCommonService
    @Autowired
    lateinit var storeI18nMessageService: StoreI18nMessageService
    @Autowired
    lateinit var client: Client

    private val logger = LoggerFactory.getLogger(MarketAtomArchiveServiceImpl::class.java)

    override fun getFileStr(
        projectCode: String,
        atomCode: String,
        version: String,
        fileName: String
    ): String {
        logger.info("getFileStr params:[$projectCode|$atomCode|$version|$fileName")
        val filePath = URLEncoder.encode("$projectCode/$atomCode/$version/$fileName", "UTF-8")
        val taskJsonStr = client.get(ServiceArchiveAtomResource::class).getAtomFileContent(filePath).data
        return taskJsonStr!!
    }

    @Suppress("UNCHECKED_CAST")
    override fun verifyAtomPackageByUserId(
        userId: String,
        projectCode: String,
        atomCode: String,
        version: String,
        releaseType: ReleaseTypeEnum?,
        os: String?
    ): Result<Boolean> {
        logger.info("verifyAtomPackageByUserId params[$userId|$projectCode|$atomCode|$version|$releaseType|$os]")
        val atomCount = atomDao.countByCode(dslContext, atomCode)
        if (atomCount < 0) {
            return I18nUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf(atomCode),
                language = I18nUtil.getLanguage(userId)
            )
        }
        val atomRecord = atomDao.getNewestAtomByCode(dslContext, atomCode)!!
        if (atomRecord.classType != atomCode) {
            // 校验用户是否是该插件的开发成员
            val flag = storeMemberDao.isStoreMember(
                dslContext = dslContext,
                userId = userId,
                storeCode = atomCode,
                storeType = StoreTypeEnum.ATOM.type.toByte()
            )
            if (!flag) {
                throw ErrorCodeException(
                    errorCode = GET_INFO_NO_PERMISSION,
                    params = arrayOf(atomCode)
                )
            }
        }
        // 不是重新上传的包才需要校验版本号
        if (null != releaseType) {
            val osList = JsonUtil.getObjectMapper().readValue(os, ArrayList::class.java) as ArrayList<String>
            val validateAtomVersionResult = marketAtomCommonService.validateAtomVersion(
                atomRecord = atomRecord,
                releaseType = releaseType,
                osList = osList,
                version = version
            )
            logger.info("validateAtomVersionResult is :$validateAtomVersionResult")
            if (validateAtomVersionResult.isNotOk()) {
                return validateAtomVersionResult
            }
        }
        return Result(true)
    }

    @Suppress("UNCHECKED_CAST")
    override fun verifyAtomTaskJson(
        userId: String,
        projectCode: String,
        atomCode: String,
        version: String
    ): Result<GetAtomConfigResult?> {
        val taskJsonStr = storeI18nMessageService.parseJsonStrI18nInfo(
            jsonStr = getFileStr(projectCode, atomCode, version, TASK_JSON_NAME),
            keyPrefix = StoreUtils.getStoreFieldKeyPrefix(StoreTypeEnum.ATOM, atomCode, version)
        )
        val getAtomConfResult = marketAtomCommonService.parseBaseTaskJson(
            taskJsonStr = taskJsonStr,
            projectCode = projectCode,
            atomCode = atomCode,
            version = version,
            userId = userId
        )
        return if (getAtomConfResult.errorCode != "0") {
            I18nUtil.generateResponseDataObject(
                messageCode = getAtomConfResult.errorCode,
                params = getAtomConfResult.errorParams,
                language = I18nUtil.getLanguage(userId)
            )
        } else {
            Result(getAtomConfResult)
        }
    }

    override fun validateReleaseType(
        userId: String,
        projectCode: String,
        atomCode: String,
        version: String,
        fieldCheckConfirmFlag: Boolean?
    ): Result<Boolean> {
        val atomInfo = atomDao.getPipelineAtom(dslContext, atomCode, version)
            ?: throw ErrorCodeException(
                errorCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf("$atomCode+$version")
            )
        val taskJsonStr = getFileStr(projectCode, atomCode, version, TASK_JSON_NAME)
        val getAtomConfResult = marketAtomCommonService.parseBaseTaskJson(
            taskJsonStr = taskJsonStr,
            projectCode = projectCode,
            atomCode = atomCode,
            version = version,
            userId = userId
        )
        if (getAtomConfResult.errorCode != "0") {
            return I18nUtil.generateResponseDataObject(
                messageCode = getAtomConfResult.errorCode,
                params = getAtomConfResult.errorParams,
                language = I18nUtil.getLanguage(userId)
            )
        }
        val taskDataMap = getAtomConfResult.taskDataMap
        val atomId = atomInfo.id
        val atomVersionRecord = marketAtomVersionLogDao.getAtomVersion(dslContext, atomInfo.id)
        val releaseType = ReleaseTypeEnum.getReleaseTypeObj(atomVersionRecord.releaseType.toInt())!!
        marketAtomCommonService.validateReleaseType(
            atomId = atomId,
            atomCode = atomCode,
            version = version,
            releaseType = releaseType,
            taskDataMap = taskDataMap,
            fieldCheckConfirmFlag = fieldCheckConfirmFlag
        )
        return Result(true)
    }

    @Suppress("UNCHECKED_CAST")
    override fun updateAtomPkgInfo(
        userId: String,
        atomId: String,
        atomPkgInfoUpdateRequest: AtomPkgInfoUpdateRequest
    ): Result<Boolean> {
        val taskDataMap = atomPkgInfoUpdateRequest.taskDataMap
        val executionInfoMap = taskDataMap[KEY_EXECUTION] as Map<String, Any>
        val propsMap = mutableMapOf<String, Any?>()
        val releaseInfoMap = taskDataMap[KEY_RELEASE_INFO] as? Map<String, Any>
        val configInfoMap = releaseInfoMap?.get(ReleaseInfo::configInfo.name) as? Map<String, Any>
        val frontendType = configInfoMap?.get(AtomConfigInfo::frontendType.name) as? String
        val inputDataMap = taskDataMap[KEY_INPUT] as? Map<String, Any>
        if (frontendType == FrontendTypeEnum.HISTORY.name) {
            inputDataMap?.let { propsMap.putAll(inputDataMap) }
        } else {
            propsMap[KEY_INPUT_GROUPS] = taskDataMap[KEY_INPUT_GROUPS]
            propsMap[KEY_INPUT] = inputDataMap
            propsMap[KEY_OUTPUT] = taskDataMap[KEY_OUTPUT]
            propsMap[KEY_CONFIG] = taskDataMap[KEY_CONFIG]
        }
        dslContext.transaction { t ->
            val context = DSL.using(t)
            val props = JsonUtil.toJson(propsMap)
            marketAtomDao.updateMarketAtomProps(context, atomId, props, userId)
            marketAtomEnvInfoDao.deleteAtomEnvInfoById(context, atomId)
            marketAtomEnvInfoDao.addMarketAtomEnvInfo(context, atomId, atomPkgInfoUpdateRequest.atomEnvRequests)
        }
        return Result(true)
    }
}
