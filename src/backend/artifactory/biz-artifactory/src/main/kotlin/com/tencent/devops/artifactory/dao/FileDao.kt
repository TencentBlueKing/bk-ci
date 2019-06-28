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

package com.tencent.devops.artifactory.dao

import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.model.artifactory.tables.TFileInfo
import com.tencent.devops.model.artifactory.tables.TFilePropsInfo
import com.tencent.devops.model.artifactory.tables.records.TFileInfoRecord
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.Record
import org.jooq.Result
import org.springframework.stereotype.Repository

@Repository
class FileDao {

    fun addFileInfo(
        dslContext: DSLContext,
        userId: String,
        fileId: String,
        projectCode: String?,
        fileType: String,
        filePath: String,
        fileName: String,
        fileSize: Long
    ) {
        with(TFileInfo.T_FILE_INFO) {
            dslContext.insertInto(
                this,
                ID,
                PROJECT_CODE,
                FILE_TYPE,
                FILE_PATH,
                FILE_NAME,
                FILE_SIZE,
                CREATOR,
                MODIFIER
            )
                .values(
                    fileId,
                    projectCode,
                    fileType,
                    filePath,
                    fileName,
                    fileSize,
                    userId,
                    userId
                ).execute()
        }
    }

    fun getFileInfo(dslContext: DSLContext, filePath: String): TFileInfoRecord? {
        return with(TFileInfo.T_FILE_INFO) {
            dslContext.selectFrom(this).where(FILE_PATH.eq(filePath)).fetchAny()
        }
    }

    fun getFileMeta(dslContext: DSLContext, fileId: String): Map<String, String> {
        val meta = mutableMapOf<String, String>()
        with(TFilePropsInfo.T_FILE_PROPS_INFO) {
            dslContext.selectFrom(this)
                .where(FILE_ID.eq(fileId))
                .fetch()?.forEach {
                    meta[it.propsKey] = it.propsValue
                }
        }
        return meta
    }

    fun batchAddFileProps(dslContext: DSLContext, userId: String, fileId: String, props: Map<String, String>) {
        with(TFilePropsInfo.T_FILE_PROPS_INFO) {
            val addStep = props.map {
                dslContext.insertInto(
                    this,
                    ID,
                    PROPS_KEY,
                    PROPS_VALUE,
                    FILE_ID,
                    CREATOR,
                    MODIFIER
                )
                    .values(
                        UUIDUtil.generate(),
                        it.key,
                        it.value,
                        fileId,
                        userId,
                        userId
                    )
            }
            dslContext.batch(addStep).execute()
        }
    }

    fun getFileListByProps(
        dslContext: DSLContext,
        projectCode: String,
        fileTypeList: List<String>?,
        props: Map<String, String>,
        page: Int?,
        pageSize: Int?
    ): Result<out Record>? {
        val a = TFileInfo.T_FILE_INFO.`as`("a")
        val b = TFilePropsInfo.T_FILE_PROPS_INFO.`as`("b")
        val propsCount = generatePropsCountField(props, dslContext, b, a)
        val t = dslContext.select(
            a.ID.`as`("id"),
            a.PROJECT_CODE.`as`("projectCode"),
            a.FILE_TYPE.`as`("fileType"),
            a.FILE_PATH.`as`("filePath"),
            a.FILE_NAME.`as`("fileName"),
            a.FILE_SIZE.`as`("fileSize"),
            a.CREATOR.`as`("creator"),
            a.MODIFIER.`as`("modifier"),
            a.CREATE_TIME.`as`("createTime"),
            a.UPDATE_TIME.`as`("updateTime"),
            propsCount
        ).from(a).where(setFileByPropsCondition(a, projectCode, fileTypeList)).asTable("t")
        val baseStep =
            dslContext.selectFrom(t).where(t.field("propsCount", Int::class.java).eq(props.keys.size))
                .orderBy(t.field("createTime").desc())
        return if (null != page && null != pageSize) {
            baseStep.limit((page - 1) * pageSize, pageSize).fetch()
        } else {
            baseStep.fetch()
        }
    }

    private fun setFileByPropsCondition(
        a: TFileInfo,
        projectCode: String,
        fileTypeList: List<String>?
    ): MutableList<Condition> {
        val conditions = mutableListOf<Condition>()
        conditions.add(a.PROJECT_CODE.eq(projectCode))
        if (null != fileTypeList) {
            conditions.add(a.FILE_TYPE.`in`(fileTypeList))
        }
        return conditions
    }

    private fun generatePropsCountField(
        props: Map<String, String>,
        dslContext: DSLContext,
        b: TFilePropsInfo,
        a: TFileInfo
    ): Field<Int> {
        val propKeys = props.keys
        val propValues = props.values
        return dslContext.selectCount().from(b).where(
            a.ID.eq(b.FILE_ID)
                .and(b.PROPS_KEY.`in`(propKeys)).and(b.PROPS_VALUE.`in`(propValues))
        ).asField<Int>("propsCount")
    }

    fun getFileCountByProps(
        dslContext: DSLContext,
        projectCode: String,
        fileTypeList: List<String>?,
        props: Map<String, String>
    ): Long {
        val a = TFileInfo.T_FILE_INFO.`as`("a")
        val b = TFilePropsInfo.T_FILE_PROPS_INFO.`as`("b")
        val propsCount = generatePropsCountField(props, dslContext, b, a)
        val t = dslContext.select(
            a.ID.`as`("id"),
            a.PROJECT_CODE.`as`("projectCode"),
            a.FILE_TYPE.`as`("fileType"),
            a.FILE_PATH.`as`("filePath"),
            a.FILE_NAME.`as`("fileName"),
            a.FILE_SIZE.`as`("fileSize"),
            a.CREATOR.`as`("creator"),
            a.MODIFIER.`as`("modifier"),
            a.CREATE_TIME.`as`("createTime"),
            a.UPDATE_TIME.`as`("updateTime"),
            propsCount
        ).from(a).where(setFileByPropsCondition(a, projectCode, fileTypeList)).asTable("t")
        return dslContext.selectCount().from(t).where(t.field("propsCount", Int::class.java).eq(props.keys.size))
            .fetchOne(0, Long::class.java)
    }
}