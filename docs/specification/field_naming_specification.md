# 代码命名规范

## 包名:

- 不允许有大写
- 包名有enum的要改为enums，Java与Kotlin的兼容问题。
- 包名不要命名为build，防止被误过滤

## 类名：

- 大驼峰命名，例如PipelineInfo

## 变量名：

- 所有变量没有意外的情况下全以小驼峰命名，例如：helloWorld

  ### Boolean 变量命名规范：

  ​     Boolean类型不允许以is开头, 否则在json序列化反序列化会出现字段赋值失败， github示例代码链接:

  [JsonUtilTest.isBoolean](https://github.com/Tencent/bk-ci/blob/master/src/backend/ci/core/common/common-api/src/test/kotlin/com/tencent/devops/common/api/util/JsonUtilTest.kt)

  ​    在isBoolean 测试用例的最后println输出会发现：在json序列化之后再反序列化回Bean时，is开头的isSecrecy字段值丢失了。

1. ```
   1.     @Test
   2.     fun isBoolean() {
   3.         val p = IsBoolean(helmChartEnabled = true, offlined = true, isSecrecy = true, exactResource = 999)
   4.         val json = JsonUtil.toJson(p)
   5.         println(JsonUtil.to(json, IsBoolean::class.java))
   6.     }
   7. 
   8.     data class IsBoolean(
   9.         val helmChartEnabled: Boolean?, // 正确的命名
   10.         @get:JsonProperty("offlined") // 正确的json命名
   11.         val offlined: Boolean?, // 正确的命名
   12.         @get:JsonProperty("is_secrecy") // 错误的json命名
   13.         val isSecrecy: Boolean?, // 错误的字段示例命名，会导致反序列化的空值
   14.         @get:JsonProperty("is_exact_resource")
   15.         val exactResource: Int = 1
   16.     )
   ```

   