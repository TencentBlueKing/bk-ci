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

import com.tencent.devops.model.store.tables.TClassify
import com.tencent.devops.model.store.tables.TExtensionService
import com.tencent.devops.model.store.tables.TExtensionServiceFeature
import com.tencent.devops.model.store.tables.TExtensionServiceItemRel
import com.tencent.devops.model.store.tables.TExtensionServiceLabelRel
import com.tencent.devops.model.store.tables.TLabel
import com.tencent.devops.model.store.tables.TStoreMember
import com.tencent.devops.model.store.tables.TStoreProjectRel
import com.tencent.devops.model.store.tables.TStoreStatisticsTotal
import com.tencent.devops.model.store.tables.records.TExtensionServiceRecord
import com.tencent.devops.store.pojo.ExtServiceCreateInfo
import com.tencent.devops.store.pojo.ExtServiceUpdateInfo
import com.tencent.devops.store.pojo.common.enums.StoreProjectTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.dto.ServiceApproveReq
import com.tencent.devops.store.pojo.enums.ExtServiceSortTypeEnum
import com.tencent.devops.store.pojo.enums.ExtServiceStatusEnum
import com.tencent.devops.store.pojo.enums.ServiceTypeEnum
import com.tencent.devops.store.utils.VersionUtils
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Record2
import org.jooq.Result
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDateTime

@Repository
class ExtServiceDao {

    fun createExtService(
        dslContext: DSLContext,
        userId: String,
        id: String,
        extServiceCreateInfo: ExtServiceCreateInfo
    ) {
        with(TExtensionService.T_EXTENSION_SERVICE) {
            dslContext.insertInto(
                this,
                ID,
                SERVICE_NAME,
                SERVICE_CODE,
                CLASSIFY_ID,
                VERSION,
                SERVICE_STATUS,
                SERVICE_STATUS_MSG,
                LOGO_URL,
                ICON,
                SUMMARY,
                DESCRIPTION,
                PUBLISHER,
                PUB_TIME,
                LATEST_FLAG,
                DELETE_FLAG,
                OWNER,
                CREATOR,
                MODIFIER,
                CREATE_TIME,
                UPDATE_TIME
            )
                .values(
                    id,
                    extServiceCreateInfo.serviceName,
                    extServiceCreateInfo.serviceCode,
                    extServiceCreateInfo.classify,
                    extServiceCreateInfo.version,
                    extServiceCreateInfo.status.toByte(),
                    extServiceCreateInfo.statusMsg,
                    extServiceCreateInfo.logoUrl,
                    extServiceCreateInfo.iconData,
                    extServiceCreateInfo.summary,
                    extServiceCreateInfo.description,
                    extServiceCreateInfo.publisher,
                    LocalDateTime.now(),
                    extServiceCreateInfo.latestFlag,
                    extServiceCreateInfo.deleteFlag,
                    userId,
                    extServiceCreateInfo.creatorUser,
                    userId,
                    LocalDateTime.now(),
                    LocalDateTime.now()
                )
                .execute()
        }
    }

    /**
     * 清空LATEST_FLAG
     */
    fun cleanLatestFlag(dslContext: DSLContext, serviceCode: String) {
        with(TExtensionService.T_EXTENSION_SERVICE) {
            dslContext.update(this)
                .set(LATEST_FLAG, false)
                .where(SERVICE_CODE.eq(serviceCode))
                .execute()
        }
    }

    fun deleteExtService(
        dslContext: DSLContext,
        userId: String,
        serviceId: String
    ) {
        with(TExtensionService.T_EXTENSION_SERVICE) {
            dslContext.update(this).set(DELETE_FLAG, true).set(MODIFIER, userId).set(UPDATE_TIME, LocalDateTime.now())
                .where(ID.eq(serviceId)).execute()
        }
    }

    fun deleteExtServiceData(
        dslContext: DSLContext,
        serviceCode: String
    ) {
        with(TExtensionService.T_EXTENSION_SERVICE) {
            dslContext.deleteFrom(this).where(SERVICE_CODE.eq(serviceCode)).execute()
        }
    }

    fun getExtServiceIds(dslContext: DSLContext, serviceCode: String): Result<Record2<String, Boolean>> {
        with(TExtensionService.T_EXTENSION_SERVICE) {
            return dslContext.select(ID.`as`("id"), LATEST_FLAG.`as`("latestFlag")).from(this)
                .where(SERVICE_CODE.eq(serviceCode))
                .and(LATEST_FLAG.eq(true))
                .fetch()
        }
    }

