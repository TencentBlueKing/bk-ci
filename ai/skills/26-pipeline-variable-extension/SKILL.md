---
name: 26-pipeline-variable-extension
description: 流水线变量字段扩展指南，涵盖变量字段定义、类型扩展、变量作用域、变量继承、自定义变量处理。当用户扩展流水线变量、添加新变量字段、处理变量作用域或实现变量继承时使用。
---

# Skill 26: 流水线变量字段扩展

## 适用场景
当需要为流水线变量（Pipeline Variable）新增字段时，本指南提供完整的改动路径和规范。

## 核心概念

### 两种变量模型

BK-CI 中流水线变量存在两种数据模型的双向转换：

1. **BuildFormProperty**（后端内部模型）
   - 位置：`common-pipeline/src/main/kotlin/.../BuildFormProperty.kt`
   - 用途：微服务内部使用，数据库存储格式
   - 特点：完整的字段定义，包含 Swagger 注解

2. **Variable**（YAML 模型）
   - 位置：`common-pipeline-yaml/src/main/kotlin/.../yaml/v3/models/Variable.kt`
   - 用途：YAML 流水线定义，对外 API 交互
   - 特点：Jackson 注解，支持 JSON 序列化

### 转换器（VariableTransfer）
- 位置：`common-pipeline-yaml/src/main/kotlin/.../yaml/transfer/VariableTransfer.kt`
- 职责：实现 `BuildFormProperty` ↔ `Variable` 的双向转换
- 核心方法：
  - `makeVariableFromModel()` - Model → YAML
  - `makeVariableFromYaml()` - YAML → Model

### YAML 模板解析器（YamlObjects）
- 位置：`common-pipeline-yaml/src/main/kotlin/.../yaml/v3/parsers/template/YamlObjects.kt`
- 职责：从原始 YAML Map 解析构建 `Variable` 对象（模板引用场景）
- 核心方法：`getVariable()` - 解析 YAML 变量定义
- **重要**：此文件与 `VariableTransfer` 是两条独立的解析路径，新增字段时**必须同时修改**

---

## 改动清单（按顺序执行）

### 1. 定义 YAML 模型字段

**文件**：`src/backend/ci/core/common/common-pipeline-yaml/src/main/kotlin/com/tencent/devops/process/yaml/v3/models/Variable.kt`

**操作**：在 `Variable` 或 `VariableProps` data class 中添加字段

**规范**：
```kotlin
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class Variable(
    // ... 现有字段
    
    @get:JsonProperty("new-field-name")  // YAML 中的字段名（kebab-case）
    @get:Schema(title = "字段中文描述", required = false)
    var newFieldName: Boolean? = null,  // Kotlin 属性名（camelCase）
    
    // 注意事项：
    // 1. 使用可空类型（Type?）避免破坏已有数据
    // 2. @JsonProperty 注解指定 YAML 序列化名称
    // 3. @Schema 提供 API 文档描述
)
```

**实际案例（#12471）**：
```kotlin
data class Variable(
    // ... 其他字段
    @get:JsonProperty("as-instance-input")
    @get:Schema(title = "默认为实例入参,只有模版才有值,流水线没有值", required = false)
    var asInstanceInput: Boolean? = null,
)
```

---

### 2. 定义内部模型字段

**文件**：`src/backend/ci/core/common/common-pipeline/src/main/kotlin/com/tencent/devops/common/pipeline/pojo/BuildFormProperty.kt`

**操作**：在 `BuildFormProperty` data class 中添加对应字段

**规范**：
```kotlin
@Schema(title = "构建模型-表单元素属性")
data class BuildFormProperty(
    // ... 现有字段
    
    @get:Schema(
        title = "字段中文描述，说明用途和作用范围",
        required = false
    )
    var newFieldName: Boolean? = null
)
```

**注意事项**：
- 字段名与 Variable 中保持一致（camelCase）
- 使用 Swagger v3 注解（`@Schema`）
- 参数顺序：建议放在 data class 末尾，避免影响现有构造函数调用

**实际案例（#12471）**：
```kotlin
data class BuildFormProperty(
    // ... 其他字段
    @get:Schema(
        title = "在新增实例、以及新增变量时作用，控制实例化页面「实例入参」按钮, 当required:true时,值才生效",
        required = false
    )
    var asInstanceInput: Boolean? = null
)
```

---

### 3. 实现 Model → YAML 转换逻辑

