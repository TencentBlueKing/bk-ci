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

package com.tencent.devops.store.service.atom

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.model.store.tables.records.TStoreDeptRelRecord
import com.tencent.devops.store.dao.common.StoreAudtConfDao
import com.tencent.devops.store.pojo.common.StoreApproveRequest
import com.tencent.devops.store.pojo.common.VisibleAuditInfo
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.OpStoreAuditConfService
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class OpStoreAuditConfServiceImpl @Autowired constructor(
    private val storeAuditConfDao: StoreAudtConfDao,
    private val dslContext: DSLContext
) : OpStoreAuditConfService {
    /**
     * 查询给定条件的审核范围记录
     * @param storeName 审核组件名称
     * @param storeType 审核组件类型（0：插件 1：模板）
     * @param status 审核状态
     * @param page 分页页数
     * @param pageSize 分页每页记录条数
     */
    override fun getAllAuditConf(
        storeName: String?,
        storeType: Byte?,
        status: Byte?,
        page: Int?,
        pageSize: Int?
    ): Result<Page<VisibleAuditInfo>> {
        val res = storeAuditConfDao.getDeptRel(dslContext, storeName, storeType, status)
        val auditList = mutableListOf<VisibleAuditInfo>()
        res?.forEach {
            auditList.add(generateAuditConf(it))
        }
        val count = auditList.size.toLong()
        val totalPages = PageUtil.calTotalPage(pageSize, count)
        return Result(Page(count = count, page = page ?: 1, pageSize = pageSize ?: 10, totalPages = totalPages, records = auditList))
    }

    /**
     * 根据审核记录中记录的组件码获取相应组件的名称
     * @param storeDeptRel 审核记录对象
     */
    private fun generateAuditConf(storeDeptRel: TStoreDeptRelRecord): VisibleAuditInfo {
        val storeName: String? = when {
            storeDeptRel.storeType == StoreTypeEnum.ATOM.type.toByte() -> storeAuditConfDao.getAtomName(dslContext, storeDeptRel.storeCode)
            storeDeptRel.storeType == StoreTypeEnum.TEMPLATE.type.toByte() -> storeAuditConfDao.getTemplateName(dslContext, storeDeptRel.storeCode)
            storeDeptRel.storeType == StoreTypeEnum.IMAGE.type.toByte() -> storeAuditConfDao.getImageName(dslContext, storeDeptRel.storeCode)
            else -> "Unknown StoreName"
        }
        return VisibleAuditInfo(
                id = storeDeptRel.id,
                storeName = storeName ?: "",
                deptName = storeDeptRel.deptName,
                deptId = storeDeptRel.deptId,
                status = storeDeptRel.status,
                comment = storeDeptRel.comment,
                modifier = storeDeptRel.modifier,
                storeType = storeDeptRel.storeType,
                modifierTime = storeDeptRel.updateTime.timestampmilli()
        )
    }

    /**
     * 审核可见范围，根据记录ID修改相应的审核状态
     * @param userId 审核人ID
     * @param id 审核记录ID
     * @param storeApproveRequest 审核信息对象，半酣审核状态和驳回原因
     */
    override fun approveVisibleDept(userId: String, id: String, storeApproveRequest: StoreApproveRequest): Result<Boolean> {
        val isExists = storeAuditConfDao.countDeptRel(dslContext, id)
        if (isExists == 0)
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PARAMETER_IS_INVALID, arrayOf("记录ID"), false)

        storeAuditConfDao.approveVisibleDept(dslContext, userId, id, storeApproveRequest)
        return Result(true)
    }

    /**
     * 根据审核记录的ID删除一条审核记录
     * @param id 审核记录的ID
     */
    override fun deleteAuditConf(id: String): Result<Boolean> {
        storeAuditConfDao.deleteDeptRel(dslContext, id)
        return Result(true)
    }
}