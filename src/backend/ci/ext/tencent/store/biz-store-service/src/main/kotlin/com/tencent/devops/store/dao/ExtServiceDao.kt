package com.tencent.devops.store.dao

import com.tencent.devops.model.store.tables.TExtensionService
import com.tencent.devops.model.store.tables.TStoreMember
import com.tencent.devops.model.store.tables.records.TExtensionServiceRecord
import com.tencent.devops.store.pojo.ExtServiceCreateInfo
import com.tencent.devops.store.pojo.ExtServiceUpdateInfo
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.enums.ExtServiceStatusEnum
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Result
import org.springframework.stereotype.Repository
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
                CREATOR,
                MODIFIER,
                CREATE_TIME,
                UPDATE_TIME
            )
                .values(
                    id,
                    extServiceCreateInfo.serviceName,
                    extServiceCreateInfo.serviceCode,
                    extServiceCreateInfo.category,
                    extServiceCreateInfo.version,
                    extServiceCreateInfo.status.toByte(),
                    extServiceCreateInfo.statusMsg,
                    extServiceCreateInfo.logoUrl,
                    extServiceCreateInfo.icon,
                    extServiceCreateInfo.sunmmary,
                    extServiceCreateInfo.description,
                    extServiceCreateInfo.publisher,
                    LocalDateTime.now(),
                    extServiceCreateInfo.latestFlag,
                    extServiceCreateInfo.deleteFlag,
                    extServiceCreateInfo.creatorUser,
                    userId,
                    LocalDateTime.now(),
                    LocalDateTime.now()
                )
                .execute()
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
            val summary = extServiceUpdateInfo.sunmmary
            if (null != summary) {
                baseStep.set(SUMMARY, summary)
            }
            val description = extServiceUpdateInfo.description
            if (null != description) {
                baseStep.set(DESCRIPTION, description)
            }
            val logoUrl = extServiceUpdateInfo.logoUrl
            if (null != logoUrl) {
                baseStep.set(LOGO_URL, logoUrl)
            }
            val publisher = extServiceUpdateInfo.publisher
            if (null != publisher) {
                baseStep.set(PUBLISHER, publisher)
            }
            baseStep.set(MODIFIER, userId).set(UPDATE_TIME, LocalDateTime.now())
                .where(ID.eq(serviceId))
                .execute()
        }
    }

    fun countByUser(
        dslContext: DSLContext,
        userId: String,
        serviceCode: String?
    ): Int {
        val a = TExtensionService.T_EXTENSION_SERVICE.`as`("a")
        val b = TStoreMember.T_STORE_MEMBER.`as`("b")
        val conditions = generateGetMemberConditions(a, userId, b, serviceCode)
        return dslContext.select(a.SERVICE_CODE.countDistinct())
            .from(a)
            .leftJoin(b)
            .on(a.SERVICE_CODE.eq(b.STORE_CODE))
            .where(conditions)
            .fetchOne(0, Int::class.java)
    }

    fun countReleaseServiceByCode(dslContext: DSLContext, serviceCode: String): Int {
        with(TExtensionService.T_EXTENSION_SERVICE) {
            return dslContext.selectCount().from(this).where(SERVICE_CODE.eq(serviceCode).and(SERVICE_STATUS.eq(ExtServiceStatusEnum.RELEASED.status.toByte()))).fetchOne(0, Int::class.java)
        }
    }

    fun getMyService(
        dslContext: DSLContext,
        userId: String,
        serviceCode: String?,
        page: Int?,
        pageSize: Int?
    ): Result<out Record>? {
        val a = TExtensionService.T_EXTENSION_SERVICE.`as`("a")
        val b = TStoreMember.T_STORE_MEMBER.`as`("b")
        val t = dslContext.select(a.SERVICE_CODE.`as`("serviceCode"), a.CREATE_TIME.max().`as`("createTime")).from(a)
            .groupBy(a.SERVICE_CODE) // 查找每组serviceCode最新的记录
        val conditions = generateGetMemberConditions(a, userId, b, serviceCode)
        val baseStep = dslContext.select(
            a.ID.`as`("serviceId"),
            a.SERVICE_CODE.`as`("serviceCode"),
            a.SERVICE_NAME.`as`("serviceName"),
            a.CLASSIFY_ID.`as`("category"),
            a.LOGO_URL.`as`("logoUrl"),
            a.VERSION.`as`("version"),
            a.SERVICE_STATUS.`as`("serviceStatus"),
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

    fun getServiceByCode(dslContext: DSLContext, serviceCode: String): TExtensionServiceRecord? {
        return with(TExtensionService.T_EXTENSION_SERVICE) {
            dslContext.selectFrom(this).where(DELETE_FLAG.eq(false)).and(SERVICE_CODE.eq(serviceCode)).fetchOne()
        }
    }

    fun listServiceByCode(dslContext: DSLContext, serviceCode: String): Result<TExtensionServiceRecord?> {
        return with(TExtensionService.T_EXTENSION_SERVICE) {
            dslContext.selectFrom(this).where(DELETE_FLAG.eq(false)).and(SERVICE_CODE.eq(serviceCode))
                .orderBy(CREATE_TIME.desc()).fetch()
        }
    }

    fun getServiceByName(dslContext: DSLContext, serviceName: String): TExtensionServiceRecord? {
        return with(TExtensionService.T_EXTENSION_SERVICE) {
            dslContext.selectFrom(this).where(DELETE_FLAG.eq(false)).and(SERVICE_NAME.eq(serviceName)).fetchOne()
        }
    }

    fun listServiceByName(dslContext: DSLContext, serviceName: String): Result<TExtensionServiceRecord?> {
        return with(TExtensionService.T_EXTENSION_SERVICE) {
            dslContext.selectFrom(this).where(DELETE_FLAG.eq(false)).and(SERVICE_NAME.eq(serviceName))
                .orderBy(CREATE_TIME.desc()).fetch()
        }
    }

    private fun generateGetMemberConditions(
        a: TExtensionService,
        userId: String,
        b: TStoreMember,
        serviceCode: String?
    ): MutableList<Condition> {
        val conditions = mutableListOf<Condition>()
        conditions.add(a.DELETE_FLAG.eq(false)) // 只查没有被删除的插件
        conditions.add(b.USERNAME.eq(userId))
        conditions.add(b.STORE_TYPE.eq(StoreTypeEnum.SERVICE.type.toByte()))
        if (null != serviceCode) {
            conditions.add(a.SERVICE_CODE.contains(serviceCode))
        }
        return conditions
    }
}