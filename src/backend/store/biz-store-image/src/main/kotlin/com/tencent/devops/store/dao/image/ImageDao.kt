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
import com.tencent.devops.model.store.tables.TImage
import com.tencent.devops.model.store.tables.TImageFeature
import com.tencent.devops.model.store.tables.TStoreMember
import com.tencent.devops.model.store.tables.TStoreProjectRel
import com.tencent.devops.model.store.tables.records.TImageRecord
import com.tencent.devops.store.dao.image.Constants.KEY_CREATE_TIME
import com.tencent.devops.store.dao.image.Constants.KEY_CREATOR
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_CODE
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_ID
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_NAME
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_REPO_NAME
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_REPO_URL
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_SIZE
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_SOURCE_TYPE
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_STATUS
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_TAG
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_VERSION
import com.tencent.devops.store.dao.image.Constants.KEY_MODIFIER
import com.tencent.devops.store.dao.image.Constants.KEY_UPDATE_TIME
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.image.enums.ImageStatusEnum
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Record14
import org.jooq.Record9
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class ImageDao {
    data class ImageUpdateBean constructor(
        val imageName: String?,
        val classifyId: String?,
        val version: String?,
        val imageSourceType: ImageType?,
        val imageRepoUrl: String?,
        val imageRepoName: String?,
        val imageRepoPath: String?,
        val ticketId: String?,
        val imageStatus: ImageStatusEnum?,
        val imageStatusMsg: String?,
        val imageSize: String?,
        val imageTag: String?,
        val logoUrl: String?,
        val icon: String?,
        val summary: String?,
        val description: String?,
        val publisher: String?,
        val latestFlag: String?,
        val modifier: String?
    )

    fun countByName(
        dslContext: DSLContext,
        imageName: String?
    ): Int {
        with(TImage.T_IMAGE) {
            val conditions = mutableListOf<Condition>()
            val baseStep = dslContext.selectCount().from(this)
            if (!imageName.isNullOrBlank()) {
                conditions.add(IMAGE_NAME.eq(imageName))
            }
            baseStep.where(conditions)
            return baseStep.fetchOne(0, Int::class.java)
        }
    }

    fun countByUserIdAndName(
        dslContext: DSLContext,
        userId: String,
        imageName: String?
    ): Int {
        with(TImage.T_IMAGE) {
            val tStoreMember = TStoreMember.T_STORE_MEMBER.`as`("tStoreMember")
            val conditions = mutableListOf<Condition>()
            val baseStep = dslContext.selectCount().from(this)
                .join(tStoreMember)
                .on(this.IMAGE_CODE.eq(tStoreMember.STORE_CODE))
            if (!imageName.isNullOrBlank()) {
                conditions.add(IMAGE_NAME.eq(imageName))
            }
            conditions.add(tStoreMember.USERNAME.eq(userId))
            baseStep.where(conditions)
                .groupBy(IMAGE_CODE)
            return baseStep.fetch().size
        }
    }

    fun countByNameLike(dslContext: DSLContext, imageName: String): Int {
        with(TImage.T_IMAGE) {
            return dslContext.selectCount().from(this).where(IMAGE_NAME.like(imageName)).fetchOne(0, Int::class.java)
        }
    }

    fun countByCode(dslContext: DSLContext, imageCode: String): Int {
        with(TImage.T_IMAGE) {
            return dslContext.selectCount().from(this).where(IMAGE_CODE.eq(imageCode)).fetchOne(0, Int::class.java)
        }
    }

    fun countByTag(
        dslContext: DSLContext,
        imageRepoUrl: String?,
        imageRepoName: String,
        imageTag: String?
    ): Int {
        with(TImage.T_IMAGE) {
            return dslContext.selectCount().from(this)
                .where(IMAGE_REPO_URL.eq(imageRepoUrl))
                .and(IMAGE_REPO_NAME.eq(imageRepoName))
                .and(IMAGE_TAG.eq(imageTag))
                .fetchOne(0, Int::class.java)
        }
    }

    fun getImage(dslContext: DSLContext, imageId: String): TImageRecord? {
        return with(TImage.T_IMAGE) {
            dslContext.selectFrom(this)
                .where(ID.eq(imageId))
                .fetchOne()
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

    fun getLatestImageByBaseVersion(
        dslContext: DSLContext,
        imageCode: String,
        imageStatusSet: Set<Byte>,
        baseVersion: String?
    ): TImageRecord? {
        with(TImage.T_IMAGE) {
            val conditions = mutableSetOf<Condition>()
            val baseStep = dslContext.selectFrom(this)
            conditions.add(IMAGE_CODE.eq(imageCode))
            conditions.add(IMAGE_STATUS.`in`(imageStatusSet))
            if (null != baseVersion) {
                conditions.add(VERSION.like("$baseVersion%"))
            }
            return baseStep.where(conditions)
                .orderBy(VERSION.desc())
                .limit(1)
                .fetchOne()
        }
    }

    fun getImage(dslContext: DSLContext, imageCode: String, version: String): TImageRecord? {
        return with(TImage.T_IMAGE) {
            dslContext.selectFrom(this)
                .where(IMAGE_CODE.eq(imageCode).and(VERSION.like("$version%")))
                .fetchOne()
        }
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
    ): Result<Record14<String, String, String, String, String, String, String, String, String, Byte, String, String, LocalDateTime, LocalDateTime>> {
        val tImage = TImage.T_IMAGE.`as`("tImage")
        val tStoreMember = TStoreMember.T_STORE_MEMBER.`as`("tStoreMember")
        val conditions = generateGetMyImageConditions(tImage, userId, tStoreMember, imageName)
        val t = dslContext.select(tImage.IMAGE_CODE.`as`("imageCode"), tImage.CREATE_TIME.max().`as`("createTime"))
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
            tImage.UPDATE_TIME.`as`(KEY_UPDATE_TIME)
        ).from(tImage)
            .join(t)
            .on(
                tImage.IMAGE_CODE.eq(
                    t.field(
                        "imageCode",
                        String::class.java
                    )
                ).and(tImage.CREATE_TIME.eq(t.field("createTime", LocalDateTime::class.java)))
            )
            .join(tStoreMember)
            .on(tImage.IMAGE_CODE.eq(tStoreMember.STORE_CODE))
            .where(conditions)
            .groupBy(tImage.IMAGE_CODE)
            .orderBy(tImage.UPDATE_TIME.desc())
        return if (pageSize != null && pageSize > 0 && page != null && page > 0) {
            query.limit((page - 1) * pageSize, pageSize).fetch()
        } else {
            query.fetch()
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
        val tImage = TImage.T_IMAGE.`as`("tImage")
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
        if (pageSize != null && pageSize > 0 && page != null && page > 0) {
            return query.limit((page - 1) * pageSize, pageSize).fetch()
        } else {
            return query.fetch()
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
            if (!imageUpdateBean.logoUrl.isNullOrBlank()) {
                baseQuery = baseQuery.set(LOGO_URL, imageUpdateBean.logoUrl)
            }
            if (imageUpdateBean.imageStatus != null) {
                baseQuery = baseQuery.set(IMAGE_STATUS, imageUpdateBean.imageStatus.status.toByte())
            }
            if (!imageUpdateBean.imageSize.isNullOrBlank()) {
                baseQuery = baseQuery.set(IMAGE_SIZE, imageUpdateBean.imageSize)
            }
            if (!imageUpdateBean.imageTag.isNullOrBlank()) {
                baseQuery = baseQuery.set(IMAGE_TAG, imageUpdateBean.imageTag)
            }
            if (!imageUpdateBean.summary.isNullOrBlank()) {
                baseQuery = baseQuery.set(SUMMARY, imageUpdateBean.summary)
            }
            if (!imageUpdateBean.description.isNullOrBlank()) {
                baseQuery = baseQuery.set(DESCRIPTION, imageUpdateBean.description)
            }
            if (!imageUpdateBean.publisher.isNullOrBlank()) {
                baseQuery = baseQuery.set(PUBLISHER, imageUpdateBean.publisher)
            }
            if (!imageUpdateBean.version.isNullOrBlank()) {
                baseQuery = baseQuery.set(VERSION, imageUpdateBean.version)
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
        val a = TImage.T_IMAGE.`as`("a")
        val tImageFeature = TImageFeature.T_IMAGE_FEATURE.`as`("tImageFeature")
        val b = TStoreProjectRel.T_STORE_PROJECT_REL.`as`("b")
        val t = dslContext.select(
            a.VERSION.`as`("version"),
            a.IMAGE_TAG.`as`("imageTag"),
            a.CREATE_TIME.`as`("createTime"),
            a.IMAGE_STATUS.`as`("imageStatus")
        ).from(a).join(tImageFeature).on(a.IMAGE_CODE.eq(tImageFeature.IMAGE_CODE))
            .where(
                a.IMAGE_CODE.eq(imageCode).and(tImageFeature.PUBLIC_FLAG.eq(true)).and(
                    a.IMAGE_STATUS.`in`(
                        imageStatusList
                    )
                )
            )
            .union(
                dslContext.select(
                    a.VERSION.`as`("version"),
                    a.IMAGE_TAG.`as`("imageTag"),
                    a.CREATE_TIME.`as`("createTime"),
                    a.IMAGE_STATUS.`as`("imageStatus")
                ).from(a).join(b).on(a.IMAGE_CODE.eq(b.STORE_CODE)).join(tImageFeature).on(a.IMAGE_CODE.eq(tImageFeature.IMAGE_CODE))
                    .where(
                        a.IMAGE_CODE.eq(imageCode).and(tImageFeature.PUBLIC_FLAG.eq(false)).and(
                            a.IMAGE_STATUS.`in`(
                                imageStatusList
                            )
                        )
                            .andExists(
                                dslContext.selectOne().from(b).where(
                                    a.IMAGE_CODE.eq(b.STORE_CODE).and(
                                        b.STORE_TYPE.eq(
                                            StoreTypeEnum.IMAGE.type.toByte()
                                        )
                                    ).and(b.PROJECT_CODE.eq(projectCode))
                                )
                            )
                    )
            )
            .asTable("t")
        return dslContext.select().from(t).orderBy(t.field("createTime").desc()).fetch()
    }
}