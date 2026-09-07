<!-- BEGIN MUNGE: GENERATED_TOC -->
- [v4.2.0-rc.6](#v420-rc6)
   - [Changelog since v4.2.0-rc.5](#changelog-since-v420-rc5)

- [v4.2.0-rc.5](#v420-rc5)
   - [Changelog since v4.2.0-rc.4](#changelog-since-v420-rc4)

- [v4.2.0-rc.4](#v420-rc4)
   - [Changelog since v4.2.0-rc.3](#changelog-since-v420-rc3)

- [v4.2.0-rc.3](#v420-rc3)
   - [Changelog since v4.2.0-rc.2](#changelog-since-v420-rc2)

- [v4.2.0-rc.2](#v420-rc2)
   - [Changelog since v4.2.0-rc.1](#changelog-since-v420-rc1)

- [v4.2.0-rc.1](#v420-rc1)
   - [Changelog since v4.1.0](#changelog-since-v410)

<!-- END MUNGE: GENERATED_TOC -->



<!-- NEW RELEASE NOTES ENTRY -->
# v4.2.0-rc.6
## 2026-08-18
### Changelog since v4.2.0-rc.5
### 变更概述
当前版本主要变更特性如下:

**特性**
- 创作流：支持跟随 manifest 复制到个人项目，定时触发 YAML 增加启动节点关键字，并优化 OpenAPI 接口路由不再按项目区分
- matrix job 支持失败步骤手动重试，且重试失败时仅重跑失败的 job 而非整个矩阵
- 日志服务增加多种查询接口
- 代码库支持以用户态接口获取分支列表与 Tag 列表
- 研发商店插件外部任务链接支持 SDK 上报与画布跳转
- 支持 GitHub issue 的 assignee 与 label 事件
- 版本日志支持与前端镜像分离、多集群独立挂载
- 构建历史支持隐藏操作列

**Bug 修复**
- 修复重试构建时被取消的 Job 残留 CANCELED 状态，导致最终构建状态错误标记为取消
- 修复取消并发起新构建重放已重试构建时报「流水线构建不存在」
- 修复重试时会误运行前序 matrix job
- 修复流水线级回调在 events 清空后不会清理

#### 新增

##### 流水线
- [新增] feat：matrix job 下的步骤，如果设置了失败时可手动重试，支持可以重试 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10780)

##### 代码库
- [新增] feat：支持用户态接口获取代码库分支列表/Tag 列表 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13013)

##### 研发商店
- [新增] feat：插件外部任务链接 SDK 上报与画布跳转 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13312)

##### 日志服务
- [新增] feat: 日志服务增加多种查询接口 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13230)

##### 其他
- [新增] bug: support GitHub issue assignee and label events [链接](http://github.com/TencentBlueKing/bk-ci/issues/13404)
- [新增] feat: 创作流支持跟随manifest复制到个人项目 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13432)
- [新增] feat: 增加op接口修改项目下流水线最大条数 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13423)
- [新增] feat：重试失败的 job 时，如果是 matrix job，支持仅重新运行失败的job，而不是整个矩阵全量运行 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10799)
- [新增] feat: 创作流定时触发yaml增加启动节点关键字 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13309)
- [新增] Support hiding the actions column in build history table settings [链接](http://github.com/TencentBlueKing/bk-ci/issues/13376)
- [新增] feat: 版本日志支持与前端镜像分离，支持多集群独立挂载 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13363)

#### 优化

##### 流水线
- [优化] perf: 【PAC】流水线/模版发布时的MR标题使用用户输入的描述 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13444)

##### 其他
- [优化] pref:调用创作流openapi接口请求不再通过项目区分路由到不同的集群 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13410)
- [优化] chore: 升级devopsScm到1.1.10 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13373)

#### 修复

##### 流水线
- [修复] bug: 保存草稿并发/同秒场景下 draftVersion 重复导致 T_PIPELINE_RESOURCE_DRAFT_VERSION 主键冲突 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13459)
- [修复] bug: tapd api创建的需求/bug没有ci.event_url变量 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13405)
- [修复] bug: PAC 模板实例化流水线从代码库触发时，checkbox/多选下拉框默认值被破坏为 [] [链接](http://github.com/TencentBlueKing/bk-ci/issues/13447)
- [修复] bug: 流水线/模版保存时取消子流水线分支版本校验 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13412)
- [修复] bug: 【PAC】分支版本执行时,应该使用的是分支版本设置 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12697)
- [修复] bug: 优化触发事件没权限时文案 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13103)
- [修复] bug: 流水线级回调，当开始events有值，然后改成没有值后，回调不会清理 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13313)

