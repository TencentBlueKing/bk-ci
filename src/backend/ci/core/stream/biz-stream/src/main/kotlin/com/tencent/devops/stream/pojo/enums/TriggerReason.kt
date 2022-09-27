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

package com.tencent.devops.stream.pojo.enums

enum class TriggerReason(val summary: String, val detail: String) {
    TRIGGER_SUCCESS("Trigger success", "Trigger success"),
    CI_DISABLED("CI is disabled", "CI is disabled"),
    BUILD_PUSHED_BRANCHES_DISABLED(
        "This project has not yet opened the push event monitoring configuration",
        "This project has not yet opened the push event monitoring configuration"
    ),
    BUILD_MERGE_REQUEST_DISABLED(
        "This project has not yet opened the MERGE_REQUEST event monitoring configuration",
        "This project has not yet opened the MERGE_REQUEST event monitoring configuration"
    ),
    CI_YAML_NOT_FOUND(
        "The YAML file cannot be found in the .ci directory",
        "The YAML file cannot be found in the .ci directory"
    ),
    CI_YAML_CONTENT_NULL("The YAML file is null", "The YAML file is null: %s"),
    CI_YAML_INVALID("YAML is invalid", "YAML is invalid: %s"),
    CI_TRIGGER_FORMAT_ERROR("Trigger format is error", "Trigger format is error: %s"),
    CI_YAML_NEED_MERGE_OR_REBASE(
        "The YAML is modified. " +
            "Please execute either REBASE or MERGE to update your YAML from remote target branch.",
        "The YAML %s is modified. " +
            "Please execute either REBASE or MERGE to update your YAML from remote target branch."
    ),
    CI_MERGE_CHECKING(
        "Checking whether there is a conflict in MERGE_REQUEST, please wait",
        "Checking whether there is a conflict in MERGE_REQUEST, please wait"
    ),
    CI_MERGE_CHECK_TIMEOUT("MERGE_REQUEST check timeout", "MERGE_REQUEST check timeout"),
    CI_MERGE_CONFLICT(
        "MERGE_REQUEST conflict, please resolve the conflict first",
        "MERGE_REQUEST conflict, please resolve the conflict first"
    ),
    TRIGGER_NOT_MATCH(
        "Does not meet the trigger condition",
        "Does not meet the trigger condition :%s"
    ),
    PIPELINE_RUN_ERROR("Pipeline run with error", "pipeline run with error"),
    PIPELINE_DISABLE("Pipeline is disabled", "Pipeline is disabled"),
    CI_YAML_TEMPLATE_ERROR("YAML template parse error", "YAML template parse error: %s"),
    PIPELINE_PREPARE_ERROR("Pipeline prepare error", "Pipeline prepare error: %s"),
    CREATE_QUALITY_RULES_ERROR("Create quality rules error", "Create quality rules error: %s"),
    MR_BRANCH_FILE_ERROR(
        "Mr changelist files not incomplete. Stream not trigger.",
        "In the case where the source branch has file %s and the target branch does not, " +
            "there are none in the mr changelist" +
            " .Stream not trigger."
    ),
    USER_SKIPED(
        "Skip ci by keyword in commit message",
        "Skip ci by keyword in commit message"
    ),
    REPO_TRIGGER_FAILED(
        "repo trigger failed",
        "repo trigger failed: %s"
    ),
    SAVE_PIPELINE_FAILED(
        "save pipeline failed",
        "save pipeline failed: %s"
    ),
    UNKNOWN_ERROR(
        "Unknown error, please contact DevOps-helper",
        "Unknown error, please contact DevOps-helper. %s"
    );

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
