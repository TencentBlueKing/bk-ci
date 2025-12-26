<!-- BEGIN MUNGE: GENERATED_TOC -->
- [v3.0.15](#v3015)
   - [Changelog since v3.0.14](#changelog-since-v3014)

- [v3.0.14](#v3014)
   - [Changelog since v3.0.13](#changelog-since-v3013)

- [v3.0.13](#v3013)
   - [Changelog since v3.0.12](#changelog-since-v3012)

- [v3.0.12](#v3012)
   - [Changelog since v3.0.11](#changelog-since-v3011)

- [v3.0.11](#v3011)
   - [Changelog since v3.0.0](#changelog-since-v300)
- [v3.0.1-v3.0.10]
   - 因镜像版本与仓库版本没有统一,v3.0.1-v3.0.10已有镜像版本,但没有仓库版本,所以仓库这些版本直接跳过
- [v3.0.0](#v300)
  - [Changelog since v2.1.0](#changelog-since-v210)
- [v3.0.0-rc.1](#v300-rc1)
  - [Changelog since v2.1.0](#changelog-since-v210)

<!-- END MUNGE: GENERATED_TOC -->



<!-- NEW RELEASE NOTES ENTRY -->
# v3.0.15
## 2025-08-08
### Changelog since v3.0.14
#### 新增

##### 未分类
- [新增] feat: Maven仓库发布从oss迁移到central [链接](http://github.com/TencentBlueKing/bk-ci/issues/11817)
- [新增] feat: 支持服务间jwt验证 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12067)
- [新增] feat: 去掉proxy跨网络区域的代码 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12091)
- [新增] feat: 升级turbo版本 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12055)

# v3.0.14
## 2025-07-11
### Changelog since v3.0.13
#### 修复

##### 未分类
- [修复] bug: 用户态接口增加项目访问权限校验 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11971)

# v3.0.14
## 2025-07-11
### Changelog since v3.0.13
#### 修复

##### 未分类
- [修复] bug: 用户态接口增加项目访问权限校验 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11971)

# v3.0.13
## 2025-02-12
### Changelog since v3.0.12
#### 新增

##### 未分类
- [新增] feat: 需要修复的chart包问题 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11105)

# v3.0.12
## 2025-01-08
### Changelog since v3.0.11
#### 修复

##### 未分类
- [修复] bug: 修复v3.0版本打helm chart包时报错 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11391)

# v3.0.11
## 2024-12-05
### Changelog since v3.0.0
#### 新增

##### 未分类
- [新增] feat: 调整helm的镜像使其支持配置imageRegistry [链接](http://github.com/TencentBlueKing/bk-ci/issues/11171)
- [新增] feat：依赖的服务未部署时的交互优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10612)
- [新增] feat：支持查看版本日志 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10938)
- [新增] feat: 蓝鲸7.2版本的改动 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10558)
- [新增] 产品的顶栏下拉框样式和内容统一规范 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10939)
- [新增] feat: 把docker build插件的config ns配置给去掉 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10926)
- [新增] feat: 新启动的POD需要热身 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10887)
- [新增] feat: 让worker支持在JDK17中运行 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10412)

#### 修复

##### 流水线
- [修复] 【蓝盾-评审会已评审】【PAC】feat：新建/编辑流水线支持以 Code 方式编排流水线 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8125)

##### 权限中心
- [修复] bug: 权限管理-权限续期数据同步 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11271)

