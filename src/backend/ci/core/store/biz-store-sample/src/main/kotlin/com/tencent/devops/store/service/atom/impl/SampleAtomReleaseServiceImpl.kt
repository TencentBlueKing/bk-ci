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

import com.tencent.devops.artifactory.api.service.ServiceReplicaResource
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
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.model.store.tables.records.TAtomRecord
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.constant.StoreMessageCode.NO_COMPONENT_ADMIN_AND_CREATETOR_PERMISSION
import com.tencent.devops.store.pojo.atom.AtomReleaseRequest
import com.tencent.devops.store.pojo.atom.MarketAtomCreateRequest
import com.tencent.devops.store.pojo.atom.MarketAtomUpdateRequest
import com.tencent.devops.store.pojo.atom.enums.AtomStatusEnum
import com.tencent.devops.store.pojo.common.ReleaseProcessItem
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.atom.MarketAtomEnvService
import com.tencent.devops.store.service.atom.SampleAtomReleaseService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class SampleAtomReleaseServiceImpl : SampleAtomReleaseService, AtomReleaseServiceImpl() {

    @Autowired
    private lateinit var marketAtomEnvService: MarketAtomEnvService

    private val logger = LoggerFactory.getLogger(SampleAtomReleaseServiceImpl::class.java)

    override fun handleAtomPackage(
        marketAtomCreateRequest: MarketAtomCreateRequest,
        userId: String,
        atomCode: String
    ): Result<Map<String, String>?> {
        return Result(data = null)
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
        val fileStr = marketAtomArchiveService.getFileStr(projectCode, atomCode, atomVersion, fileName)
        logger.info("getFileStr fileStr is:$fileStr")
        return fileStr
    }

    override fun asyncHandleUpdateAtom(
        context: DSLContext,
        atomId: String,
        userId: String,
        branch: String?,
        validOsNameFlag: Boolean?,
        validOsArchFlag: Boolean?
    ) = Unit

    override fun validateUpdateMarketAtomReq(
        userId: String,
        marketAtomUpdateRequest: MarketAtomUpdateRequest,
        atomRecord: TAtomRecord
    ): Result<Boolean> {
        // 开源版升级插件暂无特殊参数需要校验
        return Result(true)
    }

    override fun handleProcessInfo(
        userId: String,
        atomId: String,
        isNormalUpgrade: Boolean,
        status: Int
    ): List<ReleaseProcessItem> {
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

    override fun doCancelReleaseBus(userId: String, atomId: String) = Unit

    override fun getPreValidatePassTestStatus(atomCode: String, atomId: String, atomStatus: Byte): Byte {
        return AtomStatusEnum.RELEASED.status.toByte()
    }

    override fun doAtomReleaseBus(userId: String, atomReleaseRequest: AtomReleaseRequest) {
        with(atomReleaseRequest) {
            val atomEnvInfo = marketAtomEnvInfoDao.getAtomEnvInfo(dslContext, atomId)!!
            client.get(ServiceReplicaResource::class).createReplicaTask(
                userId = userId,
                projectId = "bk-store",
                repoName = "plugin",
                fullPath = atomEnvInfo.pkgPath!!
            )
        }
    }

    /**
     * 初始化插件版本进度
     */
    private fun initProcessInfo(): List<ReleaseProcessItem> {
        val processInfo = mutableListOf<ReleaseProcessItem>()
        processInfo.add(ReleaseProcessItem(I18nUtil.getCodeLanMessage(BEGIN), BEGIN, NUM_ONE, SUCCESS))
        processInfo.add(ReleaseProcessItem(I18nUtil.getCodeLanMessage(COMMIT), COMMIT, NUM_TWO, UNDO))
        processInfo.add(ReleaseProcessItem(I18nUtil.getCodeLanMessage(TEST), TEST, NUM_THREE, UNDO))
        processInfo.add(ReleaseProcessItem(I18nUtil.getCodeLanMessage(END), END, NUM_FOUR, UNDO))
        return processInfo
    }

    /**
     * 检查版本发布过程中的操作权限
     */
    @Suppress("ALL")
    override fun checkAtomVersionOptRight(
        userId: String,
        atomId: String,
        status: Byte,
        isNormalUpgrade: Boolean?
    ): Triple<Boolean, String, Array<String>?> {
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
            return Triple(false, NO_COMPONENT_ADMIN_AND_CREATETOR_PERMISSION, arrayOf(atomCode))
        }

        logger.info("record status=$recordStatus, status=$status")
        var validateFlag = true
        if (status == AtomStatusEnum.COMMITTING.status.toByte() &&
            recordStatus != AtomStatusEnum.INIT.status.toByte()
        ) {
            validateFlag = false
        } else if (status == AtomStatusEnum.TESTING.status.toByte() &&
            recordStatus != AtomStatusEnum.COMMITTING.status.toByte()
        ) {
            validateFlag = false
        } else if (status == AtomStatusEnum.RELEASED.status.toByte() &&
            recordStatus != AtomStatusEnum.TESTING.status.toByte()
        ) {
            validateFlag = false
        } else if (status == AtomStatusEnum.GROUNDING_SUSPENSION.status.toByte() &&
            recordStatus == AtomStatusEnum.RELEASED.status.toByte()
        ) {
            validateFlag = false
        } else if (status == AtomStatusEnum.UNDERCARRIAGING.status.toByte() &&
            recordStatus != AtomStatusEnum.RELEASED.status.toByte()
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
}
