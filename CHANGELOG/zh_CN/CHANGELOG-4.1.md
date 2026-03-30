<!-- BEGIN MUNGE: GENERATED_TOC -->
- [v4.1.0-rc.4](#v410-rc4)
   - [Changelog since v4.1.0-rc.3](#changelog-since-v410-rc3)

- [v4.1.0-rc.3](#v410-rc3)
   - [Changelog since v4.1.0-rc.2](#changelog-since-v410-rc2)

- [v4.1.0-rc.2](#v410-rc2)
   - [Changelog since v4.1.0-rc.1](#changelog-since-v410-rc1)

- [v4.1.0-rc.1](#v410-rc1)
   - [Changelog since v4.0.0](#changelog-since-v400)

<!-- END MUNGE: GENERATED_TOC -->



<!-- NEW RELEASE NOTES ENTRY -->
# v4.1.0-rc.4
## 2026-03-30
### Changelog since v4.1.0-rc.3
#### 新增

##### 流水线
- [新增] feat: 新建流水线支持按渠道查询空白模板 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12762)
- [新增] feat：执行时支持以指定组合的启动参数启动 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10798)
- [新增] feat：模板Apigw接口从v1版本升级到v2版本 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12686)
- [新增] feat：第三方构建机运行 docker 时支持 --network [链接](http://github.com/TencentBlueKing/bk-ci/issues/12298)
- [新增] feat:企业微信机器人消息通知支持发送企业微信多媒体信息 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12617)
- [新增] feat：支持快速填充启动参数 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12418)
- [新增] 定时触发器的默认定时规则优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12610)
- [新增] feat：研发商店未迁移模板兼容问题 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12588)
- [新增] feat：流水线变量支持「是否敏感」属性 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11738)
- [新增] feat: 调整构建详情页的重放事件逻辑 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12344)
- [新增] feat：流水线参数在执行预览界面的展示支持联动 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11438)
- [新增] feat:【PAC模板】流水线模板支持PAC特性 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11414)
- [新增] feat：dynamic-parameter 和 dynamic-parameter-simple 组件中的 select 和 enum-input 支持输入变量 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12343)
- [新增] feat：取消正在运行的流水线构建权限支持配置策略 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9233)
- [新增] feat: ui转code兼容16进制字符串 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12451)
- [新增] feat: 高配机型支持code [链接](http://github.com/TencentBlueKing/bk-ci/issues/12353)
- [新增] feat：增加 ci 上下文变量 debug 标识当前构建是调试还是正式运行 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12345)

##### 代码库
- [新增] feat: Bkcode支持Commit Check [链接](http://github.com/TencentBlueKing/bk-ci/issues/12559)
- [新增] feat: openapi接口repository_info_list增加scm_code查询条件 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12537)
- [新增] feat: codecc查看代码片段接口支持代码源平台 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12478)
- [新增] feat: 支持bkCode代码源 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12411)

##### 研发商店
- [新增] feat: 插件的分类和标签信息支持按服务范围筛选 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12755)
- [新增] feat: 研发商店镜像支持升级历史小版本 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12627)
- [新增] feat: 研发商店镜像支持升级历史小版本 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12598)
- [新增] feat:插件发布时发布描述内容支持读取插件代码库最近提交信息 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12080)
- [新增] feat：上架模版时，支持设置可见范围 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12423)
- [新增] feat：存量云研发包增加存储包文件的sha256摘要值 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12398)
- [新增] feat:研发商店组件包文件完整性校验支持使用sha256算法 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12362)

##### 环境管理
- [新增] windows监听环境变量更新 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12449)
- [新增] feat：构建机监控数据采集支持显卡和CPU型号 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12394)

##### 权限中心
- [新增] feat：提供用户在项目下加入的用户组列表 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12516)
- [新增] feat：新增资源类型接入权限中心 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12328)

##### 调度
- [新增] feat: 第三方构建机docker支持配置启动用户 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12703)
- [新增] feat：优化k8s构建集群方案 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10636)

##### 其他
- [新增] feat：创作流管理 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12414)
- [新增] feat: 灰度策略支持按照百分比路由 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12688)
- [新增] feat：创作流接入权限中心 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12493)
- [新增] feat: agent支持mcp [链接](http://github.com/TencentBlueKing/bk-ci/issues/12653)
- [新增] feat：新版模板版本名称支持重复 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12713)
- [新增] feat: 修复spring-amqp的Prometheus数据过多问题 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12462)
- [新增] feat：新增免校验项目授权的管理员信息apigw接口 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12659)
- [新增] feat: 去掉无用的AUTH_HEADER_DEVOPS_ACCESS_TOKEN逻辑 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12622)
- [新增] 开发openapi轻量级流水线构建历史查询接口 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12472)
- [新增] feat: openapi环境节点接口新增启停信息 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12586)
- [新增] feat: 前端容器化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12445)
- [新增] feat: websocket 逻辑优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12484)
- [新增] feat：基于SDD的AI工作流实践 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12501)
- [新增] feat: RedisLock支持单独的redis实例 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12508)
- [新增] feat：提供重置流水线权限代持的 openapi 接口 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11258)
- [新增] feat: 文件信息增加crc64值 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12461)
- [新增] feat: 降低redis的压力 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12482)
- [新增] feat: 新增部分接口 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12440)
- [新增] feat: framework升级到1.1.0 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12375)
- [新增] 提供私有构建环境集群的启用/停用 OpenAPI接口 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12406)
- [新增] 【PAC】精简开源版stream服务 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12233)

