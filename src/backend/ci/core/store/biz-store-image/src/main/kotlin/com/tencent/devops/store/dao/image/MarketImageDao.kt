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
package com.tencent.devops.store.dao.image

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.type.docker.ImageType
import com.tencent.devops.model.store.tables.TCategory
import com.tencent.devops.model.store.tables.TClassify
import com.tencent.devops.model.store.tables.TImage
import com.tencent.devops.model.store.tables.TImageAgentType
import com.tencent.devops.model.store.tables.TImageCategoryRel
import com.tencent.devops.model.store.tables.TImageFeature
import com.tencent.devops.model.store.tables.TImageLabelRel
import com.tencent.devops.model.store.tables.TLabel
import com.tencent.devops.model.store.tables.TStoreDeptRel
import com.tencent.devops.model.store.tables.TStoreProjectRel
import com.tencent.devops.model.store.tables.TStoreStatisticsTotal
import com.tencent.devops.model.store.tables.records.TImageRecord
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_AGENT_TYPE_SCOPE
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_CODE
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_DOCKER_FILE_CONTENT
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_DOCKER_FILE_TYPE
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_FEATURE_CERTIFICATION_FLAG
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_FEATURE_PUBLIC_FLAG
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_FEATURE_RECOMMEND_FLAG
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_FEATURE_WEIGHT
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_ICON
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_ID
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_LOGO_URL
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_NAME
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_RD_TYPE
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_REPO_NAME
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_REPO_URL
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_SIZE
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_SOURCE_TYPE
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_SUMMARY
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_TAG
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_VERSION
import com.tencent.devops.store.exception.image.ClassifyNotExistException
import com.tencent.devops.store.pojo.common.KEY_CATEGORY_CODE
import com.tencent.devops.store.pojo.common.KEY_CATEGORY_NAME
import com.tencent.devops.store.pojo.common.KEY_CLASSIFY_ID
import com.tencent.devops.store.pojo.common.KEY_CREATE_TIME
import com.tencent.devops.store.pojo.common.KEY_CREATOR
import com.tencent.devops.store.pojo.common.KEY_MODIFIER
import com.tencent.devops.store.pojo.common.KEY_PUBLISHER
import com.tencent.devops.store.pojo.common.KEY_PUB_TIME
import com.tencent.devops.store.pojo.common.KEY_UPDATE_TIME
import com.tencent.devops.store.pojo.common.enums.ApproveStatusEnum
import com.tencent.devops.store.pojo.common.enums.StoreProjectTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.image.enums.ImageRDTypeEnum
import com.tencent.devops.store.pojo.image.enums.ImageStatusEnum
import com.tencent.devops.store.pojo.image.enums.MarketImageSortTypeEnum
import com.tencent.devops.store.pojo.image.request.ImageBaseInfoUpdateRequest
import com.tencent.devops.store.pojo.image.request.ImageStatusInfoUpdateRequest
import com.tencent.devops.store.pojo.image.request.MarketImageRelRequest
import com.tencent.devops.store.pojo.image.request.MarketImageUpdateRequest
import com.tencent.devops.store.service.image.SupportService
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Record1
import org.jooq.Record18
import org.jooq.Result
import org.jooq.UpdateSetFirstStep
import org.jooq.conf.ParamType
import org.jooq.impl.DSL
import org.jooq.impl.DSL.groupConcat
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDateTime

