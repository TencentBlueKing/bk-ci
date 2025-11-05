package com.tencent.devops.store.pojo.event.enums;

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "条件操作符")
enum class ConditionOperator {
    EQ,
    NOT_EQ,
    IN,
    NOT_IN,
    LIKE,
    NOT_LIKE;
}