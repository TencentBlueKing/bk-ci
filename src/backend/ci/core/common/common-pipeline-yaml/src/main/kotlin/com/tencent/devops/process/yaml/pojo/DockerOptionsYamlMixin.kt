package com.tencent.devops.process.yaml.pojo

import com.fasterxml.jackson.annotation.JsonProperty

/** YAML uses kebab-case; the Agent and editor JSON protocol must keep camelCase. */
abstract class DockerOptionsYamlMixin {
    @get:JsonProperty("mount-workspace")
    abstract val mountWorkspace: Boolean?
}
