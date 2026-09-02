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

import com.tencent.devops.common.api.pojo.OS
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 流水线运行环境操作系统的校验目标。
 *
 * 部分渠道的运行环境由流水线设置指定而非编排自身指定(如创作流的创作环境)，
 * 这类渠道下编排中的插件未必适用于运行环境的操作系统，需要在保存时校验。
 * 该对象仅由具备该语义的渠道构造，其余渠道为 null，走原有逻辑、零额外开销。
 *
 * 无论本次是否变更过运行环境，都会校验编排中的插件是否适用于 [currentOs]：环境不变但新增了不适配的插件
 * 同样需要拦截。[previousOs] 只描述变更前的状态，不承担「要不要校验」的判断。
 */
@Schema(title = "流水线运行环境操作系统校验目标")
data class PipelineRunEnvOsChange(
    /**
     * 变更前运行环境的操作系统，为空表示变更前没有运行环境，即本次是首次为该流水线指定环境。
     *
     * 该字段只表达「上一次落库时是什么系统」这一个事实，不隐含是否发生了变更：
     * 与 [currentOs] 相同即本次没有换过系统。据此可派生出两个判断，两者不可各自另立口径：
     * - 报错文案是呈现「由 A 变更为 B」还是「当前环境为 A」；
     * - 存量豁免基准该按哪个操作系统计算(见 PipelineRunEnvOsCheckParam.exemptedRunEnvOsAtomKeys)。
     */
    @get:Schema(title = "变更前运行环境的操作系统", required = false)
    val previousOs: OS?,
    @get:Schema(title = "本次要校验的运行环境操作系统", required = true)
    val currentOs: OS,
    /**
     * 插件的适用操作系统按 jobType 分别声明，本次该比对哪个 jobType 的声明随渠道而定，
     * 由构造该对象的一方(已按流水线自身渠道解析过)一并给出，校验方直接用它取插件声明。
     *
     * 不由校验方自行解析：校验方多数处于请求链路末端，取到的请求渠道可能来自网关部署标签
     * (openapi)或缺省值，与 [currentOs] 所属环境不是同一个渠道口径，两者一旦不一致
     * 就会拿另一种 jobType 的声明去比对本环境的操作系统，得出错误结论。
     *
     * 取值为 store 侧 JobTypeEnum 的名称，此处不直接引用该枚举以免 common-pipeline 反向依赖 store。
     */
    @get:Schema(title = "本次要比对的插件jobType名称", required = true)
    val osJobTypeName: String
)
