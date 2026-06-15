package com.tencent.devops.common.pipeline.pojo.atom.form.enums

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "插件表单-条件之间的关系")
enum class AtomFormComponentRelyOperation {
    AND,
    OR,
    NOT
}