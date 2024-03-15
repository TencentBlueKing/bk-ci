​                                                

# bk-ci协同开发指引

### 1、 开发环境搭建

#### 1.1、安装JDK

​     jdk的版本建议使用jdk8，jdk可以直接从oracle官网下载下来安装即可（jdk下载地址：[oracle官网jdk下载地址](https://www.oracle.com/java/technologies/javase-downloads.html)）。

#### 1.2、安装gradle

​     bk-ci使用gradle作为构建工具，gradle的版本建议使用6.7，gradle可以直接从gradle官网下载下来安装即可（gradle官网地址详见：[gradle官网](https://gradle.org/)）。

#### 1.3、安装IDE开发工具

​       bk-ci研发商店是用kotlin语言开发的，IDE开发工具建议使用***IntelliJ IDEA***（建议使用能适配前面gradle版本的***IntelliJ IDEA***版本，比如**IntelliJ IDEA 2019.3**），***IntelliJ IDEA***可以直接从其官网下载下来安装即可（***IntelliJ IDEA***官网地址详见：[***IntelliJ IDEA***官网](https://www.jetbrains.com/idea/)），bk-ci开源版有自已的代码规范，协同开发前需把自已的IDE配置下以保证IDE格式化的代码是符合bk-ci代码规范的，以下是具体的配置流程：

- 配置本地IDE

  ```
  cd ci # 进入到bk-ci根目录
  java -jar ktlint --apply-to-idea-project
  ```

- 验证kotlin代码是不不是符合规范

  ```
  gradle ktlint
  ```


#### 1.4、安装MySQL

​     bk-ci使用MySQL作为数据库，MySQL的版本建议使用MySQL 5.7，MySQL可以直接从MySQL官网下载下来安装即可（MySQL官网地址详见：[MySQL官网](https://www.mysql.com/)）。



### 2、 判断是否需要变更bk-ci代码结构

​      如果只是基于开源版现有的功能做个小改动那么选择不需要改动bk-ci代码结构的模式，如果说新增一个研发商店组件市场那么就需要采用增加相关模块这种改动bk-cistore微服务代码结构的方式（新增的研发商店组件市场的代码结构可以参考容器镜像市场的代码结构）。下面就展示需要新开发一个xxx市场的代码结构图：

```
|- bk-ci/src
  |- backend/ci
    |- store                    # 研发商店微服务总目录
      |- api-store              # 公共基础api定义模块
      |- biz-store              # 公共基础api和业务服务实现模块
      |- boot-store             # 构建springboot微服务包，设置依赖构建并输出到release目录
      |- model-store            # 使用JOOQ从db中动态生成的PO，表结构有变更需要clean后重新build
```

开发xxx市场需要增加api-store-xxx、biz-store-xxx和biz-store-xxx-sample模块（需要在工程根目录下的settings.gradle配置文件导入新增的模块），这样便于基于gradle动态组装各模块部署。

### 3、 判断是否需要变更bk-ci数据库

​        如果需求涉及数据库变更，需要在**bk-ci/support-files/sql/**目录下提交数据库变更脚本文件（数据库脚本文件定义规则详见：[数据库脚本说明](https://github.com/Tencent/bk-ci/tree/master/support-files/sql)），数据库脚本执行后需要对bkci微服务的model模块clean后重新build。

### 4、 贡献文档或代码

​         贡献文档或代码到bk-ci需要符合bk-ci团队制定的规范，以下是相关规范的详细介绍：

- #### 开发编码规范

  - RESET接口规范详见：[RESET接口规范](https://github.com/Tencent/bk-ci/blob/master/docs/specification/REST_interface_specification.md)
  - 字段命名规范详见：[字段命名规范](https://github.com/Tencent/bk-ci/blob/master/docs/specification/field_naming_specification.md)
  - 日志规范详见：[日志规范](https://github.com/Tencent/bk-ci/blob/master/docs/specification/log_specification.md)



- #### 贡献文档或代码到bk-ci GitHub代码库

  - 贡献规范详见：[Contributing](https://github.com/Tencent/bk-ci/blob/master/CONTRIBUTING.md)