@Repository
@Suppress("ALL")
class MarketImageDao @Autowired constructor(
    private val supportService: SupportService
) {
    /**
     * 镜像市场搜索结果 总数
     * 参与Join的表：TImage,TImageLabelRel,TImageLabelRel结果临时表
     */
    fun count(
        dslContext: DSLContext,
        keyword: String?,
        classifyCodeList: List<String>?,
        labelCodeList: List<String>?,
        rdType: ImageRDTypeEnum?,
        score: Int?,
        imageSourceType: ImageType?
    ): Int {
        val (tImage, tImageFeature, conditions) = formatConditions(
            keyword = keyword,
            imageSourceType = imageSourceType,
            classifyCodeList = classifyCodeList,
            rdType = rdType,
            dslContext = dslContext
        )

        val baseStep = dslContext.select(DSL.countDistinct(tImage.ID)).from(tImage)

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
                .join(tImageFeature).on(tImage.IMAGE_CODE.eq(tImageFeature.IMAGE_CODE))
            conditions.add(t.field("SCORE_AVERAGE", BigDecimal::class.java)!!.ge(BigDecimal.valueOf(score.toLong())))
        }
        return baseStep.where(conditions).fetchOne(0, Int::class.java)!!
    }

    fun countByIdAndCode(dslContext: DSLContext, imageId: String, imageCode: String): Int {
        with(TImage.T_IMAGE) {
            return dslContext.selectCount().from(this).where(ID.eq(imageId).and(IMAGE_CODE.eq(imageCode)))
                .fetchOne(0, Int::class.java)!!
        }
    }

    private fun formatConditions(
        keyword: String?,
        imageSourceType: ImageType?,
        classifyCodeList: List<String>?,
        rdType: ImageRDTypeEnum?,
        dslContext: DSLContext
    ): Triple<TImage, TImageFeature, MutableList<Condition>> {
        val tImage = TImage.T_IMAGE.`as`("tImage")
        val tImageFeature = TImageFeature.T_IMAGE_FEATURE.`as`("tImageFeature")

        val conditions = mutableListOf<Condition>()
        // 隐含条件
        conditions.add(tImage.IMAGE_STATUS.eq(ImageStatusEnum.RELEASED.status.toByte())) // 已发布的
        conditions.add(tImage.LATEST_FLAG.eq(true)) // 最新版本
        if (!keyword.isNullOrEmpty()) {
            conditions.add(tImage.IMAGE_NAME.contains(keyword).or(tImage.SUMMARY.contains(keyword)))
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
        if (null != rdType) {
            conditions.add(tImageFeature.IMAGE_TYPE.eq(rdType.type.toByte()))
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
        // 搜索关键字，模糊匹配
        keyword: String?,
        // 分类代码，精确匹配
        classifyCodeList: List<String>?,
        // 标签，精确匹配
        labelCodeList: List<String>?,
        // 范畴：精确匹配
        categoryCodeList: List<String>?,
        // 研发来源：精确匹配
        rdType: ImageRDTypeEnum?,
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
    ): Result<
        Record18<String, String, String, Byte,
            String, String, String, String,
            String, String, Boolean, Boolean, String,
            LocalDateTime, String, String, LocalDateTime, LocalDateTime>>? {
        val (tImage, tImageFeature, conditions) = formatConditions(
            keyword = keyword,
            imageSourceType = imageSourceType,
            classifyCodeList = classifyCodeList,
            rdType = rdType,
            dslContext = dslContext
        )

        val baseStep = dslContext.select(
            tImage.ID.`as`(KEY_IMAGE_ID),
            tImage.IMAGE_CODE.`as`(KEY_IMAGE_CODE),
            tImage.IMAGE_NAME.`as`(KEY_IMAGE_NAME),
            tImageFeature.IMAGE_TYPE.`as`(KEY_IMAGE_RD_TYPE),
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
        // 根据范畴标签筛选
        if (categoryCodeList != null && categoryCodeList.isNotEmpty()) {
            val c = TCategory.T_CATEGORY.`as`("c")
            val categoryIdList = dslContext.select(c.ID)
                .from(c)
                .where(c.CATEGORY_CODE.`in`(categoryCodeList)).and(c.TYPE.eq(StoreTypeEnum.IMAGE.type.toByte()))
                .fetch().map { it["ID"] as String }
            val ticr = TImageCategoryRel.T_IMAGE_CATEGORY_REL.`as`("ticr")
            baseStep.leftJoin(ticr).on(tImage.ID.eq(ticr.IMAGE_ID))
            conditions.add(ticr.CATEGORY_ID.`in`(categoryIdList))
        }
        if (score != null) {
            val tas = TStoreStatisticsTotal.T_STORE_STATISTICS_TOTAL.`as`("tas")
            val t = dslContext.select(
                tas.STORE_CODE,
                tas.DOWNLOADS.`as`(MarketImageSortTypeEnum.DOWNLOAD_COUNT.name),
                tas.SCORE_AVERAGE
            ).from(tas).asTable("t")
            baseStep.leftJoin(t).on(tImage.IMAGE_CODE.eq(t.field("STORE_CODE", String::class.java)))
            conditions.add(t.field("SCORE_AVERAGE", BigDecimal::class.java)!!.ge(BigDecimal.valueOf(score.toLong())))
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
            val realSortType = when (sortType) {
                MarketImageSortTypeEnum.DOWNLOAD_COUNT -> {
                    DSL.field(MarketImageSortTypeEnum.getSortType(sortType.name))
                }
                MarketImageSortTypeEnum.CREATE_TIME -> {
                    // 创建时间按照tImageFeature表计算
                    tImageFeature.field(MarketImageSortTypeEnum.getSortType(sortType.name))
                }
                else -> {
                    // 更新时间按照tImage表计算
                    tImage.field(MarketImageSortTypeEnum.getSortType(sortType.name))
                }
            }

            // 排序
            if (desc != null && desc) {
                baseStep.where(conditions).orderBy(realSortType!!.desc())
            } else {
                baseStep.where(conditions).orderBy(realSortType!!.asc())
            }
        } else {
            baseStep.where(conditions)
        }
        // 分页
        val finalStep = if (page != null && page > 0 && pageSize != null && pageSize > 0) {
            baseStep.limit((page - 1) * pageSize, pageSize)
        } else {
            baseStep
        }
        logger.info(finalStep.getSQL(ParamType.INLINED))
        return finalStep.fetch()
    }

    fun count(
        dslContext: DSLContext,
        // 搜索关键字，模糊匹配
        keyword: String?,
        // 分类代码，精确匹配
        classifyCodeList: List<String>?,
        // 标签，精确匹配
        labelCodeList: List<String>?,
        // 范畴：精确匹配
        categoryCodeList: List<String>?,
        // 研发来源：精确匹配
        rdType: ImageRDTypeEnum?,
        // 评分大于等于score的镜像
        score: Int?,
        // 来源，精确匹配
        imageSourceType: ImageType?
    ): Int {
        val (tImage, tImageFeature, conditions) = formatConditions(
            keyword = keyword,
            imageSourceType = imageSourceType,
            classifyCodeList = classifyCodeList,
            rdType = rdType,
            dslContext = dslContext
        )
        // 查的是最近已发布版本，一个imageCode只有一条记录
        val baseStep = dslContext.select(
            DSL.count(tImage.ID)
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
        // 根据范畴标签筛选
        if (categoryCodeList != null && categoryCodeList.isNotEmpty()) {
            val c = TCategory.T_CATEGORY.`as`("c")
            val categoryIdList = dslContext.select(c.ID)
                .from(c)
                .where(c.CATEGORY_CODE.`in`(categoryCodeList)).and(c.TYPE.eq(StoreTypeEnum.IMAGE.type.toByte()))
                .fetch().map { it["ID"] as String }
            val ticr = TImageCategoryRel.T_IMAGE_CATEGORY_REL.`as`("ticr")
            baseStep.leftJoin(ticr).on(tImage.ID.eq(ticr.IMAGE_ID))
            conditions.add(ticr.CATEGORY_ID.`in`(categoryIdList))
        }
        if (score != null) {
            val tas = TStoreStatisticsTotal.T_STORE_STATISTICS_TOTAL.`as`("tas")
            val t = dslContext.select(
                tas.STORE_CODE,
                tas.DOWNLOADS.`as`(MarketImageSortTypeEnum.DOWNLOAD_COUNT.name),
                tas.SCORE_AVERAGE
            ).from(tas).asTable("t")
            baseStep.leftJoin(t).on(tImage.IMAGE_CODE.eq(t.field("STORE_CODE", String::class.java)))
            conditions.add(t.field("SCORE_AVERAGE", BigDecimal::class.java)!!.ge(BigDecimal.valueOf(score.toLong())))
        }
        baseStep.where(conditions)
        logger.info(baseStep.getSQL(ParamType.INLINED))
        return baseStep.fetchOne(0, Int::class.java)!!
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
            .where(a.CLASSIFY_CODE.eq(marketImageUpdateRequest.classifyCode)
                .and(a.TYPE.eq(StoreTypeEnum.IMAGE.type.toByte())))
            .fetchOne(0, String::class.java)
            ?: throw ClassifyNotExistException("classifyCode=${marketImageUpdateRequest.classifyCode}")
        with(TImage.T_IMAGE) {
            val steps = dslContext.update(this)
                .set(IMAGE_NAME, marketImageUpdateRequest.imageName)
                .set(CLASSIFY_ID, classifyId)
                .set(LOGO_URL, marketImageUpdateRequest.logoUrl)
                .set(ICON, marketImageUpdateRequest.iconData)
                .set(IMAGE_STATUS, ImageStatusEnum.COMMITTING.status.toByte())
                .set(IMAGE_SIZE, imageSize)
                .set(IMAGE_SOURCE_TYPE, marketImageUpdateRequest.imageSourceType.type)
                .set(IMAGE_REPO_URL, marketImageUpdateRequest.imageRepoUrl?.trim())
                .set(IMAGE_REPO_NAME, marketImageUpdateRequest.imageRepoName.trim())
                .set(IMAGE_TAG, marketImageUpdateRequest.imageTag.trim())
            if (!marketImageUpdateRequest.dockerFileType.isNullOrBlank()) {
                steps.set(DOCKER_FILE_TYPE, marketImageUpdateRequest.dockerFileType)
            }
            if (marketImageUpdateRequest.dockerFileContent != null) {
                steps.set(DOCKER_FILE_CONTENT, marketImageUpdateRequest.dockerFileContent)
            }
            steps.set(TICKET_ID, marketImageUpdateRequest.ticketId)
                .set(AGENT_TYPE_SCOPE, JsonUtil.toJson(marketImageUpdateRequest.agentTypeScope))
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
            .where(a.CLASSIFY_CODE.eq(marketImageUpdateRequest.classifyCode)
                .and(a.TYPE.eq(StoreTypeEnum.IMAGE.type.toByte())))
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
                DOCKER_FILE_TYPE,
                DOCKER_FILE_CONTENT,
                TICKET_ID,
                AGENT_TYPE_SCOPE,
                LOGO_URL,
                ICON,
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
                    marketImageUpdateRequest.imageRepoUrl?.trim(),
                    marketImageUpdateRequest.imageRepoName.trim(),
                    marketImageUpdateRequest.imageTag.trim(),
                    marketImageUpdateRequest.dockerFileType ?: "INPUT",
                    marketImageUpdateRequest.dockerFileContent ?: "",
                    marketImageUpdateRequest.ticketId,
                    JsonUtil.toJson(marketImageUpdateRequest.agentTypeScope),
                    marketImageUpdateRequest.logoUrl,
                    marketImageUpdateRequest.iconData,
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

    fun getImagesIdByImageCode(dslContext: DSLContext, imageCode: String): List<String> {
        with(TImage.T_IMAGE) {
            return dslContext.select(ID)
                .from(this)
                .where(IMAGE_CODE.eq(imageCode))
                .orderBy(CREATE_TIME.desc())
                .fetchInto(String::class.java)
        }
    }

    fun countReleaseImageByCode(dslContext: DSLContext, imageCode: String): Int {
        with(TImage.T_IMAGE) {
            return dslContext.selectCount().from(this)
                .where(IMAGE_CODE.eq(imageCode).and(IMAGE_STATUS.eq(ImageStatusEnum.RELEASED.status.toByte())))
                .fetchOne(0, Int::class.java)!!
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
        imageIdList: List<String>,
        imageBaseInfoUpdateRequest: ImageBaseInfoUpdateRequest
    ) {
        with(TImage.T_IMAGE) {
            val baseStep = dslContext.update(this)
            setUpdateImageBaseInfo(imageBaseInfoUpdateRequest, baseStep)
            baseStep.set(MODIFIER, userId)
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(ID.`in`(imageIdList))
                .execute()
        }
    }

    fun updateImageBaseInfoByCode(
        dslContext: DSLContext,
        userId: String,
        imageCode: String,
        imageBaseInfoUpdateRequest: ImageBaseInfoUpdateRequest
    ) {
        with(TImage.T_IMAGE) {
            val baseStep = dslContext.update(this)
            setUpdateImageBaseInfo(imageBaseInfoUpdateRequest, baseStep)
            baseStep.set(MODIFIER, userId)
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(IMAGE_CODE.eq(imageCode))
                .execute()
        }
    }

    private fun TImage.setUpdateImageBaseInfo(
        imageBaseInfoUpdateRequest: ImageBaseInfoUpdateRequest,
        baseStep: UpdateSetFirstStep<TImageRecord>
    ) {
        val imageName = imageBaseInfoUpdateRequest.imageName
        if (!imageName.isNullOrBlank()) {
            baseStep.set(IMAGE_NAME, imageName)
        }
        val classifyId = imageBaseInfoUpdateRequest.classifyId
        if (!classifyId.isNullOrBlank()) {
            baseStep.set(CLASSIFY_ID, classifyId)
        }
        val summary = imageBaseInfoUpdateRequest.summary
        if (!summary.isNullOrBlank()) {
            baseStep.set(SUMMARY, summary)
        }
        val description = imageBaseInfoUpdateRequest.description
        if (null != description) {
            baseStep.set(DESCRIPTION, description)
        }
        val logoUrl = imageBaseInfoUpdateRequest.logoUrl
        if (!logoUrl.isNullOrBlank()) {
            baseStep.set(LOGO_URL, logoUrl)
        }
        val iconData = imageBaseInfoUpdateRequest.iconData
        if (!iconData.isNullOrBlank()) {
            baseStep.set(ICON, iconData)
        }
        val publisher = imageBaseInfoUpdateRequest.publisher
        if (!publisher.isNullOrBlank()) {
            baseStep.set(PUBLISHER, publisher)
        }
        val imageSize = imageBaseInfoUpdateRequest.imageSize
        if (!imageSize.isNullOrBlank()) {
            baseStep.set(IMAGE_SIZE, imageSize)
        }
        val dockerFileType = imageBaseInfoUpdateRequest.dockerFileType
        if (!dockerFileType.isNullOrBlank()) {
            baseStep.set(DOCKER_FILE_TYPE, dockerFileType)
        }
        val dockerFileContent = imageBaseInfoUpdateRequest.dockerFileContent
        if (dockerFileContent != null) {
            baseStep.set(DOCKER_FILE_CONTENT, dockerFileContent)
        }
        val deleteFlag = imageBaseInfoUpdateRequest.deleteFlag
        if (null != deleteFlag) {
            baseStep.set(DELETE_FLAG, deleteFlag)
        }
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

    // 硬性过滤条件
    fun genBaseConditions(
        dslContext: DSLContext,
        inImageCodes: Collection<String>?,
        notInImageCodes: Collection<String>?,
        recommendFlag: Boolean?,
        keyword: String?,
        classifyId: String?,
        categoryCode: String?,
        rdType: ImageRDTypeEnum?
    ): MutableList<Condition> {
        val tImageFeature = TImageFeature.T_IMAGE_FEATURE.`as`("tImageFeature")
        val tCategory = TCategory.T_CATEGORY.`as`("tCategory")
        val tImage = TImage.T_IMAGE.`as`("tImage")
        val conditions = mutableListOf<Condition>()
        if (inImageCodes != null) {
            conditions.add(tImage.IMAGE_CODE.`in`(inImageCodes))
        }
        if (notInImageCodes != null) {
            conditions.add(tImage.IMAGE_CODE.notIn(notInImageCodes))
        }
        if (recommendFlag != null) {
            conditions.add(tImageFeature.RECOMMEND_FLAG.eq(recommendFlag))
        }
        if (!keyword.isNullOrBlank()) {
            conditions.add(tImage.IMAGE_NAME.contains(keyword).or(tImage.SUMMARY.contains(keyword)))
        }
        if (!classifyId.isNullOrBlank()) {
            conditions.add(tImage.CLASSIFY_ID.eq(classifyId))
        }
        if (!categoryCode.isNullOrBlank()) {
            conditions.add(tCategory.CATEGORY_CODE.eq(categoryCode))
        }
        conditions.add(tImage.DELETE_FLAG.eq(false)) // 只查没有被删除的镜像
        return conditions
    }

    /**
     * 查询未安装、可安装镜像
     */
    fun listCanInstallJobMarketImages(
        dslContext: DSLContext,
        inImageCodes: List<String>?,
        notInImageCodes: List<String>?,
        recommendFlag: Boolean?,
        keyword: String?,
        classifyId: String?,
        categoryCode: String?,
        rdType: ImageRDTypeEnum?,
        installedImageCodes: List<String>,
        visibleImageCodes: List<String>,
        offset: Int? = 0,
        limit: Int? = -1
    ): Result<Record>? {
        val tImageFeature = TImageFeature.T_IMAGE_FEATURE.`as`("tImageFeature")
        val extraConditions = mutableListOf<Condition>()
        extraConditions.add(tImageFeature.IMAGE_CODE.`in`(visibleImageCodes.subtract(installedImageCodes)))
        return listJobMarketImagesWithExtraConditions(
            dslContext = dslContext,
            inImageCodes = inImageCodes,
            notInImageCodes = notInImageCodes,
            recommendFlag = recommendFlag,
            keyword = keyword,
            classifyId = classifyId,
            categoryCode = categoryCode,
            rdType = rdType,
            extraConditions = extraConditions,
            offset = offset,
            limit = limit
        )
    }

    /**
     * 查询未安装、可安装镜像数量
     */
    fun countCanInstallJobMarketImages(
        dslContext: DSLContext,
        inImageCodes: List<String>?,
        notInImageCodes: List<String>?,
        recommendFlag: Boolean?,
        keyword: String?,
        classifyId: String?,
        categoryCode: String?,
        rdType: ImageRDTypeEnum?,
        installedImageCodes: List<String>,
        visibleImageCodes: List<String>
    ): Int {
        val tImageFeature = TImageFeature.T_IMAGE_FEATURE.`as`("tImageFeature")
        val extraConditions = mutableListOf<Condition>()
        extraConditions.add(tImageFeature.IMAGE_CODE.`in`(visibleImageCodes.subtract(installedImageCodes)))
        return countJobMarketImagesWithExtraConditions(
            dslContext = dslContext,
            inImageCodes = inImageCodes,
            notInImageCodes = notInImageCodes,
            recommendFlag = recommendFlag,
            keyword = keyword,
            classifyId = classifyId,
            categoryCode = categoryCode,
            rdType = rdType,
            extraConditions = extraConditions
        )
    }

    /**
     * 查询已安装镜像
     */
    fun listInstalledJobMarketImages(
        dslContext: DSLContext,
        inImageCodes: List<String>?,
        notInImageCodes: List<String>?,
        recommendFlag: Boolean?,
        keyword: String?,
        classifyId: String?,
        categoryCode: String?,
        rdType: ImageRDTypeEnum?,
        installedImageCodes: List<String>,
        offset: Int? = 0,
        limit: Int? = -1
    ): Result<Record>? {
        val tImageFeature = TImageFeature.T_IMAGE_FEATURE.`as`("tImageFeature")
        val extraConditions = mutableListOf<Condition>()
        extraConditions.add(tImageFeature.IMAGE_CODE.`in`(installedImageCodes))
        return listJobMarketImagesWithExtraConditions(
            dslContext = dslContext,
            inImageCodes = inImageCodes,
            notInImageCodes = notInImageCodes,
            recommendFlag = recommendFlag,
            keyword = keyword,
            classifyId = classifyId,
            categoryCode = categoryCode,
            rdType = rdType,
            extraConditions = extraConditions,
            offset = offset,
            limit = limit
        )
    }

    /**
     * 查询已安装镜像数量
     */
    fun countInstalledJobMarketImages(
        dslContext: DSLContext,
        inImageCodes: List<String>?,
        notInImageCodes: List<String>?,
        recommendFlag: Boolean?,
        keyword: String?,
        classifyId: String?,
        categoryCode: String?,
        rdType: ImageRDTypeEnum?,
        installedImageCodes: List<String>
    ): Int {
        val tImageFeature = TImageFeature.T_IMAGE_FEATURE.`as`("tImageFeature")
        val extraConditions = mutableListOf<Condition>()
        extraConditions.add(tImageFeature.IMAGE_CODE.`in`(installedImageCodes))
        return countJobMarketImagesWithExtraConditions(
            dslContext = dslContext,
            inImageCodes = inImageCodes,
            notInImageCodes = notInImageCodes,
            recommendFlag = recommendFlag,
            keyword = keyword,
            classifyId = classifyId,
            categoryCode = categoryCode,
            rdType = rdType,
            extraConditions = extraConditions
        )
    }

    /**
     * 查询不可见镜像
     */
    fun listNoVisibleJobMarketImages(
        dslContext: DSLContext,
        inImageCodes: List<String>?,
        notInImageCodes: List<String>?,
        recommendFlag: Boolean?,
        keyword: String?,
        classifyId: String?,
        categoryCode: String?,
        rdType: ImageRDTypeEnum?,
        visibleImageCodes: List<String>,
        offset: Int? = 0,
        limit: Int? = -1
    ): Result<Record>? {
        val tImageFeature = TImageFeature.T_IMAGE_FEATURE.`as`("tImageFeature")
        val extraConditions = mutableListOf<Condition>()
        extraConditions.add(tImageFeature.IMAGE_CODE.notIn(visibleImageCodes))
        return listJobMarketImagesWithExtraConditions(
            dslContext = dslContext,
            inImageCodes = inImageCodes,
            notInImageCodes = notInImageCodes,
            recommendFlag = recommendFlag,
            keyword = keyword,
            classifyId = classifyId,
            categoryCode = categoryCode,
            rdType = rdType,
            extraConditions = extraConditions,
            offset = offset,
            limit = limit
        )
    }

    /**
     * 查询不可见镜像数量
     */
    fun countNoVisibleJobMarketImages(
        dslContext: DSLContext,
        inImageCodes: List<String>?,
        notInImageCodes: List<String>?,
        recommendFlag: Boolean?,
        keyword: String?,
        classifyId: String?,
        categoryCode: String?,
        rdType: ImageRDTypeEnum?,
        visibleImageCodes: List<String>,
        offset: Int? = 0,
        limit: Int? = -1
    ): Int {
        val tImageFeature = TImageFeature.T_IMAGE_FEATURE.`as`("tImageFeature")
        val extraConditions = mutableListOf<Condition>()
        extraConditions.add(tImageFeature.IMAGE_CODE.notIn(visibleImageCodes))
        return countJobMarketImagesWithExtraConditions(
            dslContext = dslContext,
            inImageCodes = inImageCodes,
            notInImageCodes = notInImageCodes,
            recommendFlag = recommendFlag,
            keyword = keyword,
            classifyId = classifyId,
            categoryCode = categoryCode,
            rdType = rdType,
            extraConditions = extraConditions
        )
    }

    /**
     * 查询调试中镜像
     */
    fun listTestingJobMarketImages(
        dslContext: DSLContext,
        inImageCodes: Collection<String>?,
        notInImageCodes: Collection<String>?,
        recommendFlag: Boolean?,
        keyword: String?,
        classifyId: String?,
        categoryCode: String?,
        rdType: ImageRDTypeEnum?,
        offset: Int? = 0,
        limit: Int? = -1
    ): Result<Record>? {
        val validOffset = if (offset == null || offset < 0) 0 else offset
        val validLimit = if (limit == null || limit <= 0) null else limit
        val tImageFeature = TImageFeature.T_IMAGE_FEATURE.`as`("tImageFeature")
        val tImageAgentType = TImageAgentType.T_IMAGE_AGENT_TYPE.`as`("tImageAgentType")
        val tCategory = TCategory.T_CATEGORY.`as`("tCategory")
        val tImageCategoryRel = TImageCategoryRel.T_IMAGE_CATEGORY_REL.`as`("tImageCategoryRel")
        val tImage = TImage.T_IMAGE.`as`("tImage")
        val conditions = genBaseConditions(
            dslContext = dslContext,
            inImageCodes = inImageCodes,
            notInImageCodes = notInImageCodes,
            recommendFlag = recommendFlag,
            keyword = keyword,
            classifyId = classifyId,
            categoryCode = categoryCode,
            rdType = rdType
        )
        conditions.add(
            tImage.IMAGE_STATUS.`in`(setOf(
                ImageStatusEnum.TESTING.status.toByte(),
                ImageStatusEnum.AUDITING.status.toByte()
            ))
        )
        // 隐含条件：已发布的镜像中最晚的一个
        val baseQuery = dslContext.select(
            tImage.ID.`as`(KEY_IMAGE_ID),
            tImage.IMAGE_CODE.`as`(KEY_IMAGE_CODE),
            tImage.IMAGE_NAME.`as`(KEY_IMAGE_NAME),
            tImageFeature.IMAGE_TYPE.`as`(KEY_IMAGE_RD_TYPE),
            tImage.LOGO_URL.`as`(KEY_IMAGE_LOGO_URL),
            tImage.ICON.`as`(KEY_IMAGE_ICON),
            tImage.SUMMARY.`as`(KEY_IMAGE_SUMMARY),
            tImageFeature.WEIGHT.`as`(KEY_IMAGE_FEATURE_WEIGHT),
            tImage.IMAGE_SOURCE_TYPE.`as`(KEY_IMAGE_SOURCE_TYPE),
            tImage.IMAGE_REPO_URL.`as`(KEY_IMAGE_REPO_URL),
            tImage.IMAGE_REPO_NAME.`as`(KEY_IMAGE_REPO_NAME),
            tImage.IMAGE_TAG.`as`(KEY_IMAGE_TAG),
            tImage.DOCKER_FILE_TYPE.`as`(KEY_IMAGE_DOCKER_FILE_TYPE),
            tImage.DOCKER_FILE_CONTENT.`as`(KEY_IMAGE_DOCKER_FILE_CONTENT),
            tCategory.CATEGORY_CODE.`as`(KEY_CATEGORY_CODE),
            tCategory.CATEGORY_NAME.`as`(KEY_CATEGORY_NAME),
            tImage.PUBLISHER.`as`(KEY_PUBLISHER),
            tImage.MODIFIER.`as`(KEY_MODIFIER),
            tImage.UPDATE_TIME.`as`(KEY_UPDATE_TIME),
            tImageFeature.PUBLIC_FLAG.`as`(KEY_IMAGE_FEATURE_PUBLIC_FLAG),
            tImageFeature.RECOMMEND_FLAG.`as`(KEY_IMAGE_FEATURE_RECOMMEND_FLAG),
            tImageFeature.CERTIFICATION_FLAG.`as`(KEY_IMAGE_FEATURE_CERTIFICATION_FLAG),
            groupConcat(tImageAgentType.AGENT_TYPE).`as`(KEY_IMAGE_AGENT_TYPE_SCOPE)
        ).from(tImage).join(tImageFeature).on(tImage.IMAGE_CODE.eq(tImageFeature.IMAGE_CODE))
            .join(tImageCategoryRel).on(tImage.ID.eq(tImageCategoryRel.IMAGE_ID))
            .leftJoin(tCategory).on(tImageCategoryRel.IMAGE_ID.eq(tCategory.ID))
            .leftJoin(tImageAgentType).on(tImage.IMAGE_CODE.eq(tImageAgentType.IMAGE_CODE))
            .where(conditions)
            .groupBy(tImageFeature.IMAGE_CODE)
            .orderBy(tImageFeature.WEIGHT.desc(), tImage.IMAGE_NAME.asc())
        val finalQuery = if (validLimit != null) {
            baseQuery.offset(validOffset).limit(validLimit)
        } else {
            baseQuery.offset(validOffset)
        }
        logger.info(finalQuery.getSQL(ParamType.INLINED))
        return finalQuery.fetch()
    }

    /**
     * 查询调试中镜像数量
     */
    fun countTestingJobMarketImages(
        dslContext: DSLContext,
        inImageCodes: Collection<String>?,
        notInImageCodes: Collection<String>?,
        recommendFlag: Boolean?,
        keyword: String?,
        classifyId: String?,
        categoryCode: String?,
        rdType: ImageRDTypeEnum?
    ): Int {
        val tImageFeature = TImageFeature.T_IMAGE_FEATURE.`as`("tImageFeature")
        val tImageAgentType = TImageAgentType.T_IMAGE_AGENT_TYPE.`as`("tImageAgentType")
        val tCategory = TCategory.T_CATEGORY.`as`("tCategory")
        val tImageCategoryRel = TImageCategoryRel.T_IMAGE_CATEGORY_REL.`as`("tImageCategoryRel")
        val tImage = TImage.T_IMAGE.`as`("tImage")
        val conditions = genBaseConditions(
            dslContext = dslContext,
            inImageCodes = inImageCodes,
            notInImageCodes = notInImageCodes,
            recommendFlag = recommendFlag,
            keyword = keyword,
            classifyId = classifyId,
            categoryCode = categoryCode,
            rdType = rdType
        )
        conditions.add(
            tImage.IMAGE_STATUS.`in`(setOf(
                ImageStatusEnum.TESTING.status.toByte(),
                ImageStatusEnum.AUDITING.status.toByte()
            ))
        )
        // 隐含条件：已发布的镜像中最晚的一个
        val baseQuery = dslContext.select(
            DSL.countDistinct(tImage.IMAGE_CODE)
        ).from(tImage).join(tImageFeature).on(tImage.IMAGE_CODE.eq(tImageFeature.IMAGE_CODE))
            .join(tImageCategoryRel).on(tImage.ID.eq(tImageCategoryRel.IMAGE_ID))
            .leftJoin(tCategory).on(tImageCategoryRel.IMAGE_ID.eq(tCategory.ID))
            .leftJoin(tImageAgentType).on(tImage.IMAGE_CODE.eq(tImageAgentType.IMAGE_CODE))
            .where(conditions)
        logger.info(baseQuery.getSQL(ParamType.INLINED))
        return baseQuery.fetchOne(0, Int::class.java)!!
    }

    fun listJobMarketImagesWithExtraConditions(
        dslContext: DSLContext,
        inImageCodes: List<String>?,
        notInImageCodes: List<String>?,
        recommendFlag: Boolean?,
        keyword: String?,
        classifyId: String?,
        categoryCode: String?,
        rdType: ImageRDTypeEnum?,
        extraConditions: List<Condition>?,
        offset: Int? = 0,
        limit: Int? = -1
    ): Result<Record>? {
        val validOffset = if (offset == null || offset < 0) 0 else offset
        val validLimit = if (limit == null || limit <= 0) null else limit
        val tImageFeature = TImageFeature.T_IMAGE_FEATURE.`as`("tImageFeature")
        val tImageAgentType = TImageAgentType.T_IMAGE_AGENT_TYPE.`as`("tImageAgentType")
        val tCategory = TCategory.T_CATEGORY.`as`("tCategory")
        val tImageCategoryRel = TImageCategoryRel.T_IMAGE_CATEGORY_REL.`as`("tImageCategoryRel")
        val tImage = TImage.T_IMAGE.`as`("tImage")
        val baseConditions = genBaseConditions(
            dslContext = dslContext,
            inImageCodes = inImageCodes,
            notInImageCodes = notInImageCodes,
            recommendFlag = recommendFlag,
            keyword = keyword,
            classifyId = classifyId,
            categoryCode = categoryCode,
            rdType = rdType
        )
        val conditions = if (extraConditions != null && extraConditions.isNotEmpty()) {
            baseConditions.plus(extraConditions)
        } else {
            baseConditions
        }
        // 隐含条件：已发布的镜像中最晚的一个
        val latestReleasedImage = dslContext.select(
            tImage.IMAGE_CODE.`as`(KEY_IMAGE_CODE),
            DSL.max(tImage.CREATE_TIME).`as`(KEY_CREATE_TIME)
        ).from(tImage).where(
            tImage.IMAGE_STATUS.eq(ImageStatusEnum.RELEASED.status.toByte())
        ).groupBy(tImage.IMAGE_CODE).asTable("latestReleasedImage")
        val baseQuery = dslContext.select(
            tImage.ID.`as`(KEY_IMAGE_ID),
            tImage.IMAGE_CODE.`as`(KEY_IMAGE_CODE),
            tImage.IMAGE_NAME.`as`(KEY_IMAGE_NAME),
            tImageFeature.IMAGE_TYPE.`as`(KEY_IMAGE_RD_TYPE),
            tImage.LOGO_URL.`as`(KEY_IMAGE_LOGO_URL),
            tImage.ICON.`as`(KEY_IMAGE_ICON),
            tImage.SUMMARY.`as`(KEY_IMAGE_SUMMARY),
            tImageFeature.WEIGHT.`as`(KEY_IMAGE_FEATURE_WEIGHT),
            tImage.IMAGE_SOURCE_TYPE.`as`(KEY_IMAGE_SOURCE_TYPE),
            tImage.IMAGE_REPO_URL.`as`(KEY_IMAGE_REPO_URL),
            tImage.IMAGE_REPO_NAME.`as`(KEY_IMAGE_REPO_NAME),
            tImage.IMAGE_TAG.`as`(KEY_IMAGE_TAG),
            tImage.DOCKER_FILE_TYPE.`as`(KEY_IMAGE_DOCKER_FILE_TYPE),
            tImage.DOCKER_FILE_CONTENT.`as`(KEY_IMAGE_DOCKER_FILE_CONTENT),
            tCategory.CATEGORY_CODE.`as`(KEY_CATEGORY_CODE),
            tCategory.CATEGORY_NAME.`as`(KEY_CATEGORY_NAME),
            tImage.PUBLISHER.`as`(KEY_PUBLISHER),
            tImage.MODIFIER.`as`(KEY_MODIFIER),
            tImage.UPDATE_TIME.`as`(KEY_UPDATE_TIME),
            tImageFeature.PUBLIC_FLAG.`as`(KEY_IMAGE_FEATURE_PUBLIC_FLAG),
            tImageFeature.RECOMMEND_FLAG.`as`(KEY_IMAGE_FEATURE_RECOMMEND_FLAG),
            tImageFeature.CERTIFICATION_FLAG.`as`(KEY_IMAGE_FEATURE_CERTIFICATION_FLAG),
            groupConcat(tImageAgentType.AGENT_TYPE).`as`(KEY_IMAGE_AGENT_TYPE_SCOPE)
        ).from(tImage).join(tImageFeature).on(tImage.IMAGE_CODE.eq(tImageFeature.IMAGE_CODE))
            .join(tImageCategoryRel).on(tImage.ID.eq(tImageCategoryRel.IMAGE_ID))
            .leftJoin(tCategory).on(tImageCategoryRel.IMAGE_ID.eq(tCategory.ID))
            .leftJoin(tImageAgentType).on(tImage.IMAGE_CODE.eq(tImageAgentType.IMAGE_CODE))
            .join(latestReleasedImage).on(
                tImage.IMAGE_CODE.eq(latestReleasedImage.field(KEY_IMAGE_CODE, String::class.java)).and(
                    tImage.CREATE_TIME.eq(latestReleasedImage.field(KEY_CREATE_TIME, LocalDateTime::class.java))
                )
            )
            .where(conditions)
            .groupBy(tImageFeature.IMAGE_CODE)
            .orderBy(tImageFeature.WEIGHT.desc(), tImage.IMAGE_NAME.asc())
        val finalQuery = if (validLimit != null) {
            baseQuery.offset(validOffset).limit(validLimit)
        } else {
            baseQuery.offset(validOffset)
        }
        logger.info(finalQuery.getSQL(ParamType.INLINED))
        return finalQuery.fetch()
    }

    fun countJobMarketImagesWithExtraConditions(
        dslContext: DSLContext,
        inImageCodes: List<String>?,
        notInImageCodes: List<String>?,
        recommendFlag: Boolean?,
        keyword: String?,
        classifyId: String?,
        categoryCode: String?,
        rdType: ImageRDTypeEnum?,
        extraConditions: List<Condition>?
    ): Int {
        val tImageFeature = TImageFeature.T_IMAGE_FEATURE.`as`("tImageFeature")
        val tImageAgentType = TImageAgentType.T_IMAGE_AGENT_TYPE.`as`("tImageAgentType")
        val tClassify = TClassify.T_CLASSIFY.`as`("tClassify")
        val tCategory = TCategory.T_CATEGORY.`as`("tCategory")
        val tImageCategoryRel = TImageCategoryRel.T_IMAGE_CATEGORY_REL.`as`("tImageCategoryRel")
        val tImage = TImage.T_IMAGE.`as`("tImage")
        val baseConditions = genBaseConditions(
            dslContext = dslContext,
            inImageCodes = inImageCodes,
            notInImageCodes = notInImageCodes,
            recommendFlag = recommendFlag,
            keyword = keyword,
            classifyId = classifyId,
            categoryCode = categoryCode,
            rdType = rdType
        )
        val conditions = if (extraConditions != null && extraConditions.isNotEmpty()) {
            baseConditions.plus(extraConditions)
        } else {
            baseConditions
        }
        // 隐含条件：已发布的镜像中最晚的一个
        val latestReleasedImage = dslContext.select(
            tImage.IMAGE_CODE.`as`(KEY_IMAGE_CODE),
            DSL.max(tImage.CREATE_TIME).`as`(KEY_CREATE_TIME)
        ).from(tImage).where(
            tImage.IMAGE_STATUS.eq(ImageStatusEnum.RELEASED.status.toByte())
        ).groupBy(tImage.IMAGE_CODE).asTable("latestReleasedImage")
        val baseQuery = dslContext.select(
            DSL.countDistinct(tImage.IMAGE_CODE)
        ).from(tImage).join(tImageFeature).on(tImage.IMAGE_CODE.eq(tImageFeature.IMAGE_CODE))
            .join(tClassify).on(tImage.CLASSIFY_ID.eq(tClassify.ID))
            .join(tImageCategoryRel).on(tImage.ID.eq(tImageCategoryRel.IMAGE_ID))
            .leftJoin(tCategory).on(tImageCategoryRel.IMAGE_ID.eq(tCategory.ID))
            .leftJoin(tImageAgentType).on(tImage.IMAGE_CODE.eq(tImageAgentType.IMAGE_CODE))
            .join(latestReleasedImage).on(
                tImage.IMAGE_CODE.eq(latestReleasedImage.field(KEY_IMAGE_CODE, String::class.java)).and(
                    tImage.CREATE_TIME.eq(latestReleasedImage.field(KEY_CREATE_TIME, LocalDateTime::class.java))
                )
            )
            .where(conditions)
        logger.info(baseQuery.getSQL(ParamType.INLINED))
        return baseQuery.fetchOne(0, Int::class.java)!!
    }

    /**
     * 查询项目已安装的镜像Code
     */
    fun getInstalledImageCodes(
        dslContext: DSLContext,
        projectCode: String
    ): List<String> {
        logger.info("Input:projectCode:$projectCode")
        // 已安装的
        val tStoreProjectRel = TStoreProjectRel.T_STORE_PROJECT_REL.`as`("tStoreProjectRel")
        val installedImageCodes = dslContext.select(tStoreProjectRel.STORE_CODE).from(tStoreProjectRel)
            .where(tStoreProjectRel.PROJECT_CODE.eq(projectCode))
            .and(tStoreProjectRel.STORE_TYPE.eq(StoreTypeEnum.IMAGE.type.toByte()))
            .and(
                tStoreProjectRel.TYPE.`in`(
                    setOf(
                        StoreProjectTypeEnum.COMMON.type.toByte(),
                        StoreProjectTypeEnum.TEST.type.toByte()
                    )
                )
            )
            .fetch()?.map {
                it.value1()
            } ?: emptyList()
        logger.info("Output:installedImageCodes:$installedImageCodes")
        return installedImageCodes
    }

    /**
     * 根据用户部门信息获取可见镜像Code
     */
    fun getVisibleImageCodes(
        dslContext: DSLContext,
        projectCode: String,
        userDeptList: List<Int>
    ): List<String> {
        logger.info("Input:userDeptList:$userDeptList")
        // 可安装的
        val tImageFeature = TImageFeature.T_IMAGE_FEATURE.`as`("tImageFeature")
        val tStoreDeptRel = TStoreDeptRel.T_STORE_DEPT_REL.`as`("tStoreDeptRel")
        val tStoreProjectRel = TStoreProjectRel.T_STORE_PROJECT_REL.`as`("tStoreProjectRel")
        val visibleImageCodes = dslContext.select(tImageFeature.IMAGE_CODE).from(tImageFeature)
            .leftJoin(tStoreDeptRel).on(tImageFeature.IMAGE_CODE.eq(tStoreDeptRel.STORE_CODE))
            // 审核通过
            .where(tStoreDeptRel.STATUS.eq(ApproveStatusEnum.getValue(ApproveStatusEnum.PASS)))
            // 腾讯公司可见或可见范围与用户组织架构重叠
            .and(tStoreDeptRel.DEPT_ID.eq(0).or(tStoreDeptRel.DEPT_ID.`in`(userDeptList)))
            // 镜像类型
            .and(tStoreDeptRel.STORE_TYPE.eq(StoreTypeEnum.IMAGE.type.toByte()))
            // 非公共
            .and(tImageFeature.PUBLIC_FLAG.eq(false))
            .union(
                dslContext.select(tImageFeature.IMAGE_CODE).from(tImageFeature)
                    // 公共
                    .where(tImageFeature.PUBLIC_FLAG.eq(true))
            ).union(
                // 自己项目下的调试镜像
                dslContext.select(tStoreProjectRel.STORE_CODE).from(tStoreProjectRel)
                    .where(tStoreProjectRel.PROJECT_CODE.eq(projectCode))
                    .and(tStoreProjectRel.STORE_TYPE.eq(StoreTypeEnum.IMAGE.type.toByte()))
                    .and(tStoreProjectRel.TYPE.eq(StoreProjectTypeEnum.TEST.type.toByte()))
            )
            .fetch()?.map {
                it.value1()
            } ?: emptyList()
        logger.info("Output:visibleImageCodes:$visibleImageCodes")
        return visibleImageCodes
    }

    fun getTestingImageCodes(
        dslContext: DSLContext,
        projectTestImageCodes: Collection<String>
    ): Result<Record1<String>>? {
        val tImage = TImage.T_IMAGE.`as`("tImage")
        with(tImage) {
            return dslContext.selectDistinct(
                IMAGE_CODE
            ).from(this)
                .where(IMAGE_STATUS.`in`(
                    setOf(ImageStatusEnum.TESTING.status.toByte(), ImageStatusEnum.AUDITING.status.toByte()))
                ).and(IMAGE_CODE.`in`(projectTestImageCodes))
                .fetch()
        }
    }

    private val logger = LoggerFactory.getLogger(MarketImageDao::class.java)
}