#### 优化

##### 流水线
- [优化] perf: 【PAC模版】保证模版数据迁移时数据一致性 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12675)
- [优化] perf：跳转至模版目标页面优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12651)
- [优化] pref：流水线版本快速回滚工具 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12631)
- [优化] pref：流水线模板数据迁移准确性保证 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12541)
- [优化] perf: 优化流水线webhook触发流程 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11884)
- [优化] perf: 新构建产生的数据不再写入detai表 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12464)
- [优化] perf: 优化流水线版本查询sql内容 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12424)
- [优化] perf: 支持MR_ACC事件转yaml [链接](http://github.com/TencentBlueKing/bk-ci/issues/12309)
- [优化] pref：权限熔断 优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12363)
- [优化] perf：常量值还能被修改问题优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12313)
- [优化] perf：存量流水线使用了插件输出变量命名空间，无法对比差异问题优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12312)

##### 代码库
- [优化] perf：关联代码库时支持按地址搜索 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12393)

##### 研发商店
- [优化] pref: 插件上架初始化数据的服务范围默认设置为流水线 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12775)
- [优化] perf：插件视角流水线列表支持分页 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12434)
- [优化] pref: 插件下载逻辑优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12561)
- [优化] pref:研发商店通用上传接口完善对组件版本号正则表达式校验 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12490)

##### 日志服务
- [优化] perf: 日志subTag判断逻辑可以循环外获取缓存 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12646)
- [优化] perf: 增加log服务的es连接超时时间和连接数 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12609)

##### 调度
- [优化] perf: 流水线并发配额管理优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12340)

##### 其他
- [优化] pref：流水线模版apigw接口兼容 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12743)
- [优化] pref：增加调用iam获取组成员数量 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12731)
- [优化] perf: 自定义binder时将消费者组增加命名空间 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12628)
- [优化] pref：skills文件精简，减少上下文 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12594)
- [优化] docs: 云桌面文档更新 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12582)
- [优化] perf: 支持自定义中间件binder参数 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12443)
- [优化] perf：定时触发的 crontab 设置交互优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12205)
- [优化] chore: 升级api-turbo到0.0.7-RELEASE [链接](http://github.com/TencentBlueKing/bk-ci/issues/12382)

#### 修复

##### 流水线
- [修复] perf：存量模版升级到新版时变量兼容优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12471)
- [修复] bug：创建流水线时，获取研发商店模板不存在问题 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12749)
- [修复] bug：模板实例化未释放锁问题 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12721)
- [修复] feat：支持在两个插件之间插入步骤 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12679)
- [修复] 流水线执行界面修改入参值，参数没有及时更新 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12673)
- [修复] bug: 定时触发,触发时校验流水线是否存在触发的插件 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12661)
- [修复] bug: 切换产出报告为构建制品时页面未更新 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12637)
- [修复] fix: windows安装脚本需要去掉exit方便排查 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12512)
- [修复] bug：在大批量事件触发构建且每次构建都新增流水线版本的场景下，对保存流水线版本的逻辑进行优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12578)
- [修复] bug：矩阵下的job下的审核插件无法正确解析表达式 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12574)
- [修复] bug: 模板定时触发插件获取参数列表失败 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12564)
- [修复] bug: 子流水线跨项目调用时,支持跨集群调用 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12554)
- [修复] bug：审核插件的审核人为表达式时重试时审核人与预期可能不符 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12525)
- [修复] bug: 修复tgit触发器2.0版本内容为空 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12530)
- [修复] bug: 通过服务调用触发草稿状态新流水线导致record数据异常 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12513)
- [修复] bug：某些情况下质量红线插件重试未执行 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12521)
- [修复] bugfix: 流水线运行设置的分组排队数量配置为0之后，分组名不同的任务也无法发起构建 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12427)
- [修复] bug：草稿版本的触发器（代码库）不应该生效 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11913)
- [修复] bug: 构建详情查看配置界面敏感字段使用*代替 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12412)
- [修复] bug:流水线页面获取执行分析跳转url地址鉴权范围设置错误 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12408)
- [修复] bug: 流水线构建历史列表查看stage相关信息时没法展示耗费时间 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12390)
- [修复] bugfix: 构建节点互斥空指针异常 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12376)

