package com.tencent.devops.store.common.dao

import com.tencent.devops.model.store.tables.TAtom
import com.tencent.devops.model.store.tables.TAtomVersionLog
import com.tencent.devops.model.store.tables.TExtensionService
import com.tencent.devops.model.store.tables.TExtensionServiceVersionLog
import com.tencent.devops.model.store.tables.TIdeAtom
import com.tencent.devops.model.store.tables.TIdeAtomVersionLog
import com.tencent.devops.model.store.tables.TImage
import com.tencent.devops.model.store.tables.TImageVersionLog
import com.tencent.devops.model.store.tables.TStoreRelease
import com.tencent.devops.model.store.tables.TTemplate
import com.tencent.devops.model.store.tables.records.TStoreReleaseRecord
import com.tencent.devops.store.pojo.atom.enums.AtomStatusEnum
import com.tencent.devops.store.pojo.common.KEY_CREATE_TIME
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.extservice.enums.ExtServiceStatusEnum
import com.tencent.devops.store.pojo.ideatom.enums.IdeAtomStatusEnum
import com.tencent.devops.store.pojo.image.enums.ImageStatusEnum
import com.tencent.devops.store.pojo.template.enums.TemplateStatusEnum
import org.jooq.DSLContext
import org.jooq.Record2
import org.jooq.Result
import org.jooq.impl.DSL.min
import org.springframework.stereotype.Repository
import java.time.LocalDateTime


@Repository
class TxUserStorePublishersDao {

    companion object {
        private const val KEY_TEMPLATE_CODE = "templateCode"
        private const val KEY_ATOM_CODE = "atomCode"
        private const val KEY_IMAGE_CODE = "imageCode"
        private const val KEY_IDEA_CODE = "ideaCode"
        private const val KEY_EXTENSION_SERVICE_CODE = "extensionServiceCode"
    }

    fun listByAtomCode(dslContext: DSLContext, offset: Int, batchSize: Int): Result<Record2<String, String>>? {
        val tAtom = TAtom.T_ATOM


        val conditions = mutableListOf(
            tAtom.ATOM_STATUS.`in`(
                AtomStatusEnum.RELEASED.status.toByte(),
                AtomStatusEnum.UNDERCARRIAGED.status.toByte(),
                AtomStatusEnum.UNDERCARRIAGING.status.toByte()
            )
        )
        val t = dslContext.select(tAtom.ATOM_CODE.`as`(KEY_ATOM_CODE), min(tAtom.CREATE_TIME).`as`(KEY_CREATE_TIME))
            .from(tAtom)
            .where(conditions)
            .groupBy(tAtom.ATOM_CODE)

        val baseSelect = dslContext.select(
            tAtom.ID,
            tAtom.ATOM_CODE,
        )

        val baseStep = baseSelect.from(tAtom)
            .join(t)
            .on(
                tAtom.ATOM_CODE.eq(t.field(KEY_ATOM_CODE, String::class.java)).and(
                    tAtom.CREATE_TIME.eq(
                        t.field(
                            KEY_CREATE_TIME,
                            LocalDateTime::class.java
                        )
                    )
                )
            )
            .where(conditions).limit(offset, batchSize)

        return baseStep.fetch()

    }


    fun countByAtomCode(dslContext: DSLContext): Int {
        val tAtom = TAtom.T_ATOM

        val conditions = mutableListOf(
            tAtom.ATOM_STATUS.`in`(
                AtomStatusEnum.RELEASED.status.toByte(),
                AtomStatusEnum.UNDERCARRIAGED.status.toByte(),
                AtomStatusEnum.UNDERCARRIAGING.status.toByte()
            )
        )

        // 统计每个 ATOM_CODE 的数量
        val countQuery = dslContext.selectCount()
            .from(
                dslContext.select(tAtom.ATOM_CODE)
                    .from(tAtom)
                    .where(conditions)
                    .groupBy(tAtom.ATOM_CODE)
            )

        // 执行查询并返回结果
        return countQuery.fetchOne(0, Int::class.java) ?: 0
    }


