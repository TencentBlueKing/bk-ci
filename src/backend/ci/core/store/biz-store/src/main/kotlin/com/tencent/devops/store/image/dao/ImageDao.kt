/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.store.image.dao

import com.tencent.devops.common.api.constant.KEY_SUMMARY
import com.tencent.devops.common.api.constant.KEY_VERSION
import com.tencent.devops.common.api.constant.KEY_WEIGHT
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.db.utils.skipCheck
import com.tencent.devops.common.pipeline.type.docker.ImageType
import com.tencent.devops.model.store.tables.TClassify
import com.tencent.devops.model.store.tables.TImage
import com.tencent.devops.model.store.tables.TImageFeature
import com.tencent.devops.model.store.tables.TImageLabelRel
import com.tencent.devops.model.store.tables.TLabel
import com.tencent.devops.model.store.tables.TStoreMember
import com.tencent.devops.model.store.tables.TStoreProjectRel
import com.tencent.devops.model.store.tables.records.TImageRecord
import com.tencent.devops.store.common.utils.VersionUtils
import com.tencent.devops.store.image.dao.Constants.KEY_IMAGE_AGENT_TYPE_SCOPE
import com.tencent.devops.store.image.dao.Constants.KEY_IMAGE_CODE
import com.tencent.devops.store.image.dao.Constants.KEY_IMAGE_FEATURE_CERTIFICATION_FLAG
import com.tencent.devops.store.image.dao.Constants.KEY_IMAGE_FEATURE_PUBLIC_FLAG
import com.tencent.devops.store.image.dao.Constants.KEY_IMAGE_FEATURE_RECOMMEND_FLAG
import com.tencent.devops.store.image.dao.Constants.KEY_IMAGE_ID
import com.tencent.devops.store.image.dao.Constants.KEY_IMAGE_NAME
import com.tencent.devops.store.image.dao.Constants.KEY_IMAGE_REPO_NAME
import com.tencent.devops.store.image.dao.Constants.KEY_IMAGE_REPO_URL
import com.tencent.devops.store.image.dao.Constants.KEY_IMAGE_SIZE
import com.tencent.devops.store.image.dao.Constants.KEY_IMAGE_SOURCE_TYPE
import com.tencent.devops.store.image.dao.Constants.KEY_IMAGE_STATUS
import com.tencent.devops.store.image.dao.Constants.KEY_IMAGE_TAG
import com.tencent.devops.store.image.dao.Constants.KEY_IMAGE_TYPE
import com.tencent.devops.store.image.dao.Constants.KEY_IMAGE_VERSION
import com.tencent.devops.store.pojo.common.KEY_CLASSIFY_CODE
import com.tencent.devops.store.pojo.common.KEY_CLASSIFY_ID
import com.tencent.devops.store.pojo.common.KEY_CLASSIFY_NAME
import com.tencent.devops.store.pojo.common.KEY_CREATE_TIME
import com.tencent.devops.store.pojo.common.KEY_CREATOR
import com.tencent.devops.store.pojo.common.KEY_ICON
import com.tencent.devops.store.pojo.common.KEY_LATEST_FLAG
import com.tencent.devops.store.pojo.common.KEY_LOGO_URL
import com.tencent.devops.store.pojo.common.KEY_MODIFIER
import com.tencent.devops.store.pojo.common.KEY_PUBLISHER
import com.tencent.devops.store.pojo.common.KEY_PUB_TIME
import com.tencent.devops.store.pojo.common.KEY_RECOMMEND_FLAG
import com.tencent.devops.store.pojo.common.KEY_UPDATE_TIME
import com.tencent.devops.store.pojo.common.enums.StoreProjectTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.image.enums.ImageAgentTypeEnum
import com.tencent.devops.store.pojo.image.enums.ImageRDTypeEnum
import com.tencent.devops.store.pojo.image.enums.ImageStatusEnum
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.Record
import org.jooq.Record10
import org.jooq.Record15
import org.jooq.Record5
import org.jooq.Record9
import org.jooq.Result
import org.jooq.SelectOnConditionStep
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Suppress("ALL")
@Repository
class ImageDao {

    data class ImageUpdateBean constructor(
        val imageName: String?,
        val classifyId: String?,
        val version: String?,
        val imageSourceType: ImageType?,
        val imageRepoUrl: String?,
        val imageRepoName: String?,
        val ticketId: String?,
        val imageStatus: ImageStatusEnum?,
        val imageStatusMsg: String?,
        val imageSize: String?,
        val imageTag: String?,
        val dockerFileType: String?,
        val dockerFileContent: String?,
        val agentTypeList: List<ImageAgentTypeEnum>,
        val logoUrl: String?,
        val icon: String?,
        val summary: String?,
        val description: String?,
        val publisher: String?,
        val latestFlag: Boolean?,
        val modifier: String?
    )

    fun countByName(
        dslContext: DSLContext,
        imageName: String,
        imageCode: String? = null
    ): Int {
        with(TImage.T_IMAGE) {
            val conditions = mutableListOf<Condition>()
            conditions.add(IMAGE_NAME.eq(imageName))
            if (imageCode != null) {
                conditions.add(IMAGE_CODE.eq(imageCode))
            }
            return dslContext.selectCount().from(this).where(conditions).fetchOne(0, Int::class.java)!!
        }
    }

    fun countByNameAndClassifyId(
        dslContext: DSLContext,
        imageName: String?,
        classifyId: String?
    ): Int {
        with(TImage.T_IMAGE) {
            val conditions = mutableListOf<Condition>()
            val baseStep = dslContext.selectCount().from(this)
            if (!imageName.isNullOrBlank()) {
                conditions.add(IMAGE_NAME.eq(imageName))
            }
            if (!classifyId.isNullOrBlank()) {
                conditions.add(CLASSIFY_ID.eq(classifyId))
            }
            baseStep.where(conditions)
            return baseStep.fetchOne(0, Int::class.java)!!
        }
    }