    fun updateExtServiceBaseInfo(
        dslContext: DSLContext,
        userId: String,
        serviceId: String,
        extServiceUpdateInfo: ExtServiceUpdateInfo
    ) {
        with(TExtensionService.T_EXTENSION_SERVICE) {
            val baseStep = dslContext.update(this)
            val serviceName = extServiceUpdateInfo.serviceName
            if (null != serviceName) {
                baseStep.set(SERVICE_NAME, serviceName)
            }
            val summary = extServiceUpdateInfo.summary
            if (null != summary) {
                baseStep.set(SUMMARY, summary)
            }
            val status = extServiceUpdateInfo.status
            if (null != status) {
                baseStep.set(SERVICE_STATUS, status.toByte())
            }
            val statusMsg = extServiceUpdateInfo.statusMsg
            if (null != statusMsg) {
                baseStep.set(SERVICE_STATUS_MSG, statusMsg)
            }
            val version = extServiceUpdateInfo.version
            if (null != version) {
                baseStep.set(VERSION, version)
            }
            val description = extServiceUpdateInfo.description
            if (null != description) {
                baseStep.set(DESCRIPTION, description)
            }
            val logoUrl = extServiceUpdateInfo.logoUrl
            if (null != logoUrl) {
                baseStep.set(LOGO_URL, logoUrl)
            }
            val iconData = extServiceUpdateInfo.iconData
            if (null != iconData) {
                baseStep.set(ICON, iconData)
            }

            val latest = extServiceUpdateInfo.latestFlag
            if (null != latest) {
                baseStep.set(LATEST_FLAG, latest)
            }

            val publisher = extServiceUpdateInfo.publisher
            if (null != publisher) {
                baseStep.set(PUBLISHER, publisher)
                baseStep.set(PUB_TIME, LocalDateTime.now())
            }

            baseStep.set(MODIFIER, userId).set(UPDATE_TIME, LocalDateTime.now())
                .where(ID.eq(serviceId))
                .execute()
        }
    }

    fun countByUser(
        dslContext: DSLContext,
        userId: String,
        serviceName: String?
    ): Int {
        val a = TExtensionService.T_EXTENSION_SERVICE.`as`("a")
        val b = TStoreMember.T_STORE_MEMBER.`as`("b")
        val conditions = generateGetMemberConditions(a, userId, b, serviceName)
        return dslContext.select(a.SERVICE_CODE.countDistinct())
            .from(a)
            .leftJoin(b)
            .on(a.SERVICE_CODE.eq(b.STORE_CODE))
            .where(conditions)
            .fetchOne(0, Int::class.java)!!
    }

    fun countReleaseServiceByCode(dslContext: DSLContext, serviceCode: String): Int {
        with(TExtensionService.T_EXTENSION_SERVICE) {
            return dslContext.selectCount().from(this).where(
                SERVICE_CODE.eq(serviceCode).and(DELETE_FLAG.eq(false)).and(
                    SERVICE_STATUS.eq(ExtServiceStatusEnum.RELEASED.status.toByte())
                )
            ).fetchOne(0, Int::class.java)!!
        }
    }

    fun getMyService(
        dslContext: DSLContext,
        userId: String,
        serviceName: String?,
        page: Int?,
        pageSize: Int?
    ): Result<out Record>? {
        val a = TExtensionService.T_EXTENSION_SERVICE.`as`("a")
        val b = TStoreMember.T_STORE_MEMBER.`as`("b")
        val t = dslContext.select(a.SERVICE_CODE.`as`("serviceCode"), a.CREATE_TIME.max().`as`("createTime")).from(a)
            .groupBy(a.SERVICE_CODE) // 查找每组serviceCode最新的记录
        val conditions = generateGetMemberConditions(a, userId, b, serviceName)
        val baseStep = dslContext.select(
            a.ID.`as`("serviceId"),
            a.SERVICE_CODE.`as`("serviceCode"),
            a.SERVICE_NAME.`as`("serviceName"),
            a.CLASSIFY_ID.`as`("category"),
            a.LOGO_URL.`as`("logoUrl"),
            a.VERSION.`as`("version"),
            a.SERVICE_STATUS.`as`("serviceStatus"),
            a.PUBLISHER.`as`("publisher"),
            a.PUB_TIME.`as`("pubTime"),
            a.CREATOR.`as`("creator"),
            a.CREATE_TIME.`as`("createTime"),
            a.MODIFIER.`as`("modifier"),
            a.UPDATE_TIME.`as`("updateTime")
        )
            .from(a)
            .join(t)
            .on(
                a.SERVICE_CODE.eq(t.field("serviceCode", String::class.java)).and(
                    a.CREATE_TIME.eq(
                        t.field(
                            "createTime",
                            LocalDateTime::class.java
                        )
                    )
                )
            )
            .join(b)
            .on(a.SERVICE_CODE.eq(b.STORE_CODE))
            .where(conditions)
            .groupBy(a.SERVICE_CODE)
            .orderBy(a.UPDATE_TIME.desc())
        return if (null != page && null != pageSize) {
            baseStep.limit((page - 1) * pageSize, pageSize).fetch()
        } else {
            baseStep.fetch()
        }
    }