##### 其他
- [修复] bug: 修复保存草稿校验错误提示显示函数源码 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13454)
- [修复] Fix real-time updates in the Creative Stream embedded view [链接](http://github.com/TencentBlueKing/bk-ci/issues/13426)
- [修复] bug: 取消api调用流水线非正式版本的拦截 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13402)
- [修复] bug: 资源锁定当使用变量时,yaml转ui时会报错 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13421)
- [修复] fix: 取消并发起新构建(buildRestart)重放已重试过的构建时报"流水线构建[xxx]不存在" [链接](http://github.com/TencentBlueKing/bk-ci/issues/13430)
- [修复] bug:分支版本/版本落后时发布同名模板不成功 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13427)
- [修复] bug: 重试时会误运行前序matrix job [链接](http://github.com/TencentBlueKing/bk-ci/issues/13084)
- [修复] fix: refresh timer trigger start-parameter options after flow variable changes [链接](http://github.com/TencentBlueKing/bk-ci/issues/13413)
- [修复] bugfix: 环境管理启停功能被覆盖，动态环境没启停 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13398)
- [修复] bug：重试构建时被取消的Job 残留 CANCELED 状态，导致最终构建状态错误标记为取消 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13407)
- [修复] Fix boolean pipeline variable default value handling [链接](http://github.com/TencentBlueKing/bk-ci/issues/13329)
- [修复] bug:创作流插件按指定操作系统查询优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13396)
- [修复] bug:草稿即将覆盖警告弹窗获取的未发布草稿版本号有问题 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13387)
- [修复] bug: yaml schema $.concurrency.queue-length默认长度调整到200 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13292)
- [修复] 修改模板--新增变量（设置了运行时只读）后，实例更新时无法修改变量值 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13369)
- [修复] Optimize artifact download preparation text spacing [链接](http://github.com/TencentBlueKing/bk-ci/issues/13328)

# v4.2.0-rc.5
## 2026-08-04
### Changelog since v4.2.0-rc.4
### 变更概述
当前版本主要变更特性如下:

**特性**
- 创作流：支持创作流管理，创作流环境与创作环境支持系统属性及指定 OS
- 支持镜像制品的流水线元数据入库与查询
- 流水线组管理：提供流水线组管理相关 Apigw 接口，并优化流水线组展示
- 代码库触发流程由串行改为并行执行流水线
- 审核插件支持发送特定用户提醒
- 草稿并发修改交互优化
- 提供部分云研发 OpenAPI 接口
- 工蜂 Commit Check（GONGFENGSCAN）使用 CodeCC 结果 URL

**Bug 修复**
- 修复定时任务删除失效导致 Quartz RAMJobStore 内存泄漏
- 修复模板实例升级参数类型变更触发无限更新并导致页面崩溃
- 修复 webhook 构建参数溢出时丢弃整个载荷的问题（改为仅丢弃大型环境变量）
- 修复 PAC 实例化的 YAML 流水线在删除或格式错误时状态异常的问题

#### 新增

##### 流水线
- [新增] feat：草稿并发修改交互优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10553)
- [新增] feat：支持镜像制品的流水线元数据入库和查询 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13160)
- [新增] feat: 工蜂 Commit Check（GONGFENGSCAN）使用 CodeCC 结果 URL [链接](http://github.com/TencentBlueKing/bk-ci/issues/13253)
- [新增] feat: 优化代码库触发流程：串行改并行执行流水线 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13186)

##### 环境管理
- [新增] feat: 创作流环境支持系统属性 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13282)
- [新增] feat：环境管理下环境/节点的任务列表优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13269)
- [新增] feat: 第三方构建机Docker支持不挂载的关键字 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13251)

