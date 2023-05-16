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

package com.tencent.devops.auth.dao

import com.tencent.devops.auth.entity.GroupCreateInfo
import com.tencent.devops.model.auth.tables.TAuthGroupInfo
import com.tencent.devops.model.auth.tables.records.TAuthGroupInfoRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class AuthGroupDao {

    fun createGroup(dslContext: DSLContext, groupCreateInfo: GroupCreateInfo): Int {
        with(TAuthGroupInfo.T_AUTH_GROUP_INFO) {
            return dslContext.insertInto(
                this,
                GROUP_NAME,
                GROUP_CODE,
                GROUP_TYPE,
                RELATION_ID,
                DISPLAY_NAME,
                PROJECT_CODE,
                CREATE_USER,
                CREATE_TIME,
                UPDATE_USER,
                UPDATE_TIME
            ).values(
                groupCreateInfo.groupName,
                groupCreateInfo.groupCode,
                groupCreateInfo.groupType,
                groupCreateInfo.relationId,
                groupCreateInfo.displayName,
                groupCreateInfo.projectCode,
                groupCreateInfo.user,
                LocalDateTime.now(),
                null,
                null
            ).returning(ID).fetchOne()!!.id
        }
    }

    fun getGroup(dslContext: DSLContext, projectCode: String, groupCode: String): TAuthGroupInfoRecord? {
        with(TAuthGroupInfo.T_AUTH_GROUP_INFO) {
            return dslContext.selectFrom(this)
                .where(PROJECT_CODE.eq(projectCode).and(GROUP_CODE.eq(groupCode).and(IS_DELETE.eq(false)))).fetchAny()
        }
    }

    fun getGroupByProject(dslContext: DSLContext, projectCode: String): Result<TAuthGroupInfoRecord> {
        with(TAuthGroupInfo.T_AUTH_GROUP_INFO) {
            return dslContext.selectFrom(this)
                .where(PROJECT_CODE.eq(projectCode).and(IS_DELETE.eq(false))).fetch()
        }
    }

    fun getGroupByCodes(
        dslContext: DSLContext,
        projectCode: String,
        groupCodes: List<String>
    ): Result<TAuthGroupInfoRecord?> {
        with(TAuthGroupInfo.T_AUTH_GROUP_INFO) {
            return dslContext.selectFrom(this)
                .where(PROJECT_CODE.eq(projectCode).and(GROUP_CODE.`in`(groupCodes).and(IS_DELETE.eq(false)))).fetch()
        }
    }

    fun getGroupById(dslContext: DSLContext, groupId: Int): TAuthGroupInfoRecord? {
        with(TAuthGroupInfo.T_AUTH_GROUP_INFO) {
            return dslContext.selectFrom(this)
                .where(ID.eq(groupId)).fetchOne()
        }
    }

    fun getGroupByRelationId(dslContext: DSLContext, relationId: Int): TAuthGroupInfoRecord? {
        with(TAuthGroupInfo.T_AUTH_GROUP_INFO) {
            return dslContext.selectFrom(this).where(RELATION_ID.eq(relationId.toString())).fetchAny()
        }
    }

    fun getGroupByRelationIds(dslContext: DSLContext, relationIds: List<Int>): Result<TAuthGroupInfoRecord> {
        with(TAuthGroupInfo.T_AUTH_GROUP_INFO) {
            return dslContext.selectFrom(this).where(RELATION_ID.`in`(relationIds).and(IS_DELETE.eq(false))).fetch()
        }
    }

    fun getGroupByName(dslContext: DSLContext, projectCode: String, groupName: String): TAuthGroupInfoRecord? {
        with(TAuthGroupInfo.T_AUTH_GROUP_INFO) {
            return dslContext.selectFrom(this).where(PROJECT_CODE.eq(projectCode)
                .and(GROUP_NAME.eq(groupName))).fetchAny()
        }
    }

    fun batchCreateGroups(dslContext: DSLContext, groups: List<GroupCreateInfo>) {
        if (groups.isEmpty()) {
            return
        }
        dslContext.batch(groups.map {
            with(TAuthGroupInfo.T_AUTH_GROUP_INFO) {
                dslContext.insertInto(
                    this,
                    GROUP_NAME,
                    GROUP_CODE,
                    GROUP_TYPE,
                    RELATION_ID,
                    DISPLAY_NAME,
                    PROJECT_CODE,
                    CREATE_USER,
                    CREATE_TIME,
                    UPDATE_USER,
                    UPDATE_TIME
                ).values(
                    it.groupName,
                    it.groupCode,
                    it.groupType,
                    it.relationId,
                    it.displayName,
                    it.projectCode,
                    it.user,
                    LocalDateTime.now(),
                    null,
                    null
                )
            }
        }).execute()
    }

    fun update(
        dslContext: DSLContext,
        id: Int,
        groupName: String,
        displayName: String,
        userId: String
    ): Int {
        with(TAuthGroupInfo.T_AUTH_GROUP_INFO) {
            return dslContext.update(this).set(GROUP_NAME, groupName)
                .set(DISPLAY_NAME, displayName)
                .set(UPDATE_USER, userId)
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(ID.eq(id)).execute()
        }
    }

    fun updateRelationId(dslContext: DSLContext, roleId: Int, relationId: String): Int {
        with(TAuthGroupInfo.T_AUTH_GROUP_INFO) {
            return dslContext.update(this).set(RELATION_ID, relationId).where(ID.eq(roleId)).execute()
        }
    }

    fun getRelationId(dslContext: DSLContext, roleId: Int): TAuthGroupInfoRecord? {
        with(TAuthGroupInfo.T_AUTH_GROUP_INFO) {
            return dslContext.selectFrom(this).where(ID.eq(roleId)).fetchAny()
        }
    }

    fun softDelete(dslContext: DSLContext, roleId: Int) {
        with(TAuthGroupInfo.T_AUTH_GROUP_INFO) {
            dslContext.update(this).set(IS_DELETE, true).where(ID.eq(roleId)).execute()
        }
    }

    fun deleteRole(dslContext: DSLContext, roleId: Int) {
        with(TAuthGroupInfo.T_AUTH_GROUP_INFO) {
            dslContext.delete(this).where(ID.eq(roleId)).execute()
        }
    }
}
