package com.tencent.devops.common.auth.code


class BSProjectServiceCodec : ProjectAuthServiceCode {
    override fun id() = BSAuthServiceCode.PROJECT.value
}