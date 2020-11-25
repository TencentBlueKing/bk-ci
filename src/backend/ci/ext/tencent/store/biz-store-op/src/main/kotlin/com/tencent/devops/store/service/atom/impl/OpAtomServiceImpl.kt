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
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.model.store.tables.records.TAtomRecord
import com.tencent.devops.model.store.tables.records.TClassifyRecord
import com.tencent.devops.repository.pojo.enums.VisibilityLevelEnum
import com.tencent.devops.store.dao.atom.AtomDao
import com.tencent.devops.store.dao.atom.MarketAtomDao
import com.tencent.devops.store.dao.atom.MarketAtomFeatureDao
import com.tencent.devops.store.dao.common.ClassifyDao
import com.tencent.devops.store.dao.common.StoreReleaseDao
import com.tencent.devops.store.pojo.atom.ApproveReq
import com.tencent.devops.store.pojo.atom.Atom
import com.tencent.devops.store.pojo.atom.AtomResp
import com.tencent.devops.store.pojo.atom.enums.AtomCategoryEnum
import com.tencent.devops.store.pojo.atom.enums.AtomStatusEnum
import com.tencent.devops.store.pojo.atom.enums.AtomTypeEnum
import com.tencent.devops.store.pojo.atom.enums.OpSortTypeEnum
import com.tencent.devops.store.pojo.common.PASS
import com.tencent.devops.store.pojo.common.REJECT
import com.tencent.devops.store.pojo.common.StoreReleaseCreateRequest
import com.tencent.devops.store.pojo.common.enums.AuditTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.atom.OpAtomService
import com.tencent.devops.store.service.atom.AtomNotifyService
import com.tencent.devops.store.service.atom.AtomQualityService
import com.tencent.devops.store.service.websocket.StoreWebsocketService
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.util.StringUtils
import java.time.LocalDateTime

/**
 * 插件OP业务逻辑类
 *
 * since: 2019-10-29
 */
