/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.common.operate.service.model

import com.tencent.bkrepo.common.artifact.event.base.EventType
import com.tencent.bkrepo.common.mongo.dao.sharding.ShardingDocument
import com.tencent.bkrepo.common.mongo.dao.sharding.ShardingKey
import com.tencent.bkrepo.common.operate.service.model.TOperateLog.Companion.RESOURCE_KEY_IDX
import com.tencent.bkrepo.common.operate.service.model.TOperateLog.Companion.RESOURCE_KEY_IDX_DEF
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import java.time.LocalDateTime

@ShardingDocument("artifact_oplog")
@CompoundIndexes(
    CompoundIndex(name = RESOURCE_KEY_IDX, def = RESOURCE_KEY_IDX_DEF, background = true)
)
data class TOperateLog(
    var id: String? = null,
    @ShardingKey
    var createdDate: LocalDateTime = LocalDateTime.now(),
    var type: EventType,
    var projectId: String?,
    var repoName: String?,
    var resourceKey: String,
    var userId: String,
    var clientAddress: String,
    var description: Map<String, Any>
) {
    companion object {
        const val RESOURCE_KEY_IDX = "projectId_repoName_resourceKey_type_idx"
        const val RESOURCE_KEY_IDX_DEF = "{'projectId':1,'repoName':1,'resourceKey':1, 'type': 1}"
    }
}