    fun countByUserIdAndName(
        dslContext: DSLContext,
        userId: String,
        imageName: String?
    ): Int {
        with(TImage.T_IMAGE) {
            val tStoreMember = TStoreMember.T_STORE_MEMBER
            val conditions = mutableListOf<Condition>()
            val baseStep = dslContext.select(DSL.countDistinct(IMAGE_CODE)).from(this)
                .join(tStoreMember)
                .on(this.IMAGE_CODE.eq(tStoreMember.STORE_CODE))
            if (!imageName.isNullOrBlank()) {
                conditions.add(IMAGE_NAME.contains(imageName))
            }
            conditions.add(tStoreMember.USERNAME.eq(userId))
            conditions.add(DELETE_FLAG.eq(false))
            conditions.add(tStoreMember.STORE_TYPE.eq(StoreTypeEnum.IMAGE.type.toByte()))
            baseStep.where(conditions)
            return baseStep.fetch()[0].value1()
        }
    }

    fun countByNameLike(dslContext: DSLContext, imageName: String): Int {
        with(TImage.T_IMAGE) {
            return dslContext.selectCount().from(this).where(IMAGE_NAME.like(imageName)).fetchOne(0, Int::class.java)!!
        }
    }

    fun countByCode(dslContext: DSLContext, imageCode: String): Int {
        with(TImage.T_IMAGE) {
            return dslContext.selectCount().from(this).where(IMAGE_CODE.eq(imageCode)).fetchOne(0, Int::class.java)!!
        }
    }

    fun countReleaseImageByTag(
        dslContext: DSLContext,
        imageCode: String?,
        imageRepoUrl: String?,
        imageRepoName: String,
        imageTag: String?
    ): Int {
        with(TImage.T_IMAGE) {
            val conditions = mutableListOf<Condition>()
            val baseStep = dslContext.selectCount().from(this)
            if (!imageCode.isNullOrBlank()) {
                conditions.add(IMAGE_CODE.eq(imageCode))
            }
            conditions.add(IMAGE_REPO_URL.eq(imageRepoUrl))
            conditions.add(IMAGE_REPO_NAME.eq(imageRepoName))
            conditions.add(IMAGE_TAG.eq(imageTag))
            conditions.add(IMAGE_STATUS.eq(ImageStatusEnum.RELEASED.status.toByte()))
            return baseStep.where(conditions).fetchOne(0, Int::class.java)!!
        }
    }

    fun getImage(dslContext: DSLContext, imageId: String): TImageRecord? {
        return with(TImage.T_IMAGE) {
            dslContext.selectFrom(this)
                .where(ID.eq(imageId))
                .fetchOne()
        }
    }

    fun deleteByImageIds(dslContext: DSLContext, imageIds: List<String>) {
        with(TImage.T_IMAGE) {
            dslContext.deleteFrom(this)
                .where(ID.`in`(imageIds))
                .execute()
        }
    }

    fun getImageByCodeAndVersion(dslContext: DSLContext, imageCode: String, imageVersion: String): TImageRecord? {
        return with(TImage.T_IMAGE) {
            val query = dslContext.selectFrom(this)
                .where(IMAGE_CODE.eq(imageCode))
                .and(VERSION.like(VersionUtils.generateQueryVersion(imageVersion)))
            query.orderBy(VERSION.desc()).fetchAny()
        }
    }

    fun getLatestImageByCode(dslContext: DSLContext, imageCode: String): TImageRecord? {
        return with(TImage.T_IMAGE) {
            val query = dslContext.selectFrom(this)
                .where(IMAGE_CODE.eq(imageCode))
                .and(LATEST_FLAG.eq(true))
            query.fetchOne()
        }
    }

    fun getImagesByBaseVersion(
        dslContext: DSLContext,
        imageCode: String,
        imageStatusSet: Set<Byte>,
        baseVersion: String?
    ): Result<Record10<String, String, String, String, String, String, String, String, String, String>>? {
        val tImage = TImage.T_IMAGE
        val tStoreProjectRel = TStoreProjectRel.T_STORE_PROJECT_REL
        val conditions = mutableSetOf<Condition>()
        conditions.add(tImage.IMAGE_CODE.eq(imageCode))
        conditions.add(tImage.IMAGE_STATUS.`in`(imageStatusSet))
        conditions.add(tStoreProjectRel.STORE_TYPE.eq(StoreTypeEnum.IMAGE.type.toByte()))
        conditions.add(tStoreProjectRel.TYPE.eq(StoreProjectTypeEnum.INIT.type.toByte()))
        if (null != baseVersion) {
            conditions.add(tImage.VERSION.like(VersionUtils.generateQueryVersion(baseVersion)))
        }
        val baseStep = dslContext.select(
            tImage.ID.`as`(KEY_IMAGE_ID),
            tImage.IMAGE_CODE.`as`(KEY_IMAGE_CODE),
            tImage.VERSION.`as`(KEY_IMAGE_VERSION),
            tImage.IMAGE_NAME.`as`(KEY_IMAGE_NAME),
            tImage.IMAGE_SOURCE_TYPE.`as`(KEY_IMAGE_SOURCE_TYPE),
            tImage.IMAGE_REPO_URL.`as`(KEY_IMAGE_REPO_URL),
            tImage.IMAGE_REPO_NAME.`as`(KEY_IMAGE_REPO_NAME),
            tImage.IMAGE_TAG.`as`(KEY_IMAGE_TAG),
            tImage.TICKET_ID.`as`(Constants.KEY_IMAGE_TICKET_ID),
            tStoreProjectRel.PROJECT_CODE.`as`(Constants.KEY_IMAGE_INIT_PROJECT)
        ).from(tImage).join(tStoreProjectRel).on(tImage.IMAGE_CODE.eq(tStoreProjectRel.STORE_CODE))
        return baseStep.where(conditions)
            .fetch()
    }

    fun getImage(dslContext: DSLContext, imageCode: String, version: String): TImageRecord? {
        return with(TImage.T_IMAGE) {
            dslContext.selectFrom(this)
                .where(IMAGE_CODE.eq(imageCode).and(VERSION.like(VersionUtils.generateQueryVersion(version))))
                .orderBy(CREATE_TIME.desc())
                .limit(1)
                .fetchOne()
        }
    }