##### 其他
- [新增] feat: 审核插件支持发送特定用户提醒 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13382)
- [新增] feat：创作环境支持指定 OS [链接](http://github.com/TencentBlueKing/bk-ci/issues/13324)
- [新增] feat：创作流管理 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12414)
- [新增] feat：提供流水线组管理相关Apigw接口 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13289)
- [新增] feat: 提供部分云研发的openapi接口 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13279)

#### 优化
- [优化] pref: 完善商店上传文件名称校验 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13027)
- [优化] pref：codecc规则集id转换 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13354)
- [优化] pref: AI 模型 failover 增加单候选总执行时限 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13255)
- [优化] pref：强化ai定位子流水线构建能力 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13262)
- [优化] perf：流水线组展示优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12943)
- [优化] perf：UI 方式设置 matrix job 时必填检查策略优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12957)

#### 修复

##### 流水线
- [修复] bug: 修复webhook构建参数溢出处理 - 应丢弃大型环境变量而非整个载荷 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13356)
- [修复] bug：定时任务删除失效导致 Quartz RAMJobStore 内存泄漏 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13352)
- [修复] bug: 优化模版实例化时,定时触发代码库不存在的提示信息 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13346)
- [修复] bug: 代码库分支参数类型使用上一次构建参数时没有回填 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13336)
- [修复] bug: 【PAC】删除的yaml流水线应该从yaml流水线组删除 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12870)
- [修复] bug: PAC实例化流水线，如果yaml格式错误，会导致实例化状态一直是升级中 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13261)
- [修复] bug: 模板实例升级参数类型变更触发无限更新并导致页面崩溃 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13294)
- [修复] bug: 【PAC】查找默认分支yaml对应的流水线版本,不需要判断版本是否激活 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13295)
- [修复] 蓝盾构建制品页面，点击下载名字包含'#'的产物，点击无效 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13270)

##### 代码库
- [修复] bug: 平台管理 github 配置不生效 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13231)

##### 环境管理
- [修复] bugfix: 添加节点兼容性逻辑修复 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13355)
- [修复] bug: 作业执行页面滚动条消失 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13254)

##### 日志服务
- [修复] bug: 日志模块突发流量的异常处理 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13327)

##### 项目管理
- [修复] bug：蓝盾首页菜单显示的服务总数和实际展示的服务数量对不上 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13322)
- [修复] bug: 创建项目路由发布批次接口支持传入项目黑名单和项目是否开启参数 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13319)

##### 调度
- [修复] bugfix: 第三方构建跳转链接问题 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13301)

##### 其他
- [修复] bug: GitApi触发判断优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13392)
- [修复] bug: 创作流保存草稿偶发回退旧编排，版本删除确认弹窗被侧边栏遮挡 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13386)
- [修复] bug: 修复v4_user_repository_get和v4_app_repository_get api接口不能查询代码库别名 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13308)
- [修复] bugfix: 启停节点的openapi修复 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13287)
- [修复] 创作流「添加变量」面板字段应命名为「变量名」，而非「变量ID」 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13246)

# v4.2.0-rc.4
## 2026-07-16
### Changelog since v4.2.0-rc.3
### 变更概述
当前版本主要变更特性如下:

**特性**
- 创作流一期上线：支持管理、可见范围、工作空间、YAML 互转、代码库事件触发及第三方构建机
- 流水线支持 TAPD 事件触发（一期）
- 支持跨项目复制流水线
- 支持流水线公共变量管理
- 支持自定义参数类型及列表参数，便于填写复杂参数
- 研发商店支持按项目设置可见范围，并支持展示版本日志
- 环境管理重构，节点停用/启用可填写原因并记录操作日志

**Bug 修复**
- 修复删除早期流水线版本时，可能误删构建中正在使用版本的问题
- 修复构建环境自动重试时未释放复用锁的问题
- 修复「仅当前面有插件失败时才运行」条件下，失败插件无法重试或跳过的问题

#### 新增