##### 未分类
- [修复] fix UnreachableCode [链接](http://github.com/TencentBlueKing/bk-ci/issues/11172)

# v3.0.0
## 2024-09-10
### Changelog since v2.1.0
#### 新增
##### 流水线
- pipeline as code
  - [新增] feat：草稿版本UI展示 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9861)
  - [新增] 流水线版本管理机制 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8161)
  - [新增]【PAC】feat：开启PAC模式的代码库支持自动同步代码库YAML变更到蓝盾 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8130)
  - [新增] pac ui编辑流水线 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8125)
  - [新增] Code 方式创建的流水线，变量面板-输出变量未获取到问题优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10755)
  - [新增] 新建/编辑流水线时支持调试流水线 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8164)
  - [新增] 上下文使用范围限定 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10655)
  - [新增] 【PAC】feat：流水线常量 Code 语法和规范 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9971)
  - [新增] 发布流水线页面「静态」流水线组优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9962)
  - [新增] 动态流水线组支持根据代码库/.ci下的一级目录进行条件分组 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9682)
  - [新增] 【PAC】feat：支持code 方式禁用流水线 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9788)
  - [新增] 流水线维护过程中记录操作日志 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8197)
  - [新增] 【PAC】跨项目复用构建资源池，支持Code配置 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10225)
  - [新增] 【PAC】feat：自定义构建号格式支持 Code 方式定义 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10210)
  - [新增] 编辑变量交互优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9652)
  - [新增] 流水线构建详情页支持一键展开/收起 job [链接](http://github.com/TencentBlueKing/bk-ci/issues/9775)
  - [新增] 支持蓝盾新表达式运行条件 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10467)
  - [新增] 发布流水线页面，PAC模式增加说明 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10482)
  - [新增] [PAC] code互转对api用户的影响 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9813)
  - [新增] 调试记录提示和入口优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10720)
  - [新增] 流水线变量支持手动拖拽调整顺序 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10458)
  - [新增] 流水线备注支持 上下文方式 设置和引用 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10459)
  - [新增] 拉取构件支持流水线调试模式 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10291)
  - [新增] 【PAC】feat：查看流水线 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8195)
- [新增] 支持流水线指标监控 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9860)
- [新增] 流水线权限代持功能重构 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10356)
  - [新增] 增加权限代持人变量 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10890)
