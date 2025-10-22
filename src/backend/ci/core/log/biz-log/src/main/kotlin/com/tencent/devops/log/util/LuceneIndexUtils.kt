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
import org.apache.lucene.document.NumericDocValuesField
import org.apache.lucene.document.StoredField
import org.apache.lucene.document.StringField

object LuceneIndexUtils {

    fun getDocumentObject(
        buildId: String,
        logMessage: LogMessageWithLineNo
    ): Document {
        val doc = Document()
        doc.add(StringField("buildId", buildId, Field.Store.YES))
        doc.add(StringField("message", logMessage.message, Field.Store.YES))
        doc.add(StringField("timestamp", logMessage.timestamp.toString(), Field.Store.YES))
        doc.add(StringField("tag", logMessage.tag, Field.Store.YES))
        doc.add(StringField("subTag", logMessage.subTag ?: "", Field.Store.YES))
        doc.add(StringField("containerHashId", logMessage.containerHashId, Field.Store.YES))
        doc.add(StringField("jobId", logMessage.jobId, Field.Store.YES))
        doc.add(StringField("stepId", logMessage.stepId, Field.Store.YES))
        doc.add(StringField("logType", logMessage.logType.name, Field.Store.YES))
        doc.add(IntPoint("executeCount", logMessage.executeCount ?: 1))
        doc.add(StoredField("executeCount", logMessage.executeCount ?: 1))
        doc.add(NumericDocValuesField("lineNo", logMessage.lineNo))
        doc.add(StoredField("lineNo", logMessage.lineNo))
        return doc
    }
}
