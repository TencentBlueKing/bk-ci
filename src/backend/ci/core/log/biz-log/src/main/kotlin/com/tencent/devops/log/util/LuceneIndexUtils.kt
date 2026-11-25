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

package com.tencent.devops.log.util

import com.tencent.devops.common.log.pojo.message.LogMessageWithLineNo
import org.apache.lucene.document.Document
import org.apache.lucene.document.Field
import org.apache.lucene.document.IntPoint
import org.apache.lucene.document.LongPoint
import org.apache.lucene.document.SortedNumericDocValuesField
import org.apache.lucene.document.StoredField
import org.apache.lucene.document.StringField

object LuceneIndexUtils {

    const val FIELD_BUILD_ID = "buildId"
    const val FIELD_MESSAGE = "message"
    const val FIELD_TIMESTAMP = "timestamp"
    const val FIELD_TAG = "tag"
    const val FIELD_SUB_TAG = "subTag"
    const val FIELD_CONTAINER_HASH_ID = "containerHashId"
    const val FIELD_JOB_ID = "jobId"
    const val FIELD_STEP_ID = "stepId"
    const val FIELD_LOG_TYPE = "logType"
    const val FIELD_EXECUTE_COUNT = "executeCount"
    const val FIELD_LINE_NO = "lineNo"
    const val FIELD_DOC_ID = "docId"

    /**
     * 日志正文只存储、不建倒排。
     * StringField 的 term 上限是 32766 字节，超长日志行会让 IndexWriter 直接失败。
     * timestamp / lineNo 同时写 Point（范围）、SortedNumericDocValues（排序）和 StoredField（回读）。
     * 只存成字符串再按数值排序，会在查询时抛出 docvalues 类型错误。
     */
    fun getDocumentObject(
        buildId: String,
        logMessage: LogMessageWithLineNo
    ): Document {
        val executeCount = logMessage.executeCount ?: 1
        val doc = Document()
        doc.add(StringField(FIELD_DOC_ID, docId(buildId, executeCount, logMessage.lineNo), Field.Store.YES))
        addExact(doc, FIELD_BUILD_ID, buildId)
        doc.add(StoredField(FIELD_MESSAGE, logMessage.message))
        doc.add(SortedNumericDocValuesField(FIELD_TIMESTAMP, logMessage.timestamp))
        doc.add(StoredField(FIELD_TIMESTAMP, logMessage.timestamp))
        addExact(doc, FIELD_TAG, logMessage.tag)
        addExact(doc, FIELD_SUB_TAG, logMessage.subTag)
        addExact(doc, FIELD_CONTAINER_HASH_ID, logMessage.containerHashId)
        addExact(doc, FIELD_JOB_ID, logMessage.jobId)
        addExact(doc, FIELD_STEP_ID, logMessage.stepId)
        addExact(doc, FIELD_LOG_TYPE, logMessage.logType.name)
        doc.add(IntPoint(FIELD_EXECUTE_COUNT, executeCount))
        doc.add(StoredField(FIELD_EXECUTE_COUNT, executeCount))
        doc.add(LongPoint(FIELD_LINE_NO, logMessage.lineNo))
        doc.add(SortedNumericDocValuesField(FIELD_LINE_NO, logMessage.lineNo))
        doc.add(StoredField(FIELD_LINE_NO, logMessage.lineNo))
        return doc
    }

    fun docId(buildId: String, executeCount: Int, lineNo: Long): String {
        return "$buildId|$executeCount|$lineNo"
    }

    private fun addExact(doc: Document, name: String, value: String?) {
        doc.add(StringField(name, value ?: "", Field.Store.YES))
    }
}
