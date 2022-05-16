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

package com.tencent.bkrepo.repository.pojo.search

import com.tencent.bkrepo.common.query.enums.OperationType
import com.tencent.bkrepo.repository.constant.SystemMetadata
import com.tencent.bkrepo.repository.pojo.stage.ArtifactStageEnum

/**
 * 节点自定义查询构造器
 *
 * 链式构造节点QueryModel
 * example:  查询/data目录下大于1024字节的文件
 * val queryModel = NodeQueryBuilder()
 *      .select("size", "name", "path")
 *      .sortByAsc("name")
 *      .page(1, 50)
 *      .projectId("test")
 *      .repoName("generic-local")
 *      .and()
 *        .path("/data")
 *        .size(1024, OperationType.GT)
 *        .excludeFolder()
 *      .build()
 */
class NodeQueryBuilder : AbstractQueryBuilder<NodeQueryBuilder>() {

    /**
     * 添加制品晋级状态规则
     *
     */
    fun stage(stage: ArtifactStageEnum, operation: OperationType = OperationType.EQ): NodeQueryBuilder {
        return this.metadata(SystemMetadata.STAGE.key, stage.tag, operation)
    }

    /**
     * 添加文件名字段规则
     *
     * [value]为值，[operation]为查询操作类型，默认为EQ查询
     */
    fun name(value: String, operation: OperationType = OperationType.EQ): NodeQueryBuilder {
        return this.rule(true, NAME_FILED, value, operation)
    }

    /**
     * 添加路径字段规则
     *
     * [value]为值，[operation]为查询操作类型，默认为EQ查询
     */
    fun path(value: String, operation: OperationType = OperationType.EQ): NodeQueryBuilder {
        return this.rule(true, PATH_FILED, value, operation)
    }

    /**
     * 添加路径字段规则
     *
     * [value]为值，[operation]为查询操作类型，默认为EQ查询
     */
    fun fullPath(value: String, operation: OperationType = OperationType.EQ): NodeQueryBuilder {
        return this.rule(true, FULL_PATH_FILED, value, operation)
    }

    /**
     * 添加文件大小字段规则
     *
     * [value]为值，[operation]为查询操作类型，默认为EQ查询
     */
    fun size(value: Long, operation: OperationType = OperationType.EQ): NodeQueryBuilder {
        return this.rule(true, SIZE_FILED, value, operation)
    }

    /**
     * 添加sha256字段规则
     *
     * [value]为值，[operation]为查询操作类型，默认为EQ查询
     */
    fun sha256(value: String, operation: OperationType = OperationType.EQ): NodeQueryBuilder {
        return this.rule(true, SHA256_FILED, value, operation)
    }

    /**
     * 排除目录
     */
    fun excludeFolder(): NodeQueryBuilder {
        return this.rule(true, FOLDER_FILED, false, OperationType.EQ)
    }

    /**
     * 排除文件
     */
    fun excludeFile(): NodeQueryBuilder {
        return this.rule(true, FOLDER_FILED, true, OperationType.EQ)
    }

    companion object {
        private const val SIZE_FILED = "size"
        private const val NAME_FILED = "name"
        private const val PATH_FILED = "path"
        private const val FULL_PATH_FILED = "fullPath"
        private const val FOLDER_FILED = "folder"
        private const val SHA256_FILED = "sha256"
    }
}
