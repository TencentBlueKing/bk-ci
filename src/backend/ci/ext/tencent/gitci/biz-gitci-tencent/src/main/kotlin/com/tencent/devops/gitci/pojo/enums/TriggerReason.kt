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

package com.tencent.devops.gitci.pojo.enums

enum class TriggerReason(val detail: String) {
    TRIGGER_SUCCESS("Trigger success"),
    CI_DISABLED("CI is disabled"),
    BUILD_PUSHED_BRANCHES_DISABLED("This project has not yet opened the push event monitoring configuration"),
    BUILD_MERGE_REQUEST_DISABLED("This project has not yet opened the MR event monitoring configuration"),
    CI_YAML_NOT_FOUND("The YAML file cannot be found in the .ci directory"),
    CI_YAML_CONTENT_NULL("The YAML file is null"),
    CI_YAML_INVALID("YAML is invalid: %s"),
    CI_YAML_NEED_MERGE_OR_REBASE("The YAML file is modified. " +
        "Please execute either REBASE or MERGE to update your YAML file from remote target branch."),
    CI_MERGE_CHECKING("Checking whether there is a conflict in MR, please wait"),
    CI_MERGE_CHECK_TIMEOUT("MR check timeout"),
    CI_MERGE_CONFLICT("MR conflict, please resolve the conflict first"),
    TRIGGER_NOT_MATCH("Does not meet the trigger condition"),
    PIPELINE_RUN_ERROR("pipeline run with error"),
    PIPELINE_DISABLE("Pipeline is disabled"),
    CI_YAML_TEMPLATE_ERROR("YAML template parse error: %s"),
    PIPELINE_PREPARE_ERROR("Pipeline prepare error: %s"),
    UNKNOWN_ERROR("Unknown error, please contact DevOps-helper. %s");

    companion object {

        fun getTriggerReason(name: String): TriggerReason? {
            values().forEach { enumObj ->
                if (enumObj.name == name) {
                    return enumObj
                }
            }
            return null
        }
    }
}
