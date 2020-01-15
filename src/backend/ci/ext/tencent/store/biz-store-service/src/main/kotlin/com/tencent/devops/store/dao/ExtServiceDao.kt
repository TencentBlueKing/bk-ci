package com.tencent.devops.store.dao

import com.tencent.devops.model.store.tables.TAtom
import com.tencent.devops.model.store.tables.TAtomEnvInfo
import com.tencent.devops.model.store.tables.TAtomFeature
import com.tencent.devops.model.store.tables.TAtomLabelRel
import com.tencent.devops.model.store.tables.TClassify
import com.tencent.devops.model.store.tables.TExtensionService
import com.tencent.devops.model.store.tables.TExtensionServiceFeature
import com.tencent.devops.model.store.tables.TExtensionServiceLabelRel
import com.tencent.devops.model.store.tables.TLabel
import com.tencent.devops.model.store.tables.TStoreMember
import com.tencent.devops.model.store.tables.TStoreStatisticsTotal
import com.tencent.devops.model.store.tables.records.TExtensionServiceRecord
import com.tencent.devops.store.pojo.ExtServiceCreateInfo
import com.tencent.devops.store.pojo.ExtServiceUpdateInfo
import com.tencent.devops.store.pojo.atom.MarketAtomCreateRequest
import com.tencent.devops.store.pojo.atom.enums.AtomStatusEnum
import com.tencent.devops.store.pojo.atom.enums.AtomTypeEnum
import com.tencent.devops.store.pojo.atom.enums.MarketAtomSortTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.enums.ExtServiceStatusEnum
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Result
import org.jooq.impl.DSL
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

    fun getServiceLatestByCode(dslContext: DSLContext, serviceCode: String): TExtensionServiceRecord? {
        return with(TExtensionService.T_EXTENSION_SERVICE) {
            dslContext.selectFrom(this).where(DELETE_FLAG.eq(false)).and(SERVICE_CODE.eq(serviceCode)).and(LATEST_FLAG.eq(true)).fetchOne()
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
        val serviceNum = dslContext.selectCount().from(a).where(conditions).asField<Int>("atomNum")
        return dslContext.select(
            b.ID.`as`("id"),
            b.CLASSIFY_CODE.`as`("classifyCode"),
            b.CLASSIFY_NAME.`as`("classifyName"),
            serviceNum,
            b.CREATE_TIME.`as`("createTime"),
            b.UPDATE_TIME.`as`("updateTime")
        ).from(b).where(b.TYPE.eq(0)).orderBy(b.WEIGHT.desc()).fetch()
    }

    /**
     * 扩展服务商店搜索结果，总数
     */
    fun count(
        dslContext: DSLContext,
        serviceName: String?,
        classifyCode: String?,
        labelCodeList: List<String>?,
        score: Int?
    ): Int {
        val (ta, conditions) = formatConditions(serviceName, classifyCode, dslContext)

        val baseStep = dslContext.select(ta.ID.countDistinct()).from(ta)

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
        if (score != null) {
            val tas = TStoreStatisticsTotal.T_STORE_STATISTICS_TOTAL.`as`("tas")
            val t = dslContext.select(
                tas.STORE_CODE,
                tas.STORE_TYPE,
                tas.DOWNLOADS.`as`(MarketAtomSortTypeEnum.DOWNLOAD_COUNT.name),
                tas.SCORE_AVERAGE
            ).from(tas).asTable("t")
            baseStep.leftJoin(t).on(ta.SERVICE_CODE.eq(t.field("STORE_CODE", String::class.java)))
            conditions.add(t.field("SCORE_AVERAGE", BigDecimal::class.java).ge(BigDecimal.valueOf(score.toLong())))
            conditions.add(t.field("STORE_TYPE", Byte::class.java).eq(storeType))
        }

        return baseStep.where(conditions).fetchOne(0, Int::class.java)
    }

    /**
     * 插件商店搜索结果列表
     */
    fun list(
        dslContext: DSLContext,
        serviceName: String?,
        classifyCode: String?,
        labelCodeList: List<String>?,
        score: Int?,
        sortType: MarketAtomSortTypeEnum?,
        desc: Boolean?,
        page: Int?,
        pageSize: Int?
    ): Result<out Record>? {
        val (ta, conditions) = formatConditions(serviceName, classifyCode, dslContext)
        val taf = TExtensionServiceFeature.T_EXTENSION_SERVICE_FEATURE.`as`("taf")
        val baseStep = dslContext.select(
            ta.ID,
            ta.SERVICE_NAME,
            ta.CLASSIFY_ID,
            ta.SERVICE_CODE,
            ta.LOGO_URL,
            ta.PUBLISHER,
            ta.SUMMARY,
            taf.RECOMMEND_FLAG
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
        if (score != null) {
            val tas = TStoreStatisticsTotal.T_STORE_STATISTICS_TOTAL.`as`("tas")
            val t = dslContext.select(
                tas.STORE_CODE,
                tas.STORE_TYPE,
                tas.DOWNLOADS.`as`(MarketAtomSortTypeEnum.DOWNLOAD_COUNT.name),
                tas.SCORE_AVERAGE
            ).from(tas).asTable("t")
            baseStep.leftJoin(t).on(ta.SERVICE_CODE.eq(t.field("STORE_CODE", String::class.java)))
            conditions.add(t.field("SCORE_AVERAGE", BigDecimal::class.java).ge(BigDecimal.valueOf(score.toLong())))
            conditions.add(t.field("STORE_TYPE", Byte::class.java).eq(storeType))
        }

        if (null != sortType) {
            if (sortType == MarketAtomSortTypeEnum.DOWNLOAD_COUNT && score == null) {
                val tas = TStoreStatisticsTotal.T_STORE_STATISTICS_TOTAL.`as`("tas")
                val t = dslContext.select(tas.STORE_CODE, tas.DOWNLOADS.`as`(MarketAtomSortTypeEnum.DOWNLOAD_COUNT.name)).from(tas).where(tas.STORE_TYPE.eq(storeType)).asTable("t")
                baseStep.leftJoin(t).on(ta.SERVICE_CODE.eq(t.field("STORE_CODE", String::class.java)))
            }

            val realSortType = if (sortType == MarketAtomSortTypeEnum.DOWNLOAD_COUNT) {
                DSL.field(sortType.name)
            } else {
                ta.field(sortType.name)
            }

            if (desc != null && desc) {
                baseStep.where(conditions).orderBy(realSortType.desc())
            } else {
                baseStep.where(conditions).orderBy(realSortType.asc())
            }
        } else {
            baseStep.where(conditions)
        }
        return if (null != page && null != pageSize) {
            baseStep.limit((page - 1) * pageSize, pageSize).fetch()
        } else {
            baseStep.fetch()
        }
    }

    fun countReleaseAtomByCode(dslContext: DSLContext, atomCode: String): Int {
        with(TAtom.T_ATOM) {
            return dslContext.selectCount().from(this).where(ATOM_CODE.eq(atomCode).and(ATOM_STATUS.eq(AtomStatusEnum.RELEASED.status.toByte()))).fetchOne(0, Int::class.java)
        }
    }

    private fun generateGetMyAtomConditions(a: TAtom, userId: String, b: TStoreMember, atomName: String?): MutableList<Condition> {
        val conditions = mutableListOf<Condition>()
        conditions.add(a.DELETE_FLAG.eq(false)) // 只查没有被删除的插件
        conditions.add(b.USERNAME.eq(userId))
        conditions.add(b.STORE_TYPE.eq(StoreTypeEnum.ATOM.type.toByte()))
        if (null != atomName) {
            conditions.add(a.NAME.contains(atomName))
        }
        return conditions
    }

    fun countMyAtoms(
        dslContext: DSLContext,
        userId: String,
        atomName: String?
    ): Int {
        val a = TAtom.T_ATOM.`as`("a")
        val b = TStoreMember.T_STORE_MEMBER.`as`("b")
        val conditions = generateGetMyAtomConditions(a, userId, b, atomName)
        return dslContext.select(a.ATOM_CODE.countDistinct())
            .from(a)
            .leftJoin(b)
            .on(a.ATOM_CODE.eq(b.STORE_CODE))
            .where(conditions)
            .fetchOne(0, Int::class.java)
    }

    fun getMyAtoms(
        dslContext: DSLContext,
        userId: String,
        atomName: String?,
        page: Int?,
        pageSize: Int?
    ): Result<out Record>? {
        val a = TAtom.T_ATOM.`as`("a")
        val b = TStoreMember.T_STORE_MEMBER.`as`("b")
        val d = TAtomEnvInfo.T_ATOM_ENV_INFO.`as`("d")
        val t = dslContext.select(a.ATOM_CODE.`as`("atomCode"), a.CREATE_TIME.max().`as`("createTime")).from(a).groupBy(a.ATOM_CODE) // 查找每组atomCode最新的记录
        val conditions = generateGetMyAtomConditions(a, userId, b, atomName)
        val baseStep = dslContext.select(
            a.ID.`as`("atomId"),
            a.ATOM_CODE.`as`("atomCode"),
            a.NAME.`as`("name"),
            a.CATEGROY.`as`("category"),
            d.LANGUAGE.`as`("language"),
            a.LOGO_URL.`as`("logoUrl"),
            a.VERSION.`as`("version"),
            a.ATOM_STATUS.`as`("atomStatus"),
            a.CREATOR.`as`("creator"),
            a.CREATE_TIME.`as`("createTime"),
            a.MODIFIER.`as`("modifier"),
            a.UPDATE_TIME.`as`("updateTime")
        )
            .from(a)
            .join(t)
            .on(a.ATOM_CODE.eq(t.field("atomCode", String::class.java)).and(a.CREATE_TIME.eq(t.field("createTime", LocalDateTime::class.java))))
            .join(b)
            .on(a.ATOM_CODE.eq(b.STORE_CODE))
            .leftJoin(d)
            .on(a.ID.eq(d.ATOM_ID))
            .where(conditions)
            .groupBy(a.ATOM_CODE)
            .orderBy(a.UPDATE_TIME.desc())
        return if (null != page && null != pageSize) {
            baseStep.limit((page - 1) * pageSize, pageSize).fetch()
        } else {
            baseStep.fetch()
        }
    }

    fun addMarketAtom(
        dslContext: DSLContext,
        userId: String,
        id: String,
        repositoryHashId: String?,
        codeSrc: String?,
        docsLink: String,
        marketAtomCreateRequest: MarketAtomCreateRequest
    ) {
        with(TAtom.T_ATOM) {
            dslContext.insertInto(this,
                ID,
                NAME,
                ATOM_CODE,
                CLASS_TYPE,
                SERVICE_SCOPE,
                OS,
                CLASSIFY_ID,
                DOCS_LINK,
                ATOM_TYPE,
                ATOM_STATUS,
                VERSION,
                DEFAULT_FLAG,
                LATEST_FLAG,
                REPOSITORY_HASH_ID,
                CODE_SRC,
                DOCS_LINK,
                VISIBILITY_LEVEL,
                PRIVATE_REASON,
                PUBLISHER,
                CREATOR,
                MODIFIER
            )
                .values(
                    id,
                    marketAtomCreateRequest.name,
                    marketAtomCreateRequest.atomCode,
                    "",
                    "",
                    "",
                    "",
                    "",
                    AtomTypeEnum.THIRD_PARTY.type.toByte(),
                    AtomStatusEnum.INIT.status.toByte(),
                    "",
                    false,
                    true,
                    repositoryHashId,
                    codeSrc,
                    docsLink,
                    marketAtomCreateRequest.visibilityLevel?.level,
                    marketAtomCreateRequest.privateReason,
                    userId,
                    userId,
                    userId
                )
                .execute()
        }
    }

    /**
     * 设置见扩展查询条件
     */
    protected fun setExtServiceVisibleCondition(a: TExtensionService): MutableList<Condition> {
        val conditions = mutableListOf<Condition>()
        conditions.add(a.SERVICE_STATUS.eq(ExtServiceStatusEnum.RELEASED.status.toByte())) // 已发布的
        conditions.add(a.LATEST_FLAG.eq(true)) // 最新版本
        return conditions
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

    private fun formatConditions(
        serviceName: String?,
        classifyCode: String?,
        dslContext: DSLContext
    ): Pair<TExtensionService, MutableList<Condition>> {
        val ta = TExtensionService.T_EXTENSION_SERVICE.`as`("ta")
        val storeType = StoreTypeEnum.SERVICE.type.toByte()
        val conditions = setExtServiceVisibleCondition(ta)
        conditions.add(ta.DELETE_FLAG.eq(false)) // 只查没有被删除的插件
        if (!serviceName.isNullOrEmpty()) {
            conditions.add(ta.SERVICE_NAME.contains(serviceName))
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
}