---
name: 21-expression-parser
description: 表达式解析器指南，涵盖变量表达式解析、条件表达式求值、自定义函数扩展、表达式缓存、安全沙箱。当用户实现表达式解析、处理流水线变量替换、扩展表达式函数或配置表达式引擎时使用。
core_files:
  - "src/backend/ci/core/common/common-expression/"
related_skills:
  - 26-pipeline-variable-extension
token_estimate: 1500
---

# 表达式解析器

## Quick Reference

```
语法：${{ variables.xxx }} | ${{ eq(a, b) }} | ${{ and(x, y) }}
解析：ExpressionParser.createTree(expression, nameValues, functions)
求值：tree.evaluate(context)
```

### 最简示例

```kotlin
fun evaluateCondition(expression: String, variables: Map<String, String>): Boolean {
    val nameValues = variables.map { (k, v) ->
        NamedValueInfo(k, ContextValueNode(v))
    }
    
    val tree = ExpressionParser.createTree(
        expression = expression,
        nameValues = nameValues,
        functions = BuiltInFunctions.all
    )
    
    return tree?.evaluate(ExecutionContext())?.toBoolean() ?: false
}

// 使用
val shouldRun = evaluateCondition(
    expression = "eq(variables.env, 'prod')",
    variables = mapOf("env" to "prod")
)  // true
```

## When to Use

- 条件表达式解析
- 变量替换
- 动态计算

---

## 表达式语法

```yaml
# 变量引用
${{ variables.buildNo }}
${{ parameters.env }}

# 条件表达式
${{ eq(variables.branch, 'master') }}
${{ and(eq(variables.env, 'prod'), ne(variables.skip, 'true')) }}

# 内置函数
${{ contains(variables.tags, 'release') }}
${{ startsWith(variables.branch, 'feature/') }}
```

## 内置函数

| 函数 | 说明 | 示例 |
|------|------|------|
| `eq` | 相等 | `eq(a, b)` |
| `ne` | 不等 | `ne(a, b)` |
| `and` | 与 | `and(a, b)` |
| `or` | 或 | `or(a, b)` |
| `contains` | 包含 | `contains(str, 'sub')` |
| `startsWith` | 前缀 | `startsWith(str, 'pre')` |

---

## Checklist

- [ ] 解析前先验证语法
- [ ] 变量不存在时提供默认值
- [ ] 避免执行不可信表达式
- [ ] 缓存解析树避免重复解析
