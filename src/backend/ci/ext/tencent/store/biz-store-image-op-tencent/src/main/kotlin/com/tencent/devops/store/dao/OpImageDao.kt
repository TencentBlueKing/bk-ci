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

import com.tencent.devops.common.pipeline.type.docker.ImageType
import com.tencent.devops.model.store.tables.TCategory
import com.tencent.devops.model.store.tables.TClassify
import com.tencent.devops.model.store.tables.TImage
import com.tencent.devops.model.store.tables.TImageCategoryRel
import com.tencent.devops.model.store.tables.TImageFeature
import com.tencent.devops.model.store.tables.TImageLabelRel
import com.tencent.devops.model.store.tables.TImageVersionLog
import com.tencent.devops.model.store.tables.TLabel
import com.tencent.devops.model.store.tables.TStoreProjectRel
import com.tencent.devops.model.store.tables.records.TImageRecord
import com.tencent.devops.store.dao.image.Constants
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_CODE
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_ID
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_STATUS
import com.tencent.devops.store.pojo.common.KEY_CLASSIFY_CODE
import com.tencent.devops.store.pojo.common.KEY_CLASSIFY_NAME
import com.tencent.devops.store.pojo.common.KEY_CREATE_TIME
import com.tencent.devops.store.pojo.common.KEY_CREATOR
import com.tencent.devops.store.pojo.common.KEY_MODIFIER
import com.tencent.devops.store.pojo.common.KEY_PUBLISHER
import com.tencent.devops.store.pojo.common.KEY_PUB_TIME
import com.tencent.devops.store.pojo.common.KEY_UPDATE_TIME
import com.tencent.devops.store.pojo.common.KEY_VERSION_LOG_CONTENT
import com.tencent.devops.store.pojo.common.enums.StoreProjectTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.image.enums.ImageStatusEnum
import com.tencent.devops.store.pojo.image.request.OpImageSortTypeEnum
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Record2
import org.jooq.Record3
import org.jooq.Result
import org.jooq.SelectHavingStep
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * @Description
 * @Date 2019/11/26
 * @Version 1.0
 */