    fun countImageRelease(
        dslContext: DSLContext,
        imageCode: String,
        version: String,
        imageStatus: ImageStatusEnum
    ): Long {
        val tImage = TImage.T_IMAGE
        val conditions = mutableSetOf<Condition>()
        if (VersionUtils.isLatestVersion(version)) {
            conditions.add(tImage.VERSION.like(VersionUtils.generateQueryVersion(version)))
        } else {
            conditions.add(tImage.VERSION.eq(version))
        }
        conditions.add(tImage.IMAGE_CODE.eq(imageCode))
        conditions.add(tImage.IMAGE_STATUS.eq(imageStatus.status.toByte()))
        return dslContext.selectCount()
            .from(tImage)
            .where(
                conditions
            )
            .fetchOne(0, Long::class.java)!!
    }

    fun getJobImageCount(
        dslContext: DSLContext,
        projectCode: String,
        agentType: ImageAgentTypeEnum?,
        isContainAgentType: Boolean?,
        recommendFlag: Boolean?,
        classifyId: String?
    ): Long {
        val tImage = TImage.T_IMAGE
        val tStoreProjectRel = TStoreProjectRel.T_STORE_PROJECT_REL
        val tImageFeature = TImageFeature.T_IMAGE_FEATURE
        // 公共镜像查询条件组装
        val publicImageCondition =
            queryPublicImageCondition(
                tImage = tImage,
                tImageFeature = tImageFeature,
                agentType = agentType,
                isContainAgentType = isContainAgentType,
                classifyId = classifyId,
                recommendFlag = recommendFlag
            )
        // 普通镜像查询条件组装
        val normalImageConditions =
            queryNormalImageCondition(
                tImage = tImage,
                tStoreProjectRel = tStoreProjectRel,
                tImageFeature = tImageFeature,
                projectCode = projectCode,
                agentType = agentType,
                isContainAgentType = isContainAgentType,
                classifyId = classifyId,
                recommendFlag = recommendFlag
            )
        // 开发者测试镜像查询条件组装
        val initTestImageCondition =
            queryTestImageCondition(
                tImage = tImage,
                tStoreProjectRel = tStoreProjectRel,
                tImageFeature = tImageFeature,
                projectCode = projectCode,
                agentType = agentType,
                isContainAgentType = isContainAgentType,
                classifyId = classifyId,
                recommendFlag = recommendFlag
            )
        // 公共镜像和普通镜像需排除初始化项目下面有处于测试中或者审核中的镜像
        publicImageCondition.add(
            tImage.IMAGE_CODE.notIn(
                dslContext.select(tImage.IMAGE_CODE).from(tImage).join(tStoreProjectRel)
                    .on(tImage.IMAGE_CODE.eq(tStoreProjectRel.STORE_CODE)).where(
                    initTestImageCondition
                )
            )
        )
        normalImageConditions.add(
            tImage.IMAGE_CODE.notIn(
                dslContext.select(tImage.IMAGE_CODE).from(tImage).join(tStoreProjectRel).on(
                    tImage.IMAGE_CODE.eq(
                        tStoreProjectRel.STORE_CODE
                    )
                ).where(initTestImageCondition)
            )
        )
        val publicImageCount =
            dslContext.select(DSL.countDistinct(tImage.IMAGE_CODE)).from(tImage)
                .join(tImageFeature).on(tImage.IMAGE_CODE.eq(tImageFeature.IMAGE_CODE)).where(publicImageCondition)
                .fetchOne(0, Long::class.java)!!
        val normalImageCount =
            dslContext.select(DSL.countDistinct(tImage.IMAGE_CODE)).from(tImage)
                .join(tStoreProjectRel).on(tImage.IMAGE_CODE.eq(tStoreProjectRel.STORE_CODE)).join(tImageFeature)
                .on(tImage.IMAGE_CODE.eq(tImageFeature.IMAGE_CODE)).where(normalImageConditions)
                .fetchOne(0, Long::class.java)!!
        val initTestImageCount =
            dslContext.select(DSL.countDistinct(tImage.IMAGE_CODE)).from(tImage)
                .join(tStoreProjectRel).on(tImage.IMAGE_CODE.eq(tStoreProjectRel.STORE_CODE)).join(tImageFeature)
                .on(tImage.IMAGE_CODE.eq(tImageFeature.IMAGE_CODE)).where(initTestImageCondition)
                .fetchOne(0, Long::class.java)!!
        return publicImageCount + normalImageCount + initTestImageCount
    }

