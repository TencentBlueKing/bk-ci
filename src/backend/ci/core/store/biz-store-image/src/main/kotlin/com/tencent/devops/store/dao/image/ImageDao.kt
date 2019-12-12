package com.tencent.devops.store.dao.image

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.type.docker.ImageType
import com.tencent.devops.model.store.tables.TClassify
import com.tencent.devops.model.store.tables.TImage
import com.tencent.devops.model.store.tables.TImageFeature
import com.tencent.devops.model.store.tables.TImageLabelRel
import com.tencent.devops.model.store.tables.TLabel
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
import com.tencent.devops.store.pojo.common.enums.StoreProjectTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.image.enums.ImageAgentTypeEnum
import com.tencent.devops.store.pojo.image.enums.ImageStatusEnum
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Record14
import org.jooq.Record4
import org.jooq.Record9
import org.jooq.Result
import org.jooq.impl.DSL
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
        val ticketId: String?,
        val imageStatus: ImageStatusEnum?,
        val imageStatusMsg: String?,
        val imageSize: String?,
        val imageTag: String?,
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
            return baseStep.where(conditions).fetchOne(0, Int::class.java)
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
    ): Record9<String, String, String, String, String, String, String, String, String>? {
        val tImage = TImage.T_IMAGE.`as`("tImage")
        val tStoreProjectRel = TStoreProjectRel.T_STORE_PROJECT_REL.`as`("tStoreProjectRel")
        val conditions = mutableSetOf<Condition>()
        conditions.add(tImage.IMAGE_CODE.eq(imageCode))
        conditions.add(tImage.IMAGE_STATUS.`in`(imageStatusSet))
        conditions.add(tStoreProjectRel.STORE_TYPE.eq(StoreTypeEnum.IMAGE.type.toByte()))
        conditions.add(tStoreProjectRel.TYPE.eq(StoreProjectTypeEnum.INIT.type.toByte()))
        if (null != baseVersion) {
            conditions.add(tImage.VERSION.like("$baseVersion%"))
        }
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
        ).from(tImage).join(tStoreProjectRel).on(tImage.IMAGE_CODE.eq(tStoreProjectRel.STORE_CODE))
        return baseStep.where(conditions)
            .orderBy(tImage.VERSION.desc())
            .limit(1)
            .fetchOne()
    }

    fun getImage(dslContext: DSLContext, imageCode: String, version: String): TImageRecord? {
        return with(TImage.T_IMAGE) {
            dslContext.selectFrom(this)
                .where(IMAGE_CODE.eq(imageCode).and(VERSION.like("$version%")))
                .fetchOne()
        }
    }

    fun getJobImageCount(
        dslContext: DSLContext,
        projectCode: String,
        agentType: ImageAgentTypeEnum?,
        isContainAgentType: Boolean?,
        recommendFlag: Boolean?,
        classifyId: String?
    ): Long {
        val a = TImage.T_IMAGE.`as`("a")
        val c = TStoreProjectRel.T_STORE_PROJECT_REL.`as`("c")
        val d = TImageFeature.T_IMAGE_FEATURE.`as`("d")
        // 公共镜像查询条件组装
        val publicImageCondition =
            queryPublicImageCondition(a, d, agentType, isContainAgentType, classifyId, recommendFlag)
        // 普通镜像查询条件组装
        val normalImageConditions =
            queryNormalImageCondition(a, c, d, projectCode, agentType, isContainAgentType, classifyId, recommendFlag)
        // 开发者测试镜像查询条件组装
        val initTestImageCondition =
            queryInitTestImageCondition(a, c, d, projectCode, agentType, isContainAgentType, classifyId, recommendFlag)
        // 公共镜像和普通镜像需排除初始化项目下面有处于测试中或者审核中的镜像
        publicImageCondition.add(
            a.IMAGE_CODE.notIn(
                dslContext.select(a.IMAGE_CODE).from(a).join(c).on(a.IMAGE_CODE.eq(c.STORE_CODE)).where(
                    initTestImageCondition
                )
            )
        )
        normalImageConditions.add(
            a.IMAGE_CODE.notIn(
                dslContext.select(a.IMAGE_CODE).from(a).join(c).on(
                    a.IMAGE_CODE.eq(
                        c.STORE_CODE
                    )
                ).where(initTestImageCondition)
            )
        )
        val publicImageCount =
            dslContext.select(a.IMAGE_CODE.countDistinct()).from(a).join(d).on(a.IMAGE_CODE.eq(d.IMAGE_CODE)).where(publicImageCondition)
                .fetchOne(0, Long::class.java)
        val normalImageCount =
            dslContext.select(a.IMAGE_CODE.countDistinct()).from(a).join(c).on(a.IMAGE_CODE.eq(c.STORE_CODE)).join(d)
                .on(a.IMAGE_CODE.eq(d.IMAGE_CODE)).where(normalImageConditions)
                .fetchOne(0, Long::class.java)
        val initTestImageCount =
            dslContext.select(a.IMAGE_CODE.countDistinct()).from(a).join(c).on(a.IMAGE_CODE.eq(c.STORE_CODE)).join(d)
                .on(a.IMAGE_CODE.eq(d.IMAGE_CODE)).where(initTestImageCondition)
                .fetchOne(0, Long::class.java)
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
        val a = TImage.T_IMAGE.`as`("a")
        val b = TClassify.T_CLASSIFY.`as`("b")
        val c = TStoreProjectRel.T_STORE_PROJECT_REL.`as`("c")
        val d = TImageFeature.T_IMAGE_FEATURE.`as`("d")
        val e = TImageLabelRel.T_IMAGE_LABEL_REL.`as`("e")
        val f = TLabel.T_LABEL.`as`("f")
        // 公共镜像查询条件组装
        val publicImageCondition =
            queryPublicImageCondition(a, d, agentType, isContainAgentType, classifyId, recommendFlag)
        // 普通镜像查询条件组装
        val normalImageConditions =
            queryNormalImageCondition(a, c, d, projectCode, agentType, isContainAgentType, classifyId, recommendFlag)
        // 开发者测试镜像查询条件组装
        val initTestImageCondition =
            queryInitTestImageCondition(a, c, d, projectCode, agentType, isContainAgentType, classifyId, recommendFlag)
        // 公共镜像和普通镜像需排除初始化项目下面有处于测试中或者审核中的镜像
        publicImageCondition.add(
            a.IMAGE_CODE.notIn(
                dslContext.select(a.IMAGE_CODE).from(a).join(c).on(a.IMAGE_CODE.eq(c.STORE_CODE)).where(
                    initTestImageCondition
                )
            )
        )
        normalImageConditions.add(
            a.IMAGE_CODE.notIn(
                dslContext.select(a.IMAGE_CODE).from(a).join(c).on(
                    a.IMAGE_CODE.eq(
                        c.STORE_CODE
                    )
                ).where(initTestImageCondition)
            )
        )
        val labelNames = dslContext.select(DSL.groupConcatDistinct(DSL.concat(f.LABEL_NAME)))
            .from(e).join(f).on(e.LABEL_ID.eq(f.ID)).where(a.ID.eq(e.IMAGE_ID)).asField<String>("labelNames")
        val t = dslContext.select(
            a.ID.`as`("imageId"),
            a.IMAGE_CODE.`as`("imageCode"),
            a.IMAGE_NAME.`as`("imageName"),
            a.VERSION.`as`("version"),
            a.IMAGE_STATUS.`as`("imageStatus"),
            b.ID.`as`("classifyId"),
            b.CLASSIFY_CODE.`as`("classifyCode"),
            b.CLASSIFY_NAME.`as`("classifyName"),
            a.LOGO_URL.`as`("logoUrl"),
            a.ICON.`as`("icon"),
            a.SUMMARY.`as`("summary"),
            a.PUBLISHER.`as`("publisher"),
            a.PUB_TIME.`as`("pubTime"),
            a.CREATOR.`as`("creator"),
            a.CREATE_TIME.`as`("createTime"),
            a.LATEST_FLAG.`as`("latestFlag"),
            a.AGENT_TYPE_SCOPE.`as`("agentTypeScope"),
            a.IMAGE_SOURCE_TYPE.`as`("imageSourceType"),
            a.IMAGE_REPO_URL.`as`("imageRepoUrl"),
            a.IMAGE_REPO_NAME.`as`("imageRepoName"),
            a.IMAGE_TAG.`as`("imageTag"),
            a.IMAGE_SIZE.`as`("imageSize"),
            a.MODIFIER.`as`("modifier"),
            a.UPDATE_TIME.`as`("updateTime"),
            d.CERTIFICATION_FLAG.`as`("certificationFlag"),
            d.PUBLIC_FLAG.`as`("publicFlag"),
            d.IMAGE_TYPE.`as`("imageType"),
            d.WEIGHT.`as`("weight"),
            d.RECOMMEND_FLAG.`as`("recommendFlag"),
            labelNames
        )
            .from(a)
            .join(b)
            .on(a.CLASSIFY_ID.eq(b.ID))
            .join(c)
            .on(a.IMAGE_CODE.eq(c.STORE_CODE))
            .leftJoin(d)
            .on(a.IMAGE_CODE.eq(d.IMAGE_CODE))
            .where(normalImageConditions)
            .union(
                dslContext.select(
                    a.ID.`as`("imageId"),
                    a.IMAGE_CODE.`as`("imageCode"),
                    a.IMAGE_NAME.`as`("imageName"),
                    a.VERSION.`as`("version"),
                    a.IMAGE_STATUS.`as`("imageStatus"),
                    b.ID.`as`("classifyId"),
                    b.CLASSIFY_CODE.`as`("classifyCode"),
                    b.CLASSIFY_NAME.`as`("classifyName"),
                    a.LOGO_URL.`as`("logoUrl"),
                    a.ICON.`as`("icon"),
                    a.SUMMARY.`as`("summary"),
                    a.PUBLISHER.`as`("publisher"),
                    a.PUB_TIME.`as`("pubTime"),
                    a.CREATOR.`as`("creator"),
                    a.CREATE_TIME.`as`("createTime"),
                    a.LATEST_FLAG.`as`("latestFlag"),
                    a.AGENT_TYPE_SCOPE.`as`("agentTypeScope"),
                    a.IMAGE_SOURCE_TYPE.`as`("imageSourceType"),
                    a.IMAGE_REPO_URL.`as`("imageRepoUrl"),
                    a.IMAGE_REPO_NAME.`as`("imageRepoName"),
                    a.IMAGE_TAG.`as`("imageTag"),
                    a.IMAGE_SIZE.`as`("imageSize"),
                    a.MODIFIER.`as`("modifier"),
                    a.UPDATE_TIME.`as`("updateTime"),
                    d.CERTIFICATION_FLAG.`as`("certificationFlag"),
                    d.PUBLIC_FLAG.`as`("publicFlag"),
                    d.IMAGE_TYPE.`as`("imageType"),
                    d.WEIGHT.`as`("weight"),
                    d.RECOMMEND_FLAG.`as`("recommendFlag"),
                    labelNames
                )
                    .from(a)
                    .join(b)
                    .on(a.CLASSIFY_ID.eq(b.ID))
                    .leftJoin(d)
                    .on(a.IMAGE_CODE.eq(d.IMAGE_CODE))
                    .where(publicImageCondition)
            )
            .union(
                dslContext.select(
                    a.ID.`as`("imageId"),
                    a.IMAGE_CODE.`as`("imageCode"),
                    a.IMAGE_NAME.`as`("imageName"),
                    a.VERSION.`as`("version"),
                    a.IMAGE_STATUS.`as`("imageStatus"),
                    b.ID.`as`("classifyId"),
                    b.CLASSIFY_CODE.`as`("classifyCode"),
                    b.CLASSIFY_NAME.`as`("classifyName"),
                    a.LOGO_URL.`as`("logoUrl"),
                    a.ICON.`as`("icon"),
                    a.SUMMARY.`as`("summary"),
                    a.PUBLISHER.`as`("publisher"),
                    a.PUB_TIME.`as`("pubTime"),
                    a.CREATOR.`as`("creator"),
                    a.CREATE_TIME.`as`("createTime"),
                    a.LATEST_FLAG.`as`("latestFlag"),
                    a.AGENT_TYPE_SCOPE.`as`("agentTypeScope"),
                    a.IMAGE_SOURCE_TYPE.`as`("imageSourceType"),
                    a.IMAGE_REPO_URL.`as`("imageRepoUrl"),
                    a.IMAGE_REPO_NAME.`as`("imageRepoName"),
                    a.IMAGE_TAG.`as`("imageTag"),
                    a.IMAGE_SIZE.`as`("imageSize"),
                    a.MODIFIER.`as`("modifier"),
                    a.UPDATE_TIME.`as`("updateTime"),
                    d.CERTIFICATION_FLAG.`as`("certificationFlag"),
                    d.PUBLIC_FLAG.`as`("publicFlag"),
                    d.IMAGE_TYPE.`as`("imageType"),
                    d.WEIGHT.`as`("weight"),
                    d.RECOMMEND_FLAG.`as`("recommendFlag"),
                    labelNames
                )
                    .from(a)
                    .join(b)
                    .on(a.CLASSIFY_ID.eq(b.ID))
                    .join(c)
                    .on(a.IMAGE_CODE.eq(c.STORE_CODE))
                    .leftJoin(d)
                    .on(a.IMAGE_CODE.eq(d.IMAGE_CODE))
                    .where(initTestImageCondition)
            )
            .asTable("t")
        val baseStep = dslContext.select().from(t).orderBy(t.field("weight").desc(), t.field("imageName").asc())
        return if (null != page && null != pageSize) {
            baseStep.limit((page - 1) * pageSize + (offsetNum ?: 0), pageSize).fetch()
        } else {
            baseStep.fetch()
        }
    }

    private fun setQueryImageBaseCondition(
        a: TImage,
        d: TImageFeature,
        agentType: ImageAgentTypeEnum?,
        isContainAgentType: Boolean?,
        classifyId: String?,
        recommendFlag: Boolean?
    ): MutableList<Condition> {
        val conditions = mutableListOf<Condition>()
        if (agentType != null) {
            if (null != isContainAgentType && !isContainAgentType) {
                conditions.add(a.AGENT_TYPE_SCOPE.notLike("%${agentType.name}%"))
            } else if (null != isContainAgentType && isContainAgentType) {
                conditions.add(a.AGENT_TYPE_SCOPE.contains(agentType.name))
            }
        }
        if (!classifyId.isNullOrBlank()) conditions.add(a.CLASSIFY_ID.eq(classifyId))
        if (recommendFlag != null) conditions.add(d.RECOMMEND_FLAG.eq(recommendFlag))
        conditions.add(a.DELETE_FLAG.eq(false)) // 只查没有被删除的镜像
        return conditions
    }

    private fun queryPublicImageCondition(
        a: TImage,
        d: TImageFeature,
        agentType: ImageAgentTypeEnum?,
        isContainAgentType: Boolean?,
        classifyId: String?,
        recommendFlag: Boolean?
    ): MutableList<Condition> {
        val conditions = setQueryImageBaseCondition(a, d, agentType, isContainAgentType, classifyId, recommendFlag)
        conditions.add(a.IMAGE_STATUS.eq(ImageStatusEnum.RELEASED.status.toByte())) // 只查已发布的
        conditions.add(d.PUBLIC_FLAG.eq(true)) // 查公共镜像（所有项目都可用）
        conditions.add(a.LATEST_FLAG.eq(true)) // 只查最新版本的镜像
        return conditions
    }

    private fun queryNormalImageCondition(
        a: TImage,
        c: TStoreProjectRel,
        d: TImageFeature,
        projectCode: String,
        agentType: ImageAgentTypeEnum?,
        isContainAgentType: Boolean?,
        classifyId: String?,
        recommendFlag: Boolean?
    ): MutableList<Condition> {
        val conditions = setQueryImageBaseCondition(a, d, agentType, isContainAgentType, classifyId, recommendFlag)
        conditions.add(a.IMAGE_STATUS.eq(ImageStatusEnum.RELEASED.status.toByte())) // 只查已发布的
        conditions.add(d.PUBLIC_FLAG.eq(false)) // 查普通镜像
        conditions.add(a.LATEST_FLAG.eq(true)) // 只查最新版本的镜像
        conditions.add(c.PROJECT_CODE.eq(projectCode))
        conditions.add(c.STORE_TYPE.eq(StoreTypeEnum.IMAGE.type.toByte()))
        return conditions
    }

    private fun queryInitTestImageCondition(
        a: TImage,
        c: TStoreProjectRel,
        d: TImageFeature,
        projectCode: String,
        agentType: ImageAgentTypeEnum?,
        isContainAgentType: Boolean?,
        classifyId: String?,
        recommendFlag: Boolean?
    ): MutableList<Condition> {
        val conditions = setQueryImageBaseCondition(a, d, agentType, isContainAgentType, classifyId, recommendFlag)
        // 只查测试中和审核中的插件
        conditions.add(
            a.IMAGE_STATUS.`in`(
                listOf(
                    ImageStatusEnum.TESTING.status.toByte(),
                    ImageStatusEnum.AUDITING.status.toByte()
                )
            )
        )
        conditions.add(c.PROJECT_CODE.eq(projectCode))
        // 新增镜像时关联的项目或者申请成为协作者时关联的调试项目
        conditions.add(
            c.TYPE.`in`(
                listOf(
                    StoreProjectTypeEnum.INIT.type.toByte(),
                    StoreProjectTypeEnum.TEST.type.toByte()
                )
            )
        )
        conditions.add(c.STORE_TYPE.eq(StoreTypeEnum.IMAGE.type.toByte()))
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

    /**
     * 统计分类下处于已发布状态的镜像个数
     */
    fun countReleaseImageNumByClassifyId(dslContext: DSLContext, classifyId: String): Int {
        with(TImage.T_IMAGE) {
            return dslContext.selectCount().from(this)
                .where(IMAGE_STATUS.eq(ImageStatusEnum.RELEASED.status.toByte()).and(CLASSIFY_ID.eq(classifyId)))
                .fetchOne(0, Int::class.java)
        }
    }

    /**
     * 统计还在使用处于下架中或者已下架状态的模板的项目的个数
     */
    fun countUndercarriageImageNumByClassifyId(dslContext: DSLContext, classifyId: String): Int {
        val a = TImage.T_IMAGE.`as`("a")
        val b = TStoreProjectRel.T_STORE_PROJECT_REL.`as`("b")
        val templateStatusList =
            listOf(ImageStatusEnum.UNDERCARRIAGING.status.toByte(), ImageStatusEnum.UNDERCARRIAGED.status.toByte())
        return dslContext.selectCount().from(a).join(b).on(a.IMAGE_CODE.eq(b.STORE_CODE))
            .where(a.IMAGE_STATUS.`in`(templateStatusList).and(a.CLASSIFY_ID.eq(classifyId)))
            .fetchOne(0, Int::class.java)
    }

    fun listByRepoNameAndTag(
        dslContext: DSLContext,
        userId: String,
        repoName: String?,
        tag: String?
    ): Result<Record4<String, String, String, String>>? {
        val tImage = TImage.T_IMAGE.`as`("tImage")
        val tImageFeature = TImageFeature.T_IMAGE_FEATURE.`as`("tImageFeature")
        val conditions = mutableListOf<Condition>()
        if (!repoName.isNullOrBlank()) {
            conditions.add(tImage.IMAGE_REPO_NAME.eq(repoName))
        }
        if (!tag.isNullOrBlank()) {
            conditions.add(tImage.IMAGE_TAG.eq(tag))
        }
        // 用户自己发布的（测试、审核、已发布）+公共的（已发布）
        val query = dslContext.select(
            tImage.ID.`as`(KEY_IMAGE_ID),
            tImage.IMAGE_CODE.`as`(KEY_IMAGE_CODE),
            tImage.IMAGE_NAME.`as`(KEY_IMAGE_NAME),
            tImage.VERSION.`as`(KEY_IMAGE_VERSION)
        ).from(tImage)
            .join(tImageFeature).on(tImage.IMAGE_CODE.eq(tImageFeature.IMAGE_CODE))
            .where(conditions)
            .and(tImage.CREATOR.eq(userId))
            .and(
                tImage.IMAGE_STATUS.`in`(
                    setOf(
                        ImageStatusEnum.RELEASED.status.toByte(),
                        ImageStatusEnum.TESTING.status.toByte(),
                        ImageStatusEnum.AUDITING.status.toByte()
                    )
                )
            )
            .union(
                dslContext.select(
                    tImage.ID.`as`(KEY_IMAGE_ID),
                    tImage.IMAGE_CODE.`as`(KEY_IMAGE_CODE),
                    tImage.IMAGE_NAME.`as`(KEY_IMAGE_NAME),
                    tImage.VERSION.`as`(KEY_IMAGE_VERSION)
                ).from(tImage)
                    .join(tImageFeature).on(tImage.IMAGE_CODE.eq(tImageFeature.IMAGE_CODE))
                    .where(conditions)
                    .and(tImage.CREATOR.notEqual(userId))
                    .and(tImageFeature.PUBLIC_FLAG.eq(true))
                    .and(
                        tImage.IMAGE_STATUS.`in`(
                            setOf(
                                ImageStatusEnum.RELEASED.status.toByte()
                            )
                        )
                    )
            )
        return query.fetch()
    }
}