    fun getServiceById(dslContext: DSLContext, serviceId: String): TExtensionServiceRecord? {
        return with(TExtensionService.T_EXTENSION_SERVICE) {
            dslContext.selectFrom(this).where(DELETE_FLAG.eq(false)).and(ID.eq(serviceId)).fetchOne()
        }
    }

    fun getServiceByCode(
        dslContext: DSLContext,
        serviceCode: String,
        page: Int?,
        pageSize: Int?
    ): Result<TExtensionServiceRecord>? {
        val a = TExtensionService.T_EXTENSION_SERVICE.`as`("a")
        val conditions = mutableListOf<Condition>()
        conditions.add(a.SERVICE_CODE.eq(serviceCode))
        conditions.add(a.DELETE_FLAG.eq(false))
        val whereStep = dslContext.selectFrom(a).where(conditions).orderBy(a.CREATE_TIME.desc())
        return if (null != page && null != pageSize) {
            whereStep.limit((page - 1) * pageSize, pageSize).fetch()
        } else {
            whereStep.fetch()
        }
    }

    fun countByCode(dslContext: DSLContext, serviceCode: String): Int {
        return with(TExtensionService.T_EXTENSION_SERVICE) {
            dslContext.selectCount().from(this).where(DELETE_FLAG.eq(false)).and(SERVICE_CODE.eq(serviceCode))
                .fetchOne(0, Int::class.java)!!
        }
    }

    fun countByName(dslContext: DSLContext, serviceName: String, serviceCode: String? = null): Int {
        with(TExtensionService.T_EXTENSION_SERVICE) {
            val conditions = mutableListOf<Condition>()
            conditions.add(SERVICE_NAME.eq(serviceName))
            conditions.add(DELETE_FLAG.eq(false))
            if (serviceCode != null) {
                conditions.add(SERVICE_CODE.eq(serviceCode))
            }
            return dslContext.selectCount().from(this).where(conditions).fetchOne(0, Int::class.java)!!
        }
    }

    fun getServiceLatestByCode(dslContext: DSLContext, serviceCode: String): TExtensionServiceRecord? {
        return with(TExtensionService.T_EXTENSION_SERVICE) {
            dslContext.selectFrom(this).where(DELETE_FLAG.eq(false)).and(SERVICE_CODE.eq(serviceCode))
                .and(LATEST_FLAG.eq(true)).fetchOne()
        }
    }

    fun getNewestServiceByCode(dslContext: DSLContext, serviceCode: String): TExtensionServiceRecord? {
        return with(TExtensionService.T_EXTENSION_SERVICE) {
            dslContext.selectFrom(this).where(DELETE_FLAG.eq(false)).and(SERVICE_CODE.eq(serviceCode))
                .orderBy(CREATE_TIME.desc()).limit(1).fetchOne()
        }
    }

    fun listServiceByCode(dslContext: DSLContext, serviceCode: String): Result<TExtensionServiceRecord>? {
        return with(TExtensionService.T_EXTENSION_SERVICE) {
            dslContext.selectFrom(this).where(DELETE_FLAG.eq(false)).and(SERVICE_CODE.eq(serviceCode))
                .orderBy(CREATE_TIME.desc()).fetch()
        }
    }

    fun getServiceByName(dslContext: DSLContext, serviceName: String): TExtensionServiceRecord? {
        return with(TExtensionService.T_EXTENSION_SERVICE) {
            dslContext.selectFrom(this).where(SERVICE_NAME.eq(serviceName)).fetchOne()
        }
    }

    fun getExtService(dslContext: DSLContext, serviceCode: String, version: String): TExtensionServiceRecord? {
        return with(TExtensionService.T_EXTENSION_SERVICE) {
            dslContext.selectFrom(this)
                .where(SERVICE_CODE.eq(serviceCode).and(VERSION.like(VersionUtils.generateQueryVersion(version))))
                .orderBy(CREATE_TIME.desc())
                .limit(1)
                .fetchOne()
        }
    }

    fun listServiceByName(dslContext: DSLContext, serviceName: String): Result<TExtensionServiceRecord>? {
        return with(TExtensionService.T_EXTENSION_SERVICE) {
            dslContext.selectFrom(this).where(DELETE_FLAG.eq(false)).and(SERVICE_NAME.eq(serviceName))
                .orderBy(CREATE_TIME.desc()).fetch()
        }
    }

