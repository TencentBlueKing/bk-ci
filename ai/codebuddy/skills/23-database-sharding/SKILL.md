---
name: 23-database-sharding
description: 数据库分片指南，涵盖分片策略设计、分片键选择、跨分片查询、数据迁移、分片路由规则。当用户设计数据库分片、选择分片键、处理跨分片查询或进行分片数据迁移时使用。
---

# 数据库分片

数据库分片指南.

## 触发条件

当用户需要实现数据库水平扩展、分库分表、读写分离时，使用此 Skill。

## ShardingSphere 配置

```kotlin
@Configuration
@ConditionalOnProperty(
    prefix = "sharding",
    name = ["enabled"],
    havingValue = "true"
)
class BkShardingDataSourceConfiguration {
    
    @Bean
    fun shardingDataSource(
        shardingProperties: ShardingProperties
    ): DataSource {
        val config = ShardingRuleConfiguration()
        
        // 配置分片规则
        config.tables = listOf(
            createTableRule("T_PIPELINE_BUILD_HISTORY"),
            createTableRule("T_PIPELINE_BUILD_DETAIL")
        )
        
        // 配置分片算法
        config.shardingAlgorithms = mapOf(
            "project-inline" to createProjectShardingAlgorithm()
        )
        
        return ShardingSphereDataSourceFactory.createDataSource(
            createDataSourceMap(),
            listOf(config),
            Properties()
        )
    }
}
```

## 分片策略

### 按项目分片

```kotlin
fun createTableRule(tableName: String): ShardingTableRuleConfiguration {
    return ShardingTableRuleConfiguration(
        logicTable = tableName,
        actualDataNodes = "ds_\${0..3}.$tableName"
    ).apply {
        databaseShardingStrategy = StandardShardingStrategyConfiguration(
            shardingColumn = "PROJECT_ID",
            shardingAlgorithmName = "project-inline"
        )
    }
}

// 分片算法
class ProjectShardingAlgorithm : StandardShardingAlgorithm<String> {
    override fun doSharding(
        availableTargetNames: Collection<String>,
        shardingValue: PreciseShardingValue<String>
    ): String {
        val hash = shardingValue.value.hashCode() and Int.MAX_VALUE
        val index = hash % availableTargetNames.size
        return "ds_$index"
    }
}
```

## 绑定表配置

```yaml
# 绑定表：保证关联查询在同一数据源
sharding:
  binding-tables:
    - T_PIPELINE_BUILD_HISTORY,T_PIPELINE_BUILD_DETAIL
    - T_PIPELINE_INFO,T_PIPELINE_SETTING
```

## 数据源配置

```yaml
spring:
  shardingsphere:
    datasource:
      names: ds_0,ds_1,ds_2,ds_3
      ds_0:
        url: jdbc:mysql://host1:3306/devops_ci_process
      ds_1:
        url: jdbc:mysql://host2:3306/devops_ci_process
      ds_2:
        url: jdbc:mysql://host3:3306/devops_ci_process
      ds_3:
        url: jdbc:mysql://host4:3306/devops_ci_process
```

## 查询路由

```kotlin
// 带分片键查询 - 路由到单个数据源
fun getByProjectId(projectId: String): List<Build> {
    return dslContext.selectFrom(T_PIPELINE_BUILD_HISTORY)
        .where(T_PIPELINE_BUILD_HISTORY.PROJECT_ID.eq(projectId))
        .fetch()
}

// 不带分片键 - 广播到所有数据源
fun countAll(): Long {
    return dslContext.selectCount()
        .from(T_PIPELINE_BUILD_HISTORY)
        .fetchOne(0, Long::class.java) ?: 0
}
```

## 最佳实践

1. **选择合适分片键**：选择高基数、查询频繁的字段
2. **绑定表**：关联表使用相同分片键
3. **避免跨分片查询**：尽量带分片键查询
4. **数据迁移**：提前规划扩容方案

## 相关文件

- `common-db-sharding/src/main/kotlin/com/tencent/devops/common/db/config/`
