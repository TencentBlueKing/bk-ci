package com.tencent.devops.store.pojo.trigger.enums

import com.tencent.devops.store.pojo.trigger.expression.TriggerExpression
import com.tencent.devops.store.pojo.trigger.expression.EqualsExpression
import com.tencent.devops.store.pojo.trigger.expression.InExpression
import com.tencent.devops.store.pojo.trigger.expression.LikeExpression
import com.tencent.devops.store.pojo.trigger.expression.NotEqualsExpression
import com.tencent.devops.store.pojo.trigger.expression.NotInExpression
import com.tencent.devops.store.pojo.trigger.expression.NotLikeExpression
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "条件操作符")
enum class ConditionOperatorEnum(val expression: TriggerExpression) {
    EQ(EqualsExpression()),
    NOT_EQ(NotEqualsExpression()),
    IN(InExpression()),
    NOT_IN(NotInExpression()),
    LIKE(LikeExpression()),
    NOT_LIKE(NotLikeExpression());
}