    fun getJobImages(
        dslContext: DSLContext,
        projectCode: String,
        agentType: ImageAgentTypeEnum?,
        isContainAgentType: Boolean?,
        classifyId: String?,
        recommendFlag: Boolean?,
        page: Int?,
        pageSize: Int?,
        offsetNum: Int? = 0
    ): Result<out Record>? {
        val tImage = TImage.T_IMAGE
        val tClassify = TClassify.T_CLASSIFY
        val tStoreProjectRel = TStoreProjectRel.T_STORE_PROJECT_REL
        val tImageFeature = TImageFeature.T_IMAGE_FEATURE
        val tImageLabelRel = TImageLabelRel.T_IMAGE_LABEL_REL
        val tLabel = TLabel.T_LABEL
        // 公共镜像查询条件组装
        val publicImageCondition =
            queryPublicImageCondition(
                tImage = tImage,
                tImageFeature = tImageFeature,
                agentType = agentType,
                isContainAgentType = isContainAgentType,
                classifyId = classifyId,
                recommendFlag = recommendFlag
            )
        // 普通镜像查询条件组装
        val normalImageConditions =
            queryNormalImageCondition(
                tImage = tImage,
                tStoreProjectRel = tStoreProjectRel,
                tImageFeature = tImageFeature,
                projectCode = projectCode,
                agentType = agentType,
                isContainAgentType = isContainAgentType,
                classifyId = classifyId,
                recommendFlag = recommendFlag
            )
        // 开发者测试镜像查询条件组装
        val initTestImageCondition =
            queryTestImageCondition(
                tImage = tImage,
                tStoreProjectRel = tStoreProjectRel,
                tImageFeature = tImageFeature,
                projectCode = projectCode,
                agentType = agentType,
                isContainAgentType = isContainAgentType,
                classifyId = classifyId,
                recommendFlag = recommendFlag
            )
        // 公共镜像和普通镜像需排除初始化项目下面有处于测试中或者审核中的镜像
        publicImageCondition.add(
            tImage.IMAGE_CODE.notIn(
                dslContext.select(tImage.IMAGE_CODE).from(tImage).join(tStoreProjectRel)
                    .on(tImage.IMAGE_CODE.eq(tStoreProjectRel.STORE_CODE)).where(
                    initTestImageCondition
                )
            )
        )
        normalImageConditions.add(
            tImage.IMAGE_CODE.notIn(
                dslContext.select(tImage.IMAGE_CODE).from(tImage).join(tStoreProjectRel).on(
                    tImage.IMAGE_CODE.eq(
                        tStoreProjectRel.STORE_CODE
                    )
                ).where(initTestImageCondition)
            )
        )
        val labelNames = dslContext.select(DSL.groupConcatDistinct(DSL.concat(tLabel.LABEL_NAME)))
            .from(tImageLabelRel).join(tLabel).on(tImageLabelRel.LABEL_ID.eq(tLabel.ID))
            .where(tImage.ID.eq(tImageLabelRel.IMAGE_ID)).asField<String>("labelNames")
        val t = getJobImageBaseStep(dslContext, tImage, tClassify, tImageFeature, labelNames)
            .join(tStoreProjectRel)
            .on(tImage.IMAGE_CODE.eq(tStoreProjectRel.STORE_CODE))
            .where(normalImageConditions)
            .union(
                getJobImageBaseStep(dslContext, tImage, tClassify, tImageFeature, labelNames)
                    .where(publicImageCondition)
            )
            .union(
                getJobImageBaseStep(dslContext, tImage, tClassify, tImageFeature, labelNames)
                    .join(tStoreProjectRel)
                    .on(tImage.IMAGE_CODE.eq(tStoreProjectRel.STORE_CODE))
                    .where(initTestImageCondition)
            )
            .asTable("t")
        val baseStep = dslContext.select().from(t).orderBy(t.field("weight")!!.desc(), t.field("imageName")!!.asc())
        return if (null != page && null != pageSize) {
            baseStep.limit((page - 1) * pageSize + (offsetNum ?: 0), pageSize).skipCheck().fetch()
        } else {
            baseStep.skipCheck().fetch()
        }
    }

    private fun getJobImageBaseStep(
        dslContext: DSLContext,
        tImage: TImage,
        tClassify: TClassify,
        tImageFeature: TImageFeature,
        labelNames: Field<String>?
    ): SelectOnConditionStep<Record> {
        return dslContext.select(
            tImage.ID.`as`(KEY_IMAGE_ID),
            tImage.IMAGE_CODE.`as`(KEY_IMAGE_CODE),
            tImage.IMAGE_NAME.`as`(KEY_IMAGE_NAME),
            tImage.VERSION.`as`(KEY_VERSION),
            tImage.IMAGE_STATUS.`as`(KEY_IMAGE_STATUS),
            tClassify.ID.`as`(KEY_CLASSIFY_ID),
            tClassify.CLASSIFY_CODE.`as`(KEY_CLASSIFY_CODE),
            tClassify.CLASSIFY_NAME.`as`(KEY_CLASSIFY_NAME),
            tImage.LOGO_URL.`as`(KEY_LOGO_URL),
            tImage.ICON.`as`(KEY_ICON),
            tImage.SUMMARY.`as`(KEY_SUMMARY),
            tImage.PUBLISHER.`as`(KEY_PUBLISHER),
            tImage.PUB_TIME.`as`(KEY_PUB_TIME),
            tImage.CREATOR.`as`(KEY_CREATOR),
            tImage.CREATE_TIME.`as`(KEY_CREATE_TIME),
            tImage.LATEST_FLAG.`as`(KEY_LATEST_FLAG),
            tImage.AGENT_TYPE_SCOPE.`as`(KEY_IMAGE_AGENT_TYPE_SCOPE),
            tImage.IMAGE_SOURCE_TYPE.`as`(KEY_IMAGE_SOURCE_TYPE),
            tImage.IMAGE_REPO_URL.`as`(KEY_IMAGE_REPO_URL),
            tImage.IMAGE_REPO_NAME.`as`(KEY_IMAGE_REPO_NAME),
            tImage.IMAGE_TAG.`as`(KEY_IMAGE_TAG),
            tImage.IMAGE_SIZE.`as`(KEY_IMAGE_SIZE),
            tImage.MODIFIER.`as`(KEY_MODIFIER),
            tImage.UPDATE_TIME.`as`(KEY_UPDATE_TIME),
            tImageFeature.CERTIFICATION_FLAG.`as`(KEY_IMAGE_FEATURE_CERTIFICATION_FLAG),
            tImageFeature.PUBLIC_FLAG.`as`(KEY_IMAGE_FEATURE_PUBLIC_FLAG),
            tImageFeature.IMAGE_TYPE.`as`(KEY_IMAGE_TYPE),
            tImageFeature.WEIGHT.`as`(KEY_WEIGHT),
            tImageFeature.RECOMMEND_FLAG.`as`(KEY_RECOMMEND_FLAG),
            labelNames
        )
            .from(tImage)
            .join(tClassify)
            .on(tImage.CLASSIFY_ID.eq(tClassify.ID))
            .leftJoin(tImageFeature)
            .on(tImage.IMAGE_CODE.eq(tImageFeature.IMAGE_CODE))
    }

