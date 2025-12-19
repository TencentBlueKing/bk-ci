---
name: 21-expression-parser
description: 表达式解析器指南
---

# 表达式解析器

表达式解析器指南.

## 触发条件

当用户需要实现条件表达式解析、变量替换、动态计算时，使用此 Skill。

## ExpressionParser

```kotlin
class ExpressionParser private constructor() {
    companion object {
        // 创建解析树
        fun createTree(
            expression: String,
            nameValues: Iterable<NamedValueInfo>?,
            functions: Iterable<IFunctionInfo>?
        ): IExpressionNode?
        
        // 验证表达式语法
        fun validateSyntax(
            expression: String,
            context: ExecutionContext?
        ): ValidateResult
    }
}
```

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
${{ format('{0}-{1}', variables.name, variables.version) }}
```

## 内置函数

| 函数 | 说明 | 示例 |
|------|------|------|
| `eq` | 相等 | `eq(a, b)` |
| `ne` | 不等 | `ne(a, b)` |
| `and` | 与 | `and(a, b)` |
| `or` | 或 | `or(a, b)` |
| `not` | 非 | `not(a)` |
| `contains` | 包含 | `contains(str, 'sub')` |
| `startsWith` | 前缀 | `startsWith(str, 'pre')` |
| `endsWith` | 后缀 | `endsWith(str, 'suf')` |
| `format` | 格式化 | `format('{0}', a)` |

## 使用示例

```kotlin
fun evaluateCondition(
    expression: String,
    variables: Map<String, String>
): Boolean {
    val nameValues = variables.map { (k, v) ->
        NamedValueInfo(k, ContextValueNode(v))
    }
    
    val tree = ExpressionParser.createTree(
        expression = expression,
        nameValues = nameValues,
        functions = BuiltInFunctions.all
    )
    
    val context = ExecutionContext()
    val result = tree?.evaluate(context)
    
    return result?.toBoolean() ?: false
}

// 使用
val shouldRun = evaluateCondition(
    expression = "eq(variables.env, 'prod')",
    variables = mapOf("env" to "prod")
)
```

## 变量替换

```kotlin
fun replaceVariables(
    template: String,
    variables: Map<String, String>
): String {
    var result = template
    variables.forEach { (key, value) ->
        result = result.replace("\${{ variables.$key }}", value)
    }
    return result
}
```

## 最佳实践

1. **语法验证**：解析前先验证语法
2. **默认值**：变量不存在时提供默认值
3. **安全性**：避免执行不可信表达式
4. **性能**：缓存解析树避免重复解析

## 相关文件

- `common-expression/src/main/kotlin/com/tencent/devops/common/expression/`