- [新增] 流水线模板设置优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10857)
- [新增] 流水线执行历史支持根据触发人筛选 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10752)
- [新增] 流水线通知方式未生效时的交互优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10615)
- [新增] 工蜂MR触发器支持设置监听的action [链接](http://github.com/TencentBlueKing/bk-ci/issues/8949)
- [新增] MR 事件触发器支持 WIP [链接](http://github.com/TencentBlueKing/bk-ci/issues/10683)
- [新增] P4触发器支持 Code 编写 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10551)
- [新增] Git事件触发器自定义触发条件支持通过 Code 方式定义 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10497)
- [新增] 流水线日志颜色优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9934)
- [新增] openapi 触发流水线运行时，支持传入触发材料 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10302)
- [新增] 日志需要展示特殊字符 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10097)
- [新增] 流水线重命名优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10399)
- [新增] SVN事件触发的路径匹配规则增加兜底逻辑 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10510)
- [新增] 流水线执行历史列表增加「执行耗时」字段 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10251)
- [新增] 【蓝盾-产品-已评审】流水线支持展示运行进度 [链接](http://github.com/TencentBlueKing/bk-ci/issues/7932)
- [新增] 构建历史列表支持展示构建信息字段 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10724)
- [新增] 流水线支持POJO 属性按顺序导出 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10728)
- [新增] 流水线“文件”类型的变量优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10400)
- [新增] 定时触发器支持指定代码库和分支 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10300)
- [新增] 流水线模板管理编辑和实例管理优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10626)
- [新增] 保存流水线时校验引用到的子流水线权限 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10259)
- [新增] 流水线引擎动态配置管理 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10647)
- [新增] 支持在父流水线中查看异步执行的子流水线的状态 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10260)
- [新增] 新增下拉/复选类型变量时，预定义的选项支持批量输入跟输入key [链接](http://github.com/TencentBlueKing/bk-ci/issues/10290)
- [新增] 补全内置变量列表 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10436)
- [新增] 流水线构建详情页，每个 job/step 上的耗时直接显示 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10311)
- [新增] 回收站支持流水线名词搜索 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10408)
- [新增] 流水线列表最近执行展示内容优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10600)
- [新增] 制品下载无反应问题 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10555)
- [新增] 子流水线调用插件参数传递方式优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9943)
- [新增] 流水线设置查看页面并发分组配置缺失问题fix [链接](http://github.com/TencentBlueKing/bk-ci/issues/10516)
- [新增] 日志复制出来的空格异常 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10540)
- [新增] 流水线版本描述，增加长度限制 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10520)
- [新增] 构建详情页面，版本号hover可以展示对应的版本描述 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10524)
##### 代码库
- [新增] 关联工蜂代码库时，支持开启 Pipeline as Code 模式 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8115)
- [新增] 代码库优化一期功能点 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9347)
- [新增] github pr检查输出质量红线报告 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10607)
- [新增] 【openapi】关联代码库到蓝盾的api支持开启 PAC [链接](http://github.com/TencentBlueKing/bk-ci/issues/10770)
- [新增] 已开启 PAC 模式的代码库，支持关闭 PAC [链接](http://github.com/TencentBlueKing/bk-ci/issues/9993)
- [新增] 代码库触发事件结果展示优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10307)
- [新增] github check run应该支持GONGFENGSCAN渠道的流水线 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10704)
##### 质量红线
- [新增] 流水线中有多个CodeCC插件时，质量红线跳转链接要能跳转到相应任务 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10605)
- [新增] quality新增matchRuleList的app接口 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10610)
##### 环境管理
- [新增] 构建环境中的节点，支持停用/启用 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10258)
- [新增] 第三方构建机上下线记录清理 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10237)
- [新增] 装WINDOWS构建机,且点击install.bat完成安装,刷新节点没有显示 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10725)
- [新增] 支持批量安装 Agent [链接](http://github.com/TencentBlueKing/bk-ci/issues/10024)
##### 权限中心
- [新增] 支持管理员查看项目成员 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9620)
- [新增] 用户组相关接口优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10463)
- [新增] 根据组织ID拉取用户列表 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10513)
- [新增] 申请权限页面优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10145)
##### 项目管理
- [新增] 项目查看页面运营产品未显示名称问题优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10668)
- [新增] 新增项目级事件回调 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10146)
##### 研发商店
- [新增] 支持插件开发者设置默认的超时时间和默认的失败时的策略 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10019)
- [新增] 新增修改研发商店组件初始化项目的接口 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10126)
- [新增] 插件上传文件失败时重试 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10214)
- [新增] 研发商店-工作台-容器镜像,验证失败时的状态icon错位 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10696)
- [新增] 修复更新组件关联初始化项目信息时未删除关联的调试项目信息 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10621)
- [新增] 整合微拓展资源调度能力 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10122)
##### 日志服务
- [新增] Log的Service接口补充subtag 查询条件 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10536)
##### 调度
- [新增] 优化dockerhost dockerRun容器日志获取接口 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10811)
- [新增] kubernetes-manager 支持docker inspect image [链接](http://github.com/TencentBlueKing/bk-ci/issues/8862)
- [新增] 构建环境Agent并发上限为0不生效 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10740)
- [新增] 构建资源类型为第三方构建集群时支持指定Job并发数 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9810)
- [新增] 调整dockerhost默认容器超时时间 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10645)
- [新增] 第三方构建机构建资源锁定策略优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10449)
- [新增] 获取job执行最大并发/项目活跃用户度量数据 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10232)
##### Agent
- [新增] Worker杀掉当前进程父进程导致Agent误报 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10362)
- [新增] Agent启动时对相同Id不同IP的重复安装做告警 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10264)
- [新增] Agent清理进程为worker兜底 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10234)
##### Stream
- [新增] [stream] 优化大仓触发耗时 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10861)
- [新增] [stream] 优化触发流程，减少触发时长 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10753)
- [新增] stream开启CI时，必填组织架构和运营产品 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10231)
- [新增] [stream]新增获取组成员 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10711)
##### 网关
- [新增] 网关在auth_request时可以处理302的异常跳转 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10295)
- [新增] 网关默认tag不写死 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10334)
##### 其他
- [新增] 压缩http返回json串 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10323)
- [新增] 蓝鲸7.2版本的改动 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10558)
- [新增] sql doc 文档更新 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9974)
- [新增] bk-apigw接口认证方式调整 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10802)
- [新增] 修复swagger的扫包方式 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10806)
- [新增] 全局配置title/footer/logo/favicon/产品名称 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10678)
- [新增] 蓝盾网关信任安全域名的cors-header [链接](http://github.com/TencentBlueKing/bk-ci/issues/10767)
- [新增] 修复iam初始化脚本 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10658)
- [新增] openapi 访问无权限时新增文案 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10638)
- [新增] 依赖的服务未部署时的交互优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10612)
- [新增] 提高滚动发布的速度 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10236)
- [新增] 优化审计相关逻辑 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10671)
- [新增] 优化open接口切面校验 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10426)