    private fun setQueryImageBaseCondition(
        tImage: TImage,
        tImageFeature: TImageFeature,
        agentType: ImageAgentTypeEnum?,
        isContainAgentType: Boolean?,
        classifyId: String?,
        recommendFlag: Boolean?
    ): MutableList<Condition> {
        val conditions = mutableListOf<Condition>()
        if (agentType != null) {
            if (null != isContainAgentType && !isContainAgentType) {
                conditions.add(tImage.AGENT_TYPE_SCOPE.notLike("%${agentType.name}%"))
            } else if (null != isContainAgentType && isContainAgentType) {
                conditions.add(tImage.AGENT_TYPE_SCOPE.contains(agentType.name))
            }
        }
        if (!classifyId.isNullOrBlank()) conditions.add(tImage.CLASSIFY_ID.eq(classifyId))
        if (recommendFlag != null) conditions.add(tImageFeature.RECOMMEND_FLAG.eq(recommendFlag))
        conditions.add(tImage.DELETE_FLAG.eq(false)) // 只查没有被删除的镜像
        return conditions
    }

    private fun queryPublicImageCondition(
        tImage: TImage,
        tImageFeature: TImageFeature,
        agentType: ImageAgentTypeEnum?,
        isContainAgentType: Boolean?,
        classifyId: String?,
        recommendFlag: Boolean?
    ): MutableList<Condition> {
        val conditions = setQueryImageBaseCondition(
            tImage = tImage,
            tImageFeature = tImageFeature,
            agentType = agentType,
            isContainAgentType = isContainAgentType,
            classifyId = classifyId,
            recommendFlag = recommendFlag
        )
        conditions.add(tImage.IMAGE_STATUS.eq(ImageStatusEnum.RELEASED.status.toByte())) // 只查已发布的
        conditions.add(tImageFeature.PUBLIC_FLAG.eq(true)) // 查公共镜像（所有项目都可用）
        conditions.add(tImage.LATEST_FLAG.eq(true)) // 只查最新版本的镜像
        return conditions
    }

    private fun queryNormalImageCondition(
        tImage: TImage,
        tStoreProjectRel: TStoreProjectRel,
        tImageFeature: TImageFeature,
        projectCode: String,
        agentType: ImageAgentTypeEnum?,
        isContainAgentType: Boolean?,
        classifyId: String?,
        recommendFlag: Boolean?
    ): MutableList<Condition> {
        val conditions = setQueryImageBaseCondition(
            tImage = tImage,
            tImageFeature = tImageFeature,
            agentType = agentType,
            isContainAgentType = isContainAgentType,
            classifyId = classifyId,
            recommendFlag = recommendFlag
        )
        conditions.add(tImage.IMAGE_STATUS.eq(ImageStatusEnum.RELEASED.status.toByte())) // 只查已发布的
        conditions.add(tImageFeature.PUBLIC_FLAG.eq(false)) // 查普通镜像
        conditions.add(tImage.LATEST_FLAG.eq(true)) // 只查最新版本的镜像
        conditions.add(tStoreProjectRel.PROJECT_CODE.eq(projectCode))
        conditions.add(tStoreProjectRel.STORE_TYPE.eq(StoreTypeEnum.IMAGE.type.toByte()))
        return conditions
    }

    private fun queryTestImageCondition(
        tImage: TImage,
        tStoreProjectRel: TStoreProjectRel,
        tImageFeature: TImageFeature,
        projectCode: String,
        agentType: ImageAgentTypeEnum?,
        isContainAgentType: Boolean?,
        classifyId: String?,
        recommendFlag: Boolean?
    ): MutableList<Condition> {
        val conditions = setQueryImageBaseCondition(
            tImage = tImage,
            tImageFeature = tImageFeature,
            agentType = agentType,
            isContainAgentType = isContainAgentType,
            classifyId = classifyId,
            recommendFlag = recommendFlag
        )
        // 只查测试中和审核中的插件
        conditions.add(
            tImage.IMAGE_STATUS.`in`(
                listOf(
                    ImageStatusEnum.TESTING.status.toByte(),
                    ImageStatusEnum.AUDITING.status.toByte()
                )
            )
        )
        conditions.add(tStoreProjectRel.PROJECT_CODE.eq(projectCode))
        // 镜像的调试项目
        conditions.add(tStoreProjectRel.TYPE.eq(StoreProjectTypeEnum.TEST.type.toByte()))
        conditions.add(tStoreProjectRel.STORE_TYPE.eq(StoreTypeEnum.IMAGE.type.toByte()))
        return conditions
    }

    /**
     * 用于工作台搜索接口
     */
    fun listImageByNameLike(
        dslContext: DSLContext,
        userId: String,
        imageName: String?,
        page: Int? = 1,
        pageSize: Int? = -1
    ): Result<Record15<String, String, String, String, String, String, String, String, String, Byte, String, String, LocalDateTime, LocalDateTime, Boolean>>? {
        val tImage = TImage.T_IMAGE
        val tImageFeature = TImageFeature.T_IMAGE_FEATURE
        val tStoreMember = TStoreMember.T_STORE_MEMBER
        val conditions = generateGetMyImageConditions(tImage, userId, tStoreMember, imageName)
        val t =
            dslContext.select(tImage.IMAGE_CODE.`as`(KEY_IMAGE_CODE), DSL.max(tImage.CREATE_TIME).`as`(KEY_CREATE_TIME))
                .from(tImage).groupBy(tImage.IMAGE_CODE) // 查找每组atomCode最新的记录
        val query = dslContext.select(
            tImage.ID.`as`(KEY_IMAGE_ID),
            tImage.IMAGE_CODE.`as`(KEY_IMAGE_CODE),
            tImage.IMAGE_NAME.`as`(KEY_IMAGE_NAME),
            tImage.IMAGE_SOURCE_TYPE.`as`(KEY_IMAGE_SOURCE_TYPE),
            tImage.IMAGE_REPO_URL.`as`(KEY_IMAGE_REPO_URL),
            tImage.IMAGE_REPO_NAME.`as`(KEY_IMAGE_REPO_NAME),
            tImage.VERSION.`as`(KEY_IMAGE_VERSION),
            tImage.IMAGE_TAG.`as`(KEY_IMAGE_TAG),
            tImage.IMAGE_SIZE.`as`(KEY_IMAGE_SIZE),
            tImage.IMAGE_STATUS.`as`(KEY_IMAGE_STATUS),
            tImage.CREATOR.`as`(KEY_CREATOR),
            tImage.MODIFIER.`as`(KEY_MODIFIER),
            tImage.CREATE_TIME.`as`(KEY_CREATE_TIME),
            tImage.UPDATE_TIME.`as`(KEY_UPDATE_TIME),
            tImageFeature.PUBLIC_FLAG.`as`(KEY_IMAGE_FEATURE_PUBLIC_FLAG)
        ).from(tImage)
            .join(tImageFeature)
            .on(tImage.IMAGE_CODE.eq(tImageFeature.IMAGE_CODE))
            .join(t)
            .on(
                tImage.IMAGE_CODE.eq(
                    t.field(
                        KEY_IMAGE_CODE,
                        String::class.java
                    )
                ).and(tImage.CREATE_TIME.eq(t.field(KEY_CREATE_TIME, LocalDateTime::class.java)))
            )
            .join(tStoreMember)
            .on(tImage.IMAGE_CODE.eq(tStoreMember.STORE_CODE))
            .where(conditions)
            .groupBy(tImage.IMAGE_CODE)
            .orderBy(tImage.UPDATE_TIME.desc())
        return if (pageSize != null && pageSize > 0 && page != null && page > 0) {
            query.limit((page - 1) * pageSize, pageSize).skipCheck().fetch()
        } else {
            query.skipCheck().fetch()
        }
    }

