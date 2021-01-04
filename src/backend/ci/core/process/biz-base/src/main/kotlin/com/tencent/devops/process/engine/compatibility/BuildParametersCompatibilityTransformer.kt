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

package com.tencent.devops.process.engine.compatibility

import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.common.pipeline.pojo.BuildParameters

/**
 *
 * 定义流水线变量兼容转换器
 */
interface BuildParametersCompatibilityTransformer {

    /**
     * 解析前端手工启动传入的参数并与triggerContainer的BuildFormProperty替换
     * 前端传入的为正确的新变量
     */
    fun parseManualStartParam(
        paramProperties: List<BuildFormProperty>,
        paramValues: Map<String, String>
    ): MutableList<BuildParameters>

    /**
     * 转换旧变量为新变量
     *
     * 旧变量： 旧的命名(不规范）的系统变量
     * 新变量： 新的命名的系统变量
     * 同名： 旧变量转换为新变即与新变量同名
     * 转换原则： 后出现的旧变量在转换为新变量命名后不允许覆盖前面已经存在的新变量
     *
     * @param paramLists 参数列表，注意顺序和转换原则，后出现的同名变量将被丢弃
     */
    fun transform(vararg paramLists: List<BuildParameters>): List<BuildParameters>
}