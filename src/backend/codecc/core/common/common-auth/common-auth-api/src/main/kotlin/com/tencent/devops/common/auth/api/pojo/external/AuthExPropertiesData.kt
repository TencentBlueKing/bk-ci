/*
 * Tencent is pleased to support the open source community by making BK-CODECC 蓝鲸代码检查平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CODECC 蓝鲸代码检查平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.common.auth.api.pojo.external

data class AuthExPropertiesData(
        /**
         * 环境名称
         */
        val envName: String? = null,

        /**
         * 系统名称（固定）
         */
        val systemId: String? = null,

        /**
         * 用户类型
         */
        val principalType: String? = null,

        /**
         * 系统范围
         */
        val scopeType: String? = null,

        /**
         * CodeCC资源类型
         */
        val codeccResourceType: String? = null,

        /**
         * 流水线资源类型
         */
        val pipelineResourceType: String? = null,

        /**
         * CodeCC服务编码
         */
        val codeccServiceCode: String? = null,

        /**
         * 流水线服务编码
         */
        val pipelineServiceCode: String? = null,

        /**
         * 权限中心根路径
         */
        val url: String? = null,

        /**
         * 系统code
         */
        val codeccCode: String? = null,

        /**
         * 系统secret
         */
        val codeccSecret: String? = null
)