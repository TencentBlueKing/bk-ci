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
import com.tencent.devops.common.api.constant.CommonMessageCode
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
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.pojo.atom.AtomEnvRequest
import com.tencent.devops.store.pojo.atom.MarketAtomCreateRequest
import com.tencent.devops.store.pojo.atom.MarketAtomUpdateRequest
import com.tencent.devops.store.pojo.atom.UpdateAtomInfo
import com.tencent.devops.store.pojo.atom.enums.AtomStatusEnum
import com.tencent.devops.store.pojo.common.ReleaseProcessItem
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.atom.EeMarketAtomService
import com.tencent.devops.store.service.atom.MarketAtomArchiveService
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.util.StringUtils

@Service
class EeMarketAtomServiceImpl : EeMarketAtomService, MarketAtomServiceImpl() {

    @Autowired
    lateinit var marketAtomArchiveService: MarketAtomArchiveService

    private val logger = LoggerFactory.getLogger(EeMarketAtomServiceImpl::class.java)

    override fun addMarketAtom(userId: String, marketAtomCreateRequest: MarketAtomCreateRequest): Result<Boolean> {
        logger.info("the addMarketAtom userId is :$userId,marketAtomCreateRequest is :$marketAtomCreateRequest")
        val atomCode = marketAtomCreateRequest.atomCode
        val validateResult = validateAddMarketAtomReq(userId, marketAtomCreateRequest)
        logger.info("the validateResult is :$validateResult")
        if (validateResult.isNotOk()) {
            return validateResult
        }
        dslContext.transaction { t ->
            val context = DSL.using(t)
            val id = UUIDUtil.generate()
            // 添加插件基本信息
            marketAtomDao.addMarketAtom(
                context,
                userId,
                id,
                "",
                "",
                atomDetailBaseUrl + atomCode,
                marketAtomCreateRequest
            )
            // 添加插件与项目关联关系，type为0代表新增插件时关联的项目
            storeProjectRelDao.addStoreProjectRel(
                context,
                userId,
                atomCode,
                marketAtomCreateRequest.projectCode,
                0,
                StoreTypeEnum.ATOM.type.toByte()
            )
            val atomEnvRequest = AtomEnvRequest(userId, "", "", marketAtomCreateRequest.language, null, "", null, null)
            // 添加流水线插件执行环境信息
            marketAtomEnvInfoDao.addMarketAtomEnvInfo(context, id, atomEnvRequest)
            // 默认给新建插件的人赋予管理员权限
            storeMemberDao.addStoreMember(context, userId, atomCode, userId, 0, StoreTypeEnum.ATOM.type.toByte())
        }
        return Result(true)
    }

    @Suppress("UNCHECKED_CAST")
    override fun updateMarketAtom(
        userId: String,
        projectCode: String,
        marketAtomUpdateRequest: MarketAtomUpdateRequest
    ): Result<String?> {
        logger.info("the updateMarketAtom userId is :$userId,projectCode is :$projectCode,marketAtomUpdateRequest is :$marketAtomUpdateRequest")
        val atomCode = marketAtomUpdateRequest.atomCode
        val version = marketAtomUpdateRequest.version
        // 校验可执行包sha摘要内容是否有效
        val packageShaContent = redisOperation.get("$projectCode:$atomCode:$version:packageShaContent")
        if (marketAtomUpdateRequest.packageShaContent != packageShaContent) {
            return MessageCodeUtil.generateResponseDataObject(StoreMessageCode.USER_UPLOAD_PACKAGE_INVALID)
        }
        val taskJsonStr = marketAtomArchiveService.getTaskJsonStr(projectCode, atomCode, version)
        val handleUpdateResult =
            handleUpdateMarketAtom(projectCode, userId, taskJsonStr, AtomStatusEnum.TESTING, marketAtomUpdateRequest)
        logger.info("the handleUpdateResult is :$handleUpdateResult")
        return handleUpdateResult
    }

    override fun handleProcessInfo(status: Int): List<ReleaseProcessItem> {
        val processInfo = initProcessInfo()
        when (status) {
            AtomStatusEnum.INIT.status, AtomStatusEnum.COMMITTING.status -> {
                setProcessInfo(processInfo, NUM_FOUR, NUM_TWO, DOING)
            }
            AtomStatusEnum.TESTING.status -> {
                setProcessInfo(processInfo, NUM_FOUR, NUM_THREE, DOING)
            }
            AtomStatusEnum.RELEASED.status -> {
                setProcessInfo(processInfo, NUM_FOUR, NUM_FOUR, SUCCESS)
            }
        }
        return processInfo
    }

    /**
     * 初始化插件版本进度
     */
    private fun initProcessInfo(): List<ReleaseProcessItem> {
        val processInfo = mutableListOf<ReleaseProcessItem>()
        processInfo.add(ReleaseProcessItem(MessageCodeUtil.getCodeLanMessage(BEGIN), NUM_ONE, SUCCESS))
        processInfo.add(ReleaseProcessItem(MessageCodeUtil.getCodeLanMessage(COMMIT), NUM_TWO, UNDO))
        processInfo.add(ReleaseProcessItem(MessageCodeUtil.getCodeLanMessage(TEST), NUM_THREE, UNDO))
        processInfo.add(ReleaseProcessItem(MessageCodeUtil.getCodeLanMessage(END), NUM_FOUR, UNDO))
        return processInfo
    }

    override fun getPassTestStatus(): Byte {
        return AtomStatusEnum.RELEASED.status.toByte()
    }

    override fun handlePassTest(userId: String, atomId: String): Result<Boolean> {
        val atomRecord = marketAtomDao.getAtomById(dslContext, atomId)
            ?: return MessageCodeUtil.generateResponseDataObject(
                CommonMessageCode.PARAMETER_IS_INVALID,
                arrayOf(atomId)
            )
        val atomStatus = AtomStatusEnum.RELEASED.status.toByte()
        dslContext.transaction { t ->
            val context = DSL.using(t)
            // 清空旧版本LATEST_FLAG
            marketAtomDao.cleanLatestFlag(context, atomRecord["atomCode"] as String)
            marketAtomDao.updateAtomInfo(
                context,
                userId,
                atomId,
                UpdateAtomInfo(atomStatus = atomStatus, latestFlag = true)
            )
        }
        return Result(true)
    }

    override fun generateAtomVisibleData(
        storeCodeList: List<String?>,
        storeType: StoreTypeEnum
    ): Result<HashMap<String, MutableList<Int>>?> {
        return Result(data = null) // 开源版插件不设置可见范围
    }

    override fun generateInstallFlag(
        defaultFlag: Boolean,
        members: MutableList<String>?,
        userId: String,
        visibleList: MutableList<Int>?,
        userDeptList: List<Int>
    ): Boolean {
        return true // 开源版插件默认所有用户都有权限安装
    }

    override fun validatePackagePath(packagePath: String?): Boolean {
        if (StringUtils.isEmpty(packagePath)) {
            // 执行文件为空则校验失败
            return false
        }
        return true
    }
}
