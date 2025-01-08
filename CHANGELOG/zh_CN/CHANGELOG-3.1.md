<!-- BEGIN MUNGE: GENERATED_TOC -->
- [v3.1.0](#v310)
   - [Changelog since v3.0.0](#changelog-since-v300)

- [v3.1.0-rc.6](#v310-rc6)
   - [Changelog since v3.1.0-rc.5](#changelog-since-v310-rc5)

- [v3.1.0-rc.5](#v310-rc5)
   - [Changelog since v3.1.0-rc.4](#changelog-since-v310-rc4)

- [v3.1.0-rc.4](#v310-rc4)
   - [Changelog since v3.1.0-rc.3](#changelog-since-v310-rc3)

- [v3.1.0-rc.3](#v310-rc3)
   - [Changelog since v3.1.0-rc.2](#changelog-since-v310-rc2)

- [v3.1.0-rc.2](#v310-rc2)
   - [Changelog since v3.1.0-rc.1](#changelog-since-v310-rc1)

- [v3.1.0-rc.1](#v310-rc1)
   - [Changelog since v3.0.0](#changelog-since-v300)

<!-- END MUNGE: GENERATED_TOC -->



<!-- NEW RELEASE NOTES ENTRY -->
# v3.1.0
## 2025-01-08
### Changelog since v3.0.0
#### 新增

##### 流水线
- [新增] feat：导出流水线功能优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11304)
- [新增] feat：推荐版本号模版优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11186)
- [新增] feat：Git分支/Tag和Svn分支/Tag类型的变量优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10774)
- [新增] 插件配置支持字段间联动 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11251)
- [新增] feat: 优化PUSH事件预匹配逻辑 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11317)
- [新增] AI大模型融入 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10825)
- [新增] feat: copilot 编辑器支持免登录 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11290)
- [新增] feat：项目设置支持管理员配置项目下流水线的命名规范 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11057)
- [新增] feat：创建流水线时支持设置标签 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11055)
- [新增] feat：流水线变量语法支持两种风格 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10576)
- [新增] [bugfix] okhttp3 Response 不主动关闭会有潜在的内存泄露问题 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11234)
- [新增] 流水线插件开发自定义UI希望可以获取到container 的 jobid 属性 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11197)
- [新增] 【蓝盾-评审会已评审】【PAC】feat：流水线版本管理机制 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8161)
- [新增] 【蓝盾-评审会已评审】【PAC】feat：新建/编辑流水线支持以 Code 方式编排流水线 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8125)
- [新增] feat：下拉类型变量选项编辑优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10747)
- [新增] feat：流水线组管理去掉CI管理员相关信息 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11165)
- [新增] feat: 触发器的自定义触发控制回调增加事件类型 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11196)
- [新增] feat：流水线并发运行时，支持限制并发个数和排队 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10718)
- [新增] feat：推荐版本号优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10958)
- [新增] feat：流水线触发历史支持按照触发结果搜索 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11006)
- [新增] 增加构建详情的查看配置项，在构建详情界面点击插件时，默认进入的是 日志 or 配置 Tab页面。 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10808)
- [新增] feat：运行时校验权限代持人权限是否已失效 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10478)
- [新增] feat：Job 互斥组排队时，队列长度支持最长到 50  [链接](http://github.com/TencentBlueKing/bk-ci/issues/10975)
- [新增] feat：流水线列表，增加标签展示 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11054)
- [新增] feat：社区版流水线完成通知，支持通知组 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10976)
- [新增] feat：触发事件重放操作权限控制 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11052)
- [新增] feat：通过子流水线调用触发的执行，支持重试 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11015)
- [新增] feat：stage 审核支持 checklist 确认场景 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10920)
- [新增] feat: UI方式下的「所有参数满足条件时执行」和「所有参数满足条件时不执行」转为Code 优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10930)

##### 代码库
- [新增] feat：工蜂 MR 触发增加 action=edit [链接](http://github.com/TencentBlueKing/bk-ci/issues/11024)

##### 研发商店
- [新增] feat：Post action 中支持获取父任务ID [链接](http://github.com/TencentBlueKing/bk-ci/issues/10968)
- [新增] feat:java插件支持在指定的java环境下运行 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10978)

##### 环境管理
- [新增] feat: 环境管理优化改动 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11003)