    private fun generateGetMyImageConditions(
        a: TImage,
        userId: String,
        b: TStoreMember,
        imageName: String?
    ): MutableList<Condition> {
        val conditions = mutableListOf<Condition>()
        conditions.add(b.USERNAME.eq(userId))
        conditions.add(b.STORE_TYPE.eq(StoreTypeEnum.IMAGE.type.toByte()))
        if (null != imageName) {
            conditions.add(a.IMAGE_NAME.contains(imageName))
        }
        conditions.add(a.DELETE_FLAG.eq(false)) // 只查没有被删除的镜像
        return conditions
    }

    /**
     * 根据imageCode获取镜像版本列表
     */
    fun listImageByCode(
        dslContext: DSLContext,
        imageCode: String?,
        page: Int? = 1,
        pageSize: Int? = -1
    ): Result<Record9<String, String, String, String, Byte, String, String, LocalDateTime, LocalDateTime>>? {
        val tImage = TImage.T_IMAGE
        val conditions = mutableListOf<Condition>()
        if (!imageCode.isNullOrBlank()) {
            conditions.add(tImage.IMAGE_CODE.eq(imageCode))
        }
        val query = dslContext.select(
            tImage.ID.`as`(KEY_IMAGE_ID),
            tImage.IMAGE_CODE.`as`(KEY_IMAGE_CODE),
            tImage.IMAGE_NAME.`as`(KEY_IMAGE_NAME),
            tImage.VERSION.`as`(KEY_IMAGE_VERSION),
            tImage.IMAGE_STATUS.`as`(KEY_IMAGE_STATUS),
            tImage.CREATOR.`as`(KEY_CREATOR),
            tImage.MODIFIER.`as`(KEY_MODIFIER),
            tImage.CREATE_TIME.`as`(KEY_CREATE_TIME),
            tImage.UPDATE_TIME.`as`(KEY_UPDATE_TIME)
        ).from(tImage)
            .where(conditions)
            .orderBy(tImage.CREATE_TIME.desc())
        return if (pageSize != null && pageSize > 0 && page != null && page > 0) {
            query.limit((page - 1) * pageSize, pageSize).fetch()
        } else {
            query.fetch()
        }
    }

    /**
     * 根据ID更新Image表中内容
     * imageCode与version字段不可修改
     * 其余字段均可修改
     */
    fun updateImage(
        dslContext: DSLContext,
        imageId: String,
        imageUpdateBean: ImageUpdateBean
    ) {
        with(TImage.T_IMAGE) {
            var baseQuery = dslContext.update(this).set(UPDATE_TIME, LocalDateTime.now())
            if (!imageUpdateBean.imageName.isNullOrBlank()) {
                baseQuery = baseQuery.set(IMAGE_NAME, imageUpdateBean.imageName)
            }
            if (!imageUpdateBean.classifyId.isNullOrBlank()) {
                baseQuery = baseQuery.set(CLASSIFY_ID, imageUpdateBean.classifyId)
            }
            if (!imageUpdateBean.version.isNullOrBlank()) {
                baseQuery = baseQuery.set(VERSION, imageUpdateBean.version)
            }
            if (imageUpdateBean.imageSourceType != null) {
                baseQuery = baseQuery.set(IMAGE_SOURCE_TYPE, imageUpdateBean.imageSourceType.type)
            }
            if (imageUpdateBean.imageRepoUrl != null) {
                baseQuery = baseQuery.set(IMAGE_REPO_URL, imageUpdateBean.imageRepoUrl)
            }
            if (!imageUpdateBean.imageRepoName.isNullOrBlank()) {
                baseQuery = baseQuery.set(IMAGE_REPO_NAME, imageUpdateBean.imageRepoName)
            }
            if (imageUpdateBean.ticketId != null) {
                baseQuery = baseQuery.set(TICKET_ID, imageUpdateBean.ticketId)
            }
            if (imageUpdateBean.imageStatus != null) {
                baseQuery = baseQuery.set(IMAGE_STATUS, imageUpdateBean.imageStatus.status.toByte())
            }
            if (imageUpdateBean.imageStatusMsg != null) {
                baseQuery = baseQuery.set(IMAGE_STATUS_MSG, imageUpdateBean.imageStatusMsg)
            }
            if (!imageUpdateBean.imageSize.isNullOrBlank()) {
                baseQuery = baseQuery.set(IMAGE_SIZE, imageUpdateBean.imageSize)
            }
            if (!imageUpdateBean.imageTag.isNullOrBlank()) {
                baseQuery = baseQuery.set(IMAGE_TAG, imageUpdateBean.imageTag)
            }
            if (!imageUpdateBean.dockerFileType.isNullOrBlank()) {
                baseQuery = baseQuery.set(DOCKER_FILE_TYPE, imageUpdateBean.dockerFileType)
            }
            if (imageUpdateBean.dockerFileContent != null) {
                baseQuery = baseQuery.set(DOCKER_FILE_CONTENT, imageUpdateBean.dockerFileContent)
            }
            if (imageUpdateBean.agentTypeList.isNotEmpty()) {
                baseQuery = baseQuery.set(AGENT_TYPE_SCOPE, JsonUtil.toJson(imageUpdateBean.agentTypeList))
            }
            if (!imageUpdateBean.logoUrl.isNullOrBlank()) {
                baseQuery = baseQuery.set(LOGO_URL, imageUpdateBean.logoUrl)
            }
            if (imageUpdateBean.icon != null) {
                baseQuery = baseQuery.set(ICON, imageUpdateBean.icon)
            }
            if (!imageUpdateBean.summary.isNullOrBlank()) {
                baseQuery = baseQuery.set(SUMMARY, imageUpdateBean.summary)
            }
            if (imageUpdateBean.description != null) {
                baseQuery = baseQuery.set(DESCRIPTION, imageUpdateBean.description)
            }
            if (!imageUpdateBean.publisher.isNullOrBlank()) {
                baseQuery = baseQuery.set(PUBLISHER, imageUpdateBean.publisher)
            }
            if (imageUpdateBean.latestFlag != null) {
                baseQuery = baseQuery.set(LATEST_FLAG, imageUpdateBean.latestFlag)
            }
            if (!imageUpdateBean.modifier.isNullOrBlank()) {
                baseQuery = baseQuery.set(MODIFIER, imageUpdateBean.modifier)
            }
            baseQuery.where(ID.eq(imageId)).execute()
        }
    }