##### 代码库
- [修复] bug: 代码库重置oauth授权报错 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12542)
- [修复] bug:修复repository服务无法启动 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12469)

##### 研发商店
- [修复] fix：研发商店无法安装插件问题修复 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12500)
- [修复] bug:优化研发商店插件提交阶段的插件配置文件校验逻辑 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12355)
- [修复] bug：研发商店插件与流水线关联信息展示不正确 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12307)
- [修复] bug: 研发商店通用上架接口在处理复杂版本日志内容可能出错 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12401)

##### 环境管理
- [修复] bug: 环境管理中新增环境导入超过20个节点后无法保存 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12680)
- [修复] fix: 构建机节点引用流水线名称不更新问题 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12380)

##### 日志服务
- [修复] bug: 超时导致Job退出时跳过了日志超量归档 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11626)

##### 质量红线
- [修复] bug: 质量红线控制点没有配置在指标对应的插件上时，关联多条元数据的指标计算阈值有误 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12738)

##### 权限中心
- [修复] bug: 修复release-4.0 服务无法启动不起来 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12457)

##### 调度
- [修复] fix: docker构建指定工作空间不生效 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12495)

##### 其他
- [修复] bug：页面提示信息错别字修正 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12547)
- [修复] bugfix: 第三方机使用systemd重启优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12373)
- [修复] bugfix: 初始化默认镜像脚本报错 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12216)
- [修复] bug: MacOS install script 缺失 service_name 赋值 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11651)

# v4.1.0-rc.3
## 2025-10-30
### Changelog since v4.1.0-rc.2
#### 新增

##### 流水线
- [新增] feat：流水线列表中，「最近执行」信息增加 stage 进度展示 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12228)
- [新增] feat：下拉类型变量选项从接口获取时，增加支持引用变量的提示 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11726)
- [新增] feat: 审核中状态时取消的优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12300)
- [新增] feat：执行历史列表备注列增加复制备注能力 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12138)
- [新增] feat: 优化矩阵解析 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12231)

##### 代码库
- [新增] feat: 代码源支持可见范围 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12146)

##### 研发商店
- [新增] feat:增加商店组件包文件下载链接open类接口 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12254)

##### 环境管理
- [新增] feat: 环境管理支持批量修改节点导入人 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12221)
- [新增] feat: 支持批量修改第三方机器最大并发数 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12222)

##### 权限中心
- [新增] feat：用户管理相关优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12329)

##### 调度
- [新增] feat: dispatch参考build_util.lua新增多种鉴权接口 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12271)

##### 其他
- [新增] feat: 插件启动异常监控与自修复 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12299)
- [新增] feat: 提供批量导出model/code接口 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12267)
- [新增] feat: 优化redis双写的java线程池 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12263)
- [新增] feat: 关闭enableServiceLinks加快服务启动 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12260)
- [新增] feat: 压缩归档报告配置增加报告大小限制 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12150)

#### 优化

##### 流水线
- [优化] pref:优化迁移项目数据时构建流水表因为数据量大产生的深度分页问题 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12274)
- [优化] perf: 详情页的旧数据兼容下架 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9522)

##### 凭证管理
- [优化] chore: 升级bcprov-jdk15on到	1.78.1 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11888)

##### 权限中心
- [优化] pref：权限系统熔断设计-验证 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12186)
- [优化] pref：权限续期优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12293)
- [优化] pref：auth服务开源相关脚本优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12259)
- [优化] perf：我的授权- OAuth 授权提示优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12227)

#### 修复

##### 流水线
- [修复] bug：查询T_PIPELINE_BUILD_RECORD_STAGE表数据列表排序字段优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12331)
- [修复] bug: PAC流水线发布时没有刷新流水线组 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11953)
- [修复] bug: 流水线排队情况下启动下一个发生重试的构建报错 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12322)
- [修复] bug: 流水线列表页按最近执行时间排序不对 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11480)
- [修复] bug: git触发时常量定义未生效 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12308)
- [修复] bug: 新增流水线标签，编辑流水线设置增加标签，转换成code，标签丢失 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12305)
- [修复] bug: 复制流水线设置标签,再编辑时,标签消失 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12304)
- [修复] bug: 流水线正式版本执行只能执行最新的正式版本 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12301)
- [修复] bug: stage级重试构建时流水线组上锁失败 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12294)
- [修复] bug: 调试构建生成model时逻辑优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12283)
- [修复] bug: 矩阵兼容record [链接](http://github.com/TencentBlueKing/bk-ci/issues/12279)
- [修复] bug: 选择推荐版本号方式构建可能出错 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12277)