    fun listServiceByStatus(
        dslContext: DSLContext,
        serviceStatus: ExtServiceStatusEnum,
        page: Int?,
        pageSize: Int?,
        timeDescFlag: Boolean? = null
    ): Result<TExtensionServiceRecord>? {
        with(TExtensionService.T_EXTENSION_SERVICE) {
            val baseStep = dslContext.selectFrom(this).where(DELETE_FLAG.eq(false)).and(SERVICE_STATUS.eq(serviceStatus.status.toByte()))
            if (timeDescFlag != null && timeDescFlag) {
                baseStep.orderBy(CREATE_TIME.desc())
            } else {
                baseStep.orderBy(CREATE_TIME.asc())
            }
            return if (null != page && null != pageSize) {
                baseStep.limit((page - 1) * pageSize, pageSize).fetch()
            } else {
                baseStep.fetch()
            }
        }
    }

    /**
     * 审核原子时，更新状态、类型等信息
     */
    fun approveServiceFromOp(
        dslContext: DSLContext,
        userId: String,
        serviceId: String,
        serviceStatus: Byte,
        approveReq: ServiceApproveReq,
        pubTime: LocalDateTime? = null
    ) {
        with(TExtensionService.T_EXTENSION_SERVICE) {
            dslContext.update(this)
                .set(SERVICE_STATUS, serviceStatus)
                .set(SERVICE_STATUS_MSG, approveReq.message)
                .set(PUB_TIME, pubTime)
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(ID.eq(serviceId))
                .execute()
        }
    }

    /**
     * 设置可用的扩展服务版本状态为下架中、已下架
     */
    fun setServiceStatusByCode(
        dslContext: DSLContext,
        serviceCode: String,
        serviceOldStatus: Byte,
        serviceNewStatus: Byte,
        userId: String,
        msg: String?
    ) {
        with(TExtensionService.T_EXTENSION_SERVICE) {
            val baseStep = dslContext.update(this)
                .set(SERVICE_STATUS, serviceNewStatus)
            if (!msg.isNullOrEmpty()) {
                baseStep.set(SERVICE_STATUS_MSG, msg)
            }
            baseStep.set(MODIFIER, userId)
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(SERVICE_CODE.eq(serviceCode))
                .and(SERVICE_STATUS.eq(serviceOldStatus))
                .execute()
        }
    }

    fun getAllServiceClassify(dslContext: DSLContext): Result<out Record>? {
        val a = TExtensionService.T_EXTENSION_SERVICE.`as`("a")
        val b = TClassify.T_CLASSIFY.`as`("b")
        val conditions = setExtServiceVisibleCondition(a)
        conditions.add(0, a.CLASSIFY_ID.eq(b.ID))
        val serviceNum = dslContext.selectCount().from(a).where(conditions).asField<Int>("serviceNum")
        return dslContext.select(
            b.ID.`as`("id"),
            b.CLASSIFY_CODE.`as`("classifyCode"),
            b.CLASSIFY_NAME.`as`("classifyName"),
            serviceNum,
            b.CREATE_TIME.`as`("createTime"),
            b.UPDATE_TIME.`as`("updateTime")
        ).from(b).where(b.TYPE.eq(StoreTypeEnum.SERVICE.type.toByte())).orderBy(b.WEIGHT.desc()).fetch()
    }

