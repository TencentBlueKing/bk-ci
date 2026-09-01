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

package com.tencent.devops.process.yaml.v3.models.on

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * TAPD 触发器 YAML 规则（对应一个 workspace 下的 story / bug 事件配置）。
 *
 * 与代码库触发一致，TAPD 触发在 YAML 中作为 `on:` 的一个顶层条目，通过 `type: tapd`
 * 区分；`workspace-id`、`story`、`bug` 都平铺到 [PreTriggerOnV3] 顶层。
 *
 * 对应 YAML：
 * ```yaml
 * on:
 *   type: tapd
 *   workspace-id: "12345"
 *   story:
 *     id: trigger_1
 *     action:
 *       - create
 *       - update
 *   bug:
 *     id: trigger_2
 *     action:
 *       - create
 * ```
 *
 * 多 workspace：
 * ```yaml
 * on:
 *   - type: tapd
 *     workspace-id: "12345"
 *     story: { action: [create, update] }
 *   - type: tapd
 *     workspace-id: "67890"
 *     bug: { action: [create] }
 * ```
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class TapdRule(
    override val id: String? = null,
    override val name: String? = null,
    override val enable: Boolean? = true,
    @get:Schema(title = "include-actions")
    @JsonProperty("action")
    val action: List<String>? = null,
    @get:Schema(title = "users")
    @JsonProperty("users")
    val users: List<String>? = null,
    @get:Schema(title = "users-ignore")
    @JsonProperty("users-ignore")
    val usersIgnore: List<String>? = null,
    @get:Schema(title = "owners")
    @JsonProperty("owners")
    val owners: List<String>? = null,
    @get:Schema(title = "owners-ignore")
    @JsonProperty("owners-ignore")
    val ownersIgnore: List<String>? = null,
    @JsonProperty("labels")
    @get:Schema(title = "labels")
    var labels: List<String>? = null,
    @get:Schema(title = "labels-ignore")
    @JsonProperty("labels-ignore")
    val labelsIgnore: List<String>? = null,
    @get:Schema(title = "priorities")
    @JsonProperty("priorities")
    val priorities: List<String>? = null
) : Rule(id, name, enable)