##### 项目管理
- [修复] bug: service态获取项目列表接口返回增加是否拥有项目管理员权限 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12289)

##### 其他
- [修复] bug: 部分老流水线因为没有配置jobId导致插件里使用了steps类的上下文语法解析出错 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12356)
- [修复] bug: 获取构建详情接口返回报文的stageStatus字段里的name的值不符合预期 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12348)
- [修复] fix: [构建机] 修复windows2022 可能导致临时目录创建失败 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12333)
- [修复] bug: 修复流水线导出接口 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12243)

# v4.1.0-rc.2
## 2025-09-11
### Changelog since v4.1.0-rc.1
#### 新增

##### 流水线
- [新增] feat：手动触发支持配置默认的「构建信息」 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12215)

##### 代码库
- [新增] feat: githubService增加扩展接口 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12246)
- [新增] feat: Gitee类型代码库支持Check-Run [链接](http://github.com/TencentBlueKing/bk-ci/issues/12092)

##### 其他
- [新增] feat：获取项目列表的 APP态 openapi 接口，增加筛选条件 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12232)
- [新增] feat: 传递traceid到bkrepo [链接](http://github.com/TencentBlueKing/bk-ci/issues/12223)

#### 优化

##### 流水线
- [优化] pref: 分库分表逻辑支持按指定序号剔除数据源 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12212)

##### 研发商店
- [优化] pref: 插件代码库被删除时，查询插件详情信息逻辑优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12206)

##### 凭证管理
- [优化] chore: 升级bcprov-jdk15on到	1.78.1 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11888)

##### 权限中心
- [优化] perf：我的授权- OAuth 授权提示优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12227)

#### 修复

##### 流水线
- [修复] bug：约束流水线保存草稿出现名称重复异常问题 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12234)
- [修复] bug: PAC 流水线修改名称后，权限中心资源未释放 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12078)

##### 研发商店
- [修复] bug: 插件多个版本取消后再发布审核时最新标识设置重复 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12248)

##### 其他
- [修复] bug: 代码框架升级导致Jersey 的 getAnnotation方法获取不到方法上的注解 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12237)
- [修复] bugfix: 初始化默认镜像脚本报错 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12216)
- [修复] bug: 流水线事件上报处理异常数据 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12210)

# v4.1.0-rc.1
## 2025-09-01
### Changelog since v4.0.0
#### 新增

##### 流水线
- [新增] pref：模板实例异步更新逻辑优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12131)
- [新增] feat: 定时触发器支持绑定TGIT代码库 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12073)
- [新增] feat: 优化countGroupByBuildId方法 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12136)
- [新增] feat：流水线参数值超长时报错 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10665)
- [新增] feat：变量条件展示支持 Code 定义 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12110)
- [新增] 插件配置支持字段间联动 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11251)
- [新增] feat: 忽略stage审核时并发组校验 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12121)
- [新增] feat：插件「执行前暂停」支持 Code 定义 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12113)

##### 环境管理
- [新增] feat: 新增构建机标签查询API [链接](http://github.com/TencentBlueKing/bk-ci/issues/12058)

##### 质量红线
- [新增] feat: 质量红线指标仅检查所在控制点 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12137)
- [新增] feat: 自定义bash质量红线指标支持prompt提示信息 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12162)

##### 权限中心
- [新增] pref：权限系统熔断设计-权限缓存优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12173)

##### 其他
- [新增] feat: 事件监控数据采集优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12196)
- [新增] feat: 修改openapi ES配置 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11516)
- [新增] feat: 一键配置蓝鲸网关对接openapi服务 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12142)
- [新增] feat：【openapi】支持启用/禁用流水线 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12129)
- [新增] sql doc 文档更新 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9974)
- [新增] feat: 支持服务间jwt验证 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12067)

#### 优化

##### 流水线
- [优化] pref:迁移项目数据逻辑优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12117)
- [优化] pref：制品质量展示优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12191)

##### 权限中心
- [优化] pref：获取部门信息接口优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12190)

##### 项目管理
- [优化] pref：创建/更新项目流程优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12152)

##### 其他
- [优化] Bug: 第三方构建机启用 Docker ，镜像拉取策略默认值为空 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12198)
- [优化] pref:metrics服务下项目的插件信息录入优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12139)
- [优化] pref: 消息队列配置优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11878)

#### 修复

##### 代码库
- [修复] bug: 修复PAC流水线发布异常 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12168)
- [修复] bug: 调整build接口获取oauth信息检验逻辑 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12160)

##### 研发商店
- [修复] fix：添加插件到流水线时默认的插件大版本有误问题修复 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12181)

##### Agent
- [修复] bugfix: Agent升级相关bug修复 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12157)

