<!-- BEGIN MUNGE: GENERATED_TOC -->
- [v3.2.0-rc.1](#v320-rc1)
   - [Changelog since v3.1.0](#changelog-since-v310)

<!-- END MUNGE: GENERATED_TOC -->



<!-- NEW RELEASE NOTES ENTRY -->
# v3.2.0-rc.1
## 2025-02-11
### Changelog since v3.1.0
#### 新增

##### 流水线
- [新增] feat：新增service接口用于获取流水线组下流水线 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11383)
- [新增] 插件配置支持字段间联动 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11251)

##### 权限中心
- [新增] pref：获取有权限的资源接口优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11246)

##### 未分类
- [新增] 第三方构建机Docker支持JDK17 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11421)
- [新增] sql doc 文档更新 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9974)

#### 优化

##### 日志服务
- [优化] pref: 日志模块ES存储性能优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/7091)

#### 修复

##### 流水线
- [修复] bug：更新模板实例导致 流水线模型丢失父模板ID问题 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11309)
- [修复] bug: 新增错误类型枚举导致前端解析异常 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11386)

##### 研发商店
- [修复] bug:组件latestFlag参数更新时组件所有版本修改者会全部更新为同一人 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11400)
- [修复] bug：多环境情况下流水线编辑页面插件logo缓存优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11380)

##### 权限中心
- [修复] pref：用户组续期相关逻辑优化 #11305 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11305)
- [修复] bug：修改分级管理员授权范围偶现不成功现象 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11334)

##### 未分类
- [修复] bug: 修复v3.0版本打helm chart包时报错 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11391)

