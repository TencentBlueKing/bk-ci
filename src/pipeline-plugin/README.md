# 插件开发框架说明：

**一、工程的整体结构如下：**

```
|- pipeline-plugin
    |- bksdk   插件sdk
    |- demo    插件样例，你可以把工程项目名和内部逻辑修改成你自定义的
```


**二、如何开发插件：**

​     详见bksdk和demo目录下的文档。



**三、如何打成插件市场要求的zip包：**

 1、进入pipeline-plugin目录下执行"mvn clean package"命令进行打包。

 2、进入demo（插件逻辑所在的工程目录）目录下的target文件夹下就可以获取我们需要的zip包。

