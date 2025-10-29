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

package com.tencent.devops.process.yaml.v3.models.step

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.process.yaml.v3.models.IfField
import io.swagger.v3.oas.annotations.media.Schema

data class Step(
    val enable: Boolean? = null,
    val name: String?,
    val id: String?,
    @JsonProperty("if")
    val ifField: IfField?,
    @JsonProperty("if-modify")
    val ifModify: List<String>? = null,
    val uses: String?,
    val with: Map<String, Any?>?,
    @JsonProperty("timeout-minutes")
    val timeoutMinutes: String? = null,
    @JsonProperty("continue-on-error")
    val continueOnError: String?,
    @JsonProperty("retry-times")
    val retryTimes: Int?,
    @JsonProperty("can-pause-before-run")
    var canPauseBeforeRun: Boolean? = null,
    @JsonProperty("pause-notice-receivers")
    var pauseNoticeReceivers: List<String>? = null,
    val env: Map<String, Any?>? = emptyMap(),
    val run: String?,
    @get:Schema(title = "run 插件的附加参数")
    val runAdditionalOptions: Map<String, String?>?,
    var checkout: CheckoutStep?,
    @JsonProperty("can-manually-retry")
    val manualRetry: Boolean? = null,
    // 在系统内唯一标识step唯一性，不参与yaml打印
    @JsonIgnore
    val taskId: String? = null
) {
    enum class ContinueOnErrorType(val alis: String) {
        AUTO_SKIP("auto"),
        MANUAL_SKIP("manual");

        companion object {
            fun parse(alis: String?): ContinueOnErrorType? {
                if (alis == null) return null
                values().forEach {
                    if (it.alis == alis) return it
                }
                if (alis == "true") return AUTO_SKIP
                return null
            }
        }
    }
}