##### 日志服务
- [新增] 访问流水线搜索日志，全屏浏览器查询搜索的日志，上方的搜索栏会消失 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11118)

##### 权限中心
- [新增] feat：用户个人视角 权限管理优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11138)
- [新增] feat：提供项目管理相关openapi接口 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11231)
- [新增] feat：获取项目下用户组成员优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11221)
- [新增] feat：用户申请加入组优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11219)
- [新增] feat：环境支持资源级别的权限管理入口 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11074)
- [新增] feat：流水线列表展示权限控制 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10895)
- [新增] feat : 提供 获取用户加入的用户组、续期接口  [链接](http://github.com/TencentBlueKing/bk-ci/issues/11136)
- [新增] feat：auth服务 open类接口整改 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10403)
- [新增] bug：查询部门信息接口 返回字段变动导致异常 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11151)
- [新增] feat：同步用户组逻辑优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11122)

##### 项目管理
- [新增] feat：查询项目接口优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11276)
- [新增] feat：最大可授权范围更改无效修复 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11153)

##### Stream
- [新增] [stream] 项目支持关联到运营产品 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9948)
- [新增] [stream] 存留问题优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11045)

##### 调度
- [新增] feat：优化dispatch-sdk调度逻辑对其他服务的依赖 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10882)
- [新增] feat：无编译资源优化环境依赖调度 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11126)
- [新增] feat: 构建机触发用户调整为流水线权限代持人 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11117)

##### Agent
- [新增] feat：流水线/Job并发和排队数据落地 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10997)
- [新增] [bugfix] bash插件取消配置<脚本返回非零时归档文件>存在脏数据 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11177)

##### 未分类
- [新增] feat：支持管理我的 OAUTH [链接](http://github.com/TencentBlueKing/bk-ci/issues/10995)
- [新增] API自动化文档优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11339)
- [新增] feat: 健康检查优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11336)
- [新增] feat: 升级openresty到1.19 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11295)
- [新增] openapi新增文档生成能力 [链接](http://github.com/TencentBlueKing/bk-ci/issues/7412)
- [新增] feat: 升级undertow版本解决内存泄漏问题 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11300)
- [新增] feat: 整理网关的tag路由 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11050)
- [新增] sql doc 文档更新 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9974)
- [新增] feat: 修复开源版启动问题 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11202)
- [新增] feat：我的凭证列表展示创建和更新信息 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11023)
- [新增] feat: 新增鸿蒙平台 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11191)
- [新增] [feat] 插件日志10w+即归档为压缩包 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11183)
- [新增] feat: 调整helm的镜像使其支持配置imageRegistry [链接](http://github.com/TencentBlueKing/bk-ci/issues/11171)
- [新增] [feat] API文档优化-2024-10批次 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11107)
- [新增] worker和agent支持java17和java8同步运行 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10586)
- [新增] feat: 引擎等MQ场景接入SCS框架 [链接](http://github.com/TencentBlueKing/bk-ci/issues/7443)
- [新增] feat：依赖的服务未部署时的交互优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10612)
- [新增] feat：支持流水线指标监控 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9860)
- [新增] feat：同步并分表存储资源组权限数据 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10964)
- [新增] feat: 插件管理菜单对应插件列表增加默认公共插件显示 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10472)
- [新增] feat：当策略为「锁定构建号」时，执行界面可以修改当前值 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11089)
- [新增] feat: 优化AESUtil [链接](http://github.com/TencentBlueKing/bk-ci/issues/11084)
- [新增] 丰富流水线-stage准入的审核功能，支持配置角色或用户组作为审核人 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10689)
- [新增] 流水线日志支持AI修复 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10913)
- [新增] feat: 触发器变量补充 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11002)
- [新增] feat：创建自定义组并赋予组组权限 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11026)
- [新增] feat：MR 事件触发器支持 WIP [链接](http://github.com/TencentBlueKing/bk-ci/issues/10683)
- [新增] feat: 蓝鲸7.2版本的改动 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10558)
- [新增] feat：oauth2 增加密码模式  [链接](http://github.com/TencentBlueKing/bk-ci/issues/10663)
- [新增] 第三方构建机DockerUi界面支持 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10962)
- [新增] feat：流水线查看页面/编辑页面/构建详情界面 面包屑中的名字展示优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10800)
- [新增] feat: openapi filter 优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10679)
- [新增] feat: 触发器条件引入${{variables.xxx}}变量触发不了流水线 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10987)
- [新增] feat: 增加获取工蜂和github oauth url的build接口 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10826)
- [新增] feat：源材料展示优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10733)
- [新增] feat：Job 并发支持 Code 配置 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10860)
- [新增] [stream] 优化大仓触发耗时 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10861)
- [新增] feat：活跃用户记录操作和次数 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10891)
- [新增] feat：同一流水线多次构建时资源调度优先级优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9897)
- [新增] issue: 修复sample鉴权下查project全表的问题 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10941)
- [新增] 【PAC】feat：草稿版本UI展示 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9861)
- [新增] feat: 把docker build插件的config ns配置给去掉 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10926)
- [新增] feat：项目成员支持按照过期时间/用户组名称搜索 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10892)
- [新增] feat：项目成员管理优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10927)
- [新增] AgentId复用类型转换问题 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10915)
- [新增] feat：stream stage 审核的通知方式支持企业微信群 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10796)
- [新增] feat：第三方构建机 Job 间复用构建环境支持 Code 配置 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10254)
- [新增] feat: 新启动的POD需要热身 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10887)
- [新增] feat: 让worker支持在JDK17中运行 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10412)
- [新增] feat：sdk相关的api是否显示在申请列表中支持可配置化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10840)
- [新增] feat：OpenApi提供转发Turbo编译加速上报资源统计数据接口 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10508)

