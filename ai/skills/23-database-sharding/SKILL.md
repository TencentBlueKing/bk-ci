---
name: 23-database-sharding
description: 数据库分片指南，涵盖分片策略设计、分片键选择、跨分片查询、数据迁移、分片路由规则。当用户设计数据库分片、选择分片键、处理跨分片查询或进行分片数据迁移时使用。
core_files:
  - "src/backend/ci/core/common/common-db-sharding/"
related_skills:
  - 06-database-script-management
  - 44-database-design
token_estimate: 1500
---

# 数据库分片

## Quick Reference

```
框架：ShardingSphere
分片键：PROJECT_ID（按项目分片）
绑定表：关联查询的表使用相同分片键
```

### 最简示例

```kotlin
@Configuration
@ConditionalOnProperty(prefix = "sharding", name = ["enabled"], havingValue = "true")
class BkShardingDataSourceConfiguration {
    @Bean
    fun shardingDataSource(props: ShardingProperties): DataSource {
        val config = ShardingRuleConfiguration()
        config.tables = listOf(
            createTableRule("T_PIPELINE_BUILD_HISTORY"),
            createTableRule("T_PIPELINE_BUILD_DETAIL")
        )
        config.shardingAlgorithms = mapOf(
            "project-inline" to createProjectShardingAlgorithm()
        )
        return ShardingSphereDataSourceFactory.createDataSource(
            createDataSourceMap(), listOf(config), Properties()
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

## When to Use

- 数据库水平扩展
- 大表分库分表
- 读写分离

## When NOT to Use

- 数据量较小的表
- 需要频繁跨分片查询

---

## 配置示例

```yaml
spring:
  shardingsphere:
    datasource:
      names: ds_0,ds_1,ds_2,ds_3
      ds_0:
        url: jdbc:mysql://host1:3306/devops_ci_process
      ds_1:
        url: jdbc:mysql://host2:3306/devops_ci_process

sharding:
  binding-tables:
    - T_PIPELINE_BUILD_HISTORY,T_PIPELINE_BUILD_DETAIL
```

## 查询路由

```kotlin
// 带分片键 → 路由到单个数据源
dslContext.selectFrom(T_PIPELINE_BUILD_HISTORY)
    .where(T_PIPELINE_BUILD_HISTORY.PROJECT_ID.eq(projectId))
    .fetch()

// 不带分片键 → 广播到所有数据源
dslContext.selectCount().from(T_PIPELINE_BUILD_HISTORY).fetchOne()
```

---

## Checklist

- [ ] 选择高基数、查询频繁的字段作为分片键
- [ ] 关联表使用相同分片键（绑定表）
- [ ] 尽量避免跨分片查询
- [ ] 提前规划扩容方案
