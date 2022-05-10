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

package com.tencent.bkrepo.common.storage.core.config

import org.springframework.util.unit.DataSize

/**
 * 文件接收配置
 */
data class ReceiveProperties(
    /**
     * 最大接收文件大小，小于0则无限制
     */
    var maxFileSize: DataSize = DataSize.ofBytes(-1),

    /**
     * 最大接收请求大小，小于0则无限制
     */
    var maxRequestSize: DataSize = DataSize.ofBytes(-1),

    /**
     * 文件内存阈值，超过此阈值则将数据从内存写入磁盘
     * 小于0则不使用内存缓存，直接写入磁盘
     */
    var fileSizeThreshold: DataSize = DataSize.ofBytes(-1),

    /**
     * 使用本地路径阈值，超过这阈值则使用其他路径
     * 必须要超过文件内存阈值否则设置无效
     * */
    var localThreshold: DataSize = DataSize.ofBytes(-1),

    /**
     * 是否延迟解析文件
     */
    var resolveLazily: Boolean = true,

    /**
     * io拷贝buffer大小
     */
    var bufferSize: DataSize = DataSize.ofBytes(DEFAULT_BUFFER_SIZE.toLong()),

    /**
     * 每秒接收数据量
     */
    var rateLimit: DataSize = DataSize.ofBytes(-1)
)
