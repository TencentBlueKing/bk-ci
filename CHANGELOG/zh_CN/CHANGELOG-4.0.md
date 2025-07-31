<!-- BEGIN MUNGE: GENERATED_TOC -->
- [v4.0.0-rc.5](#v400-rc5)
   - [Changelog since v4.0.0-rc.4](#changelog-since-v400-rc4)

- [v4.0.0-rc.4](#v400-rc4)
   - [Changelog since v4.0.0-rc.3](#changelog-since-v400-rc3)

- [v4.0.0-rc.3](#v400-rc3)
   - [Changelog since v4.0.0-rc.2](#changelog-since-v400-rc2)

- [v4.0.0-rc.2](#v400-rc2)
   - [Changelog since v4.0.0-rc.1](#changelog-since-v400-rc1)

- [v4.0.0-rc.1](#v400-rc1)
   - [Changelog since v3.2.0](#changelog-since-v320)

<!-- END MUNGE: GENERATED_TOC -->



<!-- NEW RELEASE NOTES ENTRY -->
# v4.0.0-rc.5
## 2025-07-18
### Changelog since v4.0.0-rc.4
#### 新增

##### 流水线
- [新增] feat: 新增流水线监控事件 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11874)
- [新增] 插件输出时，支持 isSensitive 属性 [链接](http://github.com/TencentBlueKing/bk-ci/issues/5534)
- [新增] 插件配置支持字段间联动 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11251)
- [新增] feat: 调整子流水线接口参数名 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11979)
- [新增] feat: TGIT 事件触发需要支持review事件监听 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11827)
- [新增] feat: 人工审核插件获取参数支持矩阵 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11933)
- [新增] feat：代码库开启PAC时对应的流水线组名称取值策略修改 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11741)

##### 研发商店
- [新增] feat:研发商城插件通知管理员审核文案增加审核按钮链接 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11938)
- [新增] feat:增加组件创建后扩展处理 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11823)

##### 环境管理
- [新增] feat：第三方构建机节点支持标签管理 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11881)
- [新增] feat：导入第三方构建机时支持自动切换启动用户 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11945)

##### 权限中心
- [新增] feat：权限系统熔断设计 数据准确性校验 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11964)

##### 调度
- [新增] feat：无编译构建机认领任务新增容器状态二重校验 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11904)

##### Agent
- [新增] 私有构建机用 Docker 运行构建任务也能获取到自定义环境变量 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11955)
- [新增] 修复mac下的agent掉线重连长时间失效 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11918)

##### 未分类
- [新增] feat: 蓝盾国际化支持日语版 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11877)
- [新增] Tencent开源License更新 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11919)

#### 优化

##### 流水线
- [优化] perf：归档流水线时记录操作日志 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11922)
- [优化] perf：流水线治理优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11968)
- [优化] perf：流水线发布时描述是否必填优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11843)
- [优化] perf：和草稿版本对比时的展示优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11969)

##### 研发商店
- [优化] pref：插件运行时包文件下载超时时间优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11954)

##### 未分类
- [优化] pref:metrics服务数据上报接口优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12014)
- [优化] pref:调整metrics服务数据上报接口分布式锁超时时间 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12005)
- [优化] pref:获取制品库文件下载链接接口优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11999)
- [优化] pref:metrics构建数据上报接口在同一条流水线构建并发量大时可能出现线程阻塞情况 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11921)
- [优化] docs: 更新JDK制作文档 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11937)

#### 修复

##### 流水线
- [修复] fix：条件判断过程日志描述有误 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12008)
- [修复] mac公共构建机无法选择xcode16版本配置 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11980)
- [修复] bug: 流水线拥有者,无法导出流水线 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11959)

##### 研发商店
- [修复] bug: 内置插件的target参数需要支持可空 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11936)

##### 环境管理
- [修复] 环境管理节点功能右上角没有导入按钮,导致无法下载agent [链接](http://github.com/TencentBlueKing/bk-ci/issues/11950)

##### 未分类
- [修复] bug: publish-plugin插件不支持Nexus Pro 3 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11944)

# v4.0.0-rc.4
## 2025-07-04
### Changelog since v4.0.0-rc.3
#### 新增

##### 流水线
- [新增] feat: 子流水线调用插件在子流水线开启了PAC场景下支持指定分支 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11768)
- [新增] feat：支持UI界面归档流水线和管理 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10803)
- [新增] feat: PAC Code 检测流水线是否使用了命名空间 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11879)
- [新增] feat：新版定时触发器兼容旧版模版参数 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11803)
- [新增] feat: 优化流水线保存/发布时的子流水线循环依赖检测性能 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11753)
- [新增] feat: 检测stage审核参数与入参之间的不规范写法 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11853)
- [新增] 希望mac公共构建机Xcode版本选择框可以填写变量来控制 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11855)
- [新增] feat：系统内置上下文展示，缺少/job/step级别的变量 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10845)
- [新增] feat：流水线参数在执行预览界面的展示支持联动 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11438)
- [新增] feat: GIT事件触发支持输出TAG描述信息和TAPD单号 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11721)
- [新增] feat：stage审核/人工审核通知连接增加定位 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11704)
- [新增] feat：脚本插件支持用户设置错误码和错误信息 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11747)
- [新增] feat: 人工审核插件审核接口需要合理校验传参 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11770)
- [新增] feat: 流水线插件配置自动重试时日志展示超时失败日志 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11755)
- [新增] feat: 子流水线插件支持传递推荐版本号变量 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11684)

