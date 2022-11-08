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

package com.tencent.devops.project.dao

import com.tencent.devops.model.project.tables.TProject
import com.tencent.devops.model.project.tables.TUser
import com.tencent.devops.model.project.tables.records.TProjectRecord
import com.tencent.devops.model.project.tables.records.TUserRecord
import com.tencent.devops.project.pojo.ProjectDeptInfo
import java.time.LocalDateTime
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Suppress("ALL")
@Repository
class ProjectFreshDao {

    fun getProjectAfterId(dslContext: DSLContext, startId: Long, limit: Int): List<TProjectRecord> {
        with(TProject.T_PROJECT) {
            return dslContext.selectFrom(this)
                .where(ID.gt(startId))
                .and(ENGLISH_NAME.like("git_%"))
                .and(BG_ID.eq(0))
                .limit(limit)
                .fetch()
        }
    }

    fun getDevopsUserInfo(dslContext: DSLContext, userId: String): TUserRecord? {
        with(TUser.T_USER) {
            return dslContext.selectFrom(this)
                .where(USER_ID.eq(userId))
                .fetchAny()
        }
    }

    fun resetProjectDeptInfo(dslContext: DSLContext): Int {
        with(TProject.T_PROJECT) {
            return dslContext.update(this)
                .set(BG_ID, 0)
                .set(BG_NAME, "")
                .set(DEPT_ID, 0)
                .set(DEPT_NAME, "")
                .set(CENTER_ID, 0)
                .set(CENTER_NAME, "")
                .execute()
        }
    }

    fun bindProjectDept(
        dslContext: DSLContext,
        projectDeptInfo: ProjectDeptInfo,
        projectId: String,
        updateUser: String
    ): Int {
        with(TProject.T_PROJECT) {
            return dslContext.update(this)
                .set(BG_ID, projectDeptInfo.bgId.toLong())
                .set(BG_NAME, projectDeptInfo.bgName)
                .set(DEPT_ID, projectDeptInfo.deptId?.toLong())
                .set(DEPT_NAME, projectDeptInfo.deptName)
                .set(CENTER_ID, projectDeptInfo.centerId?.toLong())
                .set(CENTER_NAME, projectDeptInfo.centerName)
                .set(UPDATOR, updateUser)
                .where(ENGLISH_NAME.eq(projectId))
                .execute()
        }
    }

    fun fixProjectInfo(
        dslContext: DSLContext,
        id: Long,
        creatorBgName: String,
        creatorBgId: Long,
        creatorDeptName: String,
        creatorDeptId: Long,
        creatorCenterName: String,
        creatorCenterId: Long
    ): Int {
        with(TProject.T_PROJECT) {
            return dslContext.update(this)
                .set(CREATOR_BG_NAME, creatorBgName)
                .set(CREATOR_DEPT_NAME, creatorDeptName)
                .set(CREATOR_CENTER_NAME, creatorCenterName)
                .set(BG_NAME, creatorBgName)
                .set(DEPT_NAME, creatorDeptName)
                .set(CENTER_NAME, creatorCenterName)
                .set(BG_ID, creatorBgId)
                .set(DEPT_ID, creatorDeptId)
                .set(CENTER_ID, creatorCenterId)
                .where(ID.eq(id))
                .execute()
        }
    }

    fun updateProjectName(
        dslContext: DSLContext,
        userId: String,
        englishName: String,
        projectName: String
    ): Int {
        with(TProject.T_PROJECT) {
            return dslContext.update(this)
                .set(UPDATOR, userId)
                .set(PROJECT_NAME, projectName)
                .set(UPDATED_AT, LocalDateTime.now())
                .where(ENGLISH_NAME.eq(englishName))
                .execute()
        }
    }

    fun getProjectInfoByProjectName(
        dslContext: DSLContext,
        userId: String,
        projectName: String
    ): TProjectRecord? {
        with(TProject.T_PROJECT) {
            return dslContext.selectFrom(this)
                .where(PROJECT_NAME.eq(projectName))
                .limit(1)
                .fetchAny()
        }
    }
}
