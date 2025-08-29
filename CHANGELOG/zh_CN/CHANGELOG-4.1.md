<!-- BEGIN MUNGE: GENERATED_TOC -->
- [v4.1.0-rc.1](#v410-rc1)
   - [Changelog since v4.0.0](#changelog-since-v400)

<!-- END MUNGE: GENERATED_TOC -->



<!-- NEW RELEASE NOTES ENTRY -->
# v4.1.0-rc.1
## 2025-08-29
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