**文件**：`src/backend/ci/core/common/common-pipeline-yaml/src/main/kotlin/com/tencent/devops/process/yaml/transfer/VariableTransfer.kt`

**方法**：`makeVariableFromModel(triggerContainer: TriggerContainer?): Map<String, Variable>?`

**操作**：在构建 `Variable` 对象时，添加字段映射逻辑

**规范**：
```kotlin
fun makeVariableFromModel(triggerContainer: TriggerContainer?): Map<String, Variable>? {
    // ... 现有代码
    
    triggerContainer?.params?.forEach {
        // ... 处理 props
        
        result[it.id] = Variable(
            value = ...,
            readonly = ...,
            // 新增字段转换逻辑（根据业务规则）
            newFieldName = if (/* 条件判断 */) {
                it.newFieldName.nullIfDefault(defaultValue)
            } else null,
            // ... 其他字段
        )
    }
}
```

**实际案例（#12471）**：
```kotlin
result[it.id] = Variable(
    value = ...,
    readonly = ...,
    allowModifyAtStartup = if (const != true) it.required.nullIfDefault(true) else null,
    // 新增 asInstanceInput 转换：仅在非常量且 required=true 时转换
    asInstanceInput = if (const != true && it.required) {
        it.asInstanceInput.nullIfDefault(true)
    } else null,
    const = const,
    // ...
)
```

**关键技巧**：
- 使用 `.nullIfDefault(默认值)` 扩展方法省略默认值（减少 YAML 冗余）
- 根据业务逻辑判断是否需要序列化该字段
- 常见条件：`if (const != true)`（非常量时才处理）

---

### 4. 实现 YAML → Model 转换逻辑

**文件**：同上 `VariableTransfer.kt`

**方法**：`makeVariableFromYaml(variables: Map<String, Variable>?): List<BuildFormProperty>`

**操作**：在构建 `BuildFormProperty` 时，添加字段赋值

**规范**：
```kotlin
fun makeVariableFromYaml(variables: Map<String, Variable>?): List<BuildFormProperty> {
    // ... 现有代码
    
    variables.forEach { (key, variable) ->
        // 计算派生字段（如有必要）
        val allowModifyAtStartup = variable.allowModifyAtStartup ?: true
        
        buildFormProperties.add(
            BuildFormProperty(
                id = key,
                // ... 其他字段
                
                // 新增字段赋值（必须提供默认值处理 null 情况）
                newFieldName = variable.newFieldName ?: defaultValue
            )
        )
    }
}
```

**实际案例（#11738 sensitive 字段）**：
```kotlin
buildFormProperties.add(
    BuildFormProperty(
        id = key,
        // ...
        // ⚠️ 必须使用 ?: 提供默认值，避免 null 传递到 Model
        sensitive = variable.sensitive ?: false
    )
)
```

**注意事项**：
- 考虑字段间的依赖关系（如 `asInstanceInput` 依赖 `required`）
- **必须提供合理的默认值**（`?: defaultValue`），避免 null 传递导致下游问题
- 保持与前端约定的语义一致

---

### 4.1 更新 YamlObjects 模板解析器（易遗漏！）

**文件**：`src/backend/ci/core/common/common-pipeline-yaml/src/main/kotlin/com/tencent/devops/process/yaml/v3/parsers/template/YamlObjects.kt`

**方法**：`getVariable(fromPath: String, variable: Map<String, Any>): Variable`

**操作**：在构建 `Variable` 对象时，添加新字段的解析

**为什么需要修改这个文件**：
- `VariableTransfer` 处理的是已经反序列化好的 `Variable` 对象
- `YamlObjects.getVariable()` 处理的是原始 YAML Map（模板引用场景）
- **两条路径独立，必须同时修改**

**规范**：
```kotlin
fun getVariable(fromPath: String, variable: Map<String, Any>): Variable {
    return Variable(
        value = ...,
        readonly = getNullValue("readonly", variable)?.toBoolean(),
        // ... 其他字段
        
        // 新增字段解析
        newFieldName = getNullValue("new-field-name", variable)?.toBoolean()
    )
}
```

**实际案例（#11738 sensitive 字段）**：
```kotlin
return Variable(
    value = ...,
    const = getNullValue("const", variable)?.toBoolean(),
    allowModifyAtStartup = getNullValue("allow-modify-at-startup", variable)?.toBoolean(),
    props = props,
    ifCondition = transNullValue<Map<String, String>>(fromPath, "if", "if", variable),
    // 新增 sensitive 和 asInstanceInput 字段解析
    sensitive = getNullValue("sensitive", variable)?.toBoolean(),
    asInstanceInput = getNullValue("asInstanceInput", variable)?.toBoolean()
)
```

