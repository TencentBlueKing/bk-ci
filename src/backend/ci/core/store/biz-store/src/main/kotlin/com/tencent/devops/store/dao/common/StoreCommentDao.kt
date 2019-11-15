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

package com.tencent.devops.store.dao.common

import com.tencent.devops.model.store.tables.TStoreComment
import com.tencent.devops.model.store.tables.records.TStoreCommentRecord
import com.tencent.devops.store.pojo.common.StoreCommentRequest
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class StoreCommentDao {

    fun getStoreComment(dslContext: DSLContext, commentId: String): TStoreCommentRecord? {
        return with(TStoreComment.T_STORE_COMMENT) {
            dslContext.selectFrom(this)
                .where(ID.eq(commentId))
                .fetchOne()
        }
    }

    fun getStoreComments(
        dslContext: DSLContext,
        storeCode: String,
        storeType: Byte,
        page: Int?,
        pageSize: Int?
    ): Result<TStoreCommentRecord>? {
        with(TStoreComment.T_STORE_COMMENT) {
            val baseStep = dslContext.selectFrom(this).where(STORE_CODE.eq(storeCode).and(STORE_TYPE.eq(storeType)))
                .orderBy(CREATE_TIME.desc())
            return if (null != page && null != pageSize) {
                baseStep.limit((page - 1) * pageSize, pageSize).fetch()
            } else {
                baseStep.fetch()
            }
        }
    }

    fun getStoreCommentCount(
        dslContext: DSLContext,
        storeCode: String,
        storeType: Byte
    ): Long {
        with(TStoreComment.T_STORE_COMMENT) {
            return dslContext.selectCount().from(this).where(STORE_CODE.eq(storeCode).and(STORE_TYPE.eq(storeType)))
                .fetchOne(0, Long::class.java)
        }
    }

    fun getUserLatestCommentByStoreCode(
        dslContext: DSLContext,
        userId: String,
        storeCode: String,
        storeType: Byte
    ): TStoreCommentRecord? {
        with(TStoreComment.T_STORE_COMMENT) {
            return dslContext.selectFrom(this)
                .where(STORE_CODE.eq(storeCode).and(STORE_TYPE.eq(storeType)).and(CREATOR.eq(userId)))
                .orderBy(CREATE_TIME.desc()).limit(1).fetchOne()
        }
    }

    fun addStoreComment(
        dslContext: DSLContext,
        commentId: String,
        userId: String,
        commenterDept: String,
        storeId: String,
        storeCode: String,
        profileUrl: String?,
        storeCommentRequest: StoreCommentRequest,
        storeType: Byte
    ) {
        with(TStoreComment.T_STORE_COMMENT) {
            dslContext.insertInto(
                this,
                ID,
                STORE_ID,
                STORE_CODE,
                COMMENT_CONTENT,
                COMMENTER_DEPT,
                SCORE,
                PROFILE_URL,
                STORE_TYPE,
                CREATOR,
                MODIFIER
            ).values(
                commentId,
                storeId,
                storeCode,
                storeCommentRequest.commentContent,
                commenterDept,
                storeCommentRequest.score,
                profileUrl,
                storeType,
                userId,
                userId
            ).execute()
        }
    }

    fun updateStoreComment(
        dslContext: DSLContext,
        userId: String,
        commentId: String,
        storeCommentRequest: StoreCommentRequest
    ) {
        with(TStoreComment.T_STORE_COMMENT) {
            dslContext.update(this)
                .set(COMMENT_CONTENT, storeCommentRequest.commentContent)
                .set(SCORE, storeCommentRequest.score)
                .set(MODIFIER, userId)
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(ID.eq(commentId))
                .execute()
        }
    }

    fun updateStoreCommentPraiseCount(dslContext: DSLContext, userId: String, commentId: String, praiseCount: Int) {
        with(TStoreComment.T_STORE_COMMENT) {
            dslContext.update(this)
                .set(PRAISE_COUNT, PRAISE_COUNT + praiseCount)
                .set(MODIFIER, userId)
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(ID.eq(commentId))
                .execute()
        }
    }

    fun getStoreCommentScoreInfo(dslContext: DSLContext, storeCode: String, storeType: Byte): Result<out Record>? {
        return with(TStoreComment.T_STORE_COMMENT) {
            dslContext.select(
                SCORE.`as`("score"),
                SCORE.count().`as`("num")
            )
                .from(this)
                .where(STORE_CODE.eq(storeCode).and(STORE_TYPE.eq(storeType)))
                .groupBy(SCORE)
                .orderBy(SCORE.desc())
                .fetch()
        }
    }
}