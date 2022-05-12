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
import com.tencent.bkrepo.common.storage.core.operation.CleanupOperation
import com.tencent.bkrepo.common.storage.core.operation.FileBlockOperation
import com.tencent.bkrepo.common.storage.core.operation.HealthCheckOperation
import com.tencent.bkrepo.common.storage.credentials.StorageCredentials
import com.tencent.bkrepo.common.storage.filesystem.check.SynchronizeResult
import java.nio.file.Path

/**
 * 存储服务接口
 */
interface StorageService : FileBlockOperation, HealthCheckOperation, CleanupOperation {
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
     * 检验缓存文件一致性
     */
    fun synchronizeFile(storageCredentials: StorageCredentials? = null): SynchronizeResult

    /**
     * 获取临时目录
     */
    fun getTempPath(storageCredentials: StorageCredentials? = null): Path
}