#### 优化

##### 流水线
- [优化] pref：优化流水线项目下已安装插件关联流水线查询 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11307)
- [优化] pref:流水线相关的文件操作人调整为流水线的权限代持人 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11016)

##### 代码库
- [优化] perf: repository服务去掉对git命令依赖 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11193)

##### 研发商店
- [优化] pref:研发商店组件内置打包流水线都归属到统一的平台项目下 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10475)
- [优化] pref:nodejs安装包下载地址域名支持按部署环境返回 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11327)
- [优化] pref：研发商店通用化接口封装优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11049)
- [优化] perf:优化插件管理菜单默认插件查询 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11142)
- [优化] pref:对文件上传接口的文件存储路径进行调整 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10919)

##### 环境管理
- [优化] perf: 增加部分错误码 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11279)

##### 未分类
- [优化] perf: 版本日志日期调整为二级标题 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11162)
- [优化] perf:研发商店组件指标数据字段优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10219)
- [优化] pref：拉取插件task.json文件内容报错提示优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10446)
- [优化] pref:带矩阵的流水线运行矩阵分裂前的task任务无需写入记录表 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10873)
- [优化] pref:研发商店敏感接口签名校验优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10759)
- [优化] perf: 应用Schema改为每个版本都可以设置不同的配置 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10929)
- [优化] pref:公共构建机插件缓存区路径和变量调整优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10844)

#### 修复

##### 流水线
- [修复] bug: 修复流水线事件重放报500错误 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11333)
- [修复] bug: GIT触发器单独监听[新增分支]不生效 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11338)
- [修复] fix: 执行前暂停的插件弹窗问题处理 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11296)
- [修复] bug: 流水线另存为模版,模版名字与流水线名字一样，会报"流水线名称已被使用" [链接](http://github.com/TencentBlueKing/bk-ci/issues/11264)
- [修复] bug: 创建流水线组失败，导致代码库开启PAC一直显示同步中 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11253)
- [修复] bug: 心跳超时被取消的插件没有刷新前端状态 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11265)
- [修复] bug: [PAC].ci下的目录已经删除,但是关联的流水线组没有删除，也无法手工删除 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11254)
- [修复] bug: finally stage执行时点击跳过，失败的job状态会卡在执行中 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11143)
- [修复] feat：模版管理-列表支持展示字段和排序优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11056)
- [修复] bug: 修复github pull request id越界 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11146)
- [修复] feat： 调试记录和流水线 Job 查看页面，支持「登录调试」 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10933)

##### 代码库
- [修复] bug: 修复代码库开启PAC时,git_project_id字段为空 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11167)