**常见错误**：
```kotlin
// ❌ 错误：只修改了 VariableTransfer，遗漏了 YamlObjects
// 导致模板引用场景下新字段丢失

// ✅ 正确：两个文件都要修改
// 1. VariableTransfer.kt - 处理 API 请求的 Variable 对象
// 2. YamlObjects.kt - 处理模板引用的原始 YAML Map
```

---

### 5. 更新 YAML JSON Schema

**文件**：`src/backend/ci/core/common/common-pipeline-yaml/src/main/resources/schema/V3_0/ci.json`

**操作**：在 Variable 的 JSON Schema 定义中添加字段

**路径定位**：搜索 `"definitions"` → `"Variable"` 或相关对象

**规范**：
```json
{
  "definitions": {
    "Variable": {
      "type": "object",
      "properties": {
        "value": { "type": ["string", "number", "boolean", "object"] },
        "readonly": { "type": "boolean" },
        "allow-modify-at-startup": { "type": "boolean" },
        
        "new-field-name": {
          "type": "boolean",
          "description": "字段描述"
        }
      }
    }
  }
}
```

**实际案例（#12471）**：
```json
{
  "allow-modify-at-startup" : {
    "type" : "boolean"
  },
  "as-instance-input" : {
    "type" : "boolean"
  },
  "value-not-empty" : {
    "type" : "boolean"
  }
}
```

**作用**：
- 为 IDE 提供 YAML 自动补全
- 验证 YAML 文件格式正确性
- 生成 API 文档

---

### 6. 传递字段到服务层（可选）

**场景**：如果需要在业务服务中使用新字段

**文件**：`src/backend/ci/core/process/biz-process/src/main/kotlin/com/tencent/devops/process/service/ParamFacadeService.kt`

**操作**：在构建参数方法中传递字段

**规范**：
```kotlin
private fun copyFormProperty(
    property: BuildFormProperty,
    // ... 其他参数
): BuildFormProperty {
    return BuildFormProperty(
        id = property.id,
        required = property.required,
        // ... 其他字段
        newFieldName = property.newFieldName  // 传递新字段
    )
}
```

**实际案例（#12471）**：
```kotlin
return BuildFormProperty(
    id = property.id,
    // ... 
    displayCondition = property.displayCondition,
    asInstanceInput = property.asInstanceInput  // 新增传递
)
```

---

## 国际化（如需要）

### 7. 添加多语言支持

**文件**：`support-files/i18n/project/message_*.properties`

**操作**：如果字段涉及用户可见文案，需添加翻译

**示例**：
```properties
# message_zh_CN.properties
variable.asInstanceInput=默认为实例入参

# message_en_US.properties
variable.asInstanceInput=Default as Instance Input

# message_ja_JP.properties
variable.asInstanceInput=デフォルトでインスタンス入力
```

---

## 测试建议

### 单元测试覆盖

**测试文件位置**：`src/backend/ci/core/common/common-pipeline-yaml/src/test/kotlin/.../VariableTransferTest.kt`

**测试用例类型**：
```kotlin
@Test
fun `test makeVariableFromModel with new field`() {
    // 测试 Model → YAML 转换
    val property = BuildFormProperty(
        id = "testVar",
        newFieldName = true,
        // ...
    )
    val result = variableTransfer.makeVariableFromModel(...)
    assertEquals(true, result["testVar"]?.newFieldName)
}

@Test
fun `test makeVariableFromYaml with new field`() {
    // 测试 YAML → Model 转换
    val variable = Variable(
        value = "test",
        newFieldName = false
    )
    val result = variableTransfer.makeVariableFromYaml(mapOf("testVar" to variable))
    assertEquals(false, result[0].newFieldName)
}

@Test
fun `test new field default value`() {
    // 测试默认值处理
    val variable = Variable(value = "test", newFieldName = null)
    val result = variableTransfer.makeVariableFromYaml(mapOf("testVar" to variable))
    assertEquals(expectedDefault, result[0].newFieldName)
}
```

---

## 最佳实践

### DO ✅

