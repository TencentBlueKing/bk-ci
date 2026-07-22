<!-- BEGIN MUNGE: GENERATED_TOC -->
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