##### 研发商店
- [修复] bug：插件执行失败时的错误码类型归属错误问题优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11294)
- [修复] bug:插件最新版本使用历史版本修复方式发布后，再用普通方式发布的分支会继承上一次的 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11301)
- [修复] bug:同一个job下不同语言的插件生成启动命令时可能会因为系统变量冲突 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11229)
- [修复] bug:研发商店组件上架查看日志权限优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11208)
- [修复] bug：优化项目下安装插件分页数据查询，排除与插件关联的内置流水线 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11210)

##### 质量红线
- [修复] bug: 使用流水线变量传入多个审核人时，审批不生效 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11127)

##### 权限中心
- [修复] bug: 权限管理-权限续期数据同步 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11271)
- [修复] 权限管理用户组添加人员模板同步数据 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11217)

##### 调度
- [修复] feat：第三方构建机支持使用 dcoker 运行构建任务 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9820)

##### 未分类
- [修复] bugfix: 升级JDK17导致worker无法强杀进程 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11320)
- [修复] bugfix: Agent没有开启监控会无限打日志 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11274)
- [修复] feat：支持查看版本日志 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10938)
- [修复] bug: 版本日志根据配置控制弹框 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11260)
- [修复] bug: 去除国际化描述信息中的非法占位符信息 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11182)
- [修复] fix UnreachableCode [链接](http://github.com/TencentBlueKing/bk-ci/issues/11172)
- [修复] bug:根据Profile获取集群名称优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11137)
- [修复] bug:研发商店组件包文件上传下载优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11115)
- [修复] bug:修复更新组件关联初始化项目信息时，新增调试项目记录时未成功把旧的调试项目记录清理 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11011)
- [修复] 归档报告插件创建token没有实现 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10693)
- [修复] bug:某些构建场景下插入T_PIPELINE_BUILD_RECORD_TASK表的CONTAINER_ID字段值错误 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11029)
- [修复] bug:上架流水线模板到研发商店，但是新建流水线的时候在“研发商店”Tab搜不出来 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10865)
- [修复] 产品的顶栏下拉框样式和内容统一规范 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10939)
- [修复] bug: 新构建详情页的失败重试展示问题 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10735)
- [修复] bug:查插件环境信息接口未正确处理插件测试分支版本号情况 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10924)
- [修复] feat：支持管理员查看项目成员 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9620)

# v3.1.0-rc.6
## 2025-01-08
### Changelog since v3.1.0-rc.5
#### 新增

##### 流水线
- [新增] feat：导出流水线功能优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11304)
- [新增] feat：推荐版本号模版优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11186)
- [新增] feat：Git分支/Tag和Svn分支/Tag类型的变量优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10774)

##### 权限中心
- [新增] feat：用户个人视角 权限管理优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11138)

##### 未分类
- [新增] feat：支持管理我的 OAUTH [链接](http://github.com/TencentBlueKing/bk-ci/issues/10995)
- [新增] API自动化文档优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11339)
- [新增] feat: 健康检查优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11336)
- [新增] [feat] 插件日志10w+即归档为压缩包 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11183)

#### 优化

##### 流水线
- [优化] pref：优化流水线项目下已安装插件关联流水线查询 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11307)

##### 研发商店
- [优化] pref:研发商店组件内置打包流水线都归属到统一的平台项目下 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10475)

#### 修复

##### 流水线
- [修复] bug: 修复流水线事件重放报500错误 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11333)
- [修复] bug: GIT触发器单独监听[新增分支]不生效 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11338)

##### 研发商店
- [修复] bug：插件执行失败时的错误码类型归属错误问题优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11294)

# v3.1.0-rc.5
## 2024-12-23
### Changelog since v3.1.0-rc.4
#### 新增

##### 流水线
- [新增] 插件配置支持字段间联动 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11251)
- [新增] feat：Git分支/Tag和Svn分支/Tag类型的变量优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10774)
- [新增] feat: 优化PUSH事件预匹配逻辑 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11317)
- [新增] AI大模型融入 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10825)
- [新增] feat: copilot 编辑器支持免登录 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11290)
- [新增] feat：推荐版本号模版优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11186)
- [新增] 【蓝盾-评审会已评审】【PAC】feat：流水线版本管理机制 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8161)
- [新增] feat：运行时校验权限代持人权限是否已失效 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10478)

