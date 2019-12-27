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

import com.tencent.devops.common.api.constant.BEGIN
import com.tencent.devops.common.api.constant.COMMIT
import com.tencent.devops.common.api.constant.DOING
import com.tencent.devops.common.api.constant.END
import com.tencent.devops.common.api.constant.NUM_FOUR
import com.tencent.devops.common.api.constant.NUM_ONE
import com.tencent.devops.common.api.constant.NUM_THREE
import com.tencent.devops.common.api.constant.NUM_TWO
import com.tencent.devops.common.api.constant.SUCCESS
import com.tencent.devops.common.api.constant.TEST
import com.tencent.devops.common.api.constant.UNDO
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.model.store.tables.records.TAtomRecord
import com.tencent.devops.store.pojo.atom.MarketAtomCreateRequest
import com.tencent.devops.store.pojo.atom.MarketAtomUpdateRequest
import com.tencent.devops.store.pojo.atom.enums.AtomPackageSourceTypeEnum
import com.tencent.devops.store.pojo.atom.enums.AtomStatusEnum
import com.tencent.devops.store.pojo.common.ReleaseProcessItem
import com.tencent.devops.store.service.atom.BkAtomReleaseService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class BkAtomReleaseServiceImpl : BkAtomReleaseService, AtomReleaseServiceImpl() {

    private val logger = LoggerFactory.getLogger(BkAtomReleaseServiceImpl::class.java)

    override fun handleAtomPackage(
        marketAtomCreateRequest: MarketAtomCreateRequest,
        userId: String,
        atomCode: String
    ): Result<Map<String, String>?> {
        return Result(data = null)
    }

    override fun getAtomPackageSourceType(atomCode: String): AtomPackageSourceTypeEnum {
        return AtomPackageSourceTypeEnum.UPLOAD
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
        val fileStr = marketAtomArchiveService.getFileStr(projectCode, atomCode, atomVersion, fileName)
        logger.info("getFileStr fileStr is:$fileStr")
        return fileStr
    }

    override fun asyncHandleUpdateAtom(context: DSLContext, atomId: String, userId: String) {
    }

    override fun validateUpdateMarketAtomReq(
        userId: String,
        marketAtomUpdateRequest: MarketAtomUpdateRequest,
        atomRecord: TAtomRecord
    ): Result<Boolean> {
        // 企业版升级插件暂无特殊参数需要校验
        return Result(true)
    }

    override fun handleProcessInfo(isNormalUpgrade: Boolean, status: Int): List<ReleaseProcessItem> {
        val processInfo = initProcessInfo()
        val totalStep = NUM_FOUR
        when (status) {
            AtomStatusEnum.INIT.status, AtomStatusEnum.COMMITTING.status -> {
                storeCommonService.setProcessInfo(processInfo, totalStep, NUM_TWO, DOING)
            }
            AtomStatusEnum.TESTING.status -> {
                storeCommonService.setProcessInfo(processInfo, totalStep, NUM_THREE, DOING)
            }
            AtomStatusEnum.RELEASED.status -> {
                storeCommonService.setProcessInfo(processInfo, totalStep, NUM_FOUR, SUCCESS)
            }
        }
        return processInfo
    }

    override fun getPassTestStatus(isNormalUpgrade: Boolean): Byte {
        return AtomStatusEnum.RELEASED.status.toByte()
    }

    /**
     * 初始化插件版本进度
     */
    private fun initProcessInfo(): List<ReleaseProcessItem> {
        val processInfo = mutableListOf<ReleaseProcessItem>()
        processInfo.add(ReleaseProcessItem(MessageCodeUtil.getCodeLanMessage(BEGIN), BEGIN, NUM_ONE, SUCCESS))
        processInfo.add(ReleaseProcessItem(MessageCodeUtil.getCodeLanMessage(COMMIT), COMMIT, NUM_TWO, UNDO))
        processInfo.add(ReleaseProcessItem(MessageCodeUtil.getCodeLanMessage(TEST), TEST, NUM_THREE, UNDO))
        processInfo.add(ReleaseProcessItem(MessageCodeUtil.getCodeLanMessage(END), END, NUM_FOUR, UNDO))
        return processInfo
    }
}