@Repository
class OpImageDao @Autowired constructor() {

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
            tImage.IMAGE_CODE.`as`(Constants.KEY_IMAGE_CODE),
            tImage.CREATE_TIME.max().`as`(KEY_CREATE_TIME)
        ).from(tImage).groupBy(tImage.IMAGE_CODE)
        val t = dslContext.select(tImage.IMAGE_CODE.`as`(Constants.KEY_IMAGE_CODE), tImage.IMAGE_STATUS.`as`(Constants.KEY_IMAGE_STATUS))
            .from(tImage).join(tmp)
            .on(
                tImage.IMAGE_CODE.eq(tmp.field(Constants.KEY_IMAGE_CODE, String::class.java)).and(
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
            .on(tImage.IMAGE_CODE.eq(t.field(Constants.KEY_IMAGE_CODE, String::class.java)))
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
        return baseStep.where(conditions).fetchOne(0, Int::class.java)!!
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
            tImage.IMAGE_CODE.`as`(Constants.KEY_IMAGE_CODE),
            tImage.CREATE_TIME.max().`as`(KEY_CREATE_TIME)
        ).from(tImage).groupBy(tImage.IMAGE_CODE)
        val t = dslContext.select(tImage.IMAGE_CODE.`as`(Constants.KEY_IMAGE_CODE), tImage.IMAGE_STATUS.`as`(Constants.KEY_IMAGE_STATUS))
            .from(tImage).join(tmp)
            .on(
                tImage.IMAGE_CODE.eq(tmp.field(Constants.KEY_IMAGE_CODE, String::class.java)).and(
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
            tImage.ID.`as`(Constants.KEY_IMAGE_ID),
            tImage.IMAGE_NAME.`as`(Constants.KEY_IMAGE_NAME),
            tImage.IMAGE_CODE.`as`(Constants.KEY_IMAGE_CODE),
            tImage.IMAGE_SOURCE_TYPE.`as`(Constants.KEY_IMAGE_SOURCE_TYPE),
            tImage.VERSION.`as`(Constants.KEY_IMAGE_VERSION),
            tImage.IMAGE_STATUS.`as`(Constants.KEY_IMAGE_STATUS),
            tClassify.CLASSIFY_CODE.`as`(KEY_CLASSIFY_CODE),
            tClassify.CLASSIFY_NAME.`as`(KEY_CLASSIFY_NAME),
            tImage.PUBLISHER.`as`(KEY_PUBLISHER),
            tImage.PUB_TIME.`as`(KEY_PUB_TIME),
            tImageVersionLog.CONTENT.`as`(KEY_VERSION_LOG_CONTENT),
            tImage.LATEST_FLAG.`as`(Constants.KEY_IMAGE_LATEST_FLAG),
            tImageFeature.PUBLIC_FLAG.`as`(Constants.KEY_IMAGE_FEATURE_PUBLIC_FLAG),
            tImageFeature.RECOMMEND_FLAG.`as`(Constants.KEY_IMAGE_FEATURE_RECOMMEND_FLAG),
            tImageFeature.WEIGHT.`as`(Constants.KEY_IMAGE_FEATURE_WEIGHT),
            tImageFeature.IMAGE_TYPE.`as`(Constants.KEY_IMAGE_RD_TYPE),
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
            .on(tImage.IMAGE_CODE.eq(t.field(Constants.KEY_IMAGE_CODE, String::class.java)))
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
        if (null != sortType) {
            if (desc != null && desc) {
                baseStep.where(conditions).orderBy(DSL.field(sortType.name).desc())
            } else {
                baseStep.where(conditions).orderBy(DSL.field(sortType.name).asc())
            }
        } else {
            baseStep.where(conditions)
        }
        // 分页
        val limitStep = if (page != null && page > 0 && pageSize != null && pageSize > 0) {
            baseStep.limit((page - 1) * pageSize, pageSize)
        } else {
            baseStep
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
                conditions.add(t.field(Constants.KEY_IMAGE_STATUS)!!.`in`(imageStatusList))
            } else {
                val imageStatusList = listOf(
                    ImageStatusEnum.AUDIT_REJECT.status.toByte(),
                    ImageStatusEnum.RELEASED.status.toByte(),
                    ImageStatusEnum.GROUNDING_SUSPENSION.status.toByte(),
                    ImageStatusEnum.UNDERCARRIAGING.status.toByte(),
                    ImageStatusEnum.UNDERCARRIAGED.status.toByte()
                )
                conditions.add(t.field(KEY_IMAGE_STATUS)!!.`in`(imageStatusList))
            }
        }
        return conditions
    }

    fun listAllImages(dslContext: DSLContext): Result<Record3<String, String, Byte>>? {
        val tImage = TImage.T_IMAGE.`as`("tImage")
        return dslContext.select(
            tImage.ID.`as`(KEY_IMAGE_ID),
            tImage.IMAGE_CODE.`as`(KEY_IMAGE_CODE),
            tImage.IMAGE_STATUS.`as`(KEY_IMAGE_STATUS)
        ).from(tImage)
            .where(tImage.DELETE_FLAG.eq(false))
            .fetch()
    }

    fun listProjectImages(dslContext: DSLContext, projectCode: String): Result<Record3<String, String, Byte>>? {
        val tStoreProjectRel = TStoreProjectRel.T_STORE_PROJECT_REL.`as`("tStoreProjectRel")
        val tImage = TImage.T_IMAGE.`as`("tImage")
        return dslContext.select(
            tImage.ID.`as`(KEY_IMAGE_ID),
            tImage.IMAGE_CODE.`as`(KEY_IMAGE_CODE),
            tImage.IMAGE_STATUS.`as`(KEY_IMAGE_STATUS)
        ).from(tStoreProjectRel).join(tImage)
            .on(tStoreProjectRel.STORE_CODE.eq(tImage.IMAGE_CODE))
            .where(tStoreProjectRel.STORE_TYPE.eq(StoreTypeEnum.IMAGE.type.toByte()))
            .and(tStoreProjectRel.TYPE.eq(StoreProjectTypeEnum.TEST.type.toByte()))
            .and(tStoreProjectRel.PROJECT_CODE.eq(projectCode))
            .and(tImage.DELETE_FLAG.eq(false))
            .fetch()
    }

    fun countImageByRepoInfo(dslContext: DSLContext, repoUrl: String?, repoName: String?, tag: String?): Int {
        val tImage = TImage.T_IMAGE.`as`("tImage")
        val conditions = mutableListOf<Condition>()
        conditions.add(tImage.DELETE_FLAG.eq(false))
        if (repoUrl != null) {
            conditions.add(tImage.IMAGE_REPO_URL.eq(repoUrl))
        }
        if (repoName != null) {
            conditions.add(tImage.IMAGE_REPO_NAME.eq(repoName))
        }
        if (tag != null) {
            conditions.add(tImage.IMAGE_TAG.eq(tag))
        }
        return dslContext.selectCount().from(tImage)
            .where(conditions)
            .fetchOne(0, Int::class.java)!!
    }

    fun getImagesByRepoInfo(dslContext: DSLContext, repoUrl: String?, repoName: String?, tag: String?): Result<TImageRecord>? {
        val tImage = TImage.T_IMAGE.`as`("tImage")
        val conditions = mutableListOf<Condition>()
        conditions.add(tImage.DELETE_FLAG.eq(false))
        if (repoUrl != null) {
            conditions.add(tImage.IMAGE_REPO_URL.eq(repoUrl))
        }
        if (repoName != null) {
            conditions.add(tImage.IMAGE_REPO_NAME.eq(repoName))
        }
        if (tag != null) {
            conditions.add(tImage.IMAGE_TAG.eq(tag))
        }
        return dslContext.selectFrom(tImage)
            .where(conditions)
            .fetch()
    }

    private val logger = LoggerFactory.getLogger(OpImageDao::class.java)
}