1. **字段命名一致性**
   - YAML：`kebab-case`（如 `as-instance-input`）
   - Kotlin：`camelCase`（如 `asInstanceInput`）
   - 使用 `@JsonProperty` 映射

2. **可空类型优先**
   ```kotlin
   var newField: Boolean? = null  // ✅ 推荐
   var newField: Boolean = false  // ❌ 避免（破坏兼容性）
   ```

3. **省略默认值**
   ```kotlin
   // 使用 nullIfDefault 减少 YAML 冗余
   newField = variable.newField.nullIfDefault(true)
   ```

4. **条件序列化**
   ```kotlin
   // 仅在特定条件下才序列化字段
   asInstanceInput = if (const != true && required) {
       property.asInstanceInput.nullIfDefault(true)
   } else null
   ```

5. **注释清晰**
   ```kotlin
   // 仅在非常量且必填时生效
   @get:Schema(title = "默认为实例入参,只有模版才有值,流水线没有值", required = false)
   ```

### DON'T ❌

1. **直接修改字段顺序**
   - 避免修改 data class 已有字段的顺序
   - 新字段追加到末尾

2. **忽略向后兼容**
   ```kotlin
   // ❌ 错误：强制要求非空
   var newField: Boolean
   
   // ✅ 正确：可空类型
   var newField: Boolean? = null
   ```

3. **缺少 Schema 注解**
   ```kotlin
   // ❌ 缺少文档
   var newField: Boolean? = null
   
   // ✅ 完整注解
   @get:Schema(title = "字段说明", required = false)
   var newField: Boolean? = null
   ```

4. **忘记更新 JSON Schema**
   - 会导致 IDE 自动补全失效
   - YAML 验证可能不准确

5. **遗漏 YamlObjects.kt（最常见错误！）**
   ```kotlin
   // ❌ 错误：只修改了 VariableTransfer
   // 模板引用场景下新字段会丢失
   
   // ✅ 正确：同时修改两个文件
   // - VariableTransfer.kt
   // - YamlObjects.kt
   ```

6. **YAML → Model 转换时未提供默认值**
   ```kotlin
   // ❌ 错误：直接传递可能为 null 的值
   sensitive = variable.sensitive
   
   // ✅ 正确：使用 ?: 提供默认值
   sensitive = variable.sensitive ?: false
   ```

---

## 常见问题

### Q1: 字段应该放在 Variable 还是 VariableProps？

**判断标准**：
- **Variable**：核心字段（值、只读、常量等）
- **VariableProps**：UI 相关、类型特定属性（标签、选项、描述等）

**案例（#12471）**：
- `asInstanceInput` 放在 `Variable`：因为它是模板实例化的核心逻辑
- `label`、`description` 在 `VariableProps`：UI 展示属性

### Q2: 如何处理字段间的依赖关系？

**场景**：`asInstanceInput` 依赖 `required` 字段

**解决方案**：
```kotlin
// 先计算依赖字段
val allowModifyAtStartup = variable.allowModifyAtStartup ?: true

// 再使用条件赋值
asInstanceInput = if (allowModifyAtStartup) {
    variable.asInstanceInput ?: true
} else null
```

### Q3: 什么时候需要更新 ParamFacadeService？

**场景判断**：
- ✅ 需要：字段在业务逻辑中使用（如实例化、执行时参数校验）
- ❌ 不需要：纯粹的存储字段（如 UI 配置）

### Q4: 为什么修改了 VariableTransfer 还是有问题？

**常见原因**：遗漏了 `YamlObjects.kt` 文件

**背景**：YAML 变量解析有两条独立路径：
1. **API 请求路径**：JSON → Jackson 反序列化 → `Variable` 对象 → `VariableTransfer`
2. **模板引用路径**：YAML Map → `YamlObjects.getVariable()` → `Variable` 对象

**症状**：
- 直接定义的 YAML 变量字段正常
- 模板引用（`template: xxx.yml`）的变量字段丢失

**解决方案**：同时修改 `YamlObjects.kt` 中的 `getVariable()` 方法

```kotlin
// YamlObjects.kt
fun getVariable(fromPath: String, variable: Map<String, Any>): Variable {
    return Variable(
        // ... 其他字段
        newFieldName = getNullValue("new-field-name", variable)?.toBoolean()
    )
}
```

---

## 检查清单

变更完成后，逐项确认：

