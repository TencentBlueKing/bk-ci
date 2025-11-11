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

package com.tencent.devops.common.pipeline.pojo.transfer

import com.tencent.devops.common.api.exception.ParamBlankException
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "流水线互转操作类型")
enum class TransferActionType {
    @Schema(title = "完整转换：model -> yaml")
    FULL_MODEL2YAML {
        override fun check(data: TransferBody) {
            if (data.modelAndSetting == null) {
                throw ParamBlankException("model 不能为空")
            }
        }
    },

    @Schema(title = "完整转换：yaml -> model")
    FULL_YAML2MODEL {
        override fun check(data: TransferBody) {
            if (data.oldYaml.isBlank()) {
                throw ParamBlankException("yaml 不能为空")
            }
        }
    },

    @Schema(title = "yaml 中插入的插件")
    YAML_INSERT_TASK {
        override fun check(data: TransferBody) = Unit
    },

    // 模板
    @Schema(title = "流水线模板转换：model -> yaml")
    TEMPLATE_MODEL2YAML_PIPELINE {
        override fun check(data: TransferBody) {
            if (data.templateModelAndSetting?.templateModel == null) {
                throw ParamBlankException("templateModel 不能为空")
            }

            if (data.templateModelAndSetting.setting == null) {
                throw ParamBlankException("setting 不能为空")
            }
        }
    },

    @Schema(title = "流水线模板转换：yaml -> model")
    TEMPLATE_YAML2MODEL_PIPELINE {
        override fun check(data: TransferBody) {
            if (data.oldYaml.isBlank()) {
                throw ParamBlankException("yaml 不能为空")
            }
        }
    },

    @Schema(title = "STAGE模板转换：model -> yaml")
    TEMPLATE_MODEL2YAML_STAGE {
        override fun check(data: TransferBody) {
            if (data.templateModelAndSetting?.templateModel == null) {
                throw ParamBlankException("templateModel 不能为空")
            }
        }
    },

    @Schema(title = "STAGE模板转换：yaml -> model")
    TEMPLATE_YAML2MODEL_STAGE {
        override fun check(data: TransferBody) {
            if (data.oldYaml.isBlank()) {
                throw ParamBlankException("yaml 不能为空")
            }
        }
    },

    @Schema(title = "JOB模板转换：model -> yaml")
    TEMPLATE_MODEL2YAML_JOB {
        override fun check(data: TransferBody) {
            if (data.templateModelAndSetting?.templateModel == null) {
                throw ParamBlankException("templateModel 不能为空")
            }
        }
    },

    @Schema(title = "JOB模板转换：yaml -> model")
    TEMPLATE_YAML2MODEL_JOB {
        override fun check(data: TransferBody) {
            if (data.oldYaml.isBlank()) {
                throw ParamBlankException("yaml 不能为空")
            }
        }
    },

    @Schema(title = "STEP模板转换：model -> yaml")
    TEMPLATE_MODEL2YAML_STEP {
        override fun check(data: TransferBody) {
            if (data.templateModelAndSetting?.templateModel == null) {
                throw ParamBlankException("templateModel 不能为空")
            }
        }
    },

    @Schema(title = "STEP模板转换：yaml -> model")
    TEMPLATE_YAML2MODEL_STEP {
        override fun check(data: TransferBody) {
            if (data.oldYaml.isBlank()) {
                throw ParamBlankException("yaml 不能为空")
            }
        }
    };

    @Throws(ParamBlankException::class)
    abstract fun check(data: TransferBody)
}
