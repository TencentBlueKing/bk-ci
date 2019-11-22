package com.tencent.devops.gitci.pojo.enums

enum class TriggerReason(val reason: String) {
    TRIGGER_SUCCESS("trigger success"),
    GIT_CI_DISABLE("git ci is disabled"),
    BUILD_PUSHED_BRANCHES_DISABLE("build pushed branches is disabled"),
    BUILD_PUSHED_PULL_REQUEST_DISABLE("build pushed pull request is disabled"),
    GIT_CI_YAML_NOT_FOUND("git ci yaml file not found"),
    GIT_CI_YAML_INVALID("git ci yaml is invalid"),
    TRIGGER_NOT_MATCH("yaml trigger is not match")
}
