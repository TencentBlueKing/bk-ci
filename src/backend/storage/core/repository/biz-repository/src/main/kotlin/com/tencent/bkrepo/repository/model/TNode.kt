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

package com.tencent.bkrepo.repository.model

import com.tencent.bkrepo.common.mongo.dao.sharding.ShardingDocument
import com.tencent.bkrepo.common.mongo.dao.sharding.ShardingKey
import com.tencent.bkrepo.repository.constant.SHARDING_COUNT
import com.tencent.bkrepo.repository.model.TNode.Companion.COPY_FROM_IDX
import com.tencent.bkrepo.repository.model.TNode.Companion.COPY_FROM_IDX_DEF
import com.tencent.bkrepo.repository.model.TNode.Companion.FOLDER_IDX
import com.tencent.bkrepo.repository.model.TNode.Companion.FOLDER_IDX_DEF
import com.tencent.bkrepo.repository.model.TNode.Companion.FULL_PATH_IDX
import com.tencent.bkrepo.repository.model.TNode.Companion.FULL_PATH_IDX_DEF
import com.tencent.bkrepo.repository.model.TNode.Companion.METADATA_IDX
import com.tencent.bkrepo.repository.model.TNode.Companion.METADATA_IDX_DEF
import com.tencent.bkrepo.repository.model.TNode.Companion.PATH_IDX
import com.tencent.bkrepo.repository.model.TNode.Companion.PATH_IDX_DEF
import com.tencent.bkrepo.repository.model.TNode.Companion.SHA256_IDX
import com.tencent.bkrepo.repository.model.TNode.Companion.SHA256_IDX_DEF
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import java.time.LocalDateTime

/**
 * 资源模型
 */
@ShardingDocument("node")
@CompoundIndexes(
    CompoundIndex(name = FULL_PATH_IDX, def = FULL_PATH_IDX_DEF, unique = true, background = true),
    CompoundIndex(name = PATH_IDX, def = PATH_IDX_DEF, background = true),
    CompoundIndex(name = METADATA_IDX, def = METADATA_IDX_DEF, background = true),
    CompoundIndex(name = SHA256_IDX, def = SHA256_IDX_DEF, background = true),
    CompoundIndex(name = COPY_FROM_IDX, def = COPY_FROM_IDX_DEF, background = true),
    CompoundIndex(name = FOLDER_IDX, def = FOLDER_IDX_DEF, background = true)
)
data class TNode(
    var id: String? = null,
    var createdBy: String,
    var createdDate: LocalDateTime,
    var lastModifiedBy: String,
    var lastModifiedDate: LocalDateTime,

    var folder: Boolean,
    var path: String,
    var name: String,
    var fullPath: String,
    var size: Long,
    var expireDate: LocalDateTime? = null,
    var sha256: String? = null,
    var md5: String? = null,
    var deleted: LocalDateTime? = null,
    var copyFromCredentialsKey: String? = null,
    var copyIntoCredentialsKey: String? = null,
    var metadata: MutableList<TMetadata>? = null,

    @ShardingKey(count = SHARDING_COUNT)
    var projectId: String,
    var repoName: String
) {
    companion object {
        const val FULL_PATH_IDX = "projectId_repoName_fullPath_idx"
        const val PATH_IDX = "projectId_repoName_path_idx"
        const val METADATA_IDX = "metadata_idx"
        const val SHA256_IDX = "sha256_idx"
        const val COPY_FROM_IDX = "copy_idx"
        const val FULL_PATH_IDX_DEF = "{'projectId': 1, 'repoName': 1, 'fullPath': 1, 'deleted': 1}"
        const val PATH_IDX_DEF = "{'projectId': 1, 'repoName': 1, 'path': 1, 'deleted': 1}"
        const val METADATA_IDX_DEF = "{'metadata.key': 1, 'metadata.value': 1}"
        const val SHA256_IDX_DEF = "{'sha256': 1}"
        const val COPY_FROM_IDX_DEF = "{'copyFromCredentialsKey':1}"
        const val FOLDER_IDX = "folder_idx"
        const val FOLDER_IDX_DEF = "{'folder': 1}"
    }
}
