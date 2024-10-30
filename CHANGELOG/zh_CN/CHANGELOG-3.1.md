<!-- BEGIN MUNGE: GENERATED_TOC -->
- [v3.1.0-rc.2](#v310-rc22024-10-26)
   - [Changelog since v3.1.0-rc.1](#changelog-since-v310-rc1)

- [v3.1.0-rc.1](#v310-rc12024-10-15)
   - [Changelog since v3.0.0](#changelog-since-v300)

<!-- END MUNGE: GENERATED_TOC -->



<!-- NEW RELEASE NOTES ENTRY -->
# v3.1.0-rc.2(2024-10-26)
## Changelog since v3.1.0-rc.1
#### 新增
##### 流水线
- [新增] 推荐版本号优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10958)
- [新增] 支持流水线指标监控 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9860)
- [新增] 流水线列表，增加标签展示 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11054)
- [新增] 模版管理-列表支持展示字段和排序优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11056)
- [新增] 源材料展示优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10733)
- [新增] Post action 中支持获取父任务ID [链接](http://github.com/TencentBlueKing/bk-ci/issues/10968)
- [新增] stage 审核支持 checklist 确认场景 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10920)
- [新增] AI大模型融入 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10825)
- [新增] 丰富流水线-stage准入的审核功能，支持配置角色或用户组作为审核人 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10689)
- [新增] 流水线日志支持AI修复 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10913)
- [新增] 流水线并发运行时，支持限制并发个数和排队 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10718)
- [新增] 插件管理菜单对应插件列表增加默认公共插件显示 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10472)
- [新增] 当策略为「锁定构建号」时，执行界面可以修改当前值 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11089)
- [新增] 社区版流水线完成通知，支持通知组 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10976)
- [新增] Job 互斥组排队时，队列长度支持最长到 50 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10975)
- [新增] 触发事件重放操作权限控制 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11052)
- [新增] 通过子流水线调用触发的执行，支持重试 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11015)
- [新增] UI方式下的「所有参数满足条件时执行」和「所有参数满足条件时不执行」转为Code 优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10930)
- [新增] MR 事件触发器支持 WIP [链接](http://github.com/TencentBlueKing/bk-ci/issues/10683)
- [新增] 工蜂 MR 触发增加 action=edit [链接](http://github.com/TencentBlueKing/bk-ci/issues/11024)
##### 代码库
- [新增] 增加获取工蜂和github oauth url的build接口 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10826)
##### 权限中心
- [新增] 同步并分表存储资源组权限数据 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10964)
- [新增] 创建自定义组并赋予组组权限 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11026)
##### 环境管理
- [新增] 第三方构建机支持使用 dcoker 运行构建任务 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9820)
- [新增] 第三方构建机DockerUi界面支持 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10962)
##### Openapi
- [新增] OpenApi提供转发Turbo编译加速上报资源统计数据接口 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10508)
##### 其他
- [新增] 支持查看版本日志 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10938)
- [新增] 优化AESUtil [链接](http://github.com/TencentBlueKing/bk-ci/issues/11084)
- [新增] sql doc 文档更新 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9974)
- [新增] 引擎等MQ场景接入SCS框架 [链接](http://github.com/TencentBlueKing/bk-ci/issues/7443)

#### 优化
##### 研发商店
- [优化] 研发商店组件指标数据字段优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10219)
##### Stream
- [优化] [stream] 存留问题优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11045)

#### 修复
##### 流水线
- [修复] 某些构建场景下插入T_PIPELINE_BUILD_RECORD_TASK表的CONTAINER_ID字段值错误 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11029)
- [修复] 触发器条件引入${{variables.xxx}}变量触发不了流水线 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10987)
- [修复] 触发器变量补充 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11002)
##### 研发商店
- [修复] 研发商店组件包文件上传下载优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11115)
- [修复] 修复更新组件关联初始化项目信息时，新增调试项目记录时未成功把旧的调试项目记录清理 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11011)
##### 制品库
- [修复] 归档报告插件创建token没有实现 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10693)


# v3.1.0-rc.1(2024-10-15)
## Changelog since v3.0.0
#### 新增
##### 流水线
- [新增] 流水线查看页面/编辑页面/构建详情界面 面包屑中的名字展示优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10800)
- [新增] 流水线版本管理机制 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8161)
- [新增] Job 并发支持 Code 配置 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10860)
- [新增] 草稿版本UI展示 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9861)
##### 研发商店
- [新增] sdk相关的api是否显示在申请列表中支持可配置化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10840)
##### 权限中心
- [新增] 项目成员管理优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10927)
- [新增] 活跃用户记录操作和次数 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10891)
- [新增] 项目成员支持按照过期时间/用户组名称搜索 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10892)
- [新增] issue: 修复sample鉴权下查project全表的问题 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10941)
- [新增] oauth2 增加密码模式 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10663)
##### Stream
- [新增] [stream] 优化大仓触发耗时 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10861)
- [新增] stream stage 审核的通知方式支持企业微信群 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10796)
##### 调度
- [新增] 第三方构建机 Job 间复用构建环境支持 Code 配置 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10254)
- [新增] 同一流水线多次构建时资源调度优先级优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9897)
- [新增] 把docker build插件的config ns配置给去掉 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10926)
- [新增] AgentId复用类型转换问题 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10915)
##### Worker
- [新增] 让worker支持在JDK17中运行 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10412)
##### 其他
- [新增] 新启动的POD需要热身 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10887)
- [新增] openapi filter 优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10679)
- [新增] update lerna +yarn workspace to pnpm [链接](http://github.com/TencentBlueKing/bk-ci/issues/8125)

#### 优化
##### 流水线
- [优化] 带矩阵的流水线运行矩阵分裂前的task任务无需写入记录表 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10873)
##### 研发商店
- [优化] 拉取插件task.json文件内容报错提示优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10446)
- [优化] 研发商店敏感接口签名校验优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10759)
- [优化] 应用Schema改为每个版本都可以设置不同的配置 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10929)
- [优化] 公共构建机插件缓存区路径和变量调整优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10844)

#### 修复
##### 流水线
- [修复] 非编辑页面切换Code方式时提示保存 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10933)
- [修复] 新构建详情页的失败重试展示问题 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10735)
##### 研发商店
- [修复] 上架流水线模板到研发商店，但是新建流水线的时候在“研发商店”Tab搜不出来 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10865)
- [修复] 查插件环境信息接口未正确处理插件测试分支版本号情况 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10924)
##### 权限中心
- [修复] bk-permission 项目成员管理 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9620)
##### Stream
- [修复] stream新建环境名称正则错误修复 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10939)