#### 优化
##### 流水线
- [优化] 流水线执行历史表格优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10769)
- [优化] 流水线实例复制功能没有复制相应实例的参数值 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10580)
- [优化] 表达式解析器增加对流水线变量处理的兼容 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10609)
- [优化] 禁用流水线功能优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8190)
- [优化] UI 方式下新增/编辑变量页面改版 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8185)
- [优化] 插件执行错误码优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10326)
##### 环境管理
- [优化] 环境管理添加部分错误码 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10788)
- [优化] 环境管理部分代码优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10641)
- [优化] er:环境管理部分代码优化2 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10263)
##### 研发商店
- [优化] 支持java插件target引用变量来设置jar包执行路径 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10643)
- [优化] 研发商店敏感接口权限校验优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10418)
- [优化] 研发商店插件运行支持通过task.json中的execution.target字段指定运行参数 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10072)
- [优化] 研发商店通用化接口封装 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10123)
- [优化] 研发商店logo上传暂不支持svg图片，防止xss攻击 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10374)
##### Agent
- [修复] windwos启动构建进程时偶现142问题 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10179)
##### 其他
- [优化] 获取db集群名称方法支持db集群列表实现可配置化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10372)

#### 修复
##### 流水线
- [修复] 修正取消正在运行中构建可能产生的慢逻辑 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10874)
- [修复] 人工审核未勾选通知方式不应进行通知 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10183)
- [修复] 触发时前端手动跳过的矩阵依然运行 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10751)
- [修复] 新构建详情页插件渲染问题修复 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9185)
- [修复] git事件触发插件支持第三方服务changeFiles值总是为null [链接](http://github.com/TencentBlueKing/bk-ci/issues/10255)
- [修复] 构建历史接口的调试记录查询问题 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10814)
- [修复] 流水线触发器配置查看时可编辑 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10827)
- [修复] 文件类型变量问题修复 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10822)
- [修复] 流水线Job异步开机后随即用户取消流水线，异步开机异常导致流水线状态刷新异常 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10816)
- [修复] 为job分配多个容器并发执行业务逻辑会导致构建取消 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10517)
- [修复] 归档构件的制品页,显示有误,路径不完整,缺少文件大小 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10667)
- [修复] 修复矩阵code校验时存在的并发问题 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10771)
- [修复] stream 流水线MR触发时分支变量值有误 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10707)
- [修复] 有时候取消final stage后，构建未彻底结束 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10619)
- [修复] 归档报告插件创建token没有实现 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10693)
- [修复] 合作版工蜂force push触发流水线失败 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10680)
- [修复] 保存流水线模板权限问题 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10681)
- [修复] 忽略工蜂webhook测试请求 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10666)
- [修复] 流水线删除后，执行中的任务没终止 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8483)
- [修复] 新详情页的部分展示问题 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10557)
- [修复] 前端detail接口中返回草稿版本有误 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10545)
- [修复] 前序取消状态导致finally stage结束异常 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10533)
- [修复] 删除流水线接口异常 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10542)
- [修复] 新详情页显示问题修复 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10395)
- [修复] 解决stage审核参数值类型不一致问题 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10095)
- [修复] 回收站搜索不可用 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8440)
- [修复] 子流水线插件执行超时，但是没有把子流水线停掉 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10331)
- [修复] 流水线版本保存记录未及时清理 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10244)
- [修复] 变量只读导致无法重写 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10245)
##### 代码库
- [修复] 关联代码库已关联pac的项目名关闭弹框后未清空 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8146)
##### 项目管理
- [修复] 开源社区，项目管理界面 开源版权限需放开 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10382)
- [修复] 社区版simple权限中心前端应该隐藏最大授权范围 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10040)
- [修复] 项目最大可授权范围 序列化对比问题 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10649)
- [修复] 禁用项目不应该统计用户数 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10634)
- [修复] 修复CodeCC平台灰度标签设置不正确 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10434)
##### 研发商店
- [修复] 研发商店应用首个版本处于测试中，查询接口按实例ID查询不到测试中的应用版本 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10691)
- [修复] 调低SampleFirstStoreHostDecorateImpl的优先级配置 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10401)
- [修复] [社区]上架失败&流水线执行页面白屏问题[v2.1.0+] [链接](http://github.com/TencentBlueKing/bk-ci/issues/10357)
- [修复] 研发商店通用接口国际化配置调整 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10640)
- [修复] 开源版插件升级版本未刷新LATEST_TEST_FLAG标识状态 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10701)
##### 调度
- [修复] 无编译环境构建机执行带审核插件的矩阵job问题 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10599)
- [修复] 重试重新调度导致复用无法解锁 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10675)
##### Agent
- [修复] 修复arm64mac进程无法清理的问题 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10252)
- [修复] Agent复用在流水线重试的场景下存在问题 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10877)
- [修复] agent没有区域信息时默认没有bkrepo的网关 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10778)
- [修复] Agent复用同级节点时跳过了复用锁 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10795)
- [修复] Agent复用时取消后不能退出队列 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10589)
##### 其他
- [修复] 2.1版本process服务启动失败 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10271)
- [修复] 同步差异代码 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10319)
- [修复] 修复npm依赖漏洞 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10604)

