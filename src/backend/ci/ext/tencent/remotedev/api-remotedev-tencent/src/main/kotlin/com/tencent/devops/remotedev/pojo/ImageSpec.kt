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

package com.tencent.devops.remotedev.pojo

/**
 * 用户工作空间信息
 * @param baseRef 工作空间基础镜像即devfile中用户填写
 * @param ideRef 包含ide进程的镜像层
 * @param remotingRef 包含remoting进程的镜像层
 * @param ideLayerRef 包含除去ide进程和remoting进程的其他相关进程层
 * @param contentLayer 工作空间内容部分相关层
 */
data class ImageSpec(
    val baseRef: String,
    val ideRef: String,
    val remotingRef: String,
    val ideLayerRef: List<String>?,
    val contentLayer: List<ContentLayer>?
)

/**
 * 内容镜像层
 * @param remote 远程镜像层
 * @param direct 直接保存的内容的层
 */
data class ContentLayer(
    val remote: RemoteContentLayer?,
    val direct: DirectContentLayer?
)

/**
 * 远程拉取的内容层
 * @param url 具体层内容，这必须是一个有效的 HTTPS URL 指向到 tar.gz 文件
 * @param digest URL指向的文件的摘要（内容哈希）
 * @param diffId DiffId 是 URL 指向的未压缩数据的摘要（内容哈希）,可以为空或与摘要相同
 * @param mediaType 是图层的内容类型，应该是以下之一：
 * application/vnd.oci.image.layer.v1.tar
 * application/vnd.oci.image.layer.v1.tar+gzip
 * application/vnd.oci.image.layer.v1.tar+zstd
 * @param size 是层下载的大小（以字节为单位）
 */
data class RemoteContentLayer(
    val url: String,
    val digest: String,
    val diffId: String?,
    val mediaType: String,
    val size: Int
)

/**
 * 直接下载的层,一个未压缩的tar文件，直接添加为层
 * @param content 用作层的未压缩tar文件的字节数
 */
data class DirectContentLayer(
    val content: List<Byte>
)
