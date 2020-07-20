# 日志规范

## 日志级别规范

### DEBUG

- 代码的运行日志，正常情况生产上不会打开DEBUG，不要依赖该日志到线上排查问题

### INFO

表示正常的输出，需要打印出来的关键信息（不包含敏感信息），作为出问题排查或行为辅助数据。

- 尽量少的日志，只打印关键信息，可以形成排查依据。
- 不要打印带有敏感信息的字段。

### WARN

当业务逻辑出现错误时，需要打印的日志，但要区分以下情况：

- 该错误不是由系统本身的异常引起的，打印WARN
- 该业务错误不影响业务流程继续执行下去的，打印WARN
- 该业务错误影响业务流程继续执行下去的，但是因为用户输入问题导致的，打WARN
- 业务代码中不要乱用RuntimeException，应定义业务类型的Exception， 并且捕获打印WARN

### ERROR

ERROR日志是开发要关注解决的日志信息，原则是要尽量少，一个正常的系统应该没有ERROR日志，所以要规范ERROR的使用。分以下场景：

- 平台/系统发生**异常** 需要打印ERROR
- 与外部系统调用发生异常的，即使捕获了继续往下走，也要打印ERROR
- 影响业务继续执行下去的错误（不是用户输入不合法参数引起的），要打印ERROR

## 敏感字段的脱敏示例：

一般Bean中或多或少有敏感信息：密钥，密码，凭证，token，secretKey(不局限于此，敏感信息根据业务场景不同有不同的定义）。
开发一般会习惯了简单的直接对某个bean直接log出来，或者为了方便定位问题把敏感字段直接打印出来（这种问题应该在开发&测试阶段就暴露出来)，所以不想写各种 判断去做特定字段的log输出，但不管什么原因，只要程序log文件被各种数据工具爬到的，都可能造成泄密， 这在系统中是被禁止的，造成不必要的泄密。以下提供一种比较简单的方式从bean中对敏感字段进行脱敏：

- 最好不要用map存储，需要定义Bean
- 对Bean中的敏感字段加[@SkipLogField](https://github.com/SkipLogField)注解
- 对Bean做log输出之前使用JsonUtil.skipLogFields脱敏信息

github示例代码链接：[JsonUtilTest.skipLogFields](https://github.com/Tencent/bk-ci/tree/master/src/backend/ci/core/common/common-api/src/test/kotlin/com/tencent/devops/common/api/util/JsonUtilTest.kt)

1. ```
   1. @Test
   2.     fun skipLogFields() {
   3. 
   4.         val bean = NameAndValue(key = "name", value = "this is password 123456", type = TestType.STRING)
   5.         val allJsonData = JsonUtil.toJson(bean)
   6. 
   7.         println("正常的Json序列化不受影响: $allJsonData")
   8. 
   9.         val allFieldsMap = JsonUtil.to>(allJsonData)
   10.         // 所有字段都存在，否则就是有问题
   11.         Assert.assertNotNull(allFieldsMap["key"])
   12.         Assert.assertNotNull(allFieldsMap["value"])
   13.         Assert.assertNotNull(allFieldsMap["valueType"])
   14. 
   15.        // 日志脱密
   16.         val logJsonString = JsonUtil.skipLogFields(bean)
   17. 
   18.         Assert.assertNotNull(logJsonString)
   19.         println("脱密后的Json不会有skipLogField的敏感log信息: $logJsonString")
   20. 
   21.         val haveNoSkipLogFieldsMap = JsonUtil.to>(logJsonString!!)
   22.         // 以下字段受SkipLogField注解影响，是不会出现的，如果有则说明有问题
   23.         Assert.assertNull(haveNoSkipLogFieldsMap["value"])
   24.         Assert.assertNull(haveNoSkipLogFieldsMap["valueType"])
   25.         // 未受SkipLogField注解影响的字段是存在的
   26.         Assert.assertNotNull(haveNoSkipLogFieldsMap["key"])
   27.     }
   28.   // 通过 SkipLogFields 注解与方法可以指定哪些字段在toJson时不输出，适合在日志打印之前，做一次调用。
   29.     data class NameAndValue(
   30.         val key: String,
   31.         @SkipLogField
   32.         val value: String,
   33.         @SkipLogField("valueType") // 如果字段序列化输出命名与字段不一致，则需要填写
   34.         @get:JsonProperty("valueType")
   35.         val type: TestType
   36.     )
   37. 
   38.     enum class TestType { STRING, INT }
   ```

   