    fun listByTemplateCode(dslContext: DSLContext, offset: Int, batchSize: Int): Result<Record2<String, String>>? {
        val tTemplate = TTemplate.T_TEMPLATE


        val conditions = mutableListOf(
            tTemplate.TEMPLATE_STATUS.`in`(
                TemplateStatusEnum.RELEASED.status.toByte(),
                TemplateStatusEnum.UNDERCARRIAGED.status.toByte(),
            )
        )
        val t = dslContext.select(
            tTemplate.TEMPLATE_CODE.`as`(KEY_TEMPLATE_CODE),
            min(tTemplate.CREATE_TIME).`as`(KEY_CREATE_TIME)
        )
            .from(tTemplate)
            .where(conditions)
            .groupBy(tTemplate.TEMPLATE_CODE)

        val baseSelect = dslContext.select(
            tTemplate.TEMPLATE_CODE,
            tTemplate.CREATOR
        )

        val baseStep = baseSelect.from(tTemplate)
            .join(t)
            .on(
                tTemplate.TEMPLATE_CODE.eq(t.field(KEY_TEMPLATE_CODE, String::class.java)).and(
                    tTemplate.CREATE_TIME.eq(
                        t.field(
                            KEY_CREATE_TIME,
                            LocalDateTime::class.java
                        )
                    )
                )
            )
            .where(conditions).limit(offset, batchSize)

        return baseStep.fetch()

    }


    fun countByTemplateCode(dslContext: DSLContext): Int {
        val tTemplate = TTemplate.T_TEMPLATE


        val conditions = mutableListOf(
            tTemplate.TEMPLATE_STATUS.`in`(
                TemplateStatusEnum.RELEASED.status.toByte(),
                TemplateStatusEnum.UNDERCARRIAGED.status.toByte(),
            )
        )
        val countQuery = dslContext.selectCount()
            .from(
                dslContext.select(tTemplate.TEMPLATE_CODE)
                    .from(tTemplate)
                    .where(conditions)
                    .groupBy(tTemplate.TEMPLATE_CODE)
            )


        // 执行查询并返回结果
        return countQuery.fetchOne(0, Int::class.java) ?: 0
    }


    fun listByImageCode(dslContext: DSLContext, offset: Int, batchSize: Int): Result<Record2<String, String>>? {
        val tImage = TImage.T_IMAGE


        val conditions = mutableListOf(
            tImage.IMAGE_STATUS.`in`(
                ImageStatusEnum.RELEASED.status.toByte(),
                ImageStatusEnum.UNDERCARRIAGED.status.toByte(),
                ImageStatusEnum.UNDERCARRIAGING.status.toByte()
            )
        )
        val t = dslContext.select(tImage.IMAGE_CODE.`as`(KEY_IMAGE_CODE), min(tImage.CREATE_TIME).`as`(KEY_CREATE_TIME))
            .from(tImage)
            .where(conditions)
            .groupBy(tImage.IMAGE_CODE)

        val baseSelect = dslContext.select(
            tImage.ID,
            tImage.IMAGE_CODE,
        )

        val baseStep = baseSelect.from(tImage)
            .join(t)
            .on(
                tImage.IMAGE_CODE.eq(t.field(KEY_IMAGE_CODE, String::class.java)).and(
                    tImage.CREATE_TIME.eq(
                        t.field(
                            KEY_CREATE_TIME,
                            LocalDateTime::class.java
                        )
                    )
                )
            )
            .where(conditions).limit(offset, batchSize)

        return baseStep.fetch()

    }


    fun countByImageCode(dslContext: DSLContext): Int {
        val tImage = TImage.T_IMAGE


        val conditions = mutableListOf(
            tImage.IMAGE_STATUS.`in`(
                ImageStatusEnum.RELEASED.status.toByte(),
                ImageStatusEnum.UNDERCARRIAGED.status.toByte(),
                ImageStatusEnum.UNDERCARRIAGING.status.toByte()
            )
        )
        val countQuery = dslContext.selectCount()
            .from(
                dslContext.select(tImage.IMAGE_CODE)
                    .from(tImage)
                    .where(conditions)
                    .groupBy(tImage.IMAGE_CODE)
            )


        // 执行查询并返回结果
        return countQuery.fetchOne(0, Int::class.java) ?: 0
    }


