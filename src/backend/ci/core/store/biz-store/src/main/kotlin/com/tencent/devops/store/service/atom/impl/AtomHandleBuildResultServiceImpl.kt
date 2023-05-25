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

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.store.dao.atom.MarketAtomDao
import com.tencent.devops.store.pojo.atom.enums.AtomStatusEnum
import com.tencent.devops.store.pojo.common.ATOM_POST_VERSION_TEST_FLAG_KEY_PREFIX
import com.tencent.devops.store.pojo.common.StoreBuildResultRequest
import com.tencent.devops.store.service.atom.AtomReleaseService
import com.tencent.devops.store.service.atom.MarketAtomService
import com.tencent.devops.store.service.common.AbstractStoreHandleBuildResultService
import com.tencent.devops.store.utils.VersionUtils
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Suppress("ALL")
@Service("ATOM_HANDLE_BUILD_RESULT")
class AtomHandleBuildResultServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val redisOperation: RedisOperation,
    private val marketAtomDao: MarketAtomDao,
    private val marketAtomService: MarketAtomService,
    private val atomReleaseService: AtomReleaseService
) : AbstractStoreHandleBuildResultService() {

    private val logger = LoggerFactory.getLogger(AtomHandleBuildResultServiceImpl::class.java)

    override fun handleStoreBuildResult(storeBuildResultRequest: StoreBuildResultRequest): Result<Boolean> {
        logger.info("handleStoreBuildResult storeBuildResultRequest is:$storeBuildResultRequest")
        val atomId = storeBuildResultRequest.storeId
        val atomRecord = marketAtomDao.getAtomRecordById(dslContext, atomId)
            ?: return I18nUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf(atomId),
                language = I18nUtil.getLanguage(storeBuildResultRequest.userId)
            )
        // 防止重复的mq消息造成的状态异常
        if (atomRecord.atomStatus != AtomStatusEnum.BUILDING.status.toByte()) {
            return Result(true)
        }
        var atomStatus = AtomStatusEnum.TESTING // 构建成功将插件状态置位测试状态
        if (BuildStatus.SUCCEED != storeBuildResultRequest.buildStatus) {
            atomStatus = AtomStatusEnum.BUILD_FAIL // 构建失败
        }
        val atomCode = atomRecord.atomCode
        val version = atomRecord.version
        marketAtomService.setAtomBuildStatusByAtomCode(
            atomCode = atomCode,
            version = version,
            userId = storeBuildResultRequest.userId,
            atomStatus = atomStatus,
            msg = null
        )
        if (atomStatus == AtomStatusEnum.TESTING) {
            // 插件errorCodes.json文件数据入库
            atomReleaseService.syncAtomErrorCodeConfig(
                atomCode = atomCode,
                atomVersion = version,
                userId = atomRecord.modifier,
                repositoryHashId = atomRecord.repositoryHashId,
                branch = atomRecord.branch
            )
            // 插件大版本内有测试版本则写入缓存
            redisOperation.hset(
                key = "$ATOM_POST_VERSION_TEST_FLAG_KEY_PREFIX:$atomCode",
                hashKey = VersionUtils.convertLatestVersion(version),
                values = "true"
            )
        }
        return Result(true)
    }
}
