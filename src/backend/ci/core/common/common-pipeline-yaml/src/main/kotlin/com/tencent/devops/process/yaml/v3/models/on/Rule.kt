package com.tencent.devops.process.yaml.v3.models.on

abstract class Rule(
    open val id: String? = null,
    open val name: String? = null,
    open val enable: Boolean? = true
)
