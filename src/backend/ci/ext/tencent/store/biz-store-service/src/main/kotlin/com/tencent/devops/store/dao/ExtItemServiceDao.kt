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

package com.tencent.devops.store.dao

import com.tencent.devops.model.store.tables.TExtensionService
import com.tencent.devops.model.store.tables.TExtensionServiceFeature
import com.tencent.devops.model.store.tables.TExtensionServiceItemRel
import com.tencent.devops.model.store.tables.TStoreProjectRel
import com.tencent.devops.model.store.tables.records.TExtensionServiceItemRelRecord
import com.tencent.devops.store.pojo.common.enums.StoreProjectTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.enums.ExtServiceStatusEnum
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Record10
import org.jooq.Result
import org.jooq.SelectOnConditionStep
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class ExtItemServiceDao {

    fun updateItemService(dslContext: DSLContext, itemId: String, bkServiceId: String, userId: String) {
        with(TExtensionServiceItemRel.T_EXTENSION_SERVICE_ITEM_REL) {
            val baseStep = dslContext.update(this)
            baseStep.set(BK_SERVICE_ID, bkServiceId.toLong())
            baseStep.set(MODIFIER, userId).set(UPDATE_TIME, LocalDateTime.now())
                .where(ITEM_ID.eq(itemId))
                .execute()
        }
    }

    fun getItemByServiceId(
        dslContext: DSLContext,
        serviceIds: List<String>
    ): Result<TExtensionServiceItemRelRecord>? {
        with(TExtensionServiceItemRel.T_EXTENSION_SERVICE_ITEM_REL) {
            return dslContext.selectFrom(this).where(SERVICE_ID.`in`(serviceIds)).fetch()
        }
    }

    fun deleteByServiceId(dslContext: DSLContext, extServiceIds: List<String>) {
        with(TExtensionServiceItemRel.T_EXTENSION_SERVICE_ITEM_REL) {
            dslContext.deleteFrom(this)
                .where(SERVICE_ID.`in`(extServiceIds))
                .execute()
        }
    }

    fun getExtItemServiceList(
        dslContext: DSLContext,
        userId: String,
        itemId: String,
        projectCode: String?,
        page: Int?,
        pageSize: Int?
    ): Result<out Record>? {
        val tes = TExtensionService.T_EXTENSION_SERVICE.`as`("tes")
        val tesir = TExtensionServiceItemRel.T_EXTENSION_SERVICE_ITEM_REL.`as`("tesir")
        val tesf = TExtensionServiceFeature.T_EXTENSION_SERVICE_FEATURE.`as`("tesf")
        val tspr = TStoreProjectRel.T_STORE_PROJECT_REL.`as`("tspr")
        val baseStep = getExtItemServiceBaseStep(dslContext, tes, tesir, tesf)
        val conditions = setQueryExtItemServiceBaseCondition(tes, tesir, itemId)
        val t = if (!projectCode.isNullOrBlank()) {
            // 查询项目下的扩展服务（包含自已开发和研发商店安装）
            // 查询公共扩展服务
            val publicConditions =
                queryPublicExtItemServiceCondition(dslContext, conditions, tes, tesf, tspr, projectCode)

            // 查询普通的扩展服务
            val normalConditions =
                queryNormalExtItemServiceCondition(dslContext, conditions, tes, tesf, tspr, projectCode)

            // 查询初始化或者调试项目下状态为测试中或审核中的扩展服务
            val initTestConditions = queryTestExtItemServiceCondition(conditions, tes, tspr, projectCode)
            baseStep.where(publicConditions)
                .union(
                    getExtItemServiceBaseStep(dslContext, tes, tesir, tesf)
                        .leftJoin(tspr).on(tes.SERVICE_CODE.eq(tspr.STORE_CODE))
                        .where(normalConditions)
                )
                .union(
                    getExtItemServiceBaseStep(dslContext, tes, tesir, tesf)
                        .leftJoin(tspr).on(tes.SERVICE_CODE.eq(tspr.STORE_CODE))
                        .where(initTestConditions)
                ).asTable("t")
        } else {
            // 只查已发布的扩展服务
            conditions.add(tes.SERVICE_STATUS.eq(ExtServiceStatusEnum.RELEASED.status.toByte()))
            baseStep.where(conditions).asTable("t")
        }
        val sql = dslContext.select().from(t).orderBy(t.field("weight")!!.desc(), t.field("serviceName")!!.asc())
        return if (null != page && null != pageSize) {
            sql.limit((page - 1) * pageSize, pageSize).fetch()
        } else {
            sql.fetch()
        }
    }

    private fun queryTestExtItemServiceCondition(
        conditions: MutableList<Condition>,
        tes: TExtensionService,
        tspr: TStoreProjectRel,
        projectCode: String?
    ): MutableList<Condition> {
        val initTestConditions = mutableListOf<Condition>()
        initTestConditions.addAll(conditions)
        // 只查测试中和审核中的扩展服务
        initTestConditions.add(
            tes.SERVICE_STATUS.`in`(
                listOf(
                    ExtServiceStatusEnum.TESTING.status.toByte(),
                    ExtServiceStatusEnum.AUDITING.status.toByte()
                )
            )
        )
        initTestConditions.add(tspr.PROJECT_CODE.eq(projectCode))
        // 微扩展调试项目
        initTestConditions.add(tspr.TYPE.eq(StoreProjectTypeEnum.TEST.type.toByte()))
        return initTestConditions
    }

    private fun queryNormalExtItemServiceCondition(
        dslContext: DSLContext,
        conditions: MutableList<Condition>,
        tes: TExtensionService,
        tesf: TExtensionServiceFeature,
        tspr: TStoreProjectRel,
        projectCode: String?
    ): MutableList<Condition> {
        val normalConditions = mutableListOf<Condition>()
        normalConditions.addAll(conditions)
        normalConditions.add(tes.SERVICE_STATUS.eq(ExtServiceStatusEnum.RELEASED.status.toByte()))
        normalConditions.add(tes.LATEST_FLAG.eq(true)) // 只查最新已上架的扩展服务
        normalConditions.add(tesf.PUBLIC_FLAG.eq(false))
        normalConditions.add(tspr.PROJECT_CODE.eq(projectCode))
        normalConditions.add(tspr.STORE_TYPE.eq(StoreTypeEnum.SERVICE.type.toByte()))
        val initTestConditions = queryTestExtItemServiceCondition(conditions, tes, tspr, projectCode)
        normalConditions.add(
            tes.SERVICE_CODE.notIn(
                dslContext.select(tes.SERVICE_CODE).from(tes).join(tspr).on(
                    tes.SERVICE_CODE.eq(
                        tspr.STORE_CODE
                    )
                ).where(initTestConditions)
            )
        )
        return normalConditions
    }

    private fun queryPublicExtItemServiceCondition(
        dslContext: DSLContext,
        conditions: MutableList<Condition>,
        tes: TExtensionService,
        tesf: TExtensionServiceFeature,
        tspr: TStoreProjectRel,
        projectCode: String?
    ): MutableList<Condition> {
        val publicConditions = mutableListOf<Condition>()
        publicConditions.addAll(conditions)
        publicConditions.add(tes.SERVICE_STATUS.eq(ExtServiceStatusEnum.RELEASED.status.toByte()))
        publicConditions.add(tes.LATEST_FLAG.eq(true))
        publicConditions.add(tesf.PUBLIC_FLAG.eq(true))
        val initTestConditions = queryTestExtItemServiceCondition(conditions, tes, tspr, projectCode)
        publicConditions.add(
            tes.SERVICE_CODE.notIn(
                dslContext.select(tes.SERVICE_CODE).from(tes).join(tspr).on(
                    tes.SERVICE_CODE.eq(
                        tspr.STORE_CODE
                    )
                ).where(initTestConditions)
            )
        )
        return publicConditions
    }

    private fun setQueryExtItemServiceBaseCondition(
        tes: TExtensionService,
        tesir: TExtensionServiceItemRel,
        itemId: String
    ): MutableList<Condition> {
        val conditions = mutableListOf<Condition>()
        conditions.add(tes.DELETE_FLAG.eq(false))
        conditions.add(tesir.ITEM_ID.eq(itemId))
        return conditions
    }

    private fun getExtItemServiceBaseStep(
        dslContext: DSLContext,
        tes: TExtensionService,
        tesir: TExtensionServiceItemRel,
        tesf: TExtensionServiceFeature
    ): SelectOnConditionStep<Record10<String, String, String, String, String, String?, String, String?, Int, Boolean?>> {
        return dslContext.select(
            tes.ID.`as`("serviceId"),
            tes.SERVICE_CODE.`as`("serviceCode"),
            tes.SERVICE_NAME.`as`("serviceName"),
            tes.LOGO_URL.`as`("logoUrl"),
            tes.VERSION.`as`("version"),
            tes.SUMMARY.`as`("summary"),
            tes.PUBLISHER.`as`("publisher"),
            tesir.PROPS.`as`("props"),
            tesf.WEIGHT.`as`("weight"),
            tesf.KILL_GRAY_APP_FLAG.`as`("killGrayAppFlag")
        )
            .from(tes)
            .join(tesir)
            .on(tes.ID.eq(tesir.SERVICE_ID))
            .join(tesf)
            .on(tes.SERVICE_CODE.eq(tesf.SERVICE_CODE))
    }
}
