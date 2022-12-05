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

package com.tencent.bkrepo.common.storage.core

import com.tencent.bkrepo.common.api.constant.StringPool
import com.tencent.bkrepo.common.artifact.api.ArtifactFile
import com.tencent.bkrepo.common.artifact.stream.ArtifactInputStream
import com.tencent.bkrepo.common.artifact.stream.Range
import com.tencent.bkrepo.common.storage.core.locator.FileLocator
import com.tencent.bkrepo.common.storage.credentials.StorageCredentials
import com.tencent.bkrepo.common.storage.filesystem.FileSystemClient
import com.tencent.bkrepo.common.storage.monitor.StorageHealthMonitorHelper
import org.springframework.beans.factory.annotation.Autowired
import java.nio.file.Path
import java.nio.file.Paths

/**
 * 抽象存储服务辅助类
 */
@Suppress("LateinitUsage")
abstract class AbstractStorageSupport : StorageService {

    @Autowired
    protected lateinit var fileLocator: FileLocator

    @Autowired
    protected lateinit var fileStorage: FileStorage

    @Autowired
    protected lateinit var storageProperties: StorageProperties

    @Autowired
    protected lateinit var monitorHelper: StorageHealthMonitorHelper

    /**
     * 根据[storageCredentials]获取实际存储凭证，当storageCredentials为`null`则使用默认存储
     */
    protected fun getCredentialsOrDefault(storageCredentials: StorageCredentials?): StorageCredentials {
        return storageCredentials ?: storageProperties.defaultStorageCredentials()
    }

    /**
     * 获取fs client用于操作临时文件，
     * 临时文件用于分块上传、文件追加上传
     * cache: /data/cached/temp
     * simple:
     *   default: io.temp
     *   fs: /data/store/temp
     */
    protected fun getTempClient(credentials: StorageCredentials): FileSystemClient {
        return FileSystemClient(getTempPath(credentials))
    }

    /**
     * 获取临时目录
     */
    override fun getTempPath(storageCredentials: StorageCredentials?): Path {
        val credentials = getCredentialsOrDefault(storageCredentials)
        return if (credentials.cache.enabled) {
            Paths.get(credentials.cache.path, StringPool.TEMP)
        } else {
            Paths.get(fileStorage.getTempPath(credentials))
        }
    }

    /**
     * 实际文件数据存储抽象方法
     */
    protected abstract fun doStore(
        path: String,
        filename: String,
        artifactFile: ArtifactFile,
        credentials: StorageCredentials
    )

    /**
     * 实际文件数据加载抽象方法
     */
    protected abstract fun doLoad(
        path: String,
        filename: String,
        range: Range,
        credentials: StorageCredentials
    ): ArtifactInputStream?

    /**
     * 实际文件数据删除抽象方法
     */
    protected abstract fun doDelete(path: String, filename: String, credentials: StorageCredentials)

    /**
     * 实际判断文件存在抽象方法
     */
    protected abstract fun doExist(path: String, filename: String, credentials: StorageCredentials): Boolean
}