    fun getVersionsByImageCode(
        dslContext: DSLContext,
        projectCode: String,
        imageCode: String,
        imageStatusList: List<Byte>
    ): Result<out Record>? {
        val tImage = TImage.T_IMAGE
        val tImageFeature = TImageFeature.T_IMAGE_FEATURE
        val tStoreProjectRel = TStoreProjectRel.T_STORE_PROJECT_REL
        val t = dslContext.select(
            tImage.VERSION.`as`(KEY_VERSION),
            tImage.IMAGE_TAG.`as`(KEY_IMAGE_TAG),
            tImage.CREATE_TIME.`as`(KEY_CREATE_TIME),
            tImage.IMAGE_STATUS.`as`(KEY_IMAGE_STATUS)
        ).from(tImage).join(tImageFeature).on(tImage.IMAGE_CODE.eq(tImageFeature.IMAGE_CODE))
            .where(
                tImage.IMAGE_CODE.eq(imageCode).and(tImageFeature.PUBLIC_FLAG.eq(true)).and(
                    tImage.IMAGE_STATUS.`in`(
                        imageStatusList
                    )
                )
            )
            .union(
                dslContext.select(
                    tImage.VERSION.`as`(KEY_VERSION),
                    tImage.IMAGE_TAG.`as`(KEY_IMAGE_TAG),
                    tImage.CREATE_TIME.`as`(KEY_CREATE_TIME),
                    tImage.IMAGE_STATUS.`as`(KEY_IMAGE_STATUS)
                ).from(tImage).join(tStoreProjectRel).on(tImage.IMAGE_CODE.eq(tStoreProjectRel.STORE_CODE))
                    .join(tImageFeature).on(tImage.IMAGE_CODE.eq(tImageFeature.IMAGE_CODE))
                    .where(
                        tImage.IMAGE_CODE.eq(imageCode).and(tImageFeature.PUBLIC_FLAG.eq(false)).and(
                            tImage.IMAGE_STATUS.`in`(
                                imageStatusList
                            )
                        )
                            .andExists(
                                dslContext.selectOne().from(tStoreProjectRel).where(
                                    tImage.IMAGE_CODE.eq(tStoreProjectRel.STORE_CODE).and(
                                        tStoreProjectRel.STORE_TYPE.eq(
                                            StoreTypeEnum.IMAGE.type.toByte()
                                        )
                                    ).and(tStoreProjectRel.PROJECT_CODE.eq(projectCode))
                                )
                            )
                    )
            )
            .asTable("t")
        return dslContext.select().from(t).orderBy(t.field(KEY_CREATE_TIME)!!.desc()).skipCheck().fetch()
    }

    /**
     * 统计分类下处于已发布状态的镜像个数
     */
    fun countReleaseImageNumByClassifyId(dslContext: DSLContext, classifyId: String): Int {
        with(TImage.T_IMAGE) {
            return dslContext.selectCount().from(this)
                .where(IMAGE_STATUS.eq(ImageStatusEnum.RELEASED.status.toByte()).and(CLASSIFY_ID.eq(classifyId)))
                .fetchOne(0, Int::class.java)!!
        }
    }

    /**
     * 统计还在使用处于下架中或者已下架状态的模板的项目的个数
     */
    fun countUndercarriageImageNumByClassifyId(dslContext: DSLContext, classifyId: String): Int {
        val tImage = TImage.T_IMAGE
        val tStoreProjectRel = TStoreProjectRel.T_STORE_PROJECT_REL
        val templateStatusList =
            listOf(ImageStatusEnum.UNDERCARRIAGING.status.toByte(), ImageStatusEnum.UNDERCARRIAGED.status.toByte())
        return dslContext.select(DSL.countDistinct(tStoreProjectRel.PROJECT_CODE)).from(tImage).join(tStoreProjectRel)
            .on(tImage.IMAGE_CODE.eq(tStoreProjectRel.STORE_CODE))
            .where(
                tImage.IMAGE_STATUS.`in`(templateStatusList).and(tImage.CLASSIFY_ID.eq(classifyId))
                    .and(tStoreProjectRel.STORE_TYPE.eq(StoreTypeEnum.IMAGE.type.toByte()))
            )
            .fetchOne(0, Int::class.java)!!
    }