    /**
     * 扩展服务商店搜索结果，总数
     */
    fun count(
        dslContext: DSLContext,
        keyword: String?,
        classifyCode: String?,
        bkService: Long?,
        rdType: ServiceTypeEnum? = null,
        labelCodeList: List<String>?,
        score: Int?
    ): Int {
        val (ta, conditions) = formatConditions(keyword, classifyCode, dslContext)

        val baseStep = dslContext.select(ta.ID.countDistinct()).from(ta)

        if (rdType != null) {
            val taf = TExtensionServiceFeature.T_EXTENSION_SERVICE_FEATURE.`as`("taf")
            baseStep.leftJoin(taf).on(ta.SERVICE_CODE.eq(taf.SERVICE_CODE))
            conditions.add(taf.SERVICE_TYPE.eq(rdType.type.toByte()))
        }

        val storeType = StoreTypeEnum.SERVICE.type.toByte()
        if (labelCodeList != null && labelCodeList.isNotEmpty()) {
            val c = TLabel.T_LABEL.`as`("c")
            val labelIdList = dslContext.select(c.ID)
                .from(c)
                .where(c.LABEL_CODE.`in`(labelCodeList)).and(c.TYPE.eq(storeType))
                .fetch().map { it["ID"] as String }
            val talr = TExtensionServiceLabelRel.T_EXTENSION_SERVICE_LABEL_REL.`as`("talr")
            baseStep.leftJoin(talr).on(ta.ID.eq(talr.SERVICE_ID))
            conditions.add(talr.LABEL_ID.`in`(labelIdList))
        }
        if (bkService != null) {
            val tir = TExtensionServiceItemRel.T_EXTENSION_SERVICE_ITEM_REL.`as`("tir")
            val serviceIdList = dslContext.select(tir.SERVICE_ID).from(tir)
                .where(tir.BK_SERVICE_ID.eq(bkService)).fetch().map { it["SERVICE_ID"] as String }
            conditions.add(ta.ID.`in`(serviceIdList))
        }

        if (score != null) {
            val tas = TStoreStatisticsTotal.T_STORE_STATISTICS_TOTAL.`as`("tas")
            val t = dslContext.select(
                tas.STORE_CODE,
                tas.STORE_TYPE,
//                tas.DOWNLOADS.`as`(ExtServiceSortTypeEnum.DOWNLOAD_COUNT.name),
                tas.SCORE_AVERAGE
            ).from(tas).asTable("t")
            baseStep.leftJoin(t).on(ta.SERVICE_CODE.eq(t.field("STORE_CODE", String::class.java)))
            conditions.add(t.field("SCORE_AVERAGE", BigDecimal::class.java)!!.ge(BigDecimal.valueOf(score.toLong())))
            conditions.add(t.field("STORE_TYPE", Byte::class.java)!!.eq(storeType))
        }

        return baseStep.where(conditions).fetchOne(0, Int::class.java)!!
    }

    /**
     * 设置扩展服务状态（单个版本）
     */
    fun setServiceStatusById(
        dslContext: DSLContext,
        serviceId: String,
        serviceStatus: Byte,
        userId: String,
        msg: String?
    ) {
        with(TExtensionService.T_EXTENSION_SERVICE) {
            val baseStep = dslContext.update(this)
                .set(SERVICE_STATUS, serviceStatus)
            if (!msg.isNullOrEmpty()) {
                baseStep.set(SERVICE_STATUS_MSG, msg)
            }
            baseStep.set(MODIFIER, userId)
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(ID.eq(serviceId))
                .execute()
        }
    }

    fun queryServicesFromOp(
        dslContext: DSLContext,
        serviceName: String?,
        itemId: String?,
        isRecommend: Boolean?,
        isPublic: Boolean?,
        lableId: String?,
        isApprove: Boolean?,
        sortType: String?,
        desc: Boolean?,
        page: Int?,
        pageSize: Int?
    ): Result<Record> {
        val a = TExtensionService.T_EXTENSION_SERVICE.`as`("a")
        val b = TExtensionServiceFeature.T_EXTENSION_SERVICE_FEATURE.`as`("b")
        val c = TExtensionServiceItemRel.T_EXTENSION_SERVICE_ITEM_REL.`as`("c")
        val d = TStoreProjectRel.T_STORE_PROJECT_REL.`as`("d")
        // 查找每组serviceCode最新的记录
        val tmp = dslContext.select(
            a.SERVICE_CODE.`as`("serviceCode"),
            a.CREATE_TIME.max().`as`("createTime")
        ).from(a).groupBy(a.SERVICE_CODE)
        val ta = dslContext.select(a.SERVICE_CODE.`as`("serviceCode"), a.SERVICE_STATUS.`as`("serviceStatus")).from(a).join(tmp)
            .on(a.SERVICE_CODE.eq(tmp.field("serviceCode", String::class.java)).and(a.CREATE_TIME.eq(tmp.field("createTime", LocalDateTime::class.java))))
        val selectField = dslContext.select(
            a.ID.`as`("itemId"),
            a.SERVICE_STATUS.`as`("serviceStatus"),
            a.SERVICE_NAME.`as`("serviceName"),
            a.SERVICE_CODE.`as`("serviceCode"),
            a.VERSION.`as`("version"),
            a.PUB_TIME.`as`("pubTime"),
            a.PUBLISHER.`as`("publisher"),
            a.UPDATE_TIME.`as`("updateTime"),
            d.PROJECT_CODE.`as`("projectCode")
        ).from(a)
            .join(b)
            .on(a.SERVICE_CODE.eq(b.SERVICE_CODE))
            .join(d)
            .on(a.SERVICE_CODE.eq(d.STORE_CODE))
            .join(ta)
            .on(a.SERVICE_CODE.eq(ta.field("serviceCode", String::class.java)))
        val conditions = mutableListOf<Condition>()
        conditions.add(d.TYPE.eq(StoreProjectTypeEnum.INIT.type.toByte()))
        conditions.add(d.STORE_TYPE.eq(StoreTypeEnum.SERVICE.type.toByte()))
        conditions.add(a.DELETE_FLAG.eq(false))
        conditions.add(a.LATEST_FLAG.eq(true))
        if (null != serviceName) {
            conditions.add(a.SERVICE_NAME.like("%$serviceName%"))
        }
        if (null != itemId) {
            conditions.add(c.ITEM_ID.eq(itemId))
            selectField.join(c).on(a.ID.eq(c.SERVICE_ID))
        }

        if (null != isPublic) {
            conditions.add(b.PUBLIC_FLAG.eq(isPublic))
        }

        if (null != isRecommend) {
            conditions.add(b.RECOMMEND_FLAG.eq(isRecommend))
        }

        if (null != isApprove) {
            if (isApprove) {
                conditions.add(a.SERVICE_STATUS.eq(ExtServiceStatusEnum.AUDITING.status.toByte()))
            } else {
                conditions.add(a.SERVICE_STATUS.notEqual(ExtServiceStatusEnum.AUDITING.status.toByte()))
            }
        }
        val realSortType = a.field(sortType)
        val orderByStep = if (desc != null && desc) {
            realSortType!!.desc()
        } else {
            realSortType!!.asc()
        }
        val t = selectField.where(conditions).orderBy(orderByStep)
        val baseStep = dslContext.select().from(t)
        return if (null != page && null != pageSize) {
            baseStep.limit((page - 1) * pageSize, pageSize).fetch()
        } else {
            baseStep.fetch()
        }
    }

