​                                                

#                                                                                            开源版研发商店开发指引

## 1、研发商店简介

​       开源版的bk-ci研发商店目前支持了流水线插件市场、流水线模板市场和容器镜像市场，同时也支持用户基于bkci的开源版代码开发自已的研发商店组件市场：

- **流水线插件**：流水线执行的基本单位TASK，支持第三方用户按照协议自助开发，并分享到商店给其他用户使用。
- **流水线模板**：流水线的最佳实践和批量实例管理工具。可将项目下的最佳实践沉淀为模版，分享给其他团队。

- **容器镜像**：用来执行流水线Job（编排流水线时，针对每个Job，需要指定在哪个容器镜像下执行），可将打的镜像分享到商店给其他用户使用。



## 2、研发商店代码结构

​        研发商店的代码都放在bk-ci/src/backend/ci/core/store这个目录下面，下面展示的是研发商店的代码总体结构图、api-store和biz-store二个基础模块的代码结构图（其它模块代码结构类似）：

- **研发商店代码总体结构图**：

```
|- bk-ci/src
  |- backend/ci/core
    |- store                    # 研发商店微服务总目录
      |- api-store              # 公共基础api定义模块(包含插件和模板的业务)
      |- biz-store              # 公共基础api和业务服务实现模块(包含插件和模板的业务)
      |- boot-store             # 构建springboot微服务包，设置依赖构建并输出到release目录
      |- model-store            # 使用JOOQ从db中动态生成的PO，表结构有变更需要clean后重新build
```

- **api-store模块代码结构图**

```
|- api-store/src/main
  |- kotlin/com/tencent/devops/store
     |- api              # api接口定义目录
       |- atom           # 插件api接口定义目录
       |- common         # 研发商店公共api接口定义目录
       |- container      # 流水线构建容器api接口定义目录
       |- template       # 模板api接口定义目录
     |- constant         # 常量类目录
     |- pojo             # 实体类目录
       |- app            # 流水线job相关实体类目录
       |- atom           # 插件相关实体类目录
       |- common         # 研发商店公共实体类目录
       |- container      # 流水线构建容器相关实体类目录
       |- template       # 模板相关实体类目录
```

- **biz-store模块代码结构图**

```
|- biz-store/src/main
  |- kotlin/com/tencent/devops/store
     |- configuration    # 配置定义类目录
     |- dao              # 数据库DAO类目录
       |- atom           # 插件DAO类目录
       |- common         # 研发商店公共DAO类目录
       |- container      # 流水线构建容器DAO类目录
       |- template       # 模板DAO类目录
     |- listener         # 监听器类目录
     |- resources        # 实体类目录
       |- atom           # 插件api接口实现目录
       |- common         # 研发商店公共api接口实现目录
       |- container      # 流水线构建容器api接口实现目录
       |- template       # 模板api接口实现目录
     |- service          # 业务逻辑类目录
       |- atom           # 插件业务逻辑类目录
       |- common         # 研发商店公共业务逻辑类目录
       |- container      # 流水线构建容器业务逻辑类目录
       |- template       # 模板业务逻辑类目录   
```



## 3、如何协同开发研发商店

​      我们将协同开发研发商店的模式主要分为不需要改动研发商店代码结构和需要改动研发商店代码结构这二种，具体选择哪种模式需要结合自已遇到的业务场景来定。如何协同开发研发商店详见：[bk-ci协同开发指引](https://github.com/Tencent/bk-ci/blob/master/docs/wiki/collaborative_development_guidelines.md)

