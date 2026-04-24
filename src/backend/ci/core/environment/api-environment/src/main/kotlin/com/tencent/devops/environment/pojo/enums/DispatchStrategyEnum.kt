package com.tencent.devops.environment.pojo.enums

enum class StrategyType { DEFAULT, CUSTOM }

enum class DefaultStrategyCode(
    val scope: StrategyScope,
    val nodeRule: NodeRule,
    val displayName: String
) {
    PRE_BUILD_IDLE(StrategyScope.PRE_BUILD, NodeRule.IDLE, "最近使用+空闲节点"),
    PRE_BUILD_AVAILABLE(StrategyScope.PRE_BUILD, NodeRule.AVAILABLE, "最近使用+可用节点"),
    ALL_IDLE(StrategyScope.ALL, NodeRule.IDLE, "全部节点+空闲"),
    ALL_AVAILABLE(StrategyScope.ALL, NodeRule.AVAILABLE, "全部节点+可用");
}

enum class StrategyScope { PRE_BUILD, ALL }

enum class NodeRule { IDLE, AVAILABLE }

enum class LabelOp(val symbol: String) {
    EQUAL("=="),
    GTE(">="),
    LTE("<="),
    GT(">"),
    LT("<"),
    START_WITH("StartWith"),
    END_WITH("EndWith"),
    CONTAINS("Contains"),
    IN("in");
}
