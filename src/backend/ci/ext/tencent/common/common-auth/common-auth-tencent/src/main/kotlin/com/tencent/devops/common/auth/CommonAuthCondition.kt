package com.tencent.devops.common.auth

import org.springframework.context.annotation.Condition
import org.springframework.context.annotation.ConditionContext
import org.springframework.core.env.get
import org.springframework.core.type.AnnotatedTypeMetadata

class CommonAuthCondition : Condition {
    override fun matches(context: ConditionContext, metadata: AnnotatedTypeMetadata): Boolean {
        val authIdProvider = context.environment["auth.idProvider"]
        return listOf("client", "git", "new_v3").contains(authIdProvider)
    }
}
