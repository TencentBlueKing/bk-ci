/*
 * Tencent is pleased to support the open source community by making BK-REPO 蓝鲸制品库 available.
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
package com.tencent.devops.store.dao.image

import com.tencent.devops.common.pipeline.type.docker.ImageType
import com.tencent.devops.model.store.tables.TCategory
import com.tencent.devops.model.store.tables.TClassify
import com.tencent.devops.model.store.tables.TImage
import com.tencent.devops.model.store.tables.TImageCategoryRel
import com.tencent.devops.model.store.tables.TImageFeature
import com.tencent.devops.model.store.tables.TImageLabelRel
import com.tencent.devops.model.store.tables.TImageVersionLog
import com.tencent.devops.model.store.tables.TLabel
import com.tencent.devops.model.store.tables.TStoreStatisticsTotal
import com.tencent.devops.model.store.tables.records.TImageRecord
import com.tencent.devops.store.dao.image.Constants.KEY_CLASSIFY_CODE
import com.tencent.devops.store.dao.image.Constants.KEY_CLASSIFY_ID
import com.tencent.devops.store.dao.image.Constants.KEY_CLASSIFY_NAME
import com.tencent.devops.store.dao.image.Constants.KEY_CREATE_TIME
import com.tencent.devops.store.dao.image.Constants.KEY_CREATOR
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_CODE
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_FEATURE_PUBLIC_FLAG
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_FEATURE_RECOMMEND_FLAG
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_FEATURE_WEIGHT
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_ID
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_LATEST_FLAG
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_LOGO_URL
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_NAME
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_SIZE
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_SOURCE_TYPE
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_STATUS
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_SUMMARY
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_VERSION
import com.tencent.devops.store.dao.image.Constants.KEY_MODIFIER
import com.tencent.devops.store.dao.image.Constants.KEY_PUBLISHER
import com.tencent.devops.store.dao.image.Constants.KEY_PUB_TIME
import com.tencent.devops.store.dao.image.Constants.KEY_UPDATE_TIME
import com.tencent.devops.store.dao.image.Constants.KEY_VERSION_LOG_CONTENT
import com.tencent.devops.store.exception.image.ClassifyNotExistException
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.image.request.ImageBaseInfoUpdateRequest
import com.tencent.devops.store.pojo.image.request.MarketImageRelRequest
import com.tencent.devops.store.pojo.image.request.MarketImageUpdateRequest
import com.tencent.devops.store.pojo.image.enums.ImageStatusEnum
import com.tencent.devops.store.pojo.image.enums.MarketImageSortTypeEnum
import com.tencent.devops.store.pojo.image.enums.OpImageSortTypeEnum
import com.tencent.devops.store.pojo.image.request.ImageStatusInfoUpdateRequest
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Record17
import org.jooq.Record2
import org.jooq.Result
import org.jooq.SelectHavingStep
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDateTime

@Repository
class MarketImageDao {
    /**
     * 镜像市场搜索结果 总数
     * 参与Join的表：TImage,TImageLabelRel,TImageLabelRel结果临时表
     */
    fun count(
        dslContext: DSLContext,
        imageName: String?,
        classifyCodeList: List<String>?,
        labelCodeList: List<String>?,
        score: Int?,
        imageSourceType: ImageType?
    ): Int {
        val (tImage, tImageFeature, conditions) = formatConditions(
            imageName = imageName,
            imageSourceType = imageSourceType,
            classifyCodeList = classifyCodeList,
            dslContext = dslContext
        )

        val baseStep = dslContext.select(tImage.ID.countDistinct()).from(tImage)

        // 根据功能标签筛选
        if (labelCodeList != null && labelCodeList.isNotEmpty()) {
            val c = TLabel.T_LABEL.`as`("c")
            val labelIdList = dslContext.select(c.ID)
                .from(c)
                .where(c.LABEL_CODE.`in`(labelCodeList)).and(c.TYPE.eq(StoreTypeEnum.IMAGE.type.toByte()))
                .fetch().map { it["ID"] as String }
            val ttlr = TImageLabelRel.T_IMAGE_LABEL_REL.`as`("tilr")
            baseStep.leftJoin(ttlr).on(tImage.ID.eq(ttlr.IMAGE_ID))
            conditions.add(ttlr.LABEL_ID.`in`(labelIdList))
        }
        if (score != null) {
            val tas = TStoreStatisticsTotal.T_STORE_STATISTICS_TOTAL.`as`("tas")
            val t = dslContext.select(
                tas.STORE_CODE,
                tas.DOWNLOADS.`as`(MarketImageSortTypeEnum.DOWNLOAD_COUNT.name),
                tas.SCORE_AVERAGE
            ).from(tas)
                // 增加type为镜像的条件
                .where(tas.STORE_TYPE.eq(StoreTypeEnum.IMAGE.type.toByte()))
                .asTable("t")
            baseStep.leftJoin(t).on(tImage.IMAGE_CODE.eq(t.field("STORE_CODE", String::class.java)))
            conditions.add(t.field("SCORE_AVERAGE", BigDecimal::class.java).ge(BigDecimal.valueOf(score.toLong())))
        }
        return baseStep.where(conditions).fetchOne(0, Int::class.java)
    }

    fun countByIdAndCode(dslContext: DSLContext, imageId: String, imageCode: String): Int {
        with(TImage.T_IMAGE) {
            return dslContext.selectCount().from(this).where(ID.eq(imageId).and(IMAGE_CODE.eq(imageCode)))
                .fetchOne(0, Int::class.java)
        }
    }

    private fun formatConditions(
        imageName: String?,
        imageSourceType: ImageType?,
        classifyCodeList: List<String>?,
        dslContext: DSLContext
    ): Triple<TImage, TImageFeature, MutableList<Condition>> {
        val tImage = TImage.T_IMAGE.`as`("tImage")
        val tImageFeature = TImageFeature.T_IMAGE_FEATURE.`as`("tImageFeature")

        val conditions = mutableListOf<Condition>()
        // 隐含条件
        conditions.add(tImage.IMAGE_STATUS.eq(ImageStatusEnum.RELEASED.status.toByte())) // 已发布的
        conditions.add(tImage.LATEST_FLAG.eq(true)) // 最新版本
        if (!imageName.isNullOrEmpty()) {
            conditions.add(tImage.IMAGE_NAME.contains(imageName))
        }
        if (imageSourceType != null) {
            conditions.add(tImage.IMAGE_SOURCE_TYPE.eq(imageSourceType.type))
        }
        if (null != classifyCodeList && classifyCodeList.isNotEmpty()) {
            val a = TClassify.T_CLASSIFY.`as`("a")
            val classifyId = dslContext.select(a.ID)
                .from(a)
                .where(a.CLASSIFY_CODE.`in`(classifyCodeList).and(a.TYPE.eq(StoreTypeEnum.IMAGE.type.toByte())))
                .fetchOne(0, String::class.java)
            conditions.add(tImage.CLASSIFY_ID.eq(classifyId))
        }
        return Triple(tImage, tImageFeature, conditions)
    }

    /**
     * 镜像市场搜索结果列表
     * 参与Join的表：
     * TImage,
     * TImageLabelRel，索引列join
     * TStoreStatisticsTotal临时表，结果集较小
     */
    fun list(
        dslContext: DSLContext,
        // 镜像名称，模糊匹配
        imageName: String?,
        // 分类代码，精确匹配
        classifyCode: List<String>?,
        // 标签，精确匹配
        labelCodeList: List<String>?,
        // 评分大于等于score的镜像
        score: Int?,
        // 来源，精确匹配
        imageSourceType: ImageType?,
        // 排序字段
        sortType: MarketImageSortTypeEnum?,
        // 是否降序
        desc: Boolean?,
        page: Int?,
        pageSize: Int?
    ): Result<Record17<String, String, String, String, String, String, String, String, String, Boolean, Boolean, String, LocalDateTime, String, String, LocalDateTime, LocalDateTime>>? {
        val (tImage, tImageFeature, conditions) = formatConditions(imageName, imageSourceType, classifyCode, dslContext)

        val baseStep = dslContext.select(
            tImage.ID.`as`(KEY_IMAGE_ID),
            tImage.IMAGE_CODE.`as`(KEY_IMAGE_CODE),
            tImage.IMAGE_NAME.`as`(KEY_IMAGE_NAME),
            tImage.IMAGE_SOURCE_TYPE.`as`(KEY_IMAGE_SOURCE_TYPE),
            tImage.IMAGE_SIZE.`as`(KEY_IMAGE_SIZE),
            tImage.CLASSIFY_ID.`as`(KEY_CLASSIFY_ID),
            tImage.LOGO_URL.`as`(KEY_IMAGE_LOGO_URL),
            tImage.VERSION.`as`(KEY_IMAGE_VERSION),
            tImage.SUMMARY.`as`(KEY_IMAGE_SUMMARY),
            tImageFeature.PUBLIC_FLAG.`as`(KEY_IMAGE_FEATURE_PUBLIC_FLAG),
            tImageFeature.RECOMMEND_FLAG.`as`(KEY_IMAGE_FEATURE_RECOMMEND_FLAG),
            tImage.PUBLISHER.`as`(KEY_PUBLISHER),
            tImage.PUB_TIME.`as`(KEY_PUB_TIME),
            tImage.CREATOR.`as`(KEY_CREATOR),
            tImage.MODIFIER.`as`(KEY_MODIFIER),
            tImage.CREATE_TIME.`as`(KEY_CREATE_TIME),
            tImage.UPDATE_TIME.`as`(KEY_UPDATE_TIME)
        ).from(tImage).leftJoin(tImageFeature).on(tImage.IMAGE_CODE.eq(tImageFeature.IMAGE_CODE))

        // 根据功能标签筛选
        if (labelCodeList != null && labelCodeList.isNotEmpty()) {
            val c = TLabel.T_LABEL.`as`("c")
            val labelIdList = dslContext.select(c.ID)
                .from(c)
                .where(c.LABEL_CODE.`in`(labelCodeList)).and(c.TYPE.eq(StoreTypeEnum.IMAGE.type.toByte()))
                .fetch().map { it["ID"] as String }
            val tilr = TImageLabelRel.T_IMAGE_LABEL_REL.`as`("tilr")
            baseStep.leftJoin(tilr).on(tImage.ID.eq(tilr.IMAGE_ID))
            conditions.add(tilr.LABEL_ID.`in`(labelIdList))
        }
        if (score != null) {
            val tas = TStoreStatisticsTotal.T_STORE_STATISTICS_TOTAL.`as`("tas")
            val t = dslContext.select(
                tas.STORE_CODE,
                tas.DOWNLOADS.`as`(MarketImageSortTypeEnum.DOWNLOAD_COUNT.name),
                tas.SCORE_AVERAGE
            ).from(tas).asTable("t")
            baseStep.leftJoin(t).on(tImage.IMAGE_CODE.eq(t.field("STORE_CODE", String::class.java)))
            conditions.add(t.field("SCORE_AVERAGE", BigDecimal::class.java).ge(BigDecimal.valueOf(score.toLong())))
        }

        if (null != sortType) {
            // 按下载量排序
            if (sortType == MarketImageSortTypeEnum.DOWNLOAD_COUNT && score == null) {
                val tas = TStoreStatisticsTotal.T_STORE_STATISTICS_TOTAL.`as`("tas")
                val t =
                    dslContext.select(tas.STORE_CODE, tas.DOWNLOADS.`as`(MarketImageSortTypeEnum.DOWNLOAD_COUNT.name))
                        // 增加type为镜像的条件
                        .from(tas).where(tas.STORE_TYPE.eq(StoreTypeEnum.IMAGE.type.toByte())).asTable("t")
                baseStep.leftJoin(t).on(tImage.IMAGE_CODE.eq(t.field("STORE_CODE", String::class.java)))
            }

            // 排序字段
            val realSortType = if (sortType == MarketImageSortTypeEnum.DOWNLOAD_COUNT) {
                DSL.field(MarketImageSortTypeEnum.getSortType(sortType.name))
            } else {
                tImage.field(MarketImageSortTypeEnum.getSortType(sortType.name))
            }

            // 排序
            if (desc != null && desc) {
                baseStep.where(conditions).orderBy(realSortType.desc())
            } else {
                baseStep.where(conditions).orderBy(realSortType.asc())
            }
        } else {
            baseStep.where(conditions)
        }
        // 分页
        return if (page != null && page > 0 && pageSize != null && pageSize > 0) {
            baseStep.limit((page - 1) * pageSize, pageSize).fetch()
        } else {
            baseStep.fetch()
        }
    }

    fun addMarketImage(
        dslContext: DSLContext,
        userId: String,
        imageId: String,
        imageCode: String,
        marketImageRelRequest: MarketImageRelRequest
    ) {
        with(TImage.T_IMAGE) {
            dslContext.insertInto(
                this,
                ID,
                IMAGE_NAME,
                IMAGE_CODE,
                CLASSIFY_ID,
                VERSION,
                IMAGE_SOURCE_TYPE,
                IMAGE_REPO_URL,
                IMAGE_REPO_NAME,
                TICKET_ID,
                LATEST_FLAG,
                IMAGE_STATUS,
                IMAGE_SIZE,
                IMAGE_TAG,
                PUBLISHER,
                CREATOR,
                MODIFIER
            )
                .values(
                    imageId,
                    marketImageRelRequest.imageName,
                    imageCode,
                    "",
                    "",
                    marketImageRelRequest.imageSourceType.type,
                    "",
                    "",
                    marketImageRelRequest.ticketId,
                    true,
                    ImageStatusEnum.INIT.status.toByte(),
                    "",
                    "",
                    "",
                    userId,
                    userId
                )
                .execute()
        }
    }

    fun updateMarketImage(
        dslContext: DSLContext,
        userId: String,
        imageId: String,
        imageSize: String,
        marketImageUpdateRequest: MarketImageUpdateRequest
    ) {
        val a = TClassify.T_CLASSIFY.`as`("a")
        val classifyId = dslContext.select(a.ID).from(a)
            .where(a.CLASSIFY_CODE.eq(marketImageUpdateRequest.classifyCode).and(a.TYPE.eq(StoreTypeEnum.IMAGE.type.toByte())))
            .fetchOne(0, String::class.java)
            ?: throw ClassifyNotExistException("classifyCode=${marketImageUpdateRequest.classifyCode}")
        with(TImage.T_IMAGE) {
            dslContext.update(this)
                .set(IMAGE_NAME, marketImageUpdateRequest.imageName)
                .set(CLASSIFY_ID, classifyId)
                .set(LOGO_URL, marketImageUpdateRequest.logoUrl)
                .set(IMAGE_STATUS, ImageStatusEnum.COMMITTING.status.toByte())
                .set(IMAGE_SIZE, imageSize)
                .set(IMAGE_SOURCE_TYPE, marketImageUpdateRequest.imageSourceType.type)
                .set(IMAGE_REPO_URL, marketImageUpdateRequest.imageRepoUrl)
                .set(IMAGE_REPO_NAME, marketImageUpdateRequest.imageRepoName)
                .set(IMAGE_TAG, marketImageUpdateRequest.imageTag)
                .set(SUMMARY, marketImageUpdateRequest.summary)
                .set(DESCRIPTION, marketImageUpdateRequest.description)
                .set(PUBLISHER, marketImageUpdateRequest.publisher)
                .set(VERSION, marketImageUpdateRequest.version)
                .set(UPDATE_TIME, LocalDateTime.now())
                .set(MODIFIER, userId)
                .where(ID.eq(imageId))
                .execute()
        }
    }

    fun upgradeMarketImage(
        dslContext: DSLContext,
        userId: String,
        imageId: String,
        imageSize: String,
        imageRecord: TImageRecord,
        marketImageUpdateRequest: MarketImageUpdateRequest
    ) {
        val a = TClassify.T_CLASSIFY.`as`("a")
        val classifyId = dslContext.select(a.ID).from(a)
            .where(a.CLASSIFY_CODE.eq(marketImageUpdateRequest.classifyCode).and(a.TYPE.eq(StoreTypeEnum.IMAGE.type.toByte())))
            .fetchOne(0, String::class.java)
        with(TImage.T_IMAGE) {
            dslContext.insertInto(
                this,
                ID,
                IMAGE_NAME,
                IMAGE_CODE,
                CLASSIFY_ID,
                VERSION,
                IMAGE_STATUS,
                IMAGE_SIZE,
                IMAGE_SOURCE_TYPE,
                IMAGE_REPO_URL,
                IMAGE_REPO_NAME,
                IMAGE_TAG,
                LOGO_URL,
                SUMMARY,
                DESCRIPTION,
                PUBLISHER,
                PUB_TIME,
                LATEST_FLAG,
                CREATOR,
                MODIFIER
            )
                .values(
                    imageId,
                    marketImageUpdateRequest.imageName,
                    imageRecord.imageCode,
                    classifyId,
                    marketImageUpdateRequest.version,
                    ImageStatusEnum.COMMITTING.status.toByte(),
                    imageSize,
                    marketImageUpdateRequest.imageSourceType.type,
                    marketImageUpdateRequest.imageRepoUrl,
                    marketImageUpdateRequest.imageRepoName,
                    marketImageUpdateRequest.imageTag,
                    marketImageUpdateRequest.logoUrl,
                    marketImageUpdateRequest.summary,
                    marketImageUpdateRequest.description,
                    marketImageUpdateRequest.publisher,
                    null,
                    false,
                    userId,
                    userId
                )
                .execute()
        }
    }

    fun getLatestImageByCode(dslContext: DSLContext, imageCode: String): TImageRecord? {
        return with(TImage.T_IMAGE) {
            dslContext.selectFrom(this)
                .where(IMAGE_CODE.eq(imageCode))
                .and(LATEST_FLAG.eq(true))
                .fetchOne()
        }
    }

    fun getNewestImageByCode(dslContext: DSLContext, imageCode: String): TImageRecord? {
        return with(TImage.T_IMAGE) {
            dslContext.selectFrom(this)
                .where(IMAGE_CODE.eq(imageCode))
                .orderBy(CREATE_TIME.desc())
                .limit(1)
                .fetchOne()
        }
    }

    fun getImagesByImageCode(dslContext: DSLContext, imageCode: String): Result<TImageRecord>? {
        return with(TImage.T_IMAGE) {
            dslContext.selectFrom(this)
                .where(IMAGE_CODE.eq(imageCode))
                .orderBy(CREATE_TIME.desc())
                .fetch()
        }
    }

    fun countReleaseImageByCode(dslContext: DSLContext, imageCode: String): Int {
        with(TImage.T_IMAGE) {
            return dslContext.selectCount().from(this)
                .where(IMAGE_CODE.eq(imageCode).and(IMAGE_STATUS.eq(ImageStatusEnum.RELEASED.status.toByte())))
                .fetchOne(0, Int::class.java)
        }
    }

    fun updateImageStatusById(
        dslContext: DSLContext,
        imageId: String,
        imageStatus: Byte,
        userId: String,
        msg: String?
    ) {
        with(TImage.T_IMAGE) {
            val baseStep = dslContext.update(this)
                .set(IMAGE_STATUS, imageStatus)
            if (!msg.isNullOrEmpty()) {
                baseStep.set(IMAGE_STATUS_MSG, msg)
            }
            baseStep.set(MODIFIER, userId)
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(ID.eq(imageId))
                .execute()
        }
    }

    fun updateImageStatusByCode(
        dslContext: DSLContext,
        imageCode: String,
        latestFlag: Boolean,
        imageOldStatus: Byte,
        imageNewStatus: Byte,
        userId: String,
        msg: String?
    ) {
        with(TImage.T_IMAGE) {
            val baseStep = dslContext.update(this)
                .set(IMAGE_STATUS, imageNewStatus)
                .set(LATEST_FLAG, latestFlag)
            if (!msg.isNullOrEmpty()) {
                baseStep.set(IMAGE_STATUS_MSG, msg)
            }
            baseStep.set(MODIFIER, userId)
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(IMAGE_CODE.eq(imageCode))
                .and(IMAGE_STATUS.eq(imageOldStatus))
                .execute()
        }
    }

    /**
     * 根据imageCode删除所有镜像版本信息
     */
    fun delete(dslContext: DSLContext, imageCode: String): Int {
        with(TImage.T_IMAGE) {
            return dslContext.deleteFrom(this)
                .where(IMAGE_CODE.eq(imageCode))
                .execute()
        }
    }

    /**
     * 清空LATEST_FLAG
     */
    fun cleanLatestFlag(dslContext: DSLContext, imageCode: String) {
        with(TImage.T_IMAGE) {
            dslContext.update(this)
                .set(LATEST_FLAG, false)
                .where(IMAGE_CODE.eq(imageCode))
                .execute()
        }
    }

    /**
     * 更新状态等信息
     */
    fun updateImageStatusInfo(
        dslContext: DSLContext,
        imageId: String,
        imageStatus: Byte,
        imageStatusMsg: String,
        latestFlag: Boolean,
        pubTime: LocalDateTime?
    ) {
        with(TImage.T_IMAGE) {
            val baseStep = dslContext.update(this)
                .set(IMAGE_STATUS, imageStatus)
                .set(IMAGE_STATUS_MSG, imageStatusMsg)
                .set(LATEST_FLAG, latestFlag)
            if (null != PUB_TIME) {
                baseStep.set(PUB_TIME, pubTime)
            }
            baseStep.where(ID.eq(imageId))
                .execute()
        }
    }

    fun updateImageBaseInfo(
        dslContext: DSLContext,
        userId: String,
        imageId: String,
        imageBaseInfoUpdateRequest: ImageBaseInfoUpdateRequest
    ) {
        with(TImage.T_IMAGE) {
            val baseStep = dslContext.update(this)
            val imageName = imageBaseInfoUpdateRequest.imageName
            if (null != imageName) {
                baseStep.set(IMAGE_NAME, imageName)
            }
            val summary = imageBaseInfoUpdateRequest.summary
            if (null != summary) {
                baseStep.set(SUMMARY, summary)
            }
            val description = imageBaseInfoUpdateRequest.description
            if (null != description) {
                baseStep.set(DESCRIPTION, description)
            }
            val logoUrl = imageBaseInfoUpdateRequest.logoUrl
            if (null != logoUrl) {
                baseStep.set(LOGO_URL, logoUrl)
            }
            val publisher = imageBaseInfoUpdateRequest.publisher
            if (null != publisher) {
                baseStep.set(PUBLISHER, publisher)
            }
            val imageSize = imageBaseInfoUpdateRequest.imageSize
            if (null != imageSize) {
                baseStep.set(IMAGE_SIZE, imageSize)
            }
            baseStep.set(MODIFIER, userId)
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(ID.eq(imageId))
                .execute()
        }
    }

    fun countOpImages(
        dslContext: DSLContext,
        imageName: String?,
        imageType: ImageType?,
        classifyCode: String?,
        categoryCodeList: Set<String>?,
        labelCodeList: Set<String>?,
        processFlag: Boolean?
    ): Int {
        val tImage = TImage.T_IMAGE.`as`("tImage")
        val tClassify = TClassify.T_CLASSIFY.`as`("tClassify")
        val tImageCategoryRel = TImageCategoryRel.T_IMAGE_CATEGORY_REL.`as`("tImageCategoryRel")
        val tImageLabelRel = TImageLabelRel.T_IMAGE_LABEL_REL.`as`("tImageLabelRel")
        val tImageFeature = TImageFeature.T_IMAGE_FEATURE.`as`("tImageFeature")
        val tImageVersionLog = TImageVersionLog.T_IMAGE_VERSION_LOG.`as`("tImageVersionLog")
        // 查找每组imageCode最新的记录
        val tmp = dslContext.select(
            tImage.IMAGE_CODE.`as`(KEY_IMAGE_CODE),
            tImage.CREATE_TIME.max().`as`(KEY_CREATE_TIME)
        ).from(tImage).groupBy(tImage.IMAGE_CODE)
        val t = dslContext.select(tImage.IMAGE_CODE.`as`(KEY_IMAGE_CODE), tImage.IMAGE_STATUS.`as`(KEY_IMAGE_STATUS))
            .from(tImage).join(tmp)
            .on(
                tImage.IMAGE_CODE.eq(tmp.field(KEY_IMAGE_CODE, String::class.java)).and(
                    tImage.CREATE_TIME.eq(
                        tmp.field(
                            KEY_CREATE_TIME, LocalDateTime::class.java
                        )
                    )
                )
            )
        val conditions =
            generateQueryOpImageCondition(
                tImage = tImage,
                imageName = imageName,
                imageType = imageType,
                classifyCode = classifyCode,
                tClassify = tClassify,
                processFlag = processFlag,
                t = t
            )
        var baseStep = dslContext.selectCount().from(tImage)
            .join(tImageFeature)
            .on(tImage.IMAGE_CODE.eq(tImageFeature.IMAGE_CODE))
            .join(tClassify)
            .on(tImage.CLASSIFY_ID.eq(tClassify.ID))
            .join(t)
            .on(tImage.IMAGE_CODE.eq(t.field(KEY_IMAGE_CODE, String::class.java)))
            .join(tImageVersionLog)
            .on(tImage.ID.eq(tImageVersionLog.IMAGE_ID))
        if (categoryCodeList != null && categoryCodeList.isNotEmpty()) {
            val tCategory = TCategory.T_CATEGORY.`as`("tCategory")
            val categoryIdList = dslContext.select(tCategory.ID)
                .from(tCategory)
                .where(tCategory.CATEGORY_CODE.`in`(categoryCodeList))
                .and(tCategory.TYPE.eq(StoreTypeEnum.IMAGE.type.toByte()))
                .fetch().map { it["ID"] as String }
            baseStep = baseStep.join(tImageCategoryRel).on(tImage.ID.eq(tImageCategoryRel.IMAGE_ID))
            conditions.add(tImageCategoryRel.CATEGORY_ID.`in`(categoryIdList))
        }
        if (labelCodeList != null && labelCodeList.isNotEmpty()) {
            val tl = TLabel.T_LABEL.`as`("tl")
            val labelIdList = dslContext.select(tl.ID)
                .from(tl)
                .where(tl.LABEL_CODE.`in`(labelCodeList)).and(tl.TYPE.eq(StoreTypeEnum.IMAGE.type.toByte()))
                .fetch().map { it["ID"] as String }
            baseStep = baseStep.join(tImageLabelRel).on(tImage.ID.eq(tImageLabelRel.IMAGE_ID))
            conditions.add(tImageLabelRel.LABEL_ID.`in`(labelIdList))
        }
        return baseStep.where(conditions).fetchOne(0, Int::class.java)
    }

    fun listOpImages(
        dslContext: DSLContext,
        imageName: String?,
        imageType: ImageType?,
        classifyCode: String?,
        categoryCodeList: Set<String>?,
        labelCodeList: Set<String>?,
        processFlag: Boolean?,
        // 排序字段
        sortType: OpImageSortTypeEnum?,
        // 是否降序
        desc: Boolean?,
        page: Int? = null,
        pageSize: Int? = null,
        interfaceName: String? = "Anon interface"
    ): Result<out Record>? {
        val tImage = TImage.T_IMAGE.`as`("tImage")
        val tClassify = TClassify.T_CLASSIFY.`as`("tClassify")
        val tImageCategoryRel = TImageCategoryRel.T_IMAGE_CATEGORY_REL.`as`("tImageCategoryRel")
        val tImageLabelRel = TImageLabelRel.T_IMAGE_LABEL_REL.`as`("tImageLabelRel")
        val tImageFeature = TImageFeature.T_IMAGE_FEATURE.`as`("tImageFeature")
        val tImageVersionLog = TImageVersionLog.T_IMAGE_VERSION_LOG.`as`("tImageVersionLog")
        // 查询条件是imageCode的最新版本要是处于流程中的，但是查出来的镜像信息是imageCode的最近可见版本（latestFlag=true）
        // 通过t子查询对imageCode进行过滤：只拿最新版本要是处于流程中的imageCode，所以用的join而非leftJoin
        // 查找每组imageCode最新的记录
        val tmp = dslContext.select(
            tImage.IMAGE_CODE.`as`(KEY_IMAGE_CODE),
            tImage.CREATE_TIME.max().`as`(KEY_CREATE_TIME)
        ).from(tImage).groupBy(tImage.IMAGE_CODE)
        val t = dslContext.select(tImage.IMAGE_CODE.`as`(KEY_IMAGE_CODE), tImage.IMAGE_STATUS.`as`(KEY_IMAGE_STATUS))
            .from(tImage).join(tmp)
            .on(
                tImage.IMAGE_CODE.eq(tmp.field(KEY_IMAGE_CODE, String::class.java)).and(
                    tImage.CREATE_TIME.eq(
                        tmp.field(
                            KEY_CREATE_TIME, LocalDateTime::class.java
                        )
                    )
                )
            )
        val conditions =
            generateQueryOpImageCondition(
                tImage = tImage,
                imageName = imageName,
                imageType = imageType,
                classifyCode = classifyCode,
                tClassify = tClassify,
                processFlag = processFlag,
                t = t
            )
        var baseStep = dslContext.select(
            tImage.ID.`as`(KEY_IMAGE_ID),
            tImage.IMAGE_NAME.`as`(KEY_IMAGE_NAME),
            tImage.IMAGE_CODE.`as`(KEY_IMAGE_CODE),
            tImage.IMAGE_SOURCE_TYPE.`as`(KEY_IMAGE_SOURCE_TYPE),
            tImage.VERSION.`as`(KEY_IMAGE_VERSION),
            tImage.IMAGE_STATUS.`as`(KEY_IMAGE_STATUS),
            tClassify.CLASSIFY_CODE.`as`(KEY_CLASSIFY_CODE),
            tClassify.CLASSIFY_NAME.`as`(KEY_CLASSIFY_NAME),
            tImage.PUBLISHER.`as`(KEY_PUBLISHER),
            tImage.PUB_TIME.`as`(KEY_PUB_TIME),
            tImageVersionLog.CONTENT.`as`(KEY_VERSION_LOG_CONTENT),
            tImage.LATEST_FLAG.`as`(KEY_IMAGE_LATEST_FLAG),
            tImageFeature.PUBLIC_FLAG.`as`(KEY_IMAGE_FEATURE_PUBLIC_FLAG),
            tImageFeature.RECOMMEND_FLAG.`as`(KEY_IMAGE_FEATURE_RECOMMEND_FLAG),
            tImageFeature.WEIGHT.`as`(KEY_IMAGE_FEATURE_WEIGHT),
            tImage.CREATOR.`as`(KEY_CREATOR),
            tImage.CREATE_TIME.`as`(KEY_CREATE_TIME),
            tImage.MODIFIER.`as`(KEY_MODIFIER),
            tImage.UPDATE_TIME.`as`(KEY_UPDATE_TIME)
        ).from(tImage)
            .join(tImageFeature)
            .on(tImage.IMAGE_CODE.eq(tImageFeature.IMAGE_CODE))
            .join(tClassify)
            .on(tImage.CLASSIFY_ID.eq(tClassify.ID))
            .join(t)
            .on(tImage.IMAGE_CODE.eq(t.field(KEY_IMAGE_CODE, String::class.java)))
            .join(tImageVersionLog)
            .on(tImage.ID.eq(tImageVersionLog.IMAGE_ID))
        if (categoryCodeList != null && categoryCodeList.isNotEmpty()) {
            val tCategory = TCategory.T_CATEGORY.`as`("tCategory")
            val categoryIdList = dslContext.select(tCategory.ID)
                .from(tCategory)
                .where(tCategory.CATEGORY_CODE.`in`(categoryCodeList))
                .and(tCategory.TYPE.eq(StoreTypeEnum.IMAGE.type.toByte()))
                .fetch().map { it["ID"] as String }
            baseStep = baseStep.join(tImageCategoryRel).on(tImage.ID.eq(tImageCategoryRel.IMAGE_ID))
            conditions.add(tImageCategoryRel.CATEGORY_ID.`in`(categoryIdList))
        }
        if (labelCodeList != null && labelCodeList.isNotEmpty()) {
            val tl = TLabel.T_LABEL.`as`("tl")
            val labelIdList = dslContext.select(tl.ID)
                .from(tl)
                .where(tl.LABEL_CODE.`in`(labelCodeList)).and(tl.TYPE.eq(StoreTypeEnum.IMAGE.type.toByte()))
                .fetch().map { it["ID"] as String }
            baseStep = baseStep.join(tImageLabelRel).on(tImage.ID.eq(tImageLabelRel.IMAGE_ID))
            conditions.add(tImageLabelRel.LABEL_ID.`in`(labelIdList))
        }
        val conditionStep = if (null != sortType) {
            // 排序字段，只提供IMAGE_NAME与CREATE_TIME两种排序字段
            val realSortType = tImage.field(sortType.sortType)

            // 排序
            if (desc != null && desc) {
                baseStep.where(conditions).orderBy(realSortType.desc())
            } else {
                baseStep.where(conditions).orderBy(realSortType.asc())
            }
        } else {
            baseStep.where(conditions)
        }
        // 分页
        val limitStep = if (page != null && page > 0 && pageSize != null && pageSize > 0) {
            conditionStep.limit((page - 1) * pageSize, pageSize)
        } else {
            conditionStep
        }
        logger.info("$interfaceName:listOpImages:SQL:${limitStep.getSQL(true)}")
        return limitStep.fetch()
    }

    private fun generateQueryOpImageCondition(
        tImage: TImage,
        imageName: String?,
        imageType: ImageType?,
        classifyCode: String?,
        tClassify: TClassify,
        processFlag: Boolean?,
        t: SelectHavingStep<Record2<String, Byte>>
    ): MutableList<Condition> {
        val conditions = mutableListOf<Condition>()
        conditions.add(tImage.LATEST_FLAG.eq(true)) // 最新可见版本
        if (!imageName.isNullOrEmpty()) {
            conditions.add(tImage.IMAGE_NAME.contains(imageName))
        }
        if (null != imageType) {
            conditions.add(tImage.IMAGE_SOURCE_TYPE.eq(imageType.type))
        }
        if (!classifyCode.isNullOrEmpty()) {
            conditions.add(tClassify.CLASSIFY_CODE.eq(classifyCode))
        }
        if (null != processFlag) {
            if (processFlag) {
                val imageStatusList = listOf(
                    ImageStatusEnum.INIT.status.toByte(),
                    ImageStatusEnum.COMMITTING.status.toByte(),
                    ImageStatusEnum.CHECKING.status.toByte(),
                    ImageStatusEnum.CHECK_FAIL.status.toByte(),
                    ImageStatusEnum.TESTING.status.toByte(),
                    ImageStatusEnum.AUDITING.status.toByte()
                )
                conditions.add(t.field(KEY_IMAGE_STATUS).`in`(imageStatusList))
            } else {
                val imageStatusList = listOf(
                    ImageStatusEnum.AUDIT_REJECT.status.toByte(),
                    ImageStatusEnum.RELEASED.status.toByte(),
                    ImageStatusEnum.GROUNDING_SUSPENSION.status.toByte(),
                    ImageStatusEnum.UNDERCARRIAGING.status.toByte(),
                    ImageStatusEnum.UNDERCARRIAGED.status.toByte()
                )
                conditions.add(t.field(KEY_IMAGE_STATUS).`in`(imageStatusList))
            }
        }
        return conditions
    }

    /**
     * 根据ImageCode获取最新的已下架镜像
     */
    fun getNewestUndercarriagedImageByCode(dslContext: DSLContext, imageCode: String): TImageRecord? {
        return with(TImage.T_IMAGE) {
            dslContext.selectFrom(this)
                .where(IMAGE_CODE.eq(imageCode))
                .and(IMAGE_STATUS.eq(ImageStatusEnum.UNDERCARRIAGED.status.toByte()))
                .orderBy(CREATE_TIME.desc())
                .limit(1)
                .fetchOne()
        }
    }

    /**
     * 根据ImageId更新镜像的LatestFlag
     */
    fun updateImageLatestFlagById(
        dslContext: DSLContext,
        imageId: String,
        userId: String,
        latestFlag: Boolean
    ) {
        with(TImage.T_IMAGE) {
            val baseStep = dslContext.update(this)
            baseStep.set(LATEST_FLAG, latestFlag)
            baseStep.set(MODIFIER, userId)
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(ID.eq(imageId))
                .execute()
        }
    }

    fun updateImageStatusInfoById(
        dslContext: DSLContext,
        imageId: String,
        userId: String,
        imageStatusInfoUpdateRequest: ImageStatusInfoUpdateRequest
    ) {
        with(TImage.T_IMAGE) {
            val baseStep = dslContext.update(this)
            val imageStatus = imageStatusInfoUpdateRequest.imageStatus
            if (null != imageStatus) {
                baseStep.set(IMAGE_STATUS, imageStatus.status.toByte())
            }
            val imageStatusMsg = imageStatusInfoUpdateRequest.imageStatusMsg
            if (null != imageStatusMsg) {
                baseStep.set(IMAGE_STATUS_MSG, imageStatusMsg)
            }
            val pubTime = imageStatusInfoUpdateRequest.pubTime
            if (null != pubTime) {
                baseStep.set(PUB_TIME, pubTime)
            }
            val latestFlag = imageStatusInfoUpdateRequest.latestFlag
            if (null != latestFlag) {
                baseStep.set(LATEST_FLAG, latestFlag)
            }
            baseStep.set(MODIFIER, userId)
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(ID.eq(imageId))
                .execute()
        }
    }

    fun getReleaseImagesByCode(dslContext: DSLContext, imageCode: String): Result<TImageRecord>? {
        return with(TImage.T_IMAGE) {
            dslContext.selectFrom(this)
                .where(IMAGE_CODE.eq(imageCode))
                .and(IMAGE_STATUS.eq(ImageStatusEnum.RELEASED.status.toByte()))
                .orderBy(CREATE_TIME.desc())
                .fetch()
        }
    }

    private val logger = LoggerFactory.getLogger(MarketImageDao::class.java)
}