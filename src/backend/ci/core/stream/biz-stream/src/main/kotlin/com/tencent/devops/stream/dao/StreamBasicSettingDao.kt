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

package com.tencent.devops.stream.dao

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.model.stream.tables.TGitBasicSetting
import com.tencent.devops.model.stream.tables.records.TGitBasicSettingRecord
import com.tencent.devops.stream.pojo.StreamBasicSetting
import com.tencent.devops.stream.pojo.StreamCIInfo
import com.tencent.devops.stream.pojo.TriggerReviewSetting
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record1
import org.jooq.Result
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class StreamBasicSettingDao {

    fun saveSetting(
        dslContext: DSLContext,
        conf: StreamBasicSetting,
        projectCode: String
    ) {
        with(TGitBasicSetting.T_GIT_BASIC_SETTING) {
            dslContext.transaction { configuration ->
                val context = DSL.using(configuration)
                val record = context.selectFrom(this)
                    .where(ID.eq(conf.gitProjectId))
                    .fetchAny()
                val now = LocalDateTime.now()
                if (record == null) {
                    context.insertInto(
                        this,
                        ID,
                        NAME,
                        URL,
                        HOME_PAGE,
                        GIT_HTTP_URL,
                        GIT_SSH_URL,
                        ENABLE_CI,
                        BUILD_PUSHED_BRANCHES,
                        BUILD_PUSHED_PULL_REQUEST,
                        CREATE_TIME,
                        UPDATE_TIME,
                        PROJECT_CODE,
                        ENABLE_MR_BLOCK,
                        ENABLE_USER_ID,
                        CREATOR_BG_NAME,
                        CREATOR_DEPT_NAME,
                        CREATOR_CENTER_NAME,
                        GIT_PROJECT_DESC,
                        GIT_PROJECT_AVATAR,
                        LAST_CI_INFO,
                        NAME_WITH_NAME_SPACE,
                        PATH_WITH_NAME_SPACE,
                        TRIGGER_REVIEW_SETTING
                    ).values(
                        conf.gitProjectId,
                        conf.name,
                        conf.url,
                        conf.homepage,
                        conf.gitHttpUrl,
                        conf.gitSshUrl,
                        conf.enableCi,
                        conf.buildPushedBranches,
                        conf.buildPushedPullRequest,
                        LocalDateTime.now(),
                        LocalDateTime.now(),
                        projectCode,
                        conf.enableMrBlock,
                        conf.enableUserId,
                        conf.creatorBgName,
                        conf.creatorDeptName,
                        conf.creatorCenterName,
                        conf.gitProjectDesc,
                        conf.gitProjectAvatar,
                        if (conf.lastCiInfo == null) {
                            null
                        } else {
                            JsonUtil.toJson(conf.lastCiInfo!!, formatted = false)
                        },
                        conf.nameWithNamespace,
                        conf.pathWithNamespace,
                        JsonUtil.toJson(conf.triggerReviewSetting, formatted = false)
                    ).execute()
                } else {
                    context.update(this)
                        .set(ENABLE_CI, conf.enableCi)
                        .set(BUILD_PUSHED_BRANCHES, conf.buildPushedBranches)
                        .set(BUILD_PUSHED_PULL_REQUEST, conf.buildPushedPullRequest)
                        .set(UPDATE_TIME, now)
                        .set(PROJECT_CODE, projectCode)
                        .set(ENABLE_MR_BLOCK, conf.enableMrBlock)
                        .set(ENABLE_USER_ID, conf.enableUserId)
                        .set(CREATOR_BG_NAME, conf.creatorBgName)
                        .set(CREATOR_DEPT_NAME, conf.creatorDeptName)
                        .set(CREATOR_CENTER_NAME, conf.creatorCenterName)
                        .set(NAME_WITH_NAME_SPACE, conf.nameWithNamespace)
                        .set(PATH_WITH_NAME_SPACE, conf.pathWithNamespace)
                        .where(ID.eq(conf.gitProjectId))
                        .execute()
                }
            }
        }
    }

    fun updateInfoSetting(
        dslContext: DSLContext,
        gitProjectId: Long,
        gitProjectName: String,
        url: String,
        sshUrl: String,
        httpUrl: String,
        homePage: String,
        desc: String?,
        avatar: String?,
        pathWithNamespace: String?,
        nameWithNamespace: String
    ) {
        with(TGitBasicSetting.T_GIT_BASIC_SETTING) {
            val dsl = dslContext.update(this)
                .set(NAME, gitProjectName)
                .set(URL, url)
                .set(HOME_PAGE, homePage)
                .set(GIT_HTTP_URL, httpUrl)
                .set(GIT_SSH_URL, sshUrl)
                .set(UPDATE_TIME, LocalDateTime.now())
                .set(NAME_WITH_NAME_SPACE, nameWithNamespace)
            if (desc != null) {
                dsl.set(GIT_PROJECT_DESC, desc)
            }
            if (avatar != null) {
                dsl.set(GIT_PROJECT_AVATAR, avatar)
            }
            if (pathWithNamespace != null) {
                dsl.set(PATH_WITH_NAME_SPACE, pathWithNamespace)
            }
            dsl.where(ID.eq(gitProjectId))
                .execute()
        }
    }

    fun updateSettingLastCiInfo(
        dslContext: DSLContext,
        gitProjectId: Long,
        streamCiInfo: StreamCIInfo
    ) {
        with(TGitBasicSetting.T_GIT_BASIC_SETTING) {
            dslContext.update(this)
                .set(LAST_CI_INFO, JsonUtil.toJson(streamCiInfo, formatted = false))
                .where(ID.eq(gitProjectId))
                .execute()
        }
    }

    fun updateProjectReviewSetting(
        dslContext: DSLContext,
        gitProjectId: Long,
        triggerReviewSetting: TriggerReviewSetting
    ) {
        with(TGitBasicSetting.T_GIT_BASIC_SETTING) {
            dslContext.update(this)
                .set(TRIGGER_REVIEW_SETTING, JsonUtil.toJson(triggerReviewSetting, formatted = false))
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(ID.eq(gitProjectId))
                .execute()
        }
    }

    fun updateProjectSetting(
        dslContext: DSLContext,
        gitProjectId: Long,
        userId: String?,
        buildPushedBranches: Boolean?,
        buildPushedPullRequest: Boolean?,
        enableMrBlock: Boolean?,
        enableCi: Boolean?,
        authUserId: String?,
        creatorBgName: String?,
        creatorDeptName: String?,
        creatorCenterName: String?,
        enableCommitCheck: Boolean?,
        pathWithNamespace: String?,
        nameWithNamespace: String,
        enableMrComment: Boolean?
    ) {
        with(TGitBasicSetting.T_GIT_BASIC_SETTING) {
            val dsl = dslContext.update(this)
            if (buildPushedBranches != null) {
                dsl.set(BUILD_PUSHED_BRANCHES, buildPushedBranches)
            }
            if (buildPushedPullRequest != null) {
                dsl.set(BUILD_PUSHED_PULL_REQUEST, buildPushedPullRequest)
            }
            if (enableMrBlock != null) {
                dsl.set(ENABLE_MR_BLOCK, enableMrBlock)
            }
            if (enableCi != null) {
                dsl.set(ENABLE_CI, enableCi)
            }
            if (authUserId != null) {
                dsl.set(ENABLE_USER_ID, authUserId)
            }
            if (creatorBgName != null) {
                dsl.set(CREATOR_BG_NAME, creatorBgName)
            }
            if (creatorDeptName != null) {
                dsl.set(CREATOR_DEPT_NAME, creatorDeptName)
            }
            if (creatorCenterName != null) {
                dsl.set(CREATOR_CENTER_NAME, creatorCenterName)
            }
            if (userId != null) {
                dsl.set(OAUTH_OPERATOR, userId)
            }
            if (enableCommitCheck != null) {
                dsl.set(ENABLE_COMMIT_CHECK, enableCommitCheck)
            }
            if (enableMrComment != null) {
                dsl.set(ENABLE_MR_COMMENT, enableMrComment)
            }
            if (pathWithNamespace != null) {
                dsl.set(PATH_WITH_NAME_SPACE, pathWithNamespace)
            }
            dsl.set(UPDATE_TIME, LocalDateTime.now())
                .set(NAME_WITH_NAME_SPACE, nameWithNamespace)
                .where(ID.eq(gitProjectId))
                .execute()
        }
    }

    fun getSetting(
        dslContext: DSLContext,
        gitProjectId: Long,
        hasLastInfo: Boolean = false
    ): StreamBasicSetting? {
        with(TGitBasicSetting.T_GIT_BASIC_SETTING) {
            val conf = dslContext.selectFrom(this)
                .where(ID.eq(gitProjectId))
                .fetchAny()
            if (conf == null) {
                return null
            } else {
                return StreamBasicSetting(
                    gitProjectId = conf.id,
                    name = conf.name,
                    url = conf.url,
                    homepage = conf.homePage,
                    gitHttpUrl = conf.gitHttpUrl,
                    gitSshUrl = conf.gitSshUrl,
                    enableCi = conf.enableCi,
                    buildPushedBranches = conf.buildPushedBranches,
                    buildPushedPullRequest = conf.buildPushedPullRequest,
                    createTime = conf.createTime.timestampmilli(),
                    updateTime = conf.updateTime.timestampmilli(),
                    projectCode = conf.projectCode,
                    enableMrBlock = conf.enableMrBlock,
                    enableUserId = conf.enableUserId,
                    creatorBgName = conf.creatorBgName,
                    creatorDeptName = conf.creatorDeptName,
                    creatorCenterName = conf.creatorCenterName,
                    gitProjectDesc = conf.gitProjectDesc,
                    gitProjectAvatar = conf.gitProjectAvatar,
                    lastCiInfo = if (hasLastInfo && conf.lastCiInfo != null) {
                        JsonUtil.to(conf.lastCiInfo, object : TypeReference<StreamCIInfo>() {})
                    } else {
                        null
                    },
                    enableCommitCheck = conf.enableCommitCheck,
                    nameWithNamespace = conf.nameWithNameSpace ?: "",
                    pathWithNamespace = conf.pathWithNameSpace,
                    enableMrComment = conf.enableMrComment,
                    triggerReviewSetting = JsonUtil.toOrNull(
                        conf.triggerReviewSetting,
                        object : TypeReference<TriggerReviewSetting>() {}
                    ) ?: TriggerReviewSetting()
                )
            }
        }
    }

    fun getSettingByPathWithNameSpace(
        dslContext: DSLContext,
        pathWithNamespace: String,
        hasLastInfo: Boolean = false
    ): StreamBasicSetting? {
        with(TGitBasicSetting.T_GIT_BASIC_SETTING) {
            val conf = dslContext.selectFrom(this)
                .where(PATH_WITH_NAME_SPACE.eq(pathWithNamespace))
                .fetchAny()
            if (conf == null) {
                return null
            } else {
                return StreamBasicSetting(
                    gitProjectId = conf.id,
                    name = conf.name,
                    url = conf.url,
                    homepage = conf.homePage,
                    gitHttpUrl = conf.gitHttpUrl,
                    gitSshUrl = conf.gitSshUrl,
                    enableCi = conf.enableCi,
                    buildPushedBranches = conf.buildPushedBranches,
                    buildPushedPullRequest = conf.buildPushedPullRequest,
                    createTime = conf.createTime.timestampmilli(),
                    updateTime = conf.updateTime.timestampmilli(),
                    projectCode = conf.projectCode,
                    enableMrBlock = conf.enableMrBlock,
                    enableUserId = conf.enableUserId,
                    creatorBgName = conf.creatorBgName,
                    creatorDeptName = conf.creatorDeptName,
                    creatorCenterName = conf.creatorCenterName,
                    gitProjectDesc = conf.gitProjectDesc,
                    gitProjectAvatar = conf.gitProjectAvatar,
                    lastCiInfo = if (hasLastInfo && conf.lastCiInfo != null) {
                        JsonUtil.to(conf.lastCiInfo, object : TypeReference<StreamCIInfo>() {})
                    } else {
                        null
                    },
                    nameWithNamespace = conf.nameWithNameSpace ?: "",
                    pathWithNamespace = conf.pathWithNameSpace,
                    triggerReviewSetting = JsonUtil.toOrNull(
                        conf.triggerReviewSetting,
                        object : TypeReference<TriggerReviewSetting>() {}
                    ) ?: TriggerReviewSetting()
                )
            }
        }
    }

    fun getProjectNoHttpUrl(dslContext: DSLContext): List<TGitBasicSettingRecord> {
        with(TGitBasicSetting.T_GIT_BASIC_SETTING) {
            return dslContext.selectFrom(this)
                .where(GIT_HTTP_URL.eq(""))
                .limit(100)
                .fetch()
        }
    }

    fun getProjectNoNameSpace(dslContext: DSLContext): List<TGitBasicSettingRecord> {
        with(TGitBasicSetting.T_GIT_BASIC_SETTING) {
            return dslContext.selectFrom(this)
                .where(PATH_WITH_NAME_SPACE.isNull)
                .limit(100)
                .fetch()
        }
    }

    fun fixProjectNameSpace(
        dslContext: DSLContext,
        gitProjectId: Long,
        pathWithNamespace: String,
        nameWithNamespace: String
    ): Int {
        with(TGitBasicSetting.T_GIT_BASIC_SETTING) {
            return dslContext.update(this)
                .set(PATH_WITH_NAME_SPACE, pathWithNamespace)
                .set(NAME_WITH_NAME_SPACE, nameWithNamespace)
                .where(ID.eq(gitProjectId))
                .execute()
        }
    }

    fun fixProjectInfo(
        dslContext: DSLContext,
        gitProjectId: Long,
        httpUrl: String
    ): Int {
        with(TGitBasicSetting.T_GIT_BASIC_SETTING) {
            return dslContext.update(this)
                .set(GIT_HTTP_URL, httpUrl)
                .where(ID.eq(gitProjectId))
                .execute()
        }
    }

    fun searchProjectByIds(
        dslContext: DSLContext,
        projectIds: Set<Long>?
    ): List<TGitBasicSettingRecord> {
        if (projectIds.isNullOrEmpty()) {
            return emptyList()
        }
        with(TGitBasicSetting.T_GIT_BASIC_SETTING) {
            return dslContext.selectFrom(this)
                .where(ID.`in`(projectIds))
                .and(ENABLE_CI.eq(true))
                .fetch()
        }
    }

    fun getMaxId(
        dslContext: DSLContext,
        gitProjectIdList: List<Long>? = null
    ): Long {
        with(TGitBasicSetting.T_GIT_BASIC_SETTING) {
            val baseStep = dslContext.select(DSL.max(ID)).from(this)
            if (!gitProjectIdList.isNullOrEmpty()) {
                baseStep.where(ID.`in`(gitProjectIdList))
            }
            return baseStep.fetchOne(0, Long::class.java)!!
        }
    }

    fun updateOauthSetting(
        dslContext: DSLContext,
        gitProjectId: Long,
        userId: String,
        oauthUserId: String
    ) {
        with(TGitBasicSetting.T_GIT_BASIC_SETTING) {
            dslContext.update(this)
                .set(ENABLE_USER_ID, oauthUserId)
                .set(OAUTH_OPERATOR, userId)
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(ID.eq(gitProjectId))
                .execute()
        }
    }

    fun getBasicSettingList(
        dslContext: DSLContext,
        gitProjectIdList: List<Long>? = null,
        minId: Long? = null,
        maxId: Long? = null
    ): List<TGitBasicSettingRecord> {
        with(TGitBasicSetting.T_GIT_BASIC_SETTING) {
            val conditions = mutableListOf<Condition>()
            if (!gitProjectIdList.isNullOrEmpty()) {
                conditions.add(ID.`in`(gitProjectIdList))
            }
            if (minId != null) {
                conditions.add(ID.ge(minId))
            }
            if (maxId != null) {
                conditions.add(ID.lt(maxId))
            }
            return dslContext.selectFrom(this).where(conditions).fetch()
        }
    }

    fun getSettingByProjectCode(
        dslContext: DSLContext,
        projectCode: String
    ): TGitBasicSettingRecord? {
        return with(TGitBasicSetting.T_GIT_BASIC_SETTING) {
            dslContext.selectFrom(this).where(PROJECT_CODE.eq(projectCode)).fetchAny()
        }
    }

    fun getSettingByEnableUserId(
        dslContext: DSLContext,
        enableUserId: String,
        limit: Int
    ): Result<Record1<Long>> {
        with(TGitBasicSetting.T_GIT_BASIC_SETTING) {
            return dslContext.select(ID).from(this)
                .where(ENABLE_USER_ID.eq(enableUserId))
                .limit(limit)
                .fetch()
        }
    }

    fun updateEnableUserIdByIds(
        dslContext: DSLContext,
        newUserId: String,
        idList: List<Long>
    ): Int {
        with(TGitBasicSetting.T_GIT_BASIC_SETTING) {
            return dslContext.update(this)
                .set(ENABLE_USER_ID, newUserId)
                .where(ID.`in`(idList)).execute()
        }
    }
    fun getProjectByGitDomain(
        dslContext: DSLContext,
        gitDomain: String,
        limit: Int
    ): Result<Record1<Long>> {
        with(TGitBasicSetting.T_GIT_BASIC_SETTING) {
            return dslContext.select(ID).from(this)
                .where(HOME_PAGE.like("%$gitDomain%"))
                .limit(limit)
                .fetch()
        }
    }

    fun updateGitDomainByIds(
        dslContext: DSLContext,
        oldGitDomain: String,
        newGitDomain: String,
        idList: List<Long>
    ): Int {
        with(TGitBasicSetting.T_GIT_BASIC_SETTING) {
            return dslContext.update(this)
                .set(URL, URL.replace(oldGitDomain, newGitDomain))
                .set(HOME_PAGE, HOME_PAGE.replace(oldGitDomain, newGitDomain))
                .set(GIT_HTTP_URL, GIT_HTTP_URL.replace(oldGitDomain, newGitDomain))
                .set(GIT_SSH_URL, GIT_SSH_URL.replace(oldGitDomain, newGitDomain))
                .where(ID.`in`(idList)).execute()
        }
    }
}