##### 流水线
- [新增] feat：支持 TAPD 事件触发一期 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12959)
- [新增] feat: 普通流水线发布时生成AI摘要 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13100)
- [新增] feat：支持跨项目复制流水线 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12918)
- [新增] feat：变量条件展示支持更多运算符 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12335)
- [新增] feat:python插件支持编译运行的时使用独立虚拟环境执行 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12656)
- [新增] feat:支持按渠道获取子流水线启动参数 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13155)
- [新增] feat: 构建环境重构 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13050)
- [新增] feat: 创作流支持代码库事件触发 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12842)
- [新增] feat: 建议提供自定义参数类型及列表参数类型 ，以便于能填写复杂参数 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12689)
- [新增] feat:支持按渠道获取用户有权限流水线列表 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13115)
- [新增] feat：保存流水线时，校验是否有子流水线循环依赖 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10479)
- [新增] feat:获取构建历史接口增加节点IP返回字段 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13009)
- [新增] feat: 支持通过api接口指定触发器插件启动 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12828)
- [新增] feat：平台管理-注册事件 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12379)
- [新增] feat:流水线公共变量管理 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12010)

##### 代码库
- [新增] feat: 已关联的代码库能在原有配置中调整代码库地址 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13130)

##### 研发商店
- [新增] feta:研发商店评论相关接口统一切换为公共接口 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13242)
- [新增] feat: 研发商店-需要支持按蓝盾项目设置可见范围 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13080)
- [新增] feat:研发商店-支持应用安装路径和安装方式 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13105)
- [新增] 【研发商店】支持展示版本日志 [链接](http://github.com/TencentBlueKing/bk-ci/issues/1761)
- [新增] feat：商店插件首页接口支持按服务范围过滤数据 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12785)
- [新增] feat：OP 支持修改插件的「适用范畴」 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12803)

##### 环境管理
- [新增] feat：构建环境中停用/启用节点时，增加原因描述，并提供操作日志 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12764)
- [新增] 构建节点和部署节点添加跳过策略 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13145)
- [新增] feat: 环境管理节点筛选支持按节点状态筛选 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12851)
- [新增] feat: 增加创作流触发环境返回节点列表 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13064)
- [新增] feat：创作环境/节点管理 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12389)
- [新增] feat: 团队创作流第三方机相关 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13004)
- [新增] feat: 环境管理重构 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12416)

##### 日志服务
- [新增] feat: 日志服务增加多种查询接口 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13230)

##### 质量红线
- [新增] feat: 工蜂MR评论的CodeCC指标跳转链接优先使用配置的logPrompt [链接](http://github.com/TencentBlueKing/bk-ci/issues/13137)

##### 权限中心
- [新增] feat: 支持将项目级用户组及成员迁移到其他项目 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13109)

##### 其他
- [新增] feat：环境管理下环境/节点的任务列表优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13269)
- [新增] feat：创作流管理 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12414)
- [新增] feat: 创作流支持工作空间 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13208)
- [新增] feat: 创作流支持yaml互转 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12862)
- [新增] feat:团队创作流启动时增加触发人与机器的权限校验 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13139)
- [新增] 报告url的host根据请求来源返回对应host [链接](http://github.com/TencentBlueKing/bk-ci/issues/13135)
- [新增] feat：新增根据条件查询项目成员apigw接口 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13094)
- [新增] feat:创作流启动的时候增加会话ID系统变量 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13014)
- [新增] feat:提供获取插件yaml文件的openapi接口 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12977)
- [新增] feat: 创作流增加可见范围 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12823)
- [新增] feat: 创作流一期 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12400)
- [新增] feat: 创作流发布自动增加 AI 描述 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12825)
- [新增] feat: 创作流-第三方机 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12354)

#### 优化

##### 流水线
- [优化] perf：回收站补齐清理提醒 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13171)
- [优化] pref: 减少redis热key的访问频率 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12488)

##### 研发商店
- [优化] perf:优化插件功能白名单缓存机制 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13244)
- [优化] pref: 自动更新修复老的插件运行信息缓存 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13213)
- [优化] pref: 调整创作流插件分类信息 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12819)

