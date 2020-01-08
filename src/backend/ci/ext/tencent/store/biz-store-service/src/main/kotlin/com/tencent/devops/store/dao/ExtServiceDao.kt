package com.tencent.devops.store.dao

import com.tencent.devops.model.store.tables.TClassify
import com.tencent.devops.model.store.tables.TExtensionService
import com.tencent.devops.model.store.tables.records.TExtensionServiceRecord
import com.tencent.devops.store.pojo.ExtServiceCreateInfo
import com.tencent.devops.store.pojo.ExtServiceUpdateInfo
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class ExtServiceDao {

    fun createExtService(
        dslContext: DSLContext,
        userId: String,
        id: String,
        classType: String,
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
                    extServiceCreateInfo.modifierUser,
                    LocalDateTime.now(),
                    LocalDateTime.now()
                )
                .execute()
        }
    }

    fun updateExtServiceBaseInfo(
        dslContext: DSLContext,
        userId: String,
        atomIdList: List<String>,
        extServiceUpdateInfo: ExtServiceUpdateInfo
    ) {
        with(TExtensionService.T_EXTENSION_SERVICE) {
            val baseStep = dslContext.update(this)
            val serviceName = extServiceUpdateInfo.serviceName
            if (null != serviceName) {
                baseStep.set(SERVICE_NAME, serviceName)
            }
            val classifyCode = extServiceUpdateInfo.category
            if (null != classifyCode) {
                val a = TClassify.T_CLASSIFY.`as`("a")
                val classifyId = dslContext.select(a.ID)
                    .from(a)
                    .where(a.CLASSIFY_CODE.eq(classifyCode).and(a.TYPE.eq(0)))
                    .fetchOne(0, String::class.java)
                baseStep.set(CLASSIFY_ID, classifyId)
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
                .where(ID.`in`(atomIdList))
                .execute()
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
}