    fun listExtServiceCode(dslContext: DSLContext, offset: Int, batchSize: Int): Result<Record2<String, String>>? {
        val tExtensionService = TExtensionService.T_EXTENSION_SERVICE


        val conditions = mutableListOf(
            tExtensionService.SERVICE_STATUS.`in`(
                ExtServiceStatusEnum.RELEASED.status.toByte(),
                ExtServiceStatusEnum.UNDERCARRIAGED.status.toByte(),
                ExtServiceStatusEnum.UNDERCARRIAGING.status.toByte()
            )
        )
        val t = dslContext.select(
            tExtensionService.SERVICE_CODE.`as`(KEY_EXTENSION_SERVICE_CODE),
            min(tExtensionService.CREATE_TIME).`as`(KEY_CREATE_TIME)
        )
            .from(tExtensionService)
            .where(conditions)
            .groupBy(tExtensionService.SERVICE_CODE)

        val baseSelect = dslContext.select(
            tExtensionService.ID,
            tExtensionService.SERVICE_CODE,
        )

        val baseStep = baseSelect.from(tExtensionService)
            .join(t)
            .on(
                tExtensionService.SERVICE_CODE.eq(t.field(KEY_EXTENSION_SERVICE_CODE, String::class.java)).and(
                    tExtensionService.CREATE_TIME.eq(
                        t.field(
                            KEY_CREATE_TIME,
                            LocalDateTime::class.java
                        )
                    )
                )
            )
            .where(conditions).limit(offset, batchSize)

        return baseStep.fetch()

    }

    fun countByExtService(dslContext: DSLContext): Int {
        val tExtensionService = TExtensionService.T_EXTENSION_SERVICE


        val conditions = mutableListOf(
            tExtensionService.SERVICE_STATUS.`in`(
                ExtServiceStatusEnum.RELEASED.status.toByte(),
                ExtServiceStatusEnum.UNDERCARRIAGED.status.toByte(),
                ExtServiceStatusEnum.UNDERCARRIAGING.status.toByte()
            )
        )
        val countQuery = dslContext.selectCount()
            .from(
                dslContext.select(tExtensionService.SERVICE_CODE)
                    .from(tExtensionService)
                    .where(conditions)
                    .groupBy(tExtensionService.SERVICE_CODE)
            )


        // 执行查询并返回结果
        return countQuery.fetchOne(0, Int::class.java) ?: 0
    }

    fun listByIdeAtomCode(dslContext: DSLContext, offset: Int, batchSize: Int): Result<Record2<String, String>>? {
        val tIdeAtom = TIdeAtom.T_IDE_ATOM


        val conditions = mutableListOf(
            tIdeAtom.ATOM_STATUS.`in`(
                IdeAtomStatusEnum.RELEASED.status.toByte(),
                IdeAtomStatusEnum.UNDERCARRIAGED.status.toByte(),
            )
        )
        val t =
            dslContext.select(tIdeAtom.ATOM_CODE.`as`(KEY_IDEA_CODE), min(tIdeAtom.CREATE_TIME).`as`(KEY_CREATE_TIME))
                .from(tIdeAtom)
                .where(conditions)
                .groupBy(tIdeAtom.ATOM_CODE)

        val baseSelect = dslContext.select(
            tIdeAtom.ID,
            tIdeAtom.ATOM_CODE,
        )

        val baseStep = baseSelect.from(tIdeAtom)
            .join(t)
            .on(
                tIdeAtom.ATOM_CODE.eq(t.field(KEY_IDEA_CODE, String::class.java)).and(
                    tIdeAtom.CREATE_TIME.eq(
                        t.field(
                            KEY_CREATE_TIME,
                            LocalDateTime::class.java
                        )
                    )
                )
            )
            .where(conditions).limit(offset, batchSize)

        return baseStep.fetch()

    }


