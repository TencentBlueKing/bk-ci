package com.tencent.devops.common.api.util

object ShardingUtil {

    private const val SHARDING_ROUTING_RULE_KEY_PREFIX = "SHARDING_ROUTING_RULE"

    /**
     * 获取路由规则存在缓存中的key
     * @param clusterName 集群名称
     * @param moduleCode 模块标识
     * @param ruleType 规则类型
     * @param routingName 规则名称
     * @return 路由规则存在缓存中的key
     */
    fun getShardingRoutingRuleKey(
        clusterName: String,
        moduleCode: String,
        ruleType: String,
        routingName: String,
        tableName: String? = null
    ): String {
        val prefix = "$SHARDING_ROUTING_RULE_KEY_PREFIX:$clusterName:$moduleCode:$ruleType"
        return if (tableName.isNullOrBlank()) {
            "$prefix:$routingName"
        } else {
            "$prefix:$tableName:$routingName"
        }
    }
}