- [ ] Variable.kt 新增字段（含 @JsonProperty、@Schema）
- [ ] BuildFormProperty.kt 新增字段（含 @Schema）
- [ ] VariableTransfer.makeVariableFromModel() 添加转换逻辑
- [ ] VariableTransfer.makeVariableFromYaml() 添加转换逻辑（**注意提供默认值**）
- [ ] **YamlObjects.getVariable() 添加字段解析（易遗漏！）**
- [ ] ci.json JSON Schema 更新
- [ ] ParamFacadeService.kt 字段传递（如需要）
- [ ] 国际化文件添加翻译（如需要）
- [ ] 单元测试覆盖双向转换和默认值场景
- [ ] 代码通过 Detekt 静态检查
- [ ] 提交信息符合规范（`feat: 变量支持xxx字段 #issue`）

---

## 参考案例

### 完整实现：sensitive 字段（#11738）

**需求**：流水线变量支持「是否敏感」属性，敏感变量在日志中脱敏

**改动文件**：
1. `Variable.kt` - 添加 `sensitive` 字段
2. `BuildFormProperty.kt` - 添加 `sensitive` 字段
3. `VariableTransfer.kt` - 实现双向转换（共 2 处）
4. **`YamlObjects.kt` - 添加模板解析支持（易遗漏！）**
5. `ci.json` - 添加 Schema 定义
6. `ParamFacadeService.kt` - 传递字段

**核心逻辑**：
```kotlin
// Model → YAML (VariableTransfer.makeVariableFromModel)
sensitive = it.sensitive.nullIfDefault(false)

// YAML → Model (VariableTransfer.makeVariableFromYaml)
// ⚠️ 必须提供默认值
sensitive = variable.sensitive ?: false

// 模板解析 (YamlObjects.getVariable) - 易遗漏！
sensitive = getNullValue("sensitive", variable)?.toBoolean()
```

### 完整实现：asInstanceInput 字段（#12471）

**需求**：模板实例化时控制变量是否作为实例入参

**改动文件**：
1. `Variable.kt` - 添加 `asInstanceInput` 字段
2. `BuildFormProperty.kt` - 添加 `asInstanceInput` 字段
3. `VariableTransfer.kt` - 实现双向转换（共 2 处）
4. **`YamlObjects.kt` - 添加模板解析支持**
5. `ci.json` - 添加 Schema 定义
6. `ParamFacadeService.kt` - 传递字段

**核心逻辑**：
```kotlin
// Model → YAML: 仅在非常量且必填时转换
asInstanceInput = if (const != true && it.required) {
    it.asInstanceInput.nullIfDefault(true)
} else null

// YAML → Model: 依赖 allowModifyAtStartup
asInstanceInput = if (allowModifyAtStartup) {
    variable.asInstanceInput ?: true
} else null

// 模板解析 (YamlObjects.getVariable)
asInstanceInput = getNullValue("asInstanceInput", variable)?.toBoolean()
```

---

## 总结

新增流水线变量字段的标准流程：

```
数据模型定义（2个文件）
    ↓
转换逻辑实现（1个文件，2个方法）
    ↓
模板解析器更新（YamlObjects.kt - 易遗漏！）
    ↓
Schema 更新（1个文件）
    ↓
服务层传递（按需）
    ↓
国际化支持（按需）
    ↓
测试验证
```

遵循本指南可确保字段扩展的完整性、一致性和向后兼容性。

## 相关文件

- `Variable.kt`: `src/backend/ci/core/common/common-pipeline-yaml/src/main/kotlin/com/tencent/devops/process/yaml/v3/models/Variable.kt`
- `BuildFormProperty.kt`: `src/backend/ci/core/common/common-pipeline/src/main/kotlin/com/tencent/devops/common/pipeline/pojo/BuildFormProperty.kt`
- `VariableTransfer.kt`: `src/backend/ci/core/common/common-pipeline-yaml/src/main/kotlin/com/tencent/devops/process/yaml/transfer/VariableTransfer.kt`
- **`YamlObjects.kt`**: `src/backend/ci/core/common/common-pipeline-yaml/src/main/kotlin/com/tencent/devops/process/yaml/v3/parsers/template/YamlObjects.kt`
- `ci.json`: `src/backend/ci/core/common/common-pipeline-yaml/src/main/resources/schema/V3_0/ci.json`
- `ParamFacadeService.kt`: `src/backend/ci/core/process/biz-process/src/main/kotlin/com/tencent/devops/process/service/ParamFacadeService.kt`
