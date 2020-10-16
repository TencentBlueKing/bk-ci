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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *
 */

package com.tencent.bkrepo.common.storage.core

import com.tencent.bkrepo.common.storage.credentials.FileSystemCredentials
import com.tencent.bkrepo.common.storage.credentials.HDFSCredentials
import com.tencent.bkrepo.common.storage.credentials.InnerCosCredentials
import com.tencent.bkrepo.common.storage.credentials.S3Credentials
import com.tencent.bkrepo.common.storage.credentials.StorageCredentials
import com.tencent.bkrepo.common.storage.credentials.StorageType
import com.tencent.bkrepo.common.storage.monitor.MonitorProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.NestedConfigurationProperty
import org.springframework.util.unit.DataSize

/**
 * 存储属性配置
 */
@ConfigurationProperties("storage")
data class StorageProperties(
    /**
     * 最大文件大小
     */
    var maxFileSize: DataSize = DataSize.ofBytes(-1),

    /**
     * 最大请求大小
     */
    var maxRequestSize: DataSize = DataSize.ofBytes(-1),

    /**
     * 文件内存阈值
     */
    var fileSizeThreshold: DataSize = DataSize.ofBytes(-1),

    /**
     * 延迟解析文件
     */
    var isResolveLazily: Boolean = true,

    /**
     * 存储类型
     */
    var type: StorageType = StorageType.FILESYSTEM,

    /**
     * 磁盘监控配置
     */
    @NestedConfigurationProperty
    var monitor: MonitorProperties = MonitorProperties(),

    /**
     * 文件系统存储配置
     */
    @NestedConfigurationProperty
    var filesystem: FileSystemCredentials = FileSystemCredentials(),

    /**
     * 内部cos存储配置
     */
    @NestedConfigurationProperty
    var innercos: InnerCosCredentials = InnerCosCredentials(),

    /**
     * hdfs存储配置
     */
    @NestedConfigurationProperty
    var hdfs: HDFSCredentials = HDFSCredentials(),

    /**
     * s3存储配置
     */
    @NestedConfigurationProperty
    var s3: S3Credentials = S3Credentials()
) {
    fun defaultStorageCredentials(): StorageCredentials {
        return when (type) {
            StorageType.FILESYSTEM -> filesystem
            StorageType.INNERCOS -> innercos
            StorageType.HDFS -> hdfs
            StorageType.S3 -> s3
            else -> filesystem
        }
    }
}