##### 权限中心
- [新增] feat：提供项目管理相关openapi接口 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11231)

##### 项目管理
- [新增] feat：查询项目接口优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11276)

##### Stream
- [新增] [stream] 项目支持关联到运营产品 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9948)

##### 调度
- [新增] feat：优化dispatch-sdk调度逻辑对其他服务的依赖 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10882)

##### 未分类
- [新增] feat: 升级openresty到1.19 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11295)
- [新增] openapi新增文档生成能力 [链接](http://github.com/TencentBlueKing/bk-ci/issues/7412)
- [新增] feat: 升级undertow版本解决内存泄漏问题 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11300)
- [新增] sql doc 文档更新 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9974)

#### 优化

##### 研发商店
- [优化] pref:nodejs安装包下载地址域名支持按部署环境返回 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11327)
- [优化] pref：研发商店通用化接口封装优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11049)

##### 环境管理
- [优化] perf: 增加部分错误码 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11279)

#### 修复

##### 流水线
- [修复] fix: 执行前暂停的插件弹窗问题处理 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11296)

##### 研发商店
- [修复] bug:插件最新版本使用历史版本修复方式发布后，再用普通方式发布的分支会继承上一次的 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11301)

##### 未分类
- [修复] bugfix: 升级JDK17导致worker无法强杀进程 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11320)

# v3.1.0-rc.4
## 2024-12-05
### Changelog since v3.1.0-rc.3
#### 新增

##### 流水线
- [新增] feat：项目设置支持管理员配置项目下流水线的命名规范 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11057)
- [新增] feat：创建流水线时支持设置标签 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11055)
- [新增] 流水线插件开发自定义UI希望可以获取到container 的 jobid 属性 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11197)
- [新增] feat: 触发器的自定义触发控制回调增加事件类型 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11196)

##### 未分类
- [新增] feat: 整理网关的tag路由 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11050)
- [新增] feat：我的凭证列表展示创建和更新信息 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11023)
- [新增] worker和agent支持java17和java8同步运行 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10586)
- [新增] feat: 引擎等MQ场景接入SCS框架 [链接](http://github.com/TencentBlueKing/bk-ci/issues/7443)

#### 优化

##### 代码库
- [优化] perf: repository服务去掉对git命令依赖 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11193)

#### 修复

##### 流水线
- [修复] bug: 流水线另存为模版,模版名字与流水线名字一样，会报"流水线名称已被使用" [链接](http://github.com/TencentBlueKing/bk-ci/issues/11264)
- [修复] bug: 创建流水线组失败，导致代码库开启PAC一直显示同步中 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11253)
- [修复] bug: 心跳超时被取消的插件没有刷新前端状态 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11265)
- [修复] bug: [PAC].ci下的目录已经删除,但是关联的流水线组没有删除，也无法手工删除 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11254)

##### 权限中心
- [修复] bug: 权限管理-权限续期数据同步 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11271)

##### 调度
- [修复] feat：第三方构建机支持使用 dcoker 运行构建任务 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9820)

##### 未分类
- [修复] bugfix: Agent没有开启监控会无限打日志 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11274)
- [修复] feat：支持查看版本日志 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10938)
- [修复] bug: 版本日志根据配置控制弹框 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11260)

# v3.1.0-rc.3
## 2024-11-22
### Changelog since v3.1.0-rc.2
#### 新增

##### 流水线
- [新增] feat：流水线变量语法支持两种风格 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10576)
- [新增] [bugfix] okhttp3 Response 不主动关闭会有潜在的内存泄露问题 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11234)
- [新增] 【蓝盾-评审会已评审】【PAC】feat：新建/编辑流水线支持以 Code 方式编排流水线 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8125)
- [新增] feat：下拉类型变量选项编辑优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10747)
- [新增] feat：流水线组管理去掉CI管理员相关信息 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11165)
- [新增] 流水线插件开发自定义UI希望可以获取到container 的 jobid 属性 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11197)
- [新增] feat：流水线并发运行时，支持限制并发个数和排队 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10718)
- [新增] feat：推荐版本号优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10958)
- [新增] feat：流水线触发历史支持按照触发结果搜索 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11006)
- [新增] 增加构建详情的查看配置项，在构建详情界面点击插件时，默认进入的是 日志 or 配置 Tab页面。 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10808)
- [新增] feat：Job 互斥组排队时，队列长度支持最长到 50  [链接](http://github.com/TencentBlueKing/bk-ci/issues/10975)

##### 研发商店
- [新增] feat:java插件支持在指定的java环境下运行 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10978)

