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

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.store.dao.atom.AtomDao
import com.tencent.devops.store.dao.atom.MarketAtomEnvInfoDao
import com.tencent.devops.store.dao.common.StoreProjectRelDao
import com.tencent.devops.store.pojo.atom.AtomEnv
import com.tencent.devops.store.pojo.atom.AtomEnvRequest
import com.tencent.devops.store.pojo.atom.enums.AtomStatusEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.atom.AtomService
import com.tencent.devops.store.service.atom.MarketAtomEnvService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

/**
 * 插件执行环境逻辑类
 *
 * since: 2019-01-04
 */
@Service
class MarketAtomEnvServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val marketAtomEnvInfoDao: MarketAtomEnvInfoDao,
    private val storeProjectRelDao: StoreProjectRelDao,
    private val atomDao: AtomDao,
    private val atomService: AtomService
) : MarketAtomEnvService {
    private val logger = LoggerFactory.getLogger(MarketAtomEnvServiceImpl::class.java)

    /**
     * 根据插件代码和版本号查看插件执行环境信息
     */
    override fun getMarketAtomEnvInfo(projectCode: String, atomCode: String, version: String): Result<AtomEnv?> {
        logger.info("the atomCode is :$atomCode,version is :$version")
        val atomResult = atomService.getPipelineAtom(projectCode, atomCode, version) // 判断插件查看的权限
        if (atomResult.isNotOk()) {
            return Result(atomResult.status, atomResult.message ?: "")
        }
        val initProjectCode =
            storeProjectRelDao.getInitProjectCodeByStoreCode(dslContext, atomCode, StoreTypeEnum.ATOM.type.toByte())
        logger.info("the initProjectCode is :$initProjectCode")
        var atomStatusList: List<Byte>? = null
        if (version.contains("*")) {
            atomStatusList = if (projectCode == initProjectCode) {
                // 原生项目有权查处于测试中、审核中、已发布、下架中和已下架（需要兼容那些还在使用已下架插件插件的项目）的插件
                listOf(
                    AtomStatusEnum.TESTING.status.toByte(),
                    AtomStatusEnum.AUDITING.status.toByte(),
                    AtomStatusEnum.RELEASED.status.toByte(),
                    AtomStatusEnum.UNDERCARRIAGING.status.toByte(),
                    AtomStatusEnum.UNDERCARRIAGED.status.toByte()
                )
            } else {
                // 普通项目的查已发布、下架中和已下架（需要兼容那些还在使用已下架插件插件的项目）的插件
                listOf(
                    AtomStatusEnum.RELEASED.status.toByte(),
                    AtomStatusEnum.UNDERCARRIAGING.status.toByte(),
                    AtomStatusEnum.UNDERCARRIAGED.status.toByte()
                )
            }
        }
        val atomEnvInfoRecord = marketAtomEnvInfoDao.getProjectMarketAtomEnvInfo(
            dslContext,
            projectCode,
            atomCode,
            version.replace("*", ""),
            atomStatusList
        )
        logger.info("the atomEnvInfoRecord is :$atomEnvInfoRecord")
        return Result(
            if (atomEnvInfoRecord == null) {
                null
            } else {
                val createTime = atomEnvInfoRecord["createTime"] as LocalDateTime
                val updateTime = atomEnvInfoRecord["updateTime"] as LocalDateTime
                AtomEnv(
                    atomId = atomEnvInfoRecord["atomId"] as String,
                    atomCode = atomEnvInfoRecord["atomCode"] as String,
                    atomName = atomEnvInfoRecord["atomName"] as String,
                    atomStatus = AtomStatusEnum.getAtomStatus((atomEnvInfoRecord["atomStatus"] as Byte).toInt()),
                    creator = atomEnvInfoRecord["creator"] as String,
                    version = atomEnvInfoRecord["version"] as String,
                    summary = atomEnvInfoRecord["summary"] as? String,
                    docsLink = atomEnvInfoRecord["docsLink"] as? String,
                    props = atomEnvInfoRecord["props"] as? String,
                    createTime = createTime.timestampmilli(),
                    updateTime = updateTime.timestampmilli(),
                    projectCode = initProjectCode,
                    pkgPath = atomEnvInfoRecord["pkgPath"] as String,
                    language = atomEnvInfoRecord["language"] as? String,
                    minVersion = atomEnvInfoRecord["minVersion"] as? String,
                    target = atomEnvInfoRecord["target"] as String,
                    shaContent = atomEnvInfoRecord["shaContent"] as? String,
                    preCmd = atomEnvInfoRecord["preCmd"] as? String
                )
            }
        )
    }

    /**
     * 更新插件执行环境信息
     */
    override fun updateMarketAtomEnvInfo(
        projectCode: String,
        atomCode: String,
        version: String,
        atomEnvRequest: AtomEnvRequest
    ): Result<Boolean> {
        logger.info("the atomCode is :$atomCode,version is :$version,atomEnvRequest is :$atomEnvRequest")
        val atomResult = atomService.getPipelineAtom(projectCode, atomCode, version) // 判断插件查看的权限
        val status = atomResult.status
        if (0 != status) {
            return Result(atomResult.status, atomResult.message ?: "", false)
        }
        val atomRecord = atomDao.getPipelineAtom(dslContext, atomCode, version.replace("*", ""))
        logger.info("the atomRecord is :$atomRecord")
        return if (null != atomRecord) {
            marketAtomEnvInfoDao.updateMarketAtomEnvInfo(dslContext, atomRecord.id, atomEnvRequest)
            Result(true)
        } else {
            MessageCodeUtil.generateResponseDataObject(
                CommonMessageCode.PARAMETER_IS_INVALID,
                arrayOf("$atomCode+$version"),
                false
            )
        }
    }
}