    fun countByIdeAtomCode(dslContext: DSLContext): Int {
        val tIdeAtom = TIdeAtom.T_IDE_ATOM


        val conditions = mutableListOf(
            tIdeAtom.ATOM_STATUS.`in`(
                IdeAtomStatusEnum.RELEASED.status.toByte(),
                IdeAtomStatusEnum.UNDERCARRIAGED.status.toByte(),
            )
        )
        val countQuery = dslContext.selectCount()
            .from(
                dslContext.select(tIdeAtom.ATOM_CODE)
                    .from(tIdeAtom)
                    .where(conditions)
                    .groupBy(tIdeAtom.ATOM_CODE)
            )


        // 执行查询并返回结果
        return countQuery.fetchOne(0, Int::class.java) ?: 0
    }


    fun queryModifierByAtomId(
        dslContext: DSLContext,
        componentIds: List<String>,
        storeTypeEnum: StoreTypeEnum
    ): Result<Record2<String, String>>? {

        val tAtomVersionLog = TAtomVersionLog.T_ATOM_VERSION_LOG

        val tImageVersionLog = TImageVersionLog.T_IMAGE_VERSION_LOG

        val tIdeAtomVersionLog = TIdeAtomVersionLog.T_IDE_ATOM_VERSION_LOG

        val TExtensionServiceVersionLog = TExtensionServiceVersionLog.T_EXTENSION_SERVICE_VERSION_LOG


        when (storeTypeEnum) {
            StoreTypeEnum.ATOM -> {
                return dslContext.select(tAtomVersionLog.MODIFIER, tAtomVersionLog.ATOM_ID).from(tAtomVersionLog)
                    .where(tAtomVersionLog.ATOM_ID.`in`(componentIds)).fetch()
            }

            StoreTypeEnum.IMAGE -> {
                return dslContext.select(tImageVersionLog.MODIFIER, tImageVersionLog.IMAGE_ID).from(tImageVersionLog)
                    .where(tImageVersionLog.IMAGE_ID.`in`(componentIds)).fetch()
            }

            StoreTypeEnum.IDE_ATOM -> {
                return dslContext.select(tIdeAtomVersionLog.MODIFIER, tIdeAtomVersionLog.ATOM_ID)
                    .from(tIdeAtomVersionLog)
                    .where(tIdeAtomVersionLog.ATOM_ID.`in`(componentIds)).fetch()
            }

            StoreTypeEnum.SERVICE -> {
                return dslContext.select(TExtensionServiceVersionLog.MODIFIER, TExtensionServiceVersionLog.SERVICE_ID)
                    .from(TExtensionServiceVersionLog)
                    .where(TExtensionServiceVersionLog.SERVICE_ID.`in`(componentIds)).fetch()
            }

            else -> {
                return null
            }

        }


    }


    fun selectStoreReleaseInfoByStoreCodes(
        dslContext: DSLContext,
        storeCodes: List<String>,
        storeType: Byte
    ): List<TStoreReleaseRecord>? {
        return with(TStoreRelease.T_STORE_RELEASE) {
            dslContext.selectFrom(this)
                .where(STORE_CODE.`in`(storeCodes))
                .and(STORE_TYPE.eq(storeType)).and(FIRST_PUB_TIME.isNotNull).and(FIRST_PUB_CREATOR.isNotNull)
                .fetch()
        }

    }


    fun updateComponentFirstPublisher(
        dslContext: DSLContext,
        storeCode: String,
        storeType: Byte,
        firstPublisher: String,
        userId: String?
    ) {
        with(TStoreRelease.T_STORE_RELEASE) {
            val updateQuery = dslContext.update(this)
                .set(FIRST_PUB_CREATOR, firstPublisher)

            if (userId != null) {
                updateQuery.set(MODIFIER, userId)
            }

            updateQuery.where(STORE_CODE.eq(storeCode))
                .and(STORE_TYPE.eq(storeType))

            updateQuery.execute()
        }
    }

}