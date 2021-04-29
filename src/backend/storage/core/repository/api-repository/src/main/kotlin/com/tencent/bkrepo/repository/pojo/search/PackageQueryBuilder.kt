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

class PackageQueryBuilder : AbstractQueryBuilder<PackageQueryBuilder>() {

    /**
     * 添加包名规则
     */
    fun name(value: String, operation: OperationType = OperationType.EQ): PackageQueryBuilder {
        return this.rule(true, NAME_FILED, value, operation)
    }

    /**
     * 添加包唯一key规则
     */
    fun key(value: String, operation: OperationType = OperationType.EQ): PackageQueryBuilder {
        return this.rule(true, KEY_FILED, value, operation)
    }

    /**
     * 添加类型规则
     */
    fun type(value: String, operation: OperationType = OperationType.EQ): PackageQueryBuilder {
        return this.rule(true, TYPE_FILED, value, operation)
    }

    companion object {
        private const val KEY_FILED = "key"
        private const val NAME_FILED = "name"
        private const val TYPE_FILED = "type"
    }
}
