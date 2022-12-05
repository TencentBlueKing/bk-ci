/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bkrepo.common.storage.core.operation

import com.tencent.bkrepo.common.artifact.api.ArtifactFile
import com.tencent.bkrepo.common.storage.credentials.StorageCredentials
import com.tencent.bkrepo.common.storage.pojo.FileInfo

/**
 * 文件分块操作
 */
interface FileBlockOperation {

    /**
     * 创建可追加的文件, 返回文件追加Id
     * 追加文件组织格式: 在temp目录下创建一个具有唯一id的文件，文件名称即追加Id
     * 数据每次追加都写入到该文件中
     */
    fun createAppendId(storageCredentials: StorageCredentials?): String

    /**
     * 追加文件，返回当前文件长度
     * appendId: 文件追加Id
     */
    fun append(appendId: String, artifactFile: ArtifactFile, storageCredentials: StorageCredentials?): Long

    /**
     * 结束追加，存储并返回完整文件
     * appendId: 文件追加Id
     */
    fun finishAppend(appendId: String, storageCredentials: StorageCredentials?): FileInfo

    /**
     * 创建分块存储目录，返回分块存储Id
     * 组织格式: 在temp目录下创建一个名称唯一的目录，所有分块存储在该目录下，目录名称即blockId
     * 其中，每个分块对应两个文件，命名分别为$sequence.block和$sequence.sha256
     * $sequence.block保存文件数据，
     * $sequence.sha256保存文件sha256，用于后续分块合并时校验
     */
    fun createBlockId(storageCredentials: StorageCredentials?): String

    /**
     * 删除分块文件
     * blockId: 分块存储id
     */
    fun deleteBlockId(blockId: String, storageCredentials: StorageCredentials?)

    /**
     * 检查blockId是否存在
     * blockId: 分块存储id
     */
    fun checkBlockId(blockId: String, storageCredentials: StorageCredentials?): Boolean

    /**
     * 列出分块文件
     * blockId: 分块存储id
     */
    fun listBlock(blockId: String, storageCredentials: StorageCredentials?): List<Pair<Long, String>>

    /**
     * 存储分块文件
     * blockId: 分块存储id
     * sequence: 序列id，从1开始
     */
    fun storeBlock(
        blockId: String,
        sequence: Int,
        digest: String,
        artifactFile: ArtifactFile,
        overwrite: Boolean,
        storageCredentials: StorageCredentials?
    )

    /**
     * 合并分块文件
     * blockId: 分块存储id
     */
    fun mergeBlock(blockId: String, storageCredentials: StorageCredentials?): FileInfo
}