    fun queryCountFromOp(
        dslContext: DSLContext,
        serviceName: String?,
        itemId: String?,
        isRecommend: Boolean?,
        isPublic: Boolean?,
        isApprove: Boolean?
    ): Int {
        val a = TExtensionService.T_EXTENSION_SERVICE.`as`("a")
        val b = TExtensionServiceFeature.T_EXTENSION_SERVICE_FEATURE.`as`("b")
        val c = TExtensionServiceItemRel.T_EXTENSION_SERVICE_ITEM_REL.`as`("c")
        val d = TStoreProjectRel.T_STORE_PROJECT_REL.`as`("d")

        var selectFeild = dslContext.select(
            a.ID.countDistinct()
        ).from(a).join(b).on(a.SERVICE_CODE.eq(b.SERVICE_CODE)).join(d).on(a.SERVICE_CODE.eq(d.STORE_CODE))

        val conditions = mutableListOf<Condition>()
        conditions.add(d.TYPE.eq(StoreProjectTypeEnum.INIT.type.toByte()))
        conditions.add(d.STORE_TYPE.eq(StoreTypeEnum.SERVICE.type.toByte()))
        conditions.add(a.DELETE_FLAG.eq(false))
        conditions.add(a.LATEST_FLAG.eq(true))

        if (null != serviceName) {
            conditions.add(a.SERVICE_NAME.like("%$serviceName%"))
        }
        if (null != itemId) {
            conditions.add(c.ITEM_ID.eq(itemId))
            selectFeild.join(c).on(a.ID.eq(c.SERVICE_ID))
        }

        if (null != isPublic) {
            conditions.add(b.PUBLIC_FLAG.eq(isPublic))
        }

        if (null != isRecommend) {
            conditions.add(b.RECOMMEND_FLAG.eq(isRecommend))
        }

        if (null != isApprove) {
            if (isApprove) {
                conditions.add(a.SERVICE_STATUS.eq(ExtServiceStatusEnum.AUDITING.status.toByte()))
            } else {
                conditions.add(a.SERVICE_STATUS.notEqual(ExtServiceStatusEnum.AUDITING.status.toByte()))
            }
        }
        return selectFeild.where(conditions).fetchOne(0, Int::class.java)!!
    }

    fun getLatestFlag(dslContext: DSLContext, serviceCode: String): Boolean {
        with(TExtensionService.T_EXTENSION_SERVICE) {
            val count = dslContext.selectCount().from(this).where(SERVICE_CODE.eq(serviceCode).and(SERVICE_STATUS.eq(ExtServiceStatusEnum.RELEASED.status.toByte())))
                .fetchOne(0, Int::class.java)!!
            if (count > 0) {
                return false
            }
        }
        return true
    }

