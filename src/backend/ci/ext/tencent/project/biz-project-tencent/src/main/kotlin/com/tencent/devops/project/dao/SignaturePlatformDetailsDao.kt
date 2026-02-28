package com.tencent.devops.project.dao

import com.tencent.devops.common.db.utils.skipCheck
import com.tencent.devops.model.project.tables.TSignaturePlatformDetails
import com.tencent.devops.model.project.tables.records.TSignaturePlatformDetailsRecord
import com.tencent.devops.project.pojo.SignaturePlatformDetails
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class SignaturePlatformDetailsDao {
    fun createOrUpdate(
        dslContext: DSLContext,
        details: SignaturePlatformDetails
    ) {
        with(TSignaturePlatformDetails.T_SIGNATURE_PLATFORM_DETAILS) {
            dslContext.insertInto(
                this,
                PLATFORM,
                PLATFORM_SECRET,
                PLATFORM_NAME,
                URL,
                INFORMATION_CN,
                INFORMATION_EN,
                PROJECT_IDS
            ).values(
                details.platform,
                details.platformSecret,
                details.platformName,
                details.url,
                details.informationCn,
                details.informationEn,
                details.projectIds.joinToString(",")
            ).onDuplicateKeyUpdate()
                .set(PLATFORM_NAME, details.platformName)
                .set(URL, details.url)
                .set(INFORMATION_CN, details.informationCn)
                .set(INFORMATION_EN, details.informationEn)
                .set(PROJECT_IDS, details.projectIds.joinToString(","))
                .set(UPDATE_TIME, LocalDateTime.now())
                .execute()
        }
    }

    fun delete(dslContext: DSLContext, platform: String) {
        return with(TSignaturePlatformDetails.T_SIGNATURE_PLATFORM_DETAILS) {
            dslContext.deleteFrom(this)
                .where(PLATFORM.eq(platform))
                .execute()
        }
    }

    fun get(dslContext: DSLContext, platform: String): SignaturePlatformDetails? {
        return with(TSignaturePlatformDetails.T_SIGNATURE_PLATFORM_DETAILS) {
            dslContext.selectFrom(this)
                .where(PLATFORM.eq(platform))
                .fetchOne()?.let { convert(it) }
        }
    }

    fun list(dslContext: DSLContext): List<SignaturePlatformDetails> {
        return with(TSignaturePlatformDetails.T_SIGNATURE_PLATFORM_DETAILS) {
            dslContext.selectFrom(this)
                .orderBy(CREATE_TIME.desc())
                .skipCheck()
                .fetch().map { convert(it) }
        }
    }

    fun updateInformation(
        dslContext: DSLContext,
        platform: String,
        platformName: String?,
        informationCn: String?,
        informationEn: String?
    ): Int {
        return with(TSignaturePlatformDetails.T_SIGNATURE_PLATFORM_DETAILS) {
            val updateStep = dslContext.update(this)
                .set(UPDATE_TIME, LocalDateTime.now())
            platformName?.let { updateStep.set(PLATFORM_NAME, it) }
            informationCn?.let { updateStep.set(INFORMATION_CN, it) }
            informationEn?.let { updateStep.set(INFORMATION_EN, it) }
            updateStep.where(PLATFORM.eq(platform)).execute()
        }
    }

    private fun convert(record: TSignaturePlatformDetailsRecord): SignaturePlatformDetails {
        return SignaturePlatformDetails(
            platform = record.platform,
            platformSecret = record.platformSecret,
            platformName = record.platformName,
            url = record.url,
            informationCn = record.informationCn,
            informationEn = record.informationEn,
            projectIds = record.projectIds.split(",")
        )
    }
}