@Service
class OpAtomServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val classifyDao: ClassifyDao,
    private val atomDao: AtomDao,
    private val marketAtomDao: MarketAtomDao,
    private val atomFeatureDao: MarketAtomFeatureDao,
    private val storeReleaseDao: StoreReleaseDao,
    private val atomQualityService: AtomQualityService,
    private val atomNotifyService: AtomNotifyService,
    private val storeWebsocketService: StoreWebsocketService
) : OpAtomService {

    private val logger = LoggerFactory.getLogger(OpAtomServiceImpl::class.java)

    /**
     * op系统获取插件信息
     */
    override fun getOpPipelineAtoms(
        atomName: String?,
        atomType: AtomTypeEnum?,
        serviceScope: String?,
        os: String?,
        category: String?,
        classifyId: String?,
        atomStatus: AtomStatusEnum?,
        sortType: OpSortTypeEnum?,
        desc: Boolean?,
        page: Int?,
        pageSize: Int?
    ): Result<AtomResp<Atom>?> {
        logger.info("getOpPipelineAtoms atomName is :$atomName,serviceScope is :$serviceScope,os is :$os,atomType is :$atomType")
        logger.info("getOpPipelineAtoms category is :$category,classifyId is :$classifyId,page is :$page,pageSize is :$pageSize")
        val pipelineAtomList = atomDao.getOpPipelineAtoms(
            dslContext = dslContext,
            atomName = atomName,
            atomType = atomType,
            serviceScope = serviceScope,
            os = os,
            category = category,
            classifyId = classifyId,
            atomStatus = atomStatus,
            sortType = sortType?.sortType,
            desc = desc,
            page = page,
            pageSize = pageSize
        ).map {
            generatePipelineAtom(it)
        }
        // 处理分页逻辑
        val totalSize = atomDao.getOpPipelineAtomCount(dslContext, atomName, atomType, serviceScope, os, category, classifyId, atomStatus)
        val totalPage = PageUtil.calTotalPage(pageSize, totalSize)
        return Result(AtomResp(totalSize, page, pageSize, totalPage, pipelineAtomList))
    }

    /**
     * 根据id获取插件信息
     */
    override fun getPipelineAtom(id: String): Result<Atom?> {
        logger.info("the id is :{}", id)
        val pipelineAtomRecord = atomDao.getPipelineAtom(dslContext, id)
        logger.info("the pipelineAtomRecord is :{}", pipelineAtomRecord)
        return Result(if (pipelineAtomRecord == null) {
            null
        } else {
            generatePipelineAtom(pipelineAtomRecord)
        })
    }

    /**
     * 根据插件代码和版本号获取插件信息
     */
    override fun getPipelineAtom(atomCode: String, version: String): Result<Atom?> {
        logger.info("the atomCode is: $atomCode,version is:$version")
        val pipelineAtomRecord = atomDao.getPipelineAtom(dslContext, atomCode, version.replace("*", ""))
        logger.info("the pipelineAtomRecord is :$pipelineAtomRecord")
        return Result(
            if (pipelineAtomRecord == null) {
                null
            } else {
                generatePipelineAtom(pipelineAtomRecord)
            }
        )
    }

    /**
     * 生成插件对象
     */
    private fun generatePipelineAtom(it: TAtomRecord): Atom {
        val atomClassifyRecord = classifyDao.getClassify(dslContext, it.classifyId)
        return convert(it, atomClassifyRecord)
    }

    @Suppress("UNCHECKED_CAST")
    private fun convert(atomRecord: TAtomRecord, atomClassifyRecord: TClassifyRecord?): Atom {
        val atomFeature = atomFeatureDao.getAtomFeature(dslContext, atomRecord.atomCode)
        return Atom(
            id = atomRecord.id,
            name = atomRecord.name,
            atomCode = atomRecord.atomCode,
            classType = atomRecord.classType,
            logoUrl = atomRecord.logoUrl,
            icon = atomRecord.icon,
            summary = atomRecord.summary,
            serviceScope = if (!StringUtils.isEmpty(atomRecord.serviceScope)) JsonUtil.getObjectMapper().readValue(atomRecord.serviceScope, List::class.java) as List<String> else null,
            jobType = atomRecord.jobType,
            os = if (!StringUtils.isEmpty(atomRecord.os)) JsonUtil.getObjectMapper().readValue(atomRecord.os, List::class.java) as List<String> else null,
            classifyId = atomClassifyRecord?.id,
            classifyCode = atomClassifyRecord?.classifyCode,
            classifyName = atomClassifyRecord?.classifyName,
            docsLink = atomRecord.docsLink,
            category = AtomCategoryEnum.getAtomCategory(atomRecord.categroy.toInt()),
            atomType = AtomTypeEnum.getAtomType(atomRecord.atomType.toInt()),
            atomStatus = AtomStatusEnum.getAtomStatus(atomRecord.atomStatus.toInt()),
            description = atomRecord.description,
            version = atomRecord.version,
            creator = atomRecord.creator,
            createTime = DateTimeUtil.toDateTime(atomRecord.createTime),
            modifier = atomRecord.modifier,
            updateTime = DateTimeUtil.toDateTime(atomRecord.updateTime),
            defaultFlag = atomRecord.defaultFlag,
            latestFlag = atomRecord.latestFlag,
            htmlTemplateVersion = atomRecord.htmlTemplateVersion,
            buildLessRunFlag = atomRecord.buildLessRunFlag,
            weight = atomRecord.weight,
            props = atomDao.convertString(atomRecord.props),
            data = atomDao.convertString(atomRecord.data),
            recommendFlag = atomFeature?.recommendFlag,
            yamlFlag = atomFeature?.yamlFlag,
            publisher = atomRecord.publisher,
            visibilityLevel = VisibilityLevelEnum.getVisibilityLevel(atomRecord.visibilityLevel as Int),
            privateReason = atomRecord.privateReason
        )
    }

    /**
     * 审核插件
     */
    override fun approveAtom(userId: String, atomId: String, approveReq: ApproveReq): Result<Boolean> {
        // 判断插件是否存在
        val atom = marketAtomDao.getAtomRecordById(dslContext, atomId)
            ?: return MessageCodeUtil.generateResponseDataObject(
                CommonMessageCode.PARAMETER_IS_INVALID,
                arrayOf(atomId)
            )

        val oldStatus = atom.atomStatus
        if (oldStatus != AtomStatusEnum.AUDITING.status.toByte()) {
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PARAMETER_IS_INVALID, arrayOf(atomId))
        }

        if (approveReq.result != PASS && approveReq.result != REJECT) {
            return MessageCodeUtil.generateResponseDataObject(
                CommonMessageCode.PARAMETER_IS_INVALID,
                arrayOf(approveReq.result)
            )
        }
        val creator = atom.creator
        val atomCode = atom.atomCode
        val atomStatus =
            if (approveReq.result == PASS) {
                AtomStatusEnum.RELEASED.status.toByte()
            } else {
                AtomStatusEnum.AUDIT_REJECT.status.toByte()
            }
        val type = if (approveReq.result == PASS) AuditTypeEnum.AUDIT_SUCCESS else AuditTypeEnum.AUDIT_REJECT

        dslContext.transaction { t ->
            val context = DSL.using(t)
            val latestFlag = approveReq.result == PASS
            var pubTime: LocalDateTime? = null
            if (latestFlag) {
                pubTime = LocalDateTime.now()
                // 清空旧版本LATEST_FLAG
                marketAtomDao.cleanLatestFlag(context, approveReq.atomCode)
                // 记录发布信息
                storeReleaseDao.addStoreReleaseInfo(
                    dslContext = context,
                    userId = userId,
                    storeReleaseCreateRequest = StoreReleaseCreateRequest(
                        storeCode = atomCode,
                        storeType = StoreTypeEnum.ATOM,
                        latestUpgrader = creator,
                        latestUpgradeTime = pubTime
                    )
                )
            }

            // 入库信息，并设置当前版本的LATEST_FLAG
            marketAtomDao.approveAtomFromOp(context, userId, atomId, atomStatus, approveReq, latestFlag, pubTime)

            // 更新质量红线信息
            atomQualityService.updateQualityInApprove(approveReq.atomCode, atomStatus)
        }
        // 通过websocket推送状态变更消息,推送所有有该插件权限的用户
        storeWebsocketService.sendWebsocketMessageByAtomCodeAndAtomId(atomCode, atomId)
        // 发送通知消息
        atomNotifyService.sendAtomReleaseAuditNotifyMessage(atomId, type)
        return Result(true)
    }
}