    /**
     * 研发商店搜索结果列表
     */
    fun list(
        dslContext: DSLContext,
        keyword: String?,
        classifyCode: String?,
        bkService: Long?,
        labelCodeList: List<String>?,
        score: Int?,
        rdType: ServiceTypeEnum?,
        sortType: ExtServiceSortTypeEnum?,
        desc: Boolean?,
        page: Int?,
        pageSize: Int?
    ): Result<out Record>? {
        val (ta, conditions) = formatConditions(keyword, classifyCode, dslContext)
        val taf = TExtensionServiceFeature.T_EXTENSION_SERVICE_FEATURE.`as`("taf")
        val baseStep = dslContext.select(
            ta.ID.`as`("SERVICE_ID"),
            ta.SERVICE_NAME,
            ta.CLASSIFY_ID,
            ta.SERVICE_CODE.`as`("SERVICE_CODE"),
            ta.LOGO_URL,
            ta.PUBLISHER,
            ta.SUMMARY,
            ta.MODIFIER,
            ta.UPDATE_TIME,
            taf.RECOMMEND_FLAG,
            taf.PUBLIC_FLAG
        ).from(ta)
            .leftJoin(taf)
            .on(ta.SERVICE_CODE.eq(taf.SERVICE_CODE))

        val storeType = StoreTypeEnum.SERVICE.type.toByte()
        if (labelCodeList != null && labelCodeList.isNotEmpty()) {
            val c = TLabel.T_LABEL.`as`("c")
            val labelIdList = dslContext.select(c.ID)
                .from(c)
                .where(c.LABEL_CODE.`in`(labelCodeList)).and(c.TYPE.eq(storeType))
                .fetch().map { it["ID"] as String }
            val talr = TExtensionServiceLabelRel.T_EXTENSION_SERVICE_LABEL_REL.`as`("talr")
            baseStep.leftJoin(talr).on(ta.ID.eq(talr.SERVICE_ID))
            conditions.add(talr.LABEL_ID.`in`(labelIdList))
        }

        if (rdType != null) {
            conditions.add(taf.SERVICE_TYPE.eq(rdType.type.toByte()))
        }

        if (bkService != null) {
            val tir = TExtensionServiceItemRel.T_EXTENSION_SERVICE_ITEM_REL.`as`("tir")
            val serviceIdList = dslContext.select(tir.SERVICE_ID).from(tir)
                .where(tir.BK_SERVICE_ID.eq(bkService)).fetch().map { it["SERVICE_ID"] as String }
            conditions.add(ta.ID.`in`(serviceIdList))
        }
        if (score != null) {
            val tas = TStoreStatisticsTotal.T_STORE_STATISTICS_TOTAL.`as`("tas")
            val t = dslContext.select(
                tas.STORE_CODE,
                tas.STORE_TYPE,
                tas.DOWNLOADS.`as`(ExtServiceSortTypeEnum.DOWNLOAD_COUNT.name),
                tas.SCORE_AVERAGE
            ).from(tas).asTable("t")
            baseStep.leftJoin(t).on(ta.SERVICE_CODE.eq(t.field("STORE_CODE", String::class.java)))
            conditions.add(t.field("SCORE_AVERAGE", BigDecimal::class.java)!!.ge(BigDecimal.valueOf(score.toLong())))
            conditions.add(t.field("STORE_TYPE", Byte::class.java)!!.eq(storeType))
        }

        if (null != sortType) {
            if (sortType == ExtServiceSortTypeEnum.DOWNLOAD_COUNT && score == null) {
                val tas = TStoreStatisticsTotal.T_STORE_STATISTICS_TOTAL.`as`("tas")
                val t =
                    dslContext.select(tas.STORE_CODE, tas.DOWNLOADS.`as`(ExtServiceSortTypeEnum.DOWNLOAD_COUNT.name))
                        .from(tas).where(tas.STORE_TYPE.eq(storeType)).asTable("t")
                baseStep.leftJoin(t).on(ta.SERVICE_CODE.eq(t.field("STORE_CODE", String::class.java)))
            }
            val sortTypeField = ExtServiceSortTypeEnum.getSortType(sortType.name)
            val realSortType = if (sortType == ExtServiceSortTypeEnum.DOWNLOAD_COUNT) {
                DSL.field(sortTypeField)
            } else {
                ta.field(sortTypeField)
            }

            if (desc != null && desc) {
                baseStep.where(conditions).orderBy(realSortType!!.desc())
            } else {
                baseStep.where(conditions).orderBy(realSortType!!.asc())
            }
        } else {
            baseStep.where(conditions)
        }
        logger.info(baseStep.getSQL(true))
        return if (null != page && null != pageSize) {
            baseStep.limit((page - 1) * pageSize, pageSize).fetch()
        } else {
            baseStep.fetch()
        }
    }

