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

package com.tencent.devops.common.pipeline.pojo.element.atom

import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.ObjectReplaceEnvVarUtil
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("人工审核-自定义参数")
data class ManualReviewParam(
    @ApiModelProperty("参数名", required = true)
    var key: String = "",
    @ApiModelProperty("参数内容(Any 类型)", required = true, dataType = "string")
    var value: Any? = "",
    @ApiModelProperty("参数类型", required = false)
    val valueType: ManualReviewParamType = ManualReviewParamType.STRING,
    @ApiModelProperty("是否必填", required = true)
    val required: Boolean = false,
    @ApiModelProperty("参数描述", required = false)
    val desc: String? = "",
    @ApiModelProperty("下拉框列表")
    var options: List<ManualReviewParamPair>? = null,
    @ApiModelProperty("中文名称", required = false)
    val chineseName: String? = null,
    @ApiModelProperty("变量形式的options")
    val variableOption: String? = null
) {
    /**
     *  变量值处理，如果是已有值则直接使用，如果是变量引用则做替换
     */
    fun parseValueWithType(variables: Map<String, String>) {
        value = if (variables.containsKey(key) && !variables[key].isNullOrBlank()) {
            when (valueType) {
                ManualReviewParamType.BOOLEAN -> variables[key].toBoolean()
                // TODO 将入库保存的字符串转回数组对象
                else -> variables[key]
            }
        } else {
            ObjectReplaceEnvVarUtil.replaceEnvVar(value, variables)
        }
        options = if (!variableOption.isNullOrBlank()) {
            EnvUtils.parseEnv(variableOption, variables).let {
                val optionList = try {
                    JsonUtil.to<List<Any>>(it)
                } catch (ignore: Throwable) {
                    emptyList()
                }
                optionList.map { item -> ManualReviewParamPair(item.toString(), item.toString()) }
            }
        } else options
    }
}
