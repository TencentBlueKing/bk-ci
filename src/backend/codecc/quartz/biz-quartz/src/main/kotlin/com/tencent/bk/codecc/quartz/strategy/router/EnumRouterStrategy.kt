package com.tencent.bk.codecc.quartz.strategy.router

import com.tencent.bk.codecc.quartz.strategy.router.impl.ConsistentHashRouterStrategy

enum class EnumRouterStrategy(private val abstractRouterStrategy: AbstractRouterStrategy) {

    /**
     * 一致哈希
     */
    CONSISTENT_HASH(ConsistentHashRouterStrategy());

    fun getRouterStrategy(): AbstractRouterStrategy {
        return abstractRouterStrategy
    }
}