    fun listByRepoNameAndTag(
        dslContext: DSLContext,
        projectId: String,
        repoName: String?,
        tag: String?
    ): Result<Record5<String, String, String, String, Boolean>>? {
        val tImage = TImage.T_IMAGE
        val tImageFeature = TImageFeature.T_IMAGE_FEATURE
        val tStoreProjectRel = TStoreProjectRel.T_STORE_PROJECT_REL
        val conditions = mutableListOf<Condition>()
        if (!repoName.isNullOrBlank()) {
            conditions.add(tImage.IMAGE_REPO_NAME.eq(repoName))
        }
        if (!tag.isNullOrBlank()) {
            conditions.add(tImage.IMAGE_TAG.eq(tag))
        }
        val selfPublishConditions = mutableListOf<Condition>()
        selfPublishConditions.addAll(conditions)
        selfPublishConditions.add(tStoreProjectRel.STORE_TYPE.eq(StoreTypeEnum.IMAGE.type.toByte()))
        // 用户自己发布的（测试、审核、已发布、下架中、已下架）+公共的（已发布、下架中、已下架）
        val query = dslContext.select(
            tImage.ID.`as`(KEY_IMAGE_ID),
            tImage.IMAGE_CODE.`as`(KEY_IMAGE_CODE),
            tImage.IMAGE_NAME.`as`(KEY_IMAGE_NAME),
            tImage.VERSION.`as`(KEY_IMAGE_VERSION),
            tImageFeature.RECOMMEND_FLAG.`as`(KEY_IMAGE_FEATURE_RECOMMEND_FLAG)
        ).from(tImage)
            .join(tImageFeature).on(tImage.IMAGE_CODE.eq(tImageFeature.IMAGE_CODE))
            .join(tStoreProjectRel).on(tImage.IMAGE_CODE.eq(tStoreProjectRel.STORE_CODE))
            .where(selfPublishConditions)
            .and(tStoreProjectRel.PROJECT_CODE.eq(projectId))
            .and(
                tImage.IMAGE_STATUS.`in`(
                    setOf(
                        ImageStatusEnum.RELEASED.status.toByte(),
                        ImageStatusEnum.TESTING.status.toByte(),
                        ImageStatusEnum.AUDITING.status.toByte(),
                        ImageStatusEnum.UNDERCARRIAGING.status.toByte(),
                        ImageStatusEnum.UNDERCARRIAGED.status.toByte()
                    )
                )
            )
            .union(
                dslContext.select(
                    tImage.ID.`as`(KEY_IMAGE_ID),
                    tImage.IMAGE_CODE.`as`(KEY_IMAGE_CODE),
                    tImage.IMAGE_NAME.`as`(KEY_IMAGE_NAME),
                    tImage.VERSION.`as`(KEY_IMAGE_VERSION),
                    tImageFeature.RECOMMEND_FLAG.`as`(KEY_IMAGE_FEATURE_RECOMMEND_FLAG)
                ).from(tImage)
                    .join(tImageFeature).on(tImage.IMAGE_CODE.eq(tImageFeature.IMAGE_CODE))
                    .where(conditions)
                    .and(tImageFeature.PUBLIC_FLAG.eq(true))
                    .and(
                        tImage.IMAGE_STATUS.`in`(
                            setOf(
                                ImageStatusEnum.RELEASED.status.toByte(),
                                ImageStatusEnum.UNDERCARRIAGING.status.toByte(),
                                ImageStatusEnum.UNDERCARRIAGED.status.toByte()
                            )
                        )
                    )
            )
        return query.fetch()
    }

    /**
     * 查出可运行的自研公共镜像
     */
    fun listRunnableSelfDevelopPublicImages(
        dslContext: DSLContext
    ): Result<Record9<String, String, String, String, String, String, String, String, String>>? {
        val tImage = TImage.T_IMAGE
        val tImageFeature = TImageFeature.T_IMAGE_FEATURE
        val tStoreProjectRel = TStoreProjectRel.T_STORE_PROJECT_REL
        val conditions = mutableSetOf<Condition>()
        val imageStatusSet = setOf(
            ImageStatusEnum.RELEASED.status.toByte(),
            ImageStatusEnum.UNDERCARRIAGING.status.toByte(),
            ImageStatusEnum.UNDERCARRIAGED.status.toByte()
        )
        // 自研
        conditions.add(tImageFeature.IMAGE_TYPE.eq(ImageRDTypeEnum.SELF_DEVELOPED.type.toByte()))
        // 公共
        conditions.add(tImageFeature.PUBLIC_FLAG.eq(true))
        // 状态
        conditions.add(tImage.IMAGE_STATUS.`in`(imageStatusSet))
        // 镜像
        conditions.add(tStoreProjectRel.STORE_TYPE.eq(StoreTypeEnum.IMAGE.type.toByte()))
        // 初始化项目信息
        conditions.add(tStoreProjectRel.TYPE.eq(StoreProjectTypeEnum.INIT.type.toByte()))
        val baseStep = dslContext.select(
            tImage.ID.`as`(KEY_IMAGE_ID),
            tImage.IMAGE_CODE.`as`(KEY_IMAGE_CODE),
            tImage.IMAGE_NAME.`as`(KEY_IMAGE_NAME),
            tImage.IMAGE_SOURCE_TYPE.`as`(KEY_IMAGE_SOURCE_TYPE),
            tImage.IMAGE_REPO_URL.`as`(KEY_IMAGE_REPO_URL),
            tImage.IMAGE_REPO_NAME.`as`(KEY_IMAGE_REPO_NAME),
            tImage.IMAGE_TAG.`as`(KEY_IMAGE_TAG),
            tImage.TICKET_ID.`as`(Constants.KEY_IMAGE_TICKET_ID),
            tStoreProjectRel.PROJECT_CODE.`as`(Constants.KEY_IMAGE_INIT_PROJECT)
        ).from(tImage).join(tImageFeature).on(tImage.IMAGE_CODE.eq(tImageFeature.IMAGE_CODE))
            .join(tStoreProjectRel).on(tImage.IMAGE_CODE.eq(tStoreProjectRel.STORE_CODE))
        return baseStep.where(conditions)
            .fetch()
    }
}
