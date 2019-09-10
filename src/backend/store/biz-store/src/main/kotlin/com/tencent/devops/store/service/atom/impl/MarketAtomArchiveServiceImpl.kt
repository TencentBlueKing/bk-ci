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

import com.tencent.devops.artifactory.api.ServiceArchiveAtomResource
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.dao.atom.MarketAtomDao
import com.tencent.devops.store.dao.atom.MarketAtomEnvInfoDao
import com.tencent.devops.store.dao.common.StoreMemberDao
import com.tencent.devops.store.pojo.atom.AtomEnvRequest
import com.tencent.devops.store.pojo.atom.GetAtomConfigResult
import com.tencent.devops.store.pojo.atom.enums.ReleaseTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.atom.MarketAtomArchiveService
import com.tencent.devops.store.service.atom.MarketAtomCommonService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.util.StringUtils
import java.net.URLEncoder

@Service
class MarketAtomArchiveServiceImpl : MarketAtomArchiveService {

    @Autowired
    lateinit var dslContext: DSLContext
    @Autowired
    lateinit var marketAtomDao: MarketAtomDao
    @Autowired
    lateinit var marketAtomEnvInfoDao: MarketAtomEnvInfoDao
    @Autowired
    lateinit var storeMemberDao: StoreMemberDao
    @Autowired
    lateinit var marketAtomCommonService: MarketAtomCommonService
    @Autowired
    lateinit var client: Client

    private val logger = LoggerFactory.getLogger(MarketAtomArchiveServiceImpl::class.java)

    override fun getTaskJsonStr(projectCode: String, atomCode: String, version: String): String {
        logger.info("getTaskJsonStr projectCode is:$projectCode,atomCode is :$atomCode,version is :$version")
        val filePath = URLEncoder.encode("$projectCode/$atomCode/$version/task.json", "UTF-8")
        val taskJsonStr = client.get(ServiceArchiveAtomResource::class).getAtomFileContent(filePath).data
        logger.info("the taskJsonStr is :$taskJsonStr")
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
        logger.info("verifyAtomPackageByUserId userId is :$userId,projectCode is :$projectCode,atomCode is :$atomCode,version is :$version,releaseType is :$releaseType,os is :$os")
        // 校验用户是否是该插件的开发成员
        val flag = storeMemberDao.isStoreMember(dslContext, userId, atomCode, StoreTypeEnum.ATOM.type.toByte())
        if (!flag) {
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PERMISSION_DENIED)
        }
        val atomRecords = marketAtomDao.getAtomsByAtomCode(dslContext, atomCode)
        logger.info("the atomRecords is :$atomRecords")
        if (null == atomRecords || atomRecords.isEmpty()) {
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PARAMETER_IS_INVALID, arrayOf(atomCode))
        }
        val atomRecord = atomRecords[0]
        logger.info("the latest atomRecord is :$atomRecord")
        // 不是重新上传的包才需要校验版本号
        if (null != releaseType) {
            val osList = JsonUtil.getObjectMapper().readValue(os, ArrayList::class.java) as ArrayList<String>
            val validateAtomVersionResult =
                marketAtomCommonService.validateAtomVersion(atomRecord, releaseType, osList, version)
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
        val taskJsonStr = getTaskJsonStr(projectCode, atomCode, version)
        val getAtomConfResult =
            marketAtomCommonService.parseBaseTaskJson(taskJsonStr, projectCode, atomCode, version, userId)
        logger.info("parseTaskJson result is :$taskJsonStr")
        return if (getAtomConfResult.errorCode != "0") {
            MessageCodeUtil.generateResponseDataObject(getAtomConfResult.errorCode, getAtomConfResult.errorParams)
        } else {
            val taskDataMap = JsonUtil.toMap(taskJsonStr)
            val executionInfoMap = taskDataMap["execution"] as Map<String, Any>
            val packagePath = executionInfoMap["packagePath"] as? String
            if (StringUtils.isEmpty(packagePath)) {
                MessageCodeUtil.generateResponseDataObject(
                    StoreMessageCode.USER_REPOSITORY_TASK_JSON_FIELD_IS_NULL,
                    arrayOf("packagePath")
                )
            } else {
                val atomEnvRequest = getAtomConfResult.atomEnvRequest!!
                atomEnvRequest.pkgPath = "$projectCode/$atomCode/$version/$packagePath"
                Result(getAtomConfResult)
            }
        }
    }

    override fun updateAtomEnv(userId: String, atomId: String, atomEnvRequest: AtomEnvRequest): Result<Boolean> {
        logger.info("updateAtomEnv userId is :$userId,atomId is :$atomId,atomEnvRequest is :$atomEnvRequest")
        marketAtomEnvInfoDao.updateMarketAtomEnvInfo(dslContext, atomId, atomEnvRequest)
        return Result(true)
    }
}
