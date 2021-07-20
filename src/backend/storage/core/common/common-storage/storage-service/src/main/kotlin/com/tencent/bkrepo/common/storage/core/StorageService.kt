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

package com.tencent.bkrepo.common.storage.core

import com.tencent.bkrepo.common.artifact.api.ArtifactFile
import com.tencent.bkrepo.common.artifact.stream.ArtifactInputStream
import com.tencent.bkrepo.common.artifact.stream.Range
import com.tencent.bkrepo.common.storage.credentials.StorageCredentials
import com.tencent.bkrepo.common.storage.filesystem.check.SynchronizeResult
import com.tencent.bkrepo.common.storage.filesystem.cleanup.CleanupResult
import com.tencent.bkrepo.common.storage.pojo.FileInfo

/**
 * 存储服务接口
 */
interface StorageService {
    /**
     * 在存储实例[storageCredentials]上存储摘要为[digest]的构件[artifactFile]
     * 返回文件影响数，如果文件已经存在则返回0，否则返回1
     */
    fun store(digest: String, artifactFile: ArtifactFile, storageCredentials: StorageCredentials?): Int

    /**
     * 在存储实例[storageCredentials]上加载摘要为[digest]的文件
     * 当文件未找到时，会尝试去默认存储实例上查找文件
     */
    fun load(digest: String, range: Range, storageCredentials: StorageCredentials?): ArtifactInputStream?

    /**
     * 在存储实例[storageCredentials]上删除摘要为[digest]的文件
     */
    fun delete(digest: String, storageCredentials: StorageCredentials?)

    /**
     * 判断摘要为[digest]的文件在存储实例[storageCredentials]上是否存在
     */
    fun exist(digest: String, storageCredentials: StorageCredentials?): Boolean

    /**
     * 文件跨存储拷贝
     * A -> B
     * 若B中已经存在相同文件则立即返回
     * 若A == B，立即返回
     */
    fun copy(digest: String, fromCredentials: StorageCredentials?, toCredentials: StorageCredentials?)

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
     * $sequence.block文件保存其数据，
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

    /**
     * 清理temp目录文件，包括分块上传产生和追加上传产生的脏数据
     */
    fun cleanUp(storageCredentials: StorageCredentials? = null): CleanupResult

    /**
     * 检验缓存文件一致性
     */
    fun synchronizeFile(storageCredentials: StorageCredentials? = null): SynchronizeResult

    /**
     * 健康检查
     */
    fun checkHealth(storageCredentials: StorageCredentials? = null)
}
