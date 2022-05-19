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

package com.tencent.bkrepo.common.artifact.resolve.response

import com.tencent.bkrepo.common.api.constant.HttpStatus
import com.tencent.bkrepo.common.api.constant.StringPool
import com.tencent.bkrepo.common.artifact.stream.ArtifactInputStream
import com.tencent.bkrepo.repository.pojo.node.NodeDetail

/**
 * 构件数据资源
 */
class ArtifactResource(
    /**
     * 构件数据流
     * key: 资源名称
     * value: 资源数据
     */
    val artifactMap: Map<String, ArtifactInputStream>,
    /**
     * 节点信息，响应头中的sha256、md5、LastModify等信息会通过读取node信息写入
     */
    val node: NodeDetail? = null,
    /**
     * 构件来源渠道
     */
    val channel: ArtifactChannel = ArtifactChannel.LOCAL,
    /**
     * 是否返回Content-Disposition头
     */
    var useDisposition: Boolean = false
) {
    /**
     * 编码类型
     */
    var characterEncoding: String = StringPool.UTF_8

    /**
     * 响应状态，如果有设置则使用固定的响应状态
     */
    var status: HttpStatus? = null

    /**
     * 响应ContentType，如果有设置则使用固定的contentType
     */
    var contentType: String? = null

    /**
     * 次构造器
     */
    constructor(
        inputStream: ArtifactInputStream,
        artifactName: String,
        node: NodeDetail? = null,
        channel: ArtifactChannel = ArtifactChannel.LOCAL,
        useDisposition: Boolean = false
    ) : this(mapOf(artifactName to inputStream), node, channel, useDisposition)

    /**
     * 是否包含多个构件资源
     */
    fun containsMultiArtifact(): Boolean {
        return node?.folder == true || artifactMap.size > 1
    }

    /**
     * 获取单个资源数据
     */
    fun getSingleStream(): ArtifactInputStream {
        return artifactMap.values.first()
    }

    /**
     * 获取单个资源名称
     */
    fun getSingleName(): String {
        return artifactMap.keys.first()
    }

    /**
     * 获取数据总的长度
     */
    fun getTotalSize(): Long {
        return artifactMap.values.map { it.range.length }.sum()
    }
}