# v3.0.0-rc.1
## 2024-09-10
### Changelog since v2.1.0
#### 新增
##### 流水线
- pipeline as code
  - [新增] feat：草稿版本UI展示 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9861)
  - [新增] 流水线版本管理机制 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8161)
  - [新增]【PAC】feat：开启PAC模式的代码库支持自动同步代码库YAML变更到蓝盾 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8130)
  - [新增] pac ui编辑流水线 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8125)
  - [新增] Code 方式创建的流水线，变量面板-输出变量未获取到问题优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10755)
  - [新增] 新建/编辑流水线时支持调试流水线 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8164)
  - [新增] 上下文使用范围限定 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10655)
  - [新增] 【PAC】feat：流水线常量 Code 语法和规范 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9971)
  - [新增] 发布流水线页面「静态」流水线组优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9962)
  - [新增] 动态流水线组支持根据代码库/.ci下的一级目录进行条件分组 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9682)
  - [新增] 【PAC】feat：支持code 方式禁用流水线 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9788)
  - [新增] 流水线维护过程中记录操作日志 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8197)
  - [新增] 【PAC】跨项目复用构建资源池，支持Code配置 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10225)
  - [新增] 【PAC】feat：自定义构建号格式支持 Code 方式定义 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10210)
  - [新增] 编辑变量交互优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9652)
  - [新增] 流水线构建详情页支持一键展开/收起 job [链接](http://github.com/TencentBlueKing/bk-ci/issues/9775)
  - [新增] 支持蓝盾新表达式运行条件 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10467)
  - [新增] 发布流水线页面，PAC模式增加说明 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10482)
  - [新增] [PAC] code互转对api用户的影响 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9813)
  - [新增] 调试记录提示和入口优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10720)
  - [新增] 流水线变量支持手动拖拽调整顺序 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10458)
  - [新增] 流水线备注支持 上下文方式 设置和引用 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10459)
  - [新增] 拉取构件支持流水线调试模式 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10291)
  - [新增] 【PAC】feat：查看流水线 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8195)
- [新增] 支持流水线指标监控 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9860)
- [新增] 流水线权限代持功能重构 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10356)
  - [新增] 增加权限代持人变量 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10890)
