# 使用java开发插件示例

可以按照如下步骤一一开发插件，修改配置后体验插件开发流程

### Step 1. 登录蓝盾插件市场-插件工作台初始化插件

- 登录插件工作台
- 新增插件时，系统将初始化插件的基本信息



### Step 2. 代码库根目录下添加蓝盾插件配置文件：task.json

task.jaon配置规则详见：[插件开发规范](../../../docs/wiki/plugin_dev.md)

task.json简单示例如下：

- 必填项：

1.  修改atomCode为你的插件标识（初始化插件时填写的英文标识）
2.  修改执行配置execution.packagePath为你的插件执行包相对于打的zip包中的位置，execution.target为你的插件启动命令
3.  修改输入输出字段定义

- 非必填项：

1.  若调起执行前需安装依赖则无需填写执行配置execution.demands

```
{
  "atomCode": "demo",
  "execution": {
    "packagePath": "demo-jar-with-dependencies.jar",
    "language": "java",
    "minimumVersion": "1.8",
    "demands": [],
    "target": "java -jar demo-jar-with-dependencies.jar"
  },
  "input": {
    "desc": {
      "label": "描述",
      "default": "",
      "placeholder": "请输入描述信息",
      "type": "vuex-input",
      "desc": "描述",
      "required": true,
      "disabled": false,
      "hidden": false,
      "isSensitive": false
    }
  },
  "output": {
    "testResult": {
      "description": "升级是否成功",
      "type": "string",
      "isSensitive": false
    }
  }
}
```



### Step 3. 定义继承sdk包中的AtomBaseParam插件基本参数类的插件参数类

- 参数类需统一加上lombok框架的@Data注解(IDE开发工具需安装lombok插件)，参数类定义的参数类型统一为String格式

```
@Data
@EqualsAndHashCode(callSuper = true)
public class AtomParam extends AtomBaseParam {
    /**
     * 以下请求参数只是示例，具体可以删除修改成你要的参数
     */
    private String desc; //描述信息
}
```



### Step 4. 定义实现sdk包中的TaskAtom接口的插件任务类

- 插件任务类必须实现sdk包中的TaskAtom接口
- 插件任务类必须加上“@AtomService(paramClass = AtomParam.class)”注解才能被sdk识别和执行（paramClass对应的值为定义的参数类文件名）

```
@AtomService(paramClass = AtomParam.class)
public class DemoAtom implements TaskAtom<AtomParam> {

    private final static Logger logger = LoggerFactory.getLogger(DemoAtom.class);

    /**
     * 执行主入口
     * @param atomContext 插件上下文
     */
    @Override
    public void execute(AtomContext<AtomParam> atomContext) {
        // 1.1 拿到请求参数
        AtomParam param = atomContext.getParam();
        logger.info("the param is :{}", JsonUtil.toJson(param));
        // 1.2 拿到初始化好的返回结果对象
        AtomResult result = atomContext.getResult();
        // 2. 校验参数失败直接返回
        checkParam(param, result);
        if (result.getStatus() != Status.success) {
            return;
        }
        // 3. 模拟处理插件业务逻辑
        logger.info("the desc is :{}", param.getDesc()); //打印描述信息
        // 4. 输出参数，如果有的话
        // 输出参数是一个Map,Key是参数名， value是值对象
        Map<String, DataField> data = result.getData();
        // 假设这个是输出参数的内容
        StringData testResult = new StringData("hello");
        // 设置一个名称为testResult的出参
        data.put("testResult", testResult);
        logger.info("the testResult is :{}", JsonUtil.toJson(testResult));
        // 结束。
    }

    /**
     * 检查参数
     * @param param  请求参数
     * @param result 结果
     */
    private void checkParam(AtomParam param, AtomResult result) {
        // 参数检查
        if (StringUtils.isBlank(param.getDesc())) {
            result.setStatus(Status.failure);// 状态设置为失败
            result.setMessage("描述不能为空!"); // 失败信息回传给插件执行框架会打印出结果
        }

        /*
         其他比如判空等要自己业务检测处理，否则后面执行可能会抛出异常，状态将会是 Status.error
         这种属于插件处理不到位，算是bug行为，需要插件的开发去定位
          */
    }

}
```



### Step 5. 配置TaskAtom接口的spi实现类

1. 在 src/main/resources/ 下建立 /META-INF/services 目录， 新增一个以接口命名的文件 com.tencent.bk.devops.atom.spi.TaskAtom
2. 文件里面的内容是定义的实现spi接口的插件任务类，如：com.tencent.bk.devops.atom.task.DemoAtom

