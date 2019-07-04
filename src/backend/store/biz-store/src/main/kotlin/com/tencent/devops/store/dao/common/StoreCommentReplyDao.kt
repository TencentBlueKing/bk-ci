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

import com.tencent.devops.model.store.tables.TStoreCommentReply
import com.tencent.devops.model.store.tables.records.TStoreCommentReplyRecord
import com.tencent.devops.store.pojo.common.StoreCommentReplyRequest
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository

@Repository
class StoreCommentReplyDao {

    fun countReplyNumByCommentId(dslContext: DSLContext, commentId: String): Int {
        with(TStoreCommentReply.T_STORE_COMMENT_REPLY) {
            return dslContext.selectCount().from(this).where(COMMENT_ID.eq(commentId)).fetchOne(0, Int::class.java)
        }
    }

    fun getStoreCommentReplyById(
        dslContext: DSLContext,
        replyId: String
    ): TStoreCommentReplyRecord? {
        with(TStoreCommentReply.T_STORE_COMMENT_REPLY) {
            return dslContext.selectFrom(this).where(ID.eq(replyId)).fetchOne()
        }
    }

    fun getStoreCommentReplysByCommentId(
        dslContext: DSLContext,
        commentId: String
    ): Result<TStoreCommentReplyRecord>? {
        with(TStoreCommentReply.T_STORE_COMMENT_REPLY) {
            return dslContext.selectFrom(this).where(COMMENT_ID.eq(commentId)).orderBy(CREATE_TIME.asc()).fetch()
        }
    }

    fun addStoreCommentReply(
        dslContext: DSLContext,
        replyId: String,
        userId: String,
        replyerDept: String,
        commentId: String,
        profileUrl: String?,
        storeCommentReplyRequest: StoreCommentReplyRequest
    ) {
        with(TStoreCommentReply.T_STORE_COMMENT_REPLY) {
            dslContext.insertInto(
                this,
                ID,
                COMMENT_ID,
                REPLY_CONTENT,
                PROFILE_URL,
                REPLY_TO_USER,
                REPLYER_DEPT,
                CREATOR,
                MODIFIER
            ).values(
                replyId,
                commentId,
                storeCommentReplyRequest.replyContent,
                profileUrl,
                storeCommentReplyRequest.replyToUser,
                replyerDept,
                userId,
                userId
            ).execute()
        }
    }
}