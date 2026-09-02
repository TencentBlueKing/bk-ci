/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.common.pipeline.pojo

/**
 * 保存编排时校验「插件是否适用于其运行所在节点的操作系统」的入参。
 *
 * 目标操作系统有两种来源，本对象把两者收敛为同一次校验：
 * - 由流水线设置指定(创作流的创作环境)：[settingRunEnvOsChange] 有值，适用于编排中所有有编译环境的 Job
 * - 由编排自身指定(普通流水线)：[settingRunEnvOsChange] 为空，逐个 Job 取其声明的构建环境操作系统
 *
 * 保存链路内部的校验入参，不作接口出入参传输，故不带 Schema 注解。
 */
class PipelineRunEnvOsCheckParam(
    /** 设置指定的运行环境及其变更前后的操作系统，有值时覆盖各 Job 自身的声明；由编排指定运行环境时为空 */
    val settingRunEnvOsChange: PipelineRunEnvOsChange?,
    /**
     * 比对插件哪一种 jobType 下的操作系统声明，取值为 store 侧 JobTypeEnum 的名称。
     *
     * 须由构造方按流水线自身渠道解析后传入：校验方处于请求链路末端，其请求渠道可能来自网关部署标签
     * (openapi)或缺省值，按它解析会拿另一种 jobType 的声明去比对而得出错误结论。
     */
    val osJobTypeName: String,
    /**
     * 上一次落库状态里已有的「运行环境操作系统 + 插件版本」组合，命中即豁免，只拦本次新引入的。
     *
     * 保存期此前从不拦这项(仅前端选插件时按操作系统过滤)，存量编排难免已有不适配组合，
     * 全量拦会让这次什么都没改的用户也存不下去，存量问题留待用户下次主动调整该 Job 或该环境时收敛。
     *
     * 基准是「上一次落库的编排 + 其当时的操作系统」，故本次换了运行环境、加了插件、改了 Job 的操作系统
     * 或升了插件版本，都会算出新组合而被照常拦下。记的是基准里出现过的全部组合而非其中不适配的那些：
     * 判断是否新引入无需知道基准是否适配，因而不必为基准编排再查一次插件运行时信息。
     *
     * [Lazy] 保证只在本次确实发现不适配项时才回查基准编排，正常保存不增加任何查询。
     */
    val exemptedRunEnvOsAtomKeys: Lazy<Set<String>>
)