##### 研发商店
- [新增] feat：研发商店插件上架进入审核阶段时通知管理员审批 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11897)

##### 环境管理
- [新增] feat: 支持多参数获取第三方构建机相关接口 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11759)

##### 权限中心
- [新增] feat：支持管理员批量移出用户 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11200)
- [新增] feat：权限系统熔断设计 前置数据准备 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11789)

##### 项目管理
- [新增] feat: v4_app_project_list 接口支持渠道查询以及分页参数 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11751)

##### Agent
- [新增] Agent IP变化的更新机制优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11774)
- [新增] 构建进程结束晚于下一次构建重试 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11873)

##### 未分类
- [新增] 修复mac下的agent掉线重连长时间失效 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11918)
- [新增] Tencent开源License更新 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11919)
- [新增] feat:消息通知支持平台级别的「通讯黑名单」设置 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11885)
- [新增] feat: 希望openapi提供批量查询流水线构建任务的接口 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11889)
- [新增] feat: Maven仓库发布从oss迁移到central [链接](http://github.com/TencentBlueKing/bk-ci/issues/11817)

#### 优化

##### 流水线
- [优化] pref：支持变量分组 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11590)
- [优化] pref:流水线版本引用标识刷新接口优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11829)

##### 研发商店
- [优化] pref:研发商店组件版本号规范调整 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11780)

##### 权限中心
- [优化] pref：获取用户组成员接口优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11891)
- [优化] pref ：组成员同步优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11864)
- [优化] pref：无权限跳转申请优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11769)
- [优化] pref：用户管理接口优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11816)

##### 项目管理
- [优化] pref:db分片规则保存优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11811)

#### 修复

##### 流水线
- [修复] bug: PAC流水线提交失败原因细化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11740)
- [修复] bug: PAC模式下流水线删除后，流水线组还存在需要优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11742)
- [修复] bug: PAC创建流水线组时,初始化流水线组很慢 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11686)
- [修复] bug: 运行中重试时,重试插件的stage状态必须为运行中 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11802)
- [修复] bug: 定时触发任务无法正常移除 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11833)
- [修复] bug: 触发时兼容工蜂SVN仓库名 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11799)
- [修复] bug：流水线制品下载限制10Gb问题修复 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11793)

##### 代码库
- [修复] bug: 创建代码库并开启PAC,scmCode字段为空 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11820)

##### 研发商店
- [修复] bug:研发商店还未发布处于测试中的组件查询无需按可见范围过滤 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11863)
- [修复] bug: 插件unzip的时候要去掉根目录 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11825)
- [修复] bug:镜像市场首页按推荐使用查询失效 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11675)

##### 项目管理
- [修复] bug: 新增db分片规则时因快照读导致可能重复插入数据 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11860)

##### 未分类
- [修复] bug：解决记录用户操作数据导致的死锁问题 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11776)

# v4.0.0-rc.3
## 2025-06-10
### Changelog since v4.0.0-rc.2
#### 新增

##### 流水线
- [新增] feat: CODE支持SVN_TAG类型 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11781)

##### 未分类
- [新增] feat: 查询服务跳转增加流水线维度参数 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11765)

#### 优化

##### 流水线
- [优化] perf：流水线运行中、重试失败的步骤时提示队列满问题跟进和优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11807)

##### 日志服务
- [优化] perf: 日志模块数据清理未支持跨集群 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11814)

##### 项目管理
- [优化] pref:db分片规则保存优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11811)
- [优化] pref：启用禁用项目接口优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11763)

#### 修复

##### 流水线
- [修复] bug: 运行中重试时,重试插件的stage状态必须为运行中 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11802)
- [修复] bug: 修复当项目ID含有_时，定时任务不触发 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11800)

# v4.0.0-rc.2
## 2025-05-29
### Changelog since v4.0.0-rc.1
#### 新增

##### 流水线
- [新增] 获取项目下第三方构建机列表API增加返回主机名称 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11673)
- [新增] feat：定时触发器支持设置启动变量 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10617)
- [新增] feat: 增加表达式使用文档指引 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11723)
- [新增] feat：支持配置流水线变量超长时是否报错终止执行 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11592)
- [新增] feat：变量分组支持 Code 定义 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11698)
- [新增] feat: Github事件触发支持分支过滤 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11682)

