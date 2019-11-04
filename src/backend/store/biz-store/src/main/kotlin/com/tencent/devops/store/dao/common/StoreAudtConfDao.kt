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
package com.tencent.devops.store.dao.common

import com.tencent.devops.model.store.tables.TAtom
import com.tencent.devops.model.store.tables.TImage
import com.tencent.devops.model.store.tables.TStoreDeptRel
import com.tencent.devops.model.store.tables.TTemplate
import com.tencent.devops.model.store.tables.records.TStoreDeptRelRecord
import com.tencent.devops.store.pojo.common.StoreApproveRequest
import com.tencent.devops.store.pojo.common.enums.ApproveStatusEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class StoreAudtConfDao {
    /**
     * 获取对应ID的审核记录条数，判断这条审核记录是否存在
     * @param id 审核记录ID
     */
    fun countDeptRel(dslContext: DSLContext, id: String): Int {
        with(TStoreDeptRel.T_STORE_DEPT_REL) {
            return dslContext.selectCount()
                    .from(this)
                    .where(ID.eq(id))
                    .fetchOne(0, Int::class.java)
        }
    }

    /**
     * 审核可见范围，根据ID修改对应记录的状态和驳回信息
     * @param userId 审核人ID
     * @param id 审核记录的ID
     * @param storeApproveRequest 审核信息
     */
    fun approveVisibleDept(dslContext: DSLContext, userId: String, id: String, storeApproveRequest: StoreApproveRequest): Int {
        val status = when (storeApproveRequest.approveStatus) {
            ApproveStatusEnum.WAIT -> 0
            ApproveStatusEnum.PASS -> 1
            ApproveStatusEnum.REFUSE -> 2
        }
        with(TStoreDeptRel.T_STORE_DEPT_REL) {
            return dslContext.update(this)
                    .set(MODIFIER, userId)
                    .set(UPDATE_TIME, LocalDateTime.now())
                    .set(STATUS, status.toByte())
                    .set(COMMENT, storeApproveRequest.approveMsg)
                    .where(ID.eq(id))
                    .execute()
        }
    }

    /**
     * 根据给定条件获取相应的组件审核记录、组件名称
     * @param storeName 组件名称
     * @param storeType 组件类型
     * @param status 组件可见范围的审核状态
     */
    fun getDeptRel(dslContext: DSLContext, storeName: String?, storeType: Byte?, status: Byte?): Result<TStoreDeptRelRecord>? {
        with(TStoreDeptRel.T_STORE_DEPT_REL) {
            val storeCode: MutableList<String?>?
            if (storeName.isNullOrBlank()) {
                storeCode = null
            } else {
                storeCode = getStoreCode(dslContext, storeName, storeType)
                if (storeCode.size == 0)
                    return null
            }
            val condition = getCondition(storeCode, storeType, status)
            return dslContext.selectFrom(this)
                    .where(condition)
                    .orderBy(STATUS.asc(), UPDATE_TIME.desc())
                    .fetch()
        }
    }

    /**
     * 根据组件名称获取相应的组件码，当组件类型不确定时同时去模板信息表和插件信息表
     * 中获取将获取到的信息存放到List中返回
     * @param storeName 组件名称
     * @param storeType 组件类型
     */
    private fun getStoreCode(dslContext: DSLContext, storeName: String?, storeType: Byte?): MutableList<String?> {
        val codeList = mutableListOf<String?>()
        if (storeType == null) {
            codeList.add(getAtomCode(dslContext, storeName))
            codeList.add(getTemplateCode(dslContext, storeName))
            codeList.add(getImageCode(dslContext, storeName))
        } else if (storeType == StoreTypeEnum.ATOM.type.toByte()) {
            codeList.add(getAtomCode(dslContext, storeName))
        } else if (storeType == StoreTypeEnum.TEMPLATE.type.toByte()) {
            codeList.add(getTemplateCode(dslContext, storeName))
        } else {
            codeList.add(getImageCode(dslContext, storeName))
        }
        return codeList
    }

    /**
     * 根据组件名称从模板信息表中查询相应的组件码
     */
    private fun getImageCode(dslContext: DSLContext, storeName: String?): String? {
        with(TImage.T_IMAGE) {
            return dslContext.select(IMAGE_CODE)
                    .from(this)
                    .where(IMAGE_NAME.like(storeName))
                    .firstOrNull()
                    ?.value1()
        }
    }

    /**
     * 根据组件名称从模板信息表中查询相应的组件码
     */
    private fun getTemplateCode(dslContext: DSLContext, storeName: String?): String? {
        with(TTemplate.T_TEMPLATE) {
            return dslContext.select(TEMPLATE_CODE)
                    .from(this)
                    .where(TEMPLATE_NAME.like(storeName))
                    .firstOrNull()
                    ?.value1()
        }
    }

    /**
     * 根据组件名称从插件信息表中查询相应的组件码
     */
    private fun getAtomCode(dslContext: DSLContext, storeName: String?): String? {
        with(TAtom.T_ATOM) {
            return dslContext.select(ATOM_CODE)
                    .from(this)
                    .where(NAME.like(storeName))
                    .firstOrNull()
                    ?.value1()
        }
    }

    /**
     * 删除相应的可见范围审核信息
     */
    fun deleteDeptRel(dslContext: DSLContext, id: String): Int {
        with(TStoreDeptRel.T_STORE_DEPT_REL) {
            return dslContext.deleteFrom(this)
                    .where(ID.eq(id))
                    .execute()
        }
    }

    /**
     * 从插件信息表获取组件名称
     */
    fun getAtomName(dslContext: DSLContext, atomCode: String): String? {
        with(TAtom.T_ATOM) {
            return dslContext.select(NAME)
                    .from(this)
                    .where(ATOM_CODE.eq(atomCode))
                    .orderBy(UPDATE_TIME.desc())
                    .firstOrNull()
                    ?.value1()
        }
    }

    /**
     * 从模板信息表获取组件名称
     */
    fun getTemplateName(dslContext: DSLContext, templateCode: String): String? {
        with(TTemplate.T_TEMPLATE) {
            return dslContext.select(TEMPLATE_NAME)
                    .from(this)
                    .where(TEMPLATE_CODE.eq(templateCode))
                    .orderBy(UPDATE_TIME.desc())
                    .firstOrNull()
                    ?.value1()
        }
    }

    /**
     * 从模板信息表获取组件名称
     */
    fun getImageName(dslContext: DSLContext, imageCode: String): String? {
        with(TImage.T_IMAGE) {
            return dslContext.select(IMAGE_NAME)
                    .from(this)
                    .where(IMAGE_CODE.eq(imageCode))
                    .orderBy(UPDATE_TIME.desc())
                    .firstOrNull()
                    ?.value1()
        }
    }

    private fun TStoreDeptRel.getCondition(storeCode: MutableList<String?>?, storeType: Byte?, status: Byte?): MutableList<Condition> {
        val condition = mutableListOf<Condition>()
        if (storeType != null)
            condition.add(STORE_TYPE.eq(storeType))
        if (status != null)
            condition.add(STATUS.eq(status))
        if (storeCode != null)
            condition.add(STORE_CODE.`in`(storeCode))
        return condition
    }
}