- [新增] 流水线模板设置优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10857)
- [新增] 流水线执行历史支持根据触发人筛选 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10752)
- [新增] 流水线通知方式未生效时的交互优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10615)
- [新增] 工蜂MR触发器支持设置监听的action [链接](http://github.com/TencentBlueKing/bk-ci/issues/8949)
- [新增] MR 事件触发器支持 WIP [链接](http://github.com/TencentBlueKing/bk-ci/issues/10683)
- [新增] P4触发器支持 Code 编写 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10551)
- [新增] Git事件触发器自定义触发条件支持通过 Code 方式定义 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10497)
- [新增] 流水线日志颜色优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9934)
- [新增] openapi 触发流水线运行时，支持传入触发材料 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10302)
- [新增] 日志需要展示特殊字符 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10097)
- [新增] 流水线重命名优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10399)
- [新增] SVN事件触发的路径匹配规则增加兜底逻辑 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10510)
- [新增] 流水线执行历史列表增加「执行耗时」字段 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10251)
- [新增] 【蓝盾-产品-已评审】流水线支持展示运行进度 [链接](http://github.com/TencentBlueKing/bk-ci/issues/7932)
- [新增] 构建历史列表支持展示构建信息字段 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10724)
- [新增] 流水线支持POJO 属性按顺序导出 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10728)
- [新增] 流水线“文件”类型的变量优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10400)
- [新增] 定时触发器支持指定代码库和分支 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10300)
- [新增] 流水线模板管理编辑和实例管理优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10626)
- [新增] 保存流水线时校验引用到的子流水线权限 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10259)
- [新增] 流水线引擎动态配置管理 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10647)
- [新增] 支持在父流水线中查看异步执行的子流水线的状态 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10260)
- [新增] 新增下拉/复选类型变量时，预定义的选项支持批量输入跟输入key [链接](http://github.com/TencentBlueKing/bk-ci/issues/10290)
- [新增] 补全内置变量列表 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10436)
- [新增] 流水线构建详情页，每个 job/step 上的耗时直接显示 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10311)
- [新增] 回收站支持流水线名词搜索 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10408)
- [新增] 流水线列表最近执行展示内容优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10600)
- [新增] 制品下载无反应问题 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10555)
- [新增] 子流水线调用插件参数传递方式优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9943)
- [新增] 流水线设置查看页面并发分组配置缺失问题fix [链接](http://github.com/TencentBlueKing/bk-ci/issues/10516)
- [新增] 日志复制出来的空格异常 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10540)
- [新增] 流水线版本描述，增加长度限制 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10520)
- [新增] 构建详情页面，版本号hover可以展示对应的版本描述 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10524)
##### 代码库
- [新增] 关联工蜂代码库时，支持开启 Pipeline as Code 模式 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8115)
- [新增] 代码库优化一期功能点 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9347)
- [新增] github pr检查输出质量红线报告 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10607)
- [新增] 【openapi】关联代码库到蓝盾的api支持开启 PAC [链接](http://github.com/TencentBlueKing/bk-ci/issues/10770)
- [新增] 已开启 PAC 模式的代码库，支持关闭 PAC [链接](http://github.com/TencentBlueKing/bk-ci/issues/9993)
- [新增] 代码库触发事件结果展示优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10307)
- [新增] github check run应该支持GONGFENGSCAN渠道的流水线 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10704)
##### 质量红线
- [新增] 流水线中有多个CodeCC插件时，质量红线跳转链接要能跳转到相应任务 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10605)
- [新增] quality新增matchRuleList的app接口 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10610)
##### 环境管理
- [新增] 构建环境中的节点，支持停用/启用 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10258)
- [新增] 第三方构建机上下线记录清理 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10237)
- [新增] 装WINDOWS构建机,且点击install.bat完成安装,刷新节点没有显示 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10725)
- [新增] 支持批量安装 Agent [链接](http://github.com/TencentBlueKing/bk-ci/issues/10024)
##### 权限中心
- [新增] 支持管理员查看项目成员 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9620)
- [新增] 用户组相关接口优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10463)
- [新增] 根据组织ID拉取用户列表 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10513)
- [新增] 申请权限页面优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10145)
##### 项目管理
- [新增] 项目查看页面运营产品未显示名称问题优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10668)
- [新增] 新增项目级事件回调 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10146)
##### 研发商店
- [新增] 支持插件开发者设置默认的超时时间和默认的失败时的策略 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10019)
- [新增] 新增修改研发商店组件初始化项目的接口 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10126)
- [新增] 插件上传文件失败时重试 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10214)
- [新增] 研发商店-工作台-容器镜像,验证失败时的状态icon错位 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10696)
- [新增] 修复更新组件关联初始化项目信息时未删除关联的调试项目信息 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10621)
- [新增] 整合微拓展资源调度能力 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10122)
##### 日志服务
- [新增] Log的Service接口补充subtag 查询条件 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10536)
##### 调度
- [新增] 优化dockerhost dockerRun容器日志获取接口 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10811)
- [新增] kubernetes-manager 支持docker inspect image [链接](http://github.com/TencentBlueKing/bk-ci/issues/8862)
- [新增] 构建环境Agent并发上限为0不生效 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10740)
- [新增] 构建资源类型为第三方构建集群时支持指定Job并发数 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9810)
- [新增] 调整dockerhost默认容器超时时间 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10645)
- [新增] 第三方构建机构建资源锁定策略优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10449)
- [新增] 获取job执行最大并发/项目活跃用户度量数据 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10232)
##### Agent
- [新增] Worker杀掉当前进程父进程导致Agent误报 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10362)
- [新增] Agent启动时对相同Id不同IP的重复安装做告警 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10264)
- [新增] Agent清理进程为worker兜底 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10234)
##### Stream
- [新增] [stream] 优化大仓触发耗时 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10861)
- [新增] [stream] 优化触发流程，减少触发时长 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10753)
- [新增] stream开启CI时，必填组织架构和运营产品 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10231)
- [新增] [stream]新增获取组成员 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10711)
##### 网关
- [新增] 网关在auth_request时可以处理302的异常跳转 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10295)
- [新增] 网关默认tag不写死 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10334)
##### 其他
- [新增] 压缩http返回json串 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10323)
- [新增] 蓝鲸7.2版本的改动 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10558)
- [新增] sql doc 文档更新 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9974)
- [新增] bk-apigw接口认证方式调整 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10802)
- [新增] 修复swagger的扫包方式 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10806)
- [新增] 全局配置title/footer/logo/favicon/产品名称 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10678)
- [新增] 蓝盾网关信任安全域名的cors-header [链接](http://github.com/TencentBlueKing/bk-ci/issues/10767)
- [新增] 修复iam初始化脚本 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10658)
- [新增] openapi 访问无权限时新增文案 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10638)
- [新增] 依赖的服务未部署时的交互优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10612)
- [新增] 提高滚动发布的速度 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10236)
- [新增] 优化审计相关逻辑 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10671)
- [新增] 优化open接口切面校验 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10426)