##### 权限中心
- [优化] pref：项目下移出用户后部门校验优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13164)

##### 其他
- [优化] perf:点击批量展示批量任务历史 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13267)
- [优化] pref：优化智能助手分析流水线报错场景 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13219)
- [优化] pref: 创作流下插件权限校验优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13190)
- [优化] pref：增强蓝盾智能助手分析用户权限能力 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13117)
- [优化] pref: 部分codecc接口支持按指定tag路由至对应集群 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13129)
- [优化] pref: 支持开关控制是否把监控数据写入Influxdb [链接](http://github.com/TencentBlueKing/bk-ci/issues/13077)

#### 修复

##### 流水线
- [修复] bug: 定时触发 YAML 配置中 trigger-conf.variables 参数格式校验错误 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13192)
- [修复] [Bug] 流水线 Code(YAML) 编辑模式：代码行较长时底部空面板遮挡水平滚动条，无法横向滚动/框选 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13232)
- [修复] bug：删除早期的流水线版本记录时可能删掉构建正卡住的版本 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13218)
- [修复] bugfix: 构建环境自动重试导致没有释放复用锁 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13214)
- [修复] feat：子流水线访问优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13003)
- [修复] Fix missing add trigger action for imported pipeline drafts [链接](http://github.com/TencentBlueKing/bk-ci/issues/13202)
- [修复] bug:运行条件为【只有前面有插件运行失败时才运行】的插件会导致前面失败的插件无法重试或者跳过 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13167)
- [修复] bug: 研发商店模版,实例化时需再校验插件可见性 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13170)
- [修复] bug: 模版草稿版本的基准版本可能不是最新的正式版本 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13125)
- [修复] bug: 模版code方式编辑可能导致其他变量参数变成入参 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13126)
- [修复] bug: 模版code方式修改as-instance-input没有生效 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13113)
- [修复] fix：构建详情页面，正在运行的步骤耗时展示问题 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9705)

##### 研发商店
- [修复] bug: 在流水线选插件页面使用关键字搜索时，若结果集较大，可能会导致部分插件无法正常展示。 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12808)
- [修复] bug：当修改插件基本信息时减少服务范围时没有清理干净老配置 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13182)

##### 环境管理
- [修复] bugfix: 创作流环境相关bug修复 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13225)
- [修复] bugfix: 全局agent获取失败导致无法取消任务 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13193)
- [修复] fix: 【环境管理】节点列表数量不对 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13151)

##### 其他
- [修复] bug: 作业执行页面滚动条消失 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13254)
- [修复] bug: 修复创作流触发器bug [链接](http://github.com/TencentBlueKing/bk-ci/issues/13226)
- [修复] bug:插件查看详情，如果在创作流服务下，跳到研发商店应该是创作流插件 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13228)
- [修复] bug:处理创作流遗漏的按渠道校验的权限逻辑 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13239)
- [修复] feat: support specifying a custom workspace for creation-flow Job [链接](http://github.com/TencentBlueKing/bk-ci/issues/13209)
- [修复] bug:WEB_HOOK类型触发的创作流的节点信息没有落地 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13187)

# v4.2.0-rc.3
## 2026-06-08
### Changelog since v4.2.0-rc.2
#### 新增

##### 流水线
- [新增] feat：PAC流水线支持指定分支 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12635)

##### 环境管理
- [新增] feat: 环境管理重构 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12416)
- [新增] feat：创作环境/节点管理 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12389)

##### 权限中心
- [新增] feat：新增权限成员治理ai配套接口 #13019 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13019)

##### 项目管理
- [新增] feat：支持个人项目 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12852)

##### 其他
- [新增] feat：提供 build 接口支持插件获取当前步骤的原始配置 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12953)
- [新增] feat: 蓝盾智能助手支持多渠道大模型配置、用户模型配置与故障切换 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12958)
- [新增] feat：codecc规则集接入权限中心 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12981)
- [新增] feat: 自定义第三方构建机集群调度优先级 [链接](http://github.com/TencentBlueKing/bk-ci/issues/2680)
- [新增] feat: 依赖包版本升级优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12951)
- [新增] project模块缺少接口实现类造成启动失败修复 [链接](http://github.com/TencentBlueKing/bk-ci/issues/1285)

#### 优化

##### 权限中心
- [优化] pref：权限申请权限/权限交接场景优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13026)

