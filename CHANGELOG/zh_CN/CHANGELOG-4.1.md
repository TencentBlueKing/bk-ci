<!-- BEGIN MUNGE: GENERATED_TOC -->
- [v4.1.0-rc.2](#v410-rc2)
   - [Changelog since v4.1.0-rc.1](#changelog-since-v410-rc1)

- [v4.1.0-rc.1](#v410-rc1)
   - [Changelog since v4.0.0](#changelog-since-v400)

<!-- END MUNGE: GENERATED_TOC -->



<!-- NEW RELEASE NOTES ENTRY -->
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