    fun getProjectServiceBy(dslContext: DSLContext, projectCode: String): Result<out Record>? {
        val sa = TExtensionService.T_EXTENSION_SERVICE.`as`("sa")
        val sp = TStoreProjectRel.T_STORE_PROJECT_REL.`as`("sp")
        val sf = TExtensionServiceFeature.T_EXTENSION_SERVICE_FEATURE.`as`("sf")
        val baseStep = dslContext.select(
            sa.ID.`as`("serviceId"),
            sa.SERVICE_NAME.`as`("serviceName"),
            sa.SERVICE_CODE.`as`("serviceCode"),
            sa.PUBLISHER.`as`("publisher"),
            sa.PUB_TIME.`as`("pubTime"),
            sa.VERSION.`as`("version"),
            sa.CREATOR.`as`("creator"),
            sa.CREATE_TIME.`as`("createTime"),
            sa.MODIFIER.`as`("modifier"),
            sa.UPDATE_TIME.`as`("updateTime"),
            sa.SERVICE_STATUS.`as`("serviceStatus"),
            sf.PUBLIC_FLAG.`as`("publicFlag"),
            sp.CREATE_TIME.`as`("projectInstallTime"),
            sp.CREATOR.`as`("projectInstallUser"),
            sp.TYPE.`as`("projectType")
        ).from(sa).leftOuterJoin(sp).on(sa.SERVICE_CODE.eq(sp.STORE_CODE)).leftJoin(sf).on(sa.SERVICE_CODE.eq(sf.SERVICE_CODE))
        val condition = mutableListOf<Condition>()
        condition.add(sa.LATEST_FLAG.eq(true))
        condition.add(sp.PROJECT_CODE.eq(projectCode))
        condition.add(sp.STORE_TYPE.eq(StoreTypeEnum.SERVICE.type.toByte()))
        condition.add(sa.DELETE_FLAG.eq(false))
        condition.add(sp.TYPE.notEqual(StoreProjectTypeEnum.TEST.type.toByte()))

        return baseStep.where(condition).groupBy(sa.SERVICE_CODE).orderBy(sa.UPDATE_TIME.desc()).fetch()
    }

    /**
     * 设置见扩展查询条件
     */
    protected fun setExtServiceVisibleCondition(a: TExtensionService): MutableList<Condition> {
        val conditions = mutableListOf<Condition>()
        conditions.add(a.SERVICE_STATUS.eq(ExtServiceStatusEnum.RELEASED.status.toByte())) // 已发布的
        conditions.add(a.LATEST_FLAG.eq(true)) // 最新版本
        conditions.add(a.DELETE_FLAG.eq(false)) // 只查没有被删除的扩展
        return conditions
    }

    private fun generateGetMemberConditions(
        a: TExtensionService,
        userId: String,
        b: TStoreMember,
        serviceName: String?
    ): MutableList<Condition> {
        val conditions = mutableListOf<Condition>()
        conditions.add(a.DELETE_FLAG.eq(false)) // 只查没有被删除的插件
        conditions.add(b.USERNAME.eq(userId))
        conditions.add(b.STORE_TYPE.eq(StoreTypeEnum.SERVICE.type.toByte()))
        if (null != serviceName) {
            conditions.add(a.SERVICE_NAME.contains(serviceName))
        }
        return conditions
    }

    private fun formatConditions(
        keyword: String?,
        classifyCode: String?,
        dslContext: DSLContext
    ): Pair<TExtensionService, MutableList<Condition>> {
        val ta = TExtensionService.T_EXTENSION_SERVICE.`as`("ta")
        val storeType = StoreTypeEnum.SERVICE.type.toByte()
        val conditions = setExtServiceVisibleCondition(ta)
        if (!keyword.isNullOrEmpty()) {
            conditions.add(ta.SERVICE_NAME.contains(keyword).or(ta.SUMMARY.contains(keyword)))
        }
        if (!classifyCode.isNullOrEmpty()) {
            val a = TClassify.T_CLASSIFY.`as`("a")
            val classifyId = dslContext.select(a.ID)
                .from(a)
                .where(a.CLASSIFY_CODE.eq(classifyCode).and(a.TYPE.eq(storeType)))
                .fetchOne(0, String::class.java)
            conditions.add(ta.CLASSIFY_ID.eq(classifyId))
        }
        return Pair(ta, conditions)
    }

    fun batchUpdateService(dslContext: DSLContext, serviceRecords: List<TExtensionServiceRecord>) {
        if (serviceRecords.isEmpty()) {
            return
        }
        dslContext.batchUpdate(serviceRecords).execute()
    }

    private val logger = LoggerFactory.getLogger(ExtServiceDao::class.java)
}