##### 其他
- [优化] docs: 关联CodeCC代码库 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13075)
- [优化] pref: T_PIPELINE_WEBHOOK_QUEUE表间隙锁处理优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13052)
- [优化] pref:metrics数据上报优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13000)
- [优化] pref：优化版权声明 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12988)
- [优化] pref：优化 Skill 文档，采用渐进式披露，降低上下文占用并统一结构 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12948)
- [优化] pref:zip解压工具优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12960)

#### 修复

##### 流水线
- [修复] bug: 模版导出时,获取最新模版版本错误 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13049)
- [修复] bug: 模版实例化校验required合法性去掉常量参数 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13039)
- [修复] bug: 触发事件描述由前端组装 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12969)

##### 凭证管理
- [修复] bug: 密钥使用占位符配置 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12971)

##### 其他
- [修复] bug: 修复pac发布没有权限时报401错误 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13020)
- [修复] bug: 解决agent日志单例输出任务ID错乱和前端after接口问题 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12970)
- [修复] bug：AI 对话 AG-UI 流偶发缺失 RUN_FINISHED，导致前端会话长期停留运行中 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12994)
- [修复] bug：流水线构建信息通知勾选群消息转为Markdown格式时url链接未转换超链接 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13044)
- [修复] bugfix: 第三方机新模式部分问题修复 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12945)
- [修复] fix: 升级前端依赖版本 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12965)
- [修复] bug: 优化callback调用http工具 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12961)

# v4.2.0-rc.2
## 2026-05-15
### Changelog since v4.2.0-rc.1
#### 新增

##### 流水线
- [新增] feat：支持获取流水线失败详情 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12873)
- [新增] feat：敏感字段未设置值错误加密问题 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12871)
- [新增] feat：源模版升级 触发约束模式模版自动安装插件 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12896)
- [新增] feature: 流水线job添加默认镜像 issue #1108 [链接](http://github.com/TencentBlueKing/bk-ci/issues/1265)

##### 研发商店
- [新增] feat：研发商店插件评论通知和审核通知支持发到群组里 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12655)
- [新增] 【研发商店】支持展示版本日志 [链接](http://github.com/TencentBlueKing/bk-ci/issues/1761)

##### 权限中心
- [新增] feat：申请权限时增加对不可主动申请加入的组的标识和提醒 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12436)

##### 项目管理
- [新增] feat: 增加设置系统默认集群的op接口 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12804)
- [新增] feat：项目归属信息填写优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12527)

##### Agent
- [新增] feat: agent上报并发指标数据 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12526)
- [新增] feat：第三方构建机使用 docker 运行时支持 --network 和 --user [链接](http://github.com/TencentBlueKing/bk-ci/issues/12832)
- [新增] feat: macos agent 支持 nohead [链接](http://github.com/TencentBlueKing/bk-ci/issues/12809)
- [新增] feat: agent支持mcp [链接](http://github.com/TencentBlueKing/bk-ci/issues/12653)

##### 其他
- [新增] feat: 第三方构建机移除telegraf依赖 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12895)
- [新增] feat: 加固登录调试鉴权 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12924)
- [新增] feat: windows支持服务下使用用户session [链接](http://github.com/TencentBlueKing/bk-ci/issues/12765)
- [新增] feat：支持蓝盾智能助手 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12737)
- [新增] 蓝鲸安全治理-SAST扫描修复 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12884)
- [新增] feat: 流水线实时监控概览页 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12497)
- [新增] feat: Agent使用docker cli替换sdk，避免daemon升级带来的依赖版本问题 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12791)
- [新增] 修复编译错误的问题 #171 [链接](http://github.com/TencentBlueKing/bk-ci/issues/176)

#### 优化

##### 流水线
- [优化] pref: 减少redis热key的访问频率 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12488)

##### 研发商店
- [优化] pref： 构建机上的插件缓存文件如果损坏需重新从仓库下载覆盖 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12447)