#### 优化
##### 流水线
- [优化] 流水线执行历史表格优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10769)
- [优化] 流水线实例复制功能没有复制相应实例的参数值 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10580)
- [优化] 表达式解析器增加对流水线变量处理的兼容 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10609)
- [优化] 禁用流水线功能优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8190)
- [优化] UI 方式下新增/编辑变量页面改版 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8185)
- [优化] 插件执行错误码优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10326)
##### 环境管理
- [优化] 环境管理添加部分错误码 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10788)
- [优化] 环境管理部分代码优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10641)
- [优化] er:环境管理部分代码优化2 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10263)
##### 研发商店
- [优化] 支持java插件target引用变量来设置jar包执行路径 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10643)
- [优化] 研发商店敏感接口权限校验优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10418)
- [优化] 研发商店插件运行支持通过task.json中的execution.target字段指定运行参数 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10072)
- [优化] 研发商店通用化接口封装 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10123)
- [优化] 研发商店logo上传暂不支持svg图片，防止xss攻击 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10374)
##### Agent
- [修复] windwos启动构建进程时偶现142问题 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10179)
##### 其他
- [优化] 获取db集群名称方法支持db集群列表实现可配置化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10372)

#### 修复
##### 流水线
- [修复] 修正取消正在运行中构建可能产生的慢逻辑 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10874)
- [修复] 人工审核未勾选通知方式不应进行通知 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10183)
- [修复] 触发时前端手动跳过的矩阵依然运行 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10751)
- [修复] 新构建详情页插件渲染问题修复 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9185)
- [修复] git事件触发插件支持第三方服务changeFiles值总是为null [链接](http://github.com/TencentBlueKing/bk-ci/issues/10255)
- [修复] 构建历史接口的调试记录查询问题 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10814)
- [修复] 流水线触发器配置查看时可编辑 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10827)
- [修复] 文件类型变量问题修复 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10822)
- [修复] 流水线Job异步开机后随即用户取消流水线，异步开机异常导致流水线状态刷新异常 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10816)
- [修复] 为job分配多个容器并发执行业务逻辑会导致构建取消 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10517)
- [修复] 归档构件的制品页,显示有误,路径不完整,缺少文件大小 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10667)
- [修复] 修复矩阵code校验时存在的并发问题 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10771)
- [修复] stream 流水线MR触发时分支变量值有误 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10707)
- [修复] 有时候取消final stage后，构建未彻底结束 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10619)
- [修复] 归档报告插件创建token没有实现 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10693)
- [修复] 合作版工蜂force push触发流水线失败 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10680)
- [修复] 保存流水线模板权限问题 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10681)
- [修复] 忽略工蜂webhook测试请求 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10666)
- [修复] 流水线删除后，执行中的任务没终止 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8483)
- [修复] 新详情页的部分展示问题 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10557)
- [修复] 前端detail接口中返回草稿版本有误 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10545)
- [修复] 前序取消状态导致finally stage结束异常 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10533)
- [修复] 删除流水线接口异常 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10542)
- [修复] 新详情页显示问题修复 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10395)
- [修复] 解决stage审核参数值类型不一致问题 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10095)
- [修复] 回收站搜索不可用 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8440)
- [修复] 子流水线插件执行超时，但是没有把子流水线停掉 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10331)
- [修复] 流水线版本保存记录未及时清理 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10244)
- [修复] 变量只读导致无法重写 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10245)
##### 代码库
- [修复] 关联代码库已关联pac的项目名关闭弹框后未清空 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8146)
##### 项目管理
- [修复] 开源社区，项目管理界面 开源版权限需放开 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10382)
- [修复] 社区版simple权限中心前端应该隐藏最大授权范围 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10040)
- [修复] 项目最大可授权范围 序列化对比问题 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10649)
- [修复] 禁用项目不应该统计用户数 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10634)
- [修复] 修复CodeCC平台灰度标签设置不正确 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10434)
##### 研发商店
- [修复] 研发商店应用首个版本处于测试中，查询接口按实例ID查询不到测试中的应用版本 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10691)
- [修复] 调低SampleFirstStoreHostDecorateImpl的优先级配置 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10401)
- [修复] [社区]上架失败&流水线执行页面白屏问题[v2.1.0+] [链接](http://github.com/TencentBlueKing/bk-ci/issues/10357)
- [修复] 研发商店通用接口国际化配置调整 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10640)
- [修复] 开源版插件升级版本未刷新LATEST_TEST_FLAG标识状态 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10701)
##### 调度
- [修复] 无编译环境构建机执行带审核插件的矩阵job问题 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10599)
- [修复] 重试重新调度导致复用无法解锁 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10675)
##### Agent
- [修复] 修复arm64mac进程无法清理的问题 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10252)
- [修复] Agent复用在流水线重试的场景下存在问题 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10877)
- [修复] agent没有区域信息时默认没有bkrepo的网关 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10778)
- [修复] Agent复用同级节点时跳过了复用锁 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10795)
- [修复] Agent复用时取消后不能退出队列 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10589)
##### 其他
- [修复] 2.1版本process服务启动失败 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10271)
- [修复] 同步差异代码 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10319)
- [修复] 修复npm依赖漏洞 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10604)