##### Agent
- [新增] Agent依赖升级 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11599)
- [新增] Agent错误的异常抛出 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11573)

#### 优化

##### 代码库
- [优化] perf: 优化代码源webhook解析流程 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11694)

##### 研发商店
- [优化] pref:研发商店首页组件查询按可见范围过滤优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11676)

##### 权限中心
- [优化] pref：批量交接接口优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11725)
- [优化] pref：权限中心 open类接口优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11465)
- [优化] pref：用户管理相关接口优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11687)

##### 项目管理
- [优化] pref:db分片规则保存更新优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11732)

#### 修复

##### 流水线
- [修复] bug：被依赖的Job单步重试成功后未执行后续Job 问题 fix [链接](http://github.com/TencentBlueKing/bk-ci/issues/11412)
- [修复] 流水线模板实例化的时候复选框的默认值没有自动填上 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11761)
- [修复] bug：复制流水线页面，所属动态流水线组的逻辑有误 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11734)
- [修复] 子流水线调用插件-删除参数导致参数值变为默认值问题修复 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11709)
- [修复] 流水线组管理权限页面报错 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11714)
- [修复] bug:修复变量类型为复选框时一键复制不能点击问题 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11705)

##### 代码库
- [修复] bug: OAUTH授权界面无需校验平台管理权限 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11748)

##### Agent
- [修复] bugfix: 第三方构建机取消掉了重试的任务 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11268)

##### 未分类
- [修复] bug: ThreadPoolUtil的submitAction方法会不断创建线程池 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11702)

# v4.0.0-rc.1
## 2025-05-09
### Changelog since v3.2.0
#### 新增

##### 流水线
- [新增] feat：运行中重试的展示优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10483)
- [新增] feat：执行时显示具体资源支持通过Code设置 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11588)
- [新增] feat：流水线查看和构建详情查看配置界面敏感字段展示优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11019)
- [新增] 插件的报告功能可以默认显示最新报告 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11638)
- [新增] feat：子流水线调用插件入参类型为文本框时，前端应该为textarea组件 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11605)
- [新增] feat：matrix job 的 include/exclude  语法优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11519)
- [新增] feat：支持针对构建重放事件 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11232)
- [新增] feat：文件类型变量上传的文件支持版本管理 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11467)

##### 代码库
- [新增] perf: 重构代码库服务代码结构 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9952)
- [新增] feat：平台管理-代码源管理 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11379)
- [新增] feat: 代码源管理和代码库服务下关联代码库进行联动 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11498)

##### 研发商店
- [新增] 【研发商店】支持展示版本日志 [链接](http://github.com/TencentBlueKing/bk-ci/issues/1761)
- [新增] feat:研发商店组件支持一键发布 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11543)
- [新增] bua: 修复发布者信息同步分层根据组织名称获取ID时可能出现同名组织导致ID设置错误 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11643)

##### 环境管理
- [新增] feat: node_third_part_detail 接口获取正确的节点数据 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11607)
- [新增] 优化环境管理构建机Agent的定时维护任务 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11579)

##### 项目管理
- [新增] feat: project_list 接口增加product_id过滤条件 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11610)

##### Stream
- [新增] feat: 参照push触发增加stream跨库触发分支删除场景触发参数。 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11634)

##### 未分类
- [新增] sql doc 文档更新 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9974)
- [新增] feat：新增部分openapi [链接](http://github.com/TencentBlueKing/bk-ci/issues/11655)
- [新增] feat:【PAC模板】流水线模板支持PAC特性 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11414)
- [新增] feat: 升级JDK17 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10593)
- [新增] feat: 让mq在程序启动的时候正常初始化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11584)

#### 优化

##### 流水线
- [优化] pref：支持变量分组 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11590)
- [优化] pref: 日志归档下载链接支持域名自动切换 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11582)

##### 研发商店
- [优化] pref:完善研发商店组件配置文件参数校验 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11269)
- [优化] pref:研发商店组件包签名流程优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11572)
- [优化] pref:下载处于测试中状态的插件的执行包不从制品库缓存获取 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11615)

##### 权限中心
- [优化] pref:用户组加人接口优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11678)

#### 修复

##### 流水线
- [修复] bug: 模板发布时，需检查模板里的镜像是否已发布逻辑修复 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11600)
- [修复] bug: 回收站恢复的流水线名字会多一串数字需要优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11632)
- [修复] 编辑模板流水线，matchRuleList接口参数丢失问题 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11613)
- [修复] bug: 高峰期引擎打印的服务内的构建日志触发熔断 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11589)
- [修复] bug:流水线模板版本排序列表异常，最新更新版本未在最上方位置 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11495)

##### 研发商店
- [修复] bug:获取研发商店组件升级版本接口优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11669)
- [修复] bug: 修复商店首页隐藏应用查询 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11648)

##### 环境管理
- [修复] bug: 环境管理搜索CMDB节点返回为空 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11645)
