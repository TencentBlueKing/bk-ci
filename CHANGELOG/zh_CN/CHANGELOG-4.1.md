<!-- BEGIN MUNGE: GENERATED_TOC -->
- [v4.1.0-rc.3](#v410-rc3)
   - [Changelog since v4.1.0-rc.2](#changelog-since-v410-rc2)

- [v4.1.0-rc.2](#v410-rc2)
   - [Changelog since v4.1.0-rc.1](#changelog-since-v410-rc1)

- [v4.1.0-rc.1](#v410-rc1)
   - [Changelog since v4.0.0](#changelog-since-v400)

<!-- END MUNGE: GENERATED_TOC -->



<!-- NEW RELEASE NOTES ENTRY -->
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