##### 权限中心
- [优化] pref：优化智能体申请续期接口 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12929)
- [优化] pref：禁用项目不再发起权限续期提醒。 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12475)

##### 其他
- [优化] pref: 流水线构建数据清理优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12797)

#### 修复

##### 流水线
- [修复] bug: 【PAC】创建定时触发器报: 定时触发器[监听PAC]配置不合法,当前流水线未开启PAC [链接](http://github.com/TencentBlueKing/bk-ci/issues/12946)
- [修复] bug: 【PAC】发布pac流水线时,应该使用发布人的身份提交代码库 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12846)
- [修复] 模板批量升级实例，修改参数后切换实例，数据未保存 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12939)
- [修复] bug: 普通流水线，不能被改成约束模式流水线 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12933)
- [修复] bug: 从实例化流水线复制为模版实例化，复制后的模版不能编辑 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12899)
- [修复] bug：人工审核插件超时后自动重试不生效问题fix [链接](http://github.com/TencentBlueKing/bk-ci/issues/11661)
- [修复] bug: 【PAC】yaml文件重命名,不应该删除旧的流水线重新创建一条新的 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12658)
- [修复] bug: PAC流水线下SELF模式下注册webhook失败 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12874)
- [修复] bug: 编辑实例化流水线,常量或其他变量会提示required值异常 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12921)
- [修复] bug: 模板实例化过程中，子流水线插件权限校验异常时没有把详情抛出 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12891)

##### 代码库
- [修复] bug: ExternalCodeccRepoResource接口改成service态 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12892)

##### 其他
- [修复] bugfix: 第三方机新模式部分问题修复 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12945)

# v4.2.0-rc.1
## 2026-04-21
### Changelog since v4.1.0
#### 新增

##### 流水线
- [新增] feat: Tag 事件触发支持动作过滤 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12670)
- [新增] feat: 人工审核插件审核意见这里可以设置为必填 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12810)

##### 研发商店
- [新增] feat: go插件的安装包文件支持签名 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12694)

##### 项目管理
- [新增] feat：支持隐藏项目属性 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12796)
- [新增] feat：支持项目级别开启/禁用「共享」制品能力 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12592)

##### 其他
- [新增] feat: RedisLock支持单独的redis实例 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12508)

#### 优化

##### 流水线
- [优化] perf: 调整流水线内置参数列表 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12859)

##### 研发商店
- [优化] perf：研发商店安装模版优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12837)
- [优化] pref ：研发商店模板安装优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12771)

##### 权限中心
- [优化] pref：用户组中成员和组织统计数据展示、以及用户组名称长时的展示优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12798)

##### 其他
- [优化] Bug: openapi获取流水线轻量构建历史 未对查询结果做排序 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12830)

#### 修复

##### 流水线
- [修复] bug: 实例化更新,会导致标签丢失 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12863)
- [修复] bug: 修复删除流水线标签,动态流水线组没有更新 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12831)
- [修复] bug:部分分区表的sql查询条件缺乏分区键 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12841)
- [修复] bug: 修复删除流水线通知但是code方式还能查看 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12788)
- [修复] bug: 修复run插件code方式丢失manualCommand字段 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12789)
- [修复] bug: 基于审核人数组做矩阵分裂审核，人工审核插件点击审核会报错 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12783)
- [修复] bug:流水线在job超时报错时，当时报错的插件不会写入到BK_CI_BUILD_FAIL_TASKS这个变量里 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12741)
- [修复] bug:人工审核插件重试后通知内容里引用的变量值还是上一次执行的值 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12723)

##### 代码库
- [修复] bug: 获取代码库目录列表兼容400和404错误码 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12853)

##### 调度
- [修复] bugfix: 第三方构建机容器去掉指定工作空间 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12814)

##### 其他
- [修复] bug：超管权限校验bug [链接](http://github.com/TencentBlueKing/bk-ci/issues/12849)
- [修复] bugfix: 流水线视图api无法返回收藏和个人创建流水线 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12817)
- [修复] bug: 分块上传的apk包无法体验下载 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12821)