##### 环境管理
- [新增] feat: 环境管理优化改动 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11003)

##### 日志服务
- [新增] 访问流水线搜索日志，全屏浏览器查询搜索的日志，上方的搜索栏会消失 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11118)

##### 权限中心
- [新增] feat：获取项目下用户组成员优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11221)
- [新增] feat：用户申请加入组优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11219)
- [新增] feat：环境支持资源级别的权限管理入口 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11074)
- [新增] feat：流水线列表展示权限控制 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10895)
- [新增] feat : 提供 获取用户加入的用户组、续期接口  [链接](http://github.com/TencentBlueKing/bk-ci/issues/11136)
- [新增] feat：auth服务 open类接口整改 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10403)
- [新增] bug：查询部门信息接口 返回字段变动导致异常 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11151)
- [新增] feat：同步用户组逻辑优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11122)

##### 项目管理
- [新增] feat：最大可授权范围更改无效修复 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11153)

##### 调度
- [新增] feat：第三方构建机支持使用 dcoker 运行构建任务 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9820)
- [新增] feat：无编译资源优化环境依赖调度 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11126)
- [新增] feat: 构建机触发用户调整为流水线权限代持人 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11117)

##### Agent
- [新增] feat：流水线/Job并发和排队数据落地 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10997)
- [新增] [bugfix] bash插件取消配置<脚本返回非零时归档文件>存在脏数据 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11177)

##### 未分类
- [新增] feat: 修复开源版启动问题 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11202)
- [新增] feat: 新增鸿蒙平台 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11191)
- [新增] feat: 调整helm的镜像使其支持配置imageRegistry [链接](http://github.com/TencentBlueKing/bk-ci/issues/11171)
- [新增] [feat] API文档优化-2024-10批次 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11107)
- [新增] feat：依赖的服务未部署时的交互优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10612)
- [新增] sql doc 文档更新 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9974)
- [新增] feat：支持查看版本日志 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10938)
- [新增] feat: 蓝鲸7.2版本的改动 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10558)
- [新增] 产品的顶栏下拉框样式和内容统一规范 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10939)

#### 优化

##### 流水线
- [优化] pref:流水线相关的文件操作人调整为流水线的权限代持人 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11016)

##### 研发商店
- [优化] perf:优化插件管理菜单默认插件查询 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11142)
- [优化] pref:对文件上传接口的文件存储路径进行调整 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10919)

##### 未分类
- [优化] perf: 版本日志日期调整为二级标题 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11162)

#### 修复

##### 流水线
- [修复] bug: finally stage执行时点击跳过，失败的job状态会卡在执行中 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11143)
- [修复] feat：模版管理-列表支持展示字段和排序优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11056)
- [修复] bug: 修复github pull request id越界 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11146)

##### 代码库
- [修复] bug: 修复代码库开启PAC时,git_project_id字段为空 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11167)

##### 研发商店
- [修复] bug:同一个job下不同语言的插件生成启动命令时可能会因为系统变量冲突 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11229)
- [修复] bug:研发商店组件上架查看日志权限优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11208)
- [修复] bug：优化项目下安装插件分页数据查询，排除与插件关联的内置流水线 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11210)

##### 质量红线
- [修复] bug: 使用流水线变量传入多个审核人时，审批不生效 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11127)

##### 权限中心
- [修复] 权限管理用户组添加人员模板同步数据 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11217)

##### 未分类
- [修复] bug: 去除国际化描述信息中的非法占位符信息 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11182)
- [修复] fix UnreachableCode [链接](http://github.com/TencentBlueKing/bk-ci/issues/11172)
- [修复] bug:根据Profile获取集群名称优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11137)
# v3.1.0-rc.2
## 2024-10-26
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


# v3.1.0-rc.1
## 2024-10-15
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
