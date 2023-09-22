<!-- BEGIN MUNGE: GENERATED_TOC -->
- [v1.13.0-rc.2](#v1130-rc2)
   - [Changelog since v1.12.0-rc.8](#changelog-since-v1120-rc8)

<!-- END MUNGE: GENERATED_TOC -->



<!-- NEW RELEASE NOTES ENTRY -->
# v1.13.0-rc.2
## Changelog since v1.12.0-rc.8
#### 新增
- [新增] 申请加入用户组 itsm单据内容不详细问题 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9365)
- [新增] 离岸用户登录devx.tencent.com，项目列表和服务列表过滤 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9433)
- [新增] weCheckLicense先创建release文件夹 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9474)
- [新增] 新增v4_user_pipeline_paging_search_by_name接口分页查询根据流水线名称搜索 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9466)
- [新增] 日志组件跳转功能兼容 firefox 浏览器 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9465)
- [新增] [GoAgent]环境共享构建机优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9437)
- [新增] 新增tencent Git Oauth 授权模式 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8845)
- [新增] 流水线支持归档目录 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9320)
- [新增] 项目下的内置用户组，除了管理员组，其他组可删除 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9389)
- [新增] 权限入口优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9390)
- [新增] 迁移新版权限中心优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9306)
- [新增] 流水线产出物排序调整 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9259)

#### 优化
- [优化] 支持将分区库里的项目的数据迁移至指定数据库 TencentBlueKing [链接](http://github.com/TencentBlueKing/bk-ci/issues/9188)
- [优化] metrics部分接口性能优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9147)

#### 修复
- [修复] 接口返回对象缓存在内存时做国际化切换可能导致接口返回对象的国际化值不符合预期 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9445)
- [修复] 支持将分区库里的项目的数据迁移至指定数据库问题修复 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9464)
- [修复] openapi 判断是否项目成员没有根据项目路由 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9427)
- [修复] devcloud类型登录调试，窗口大小无法自适应 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9418)
- [修复] 获取红线拦截记录接口漏掉了拦截列表的数据 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9405)
- [修复] 修复权限迁移bug [链接](http://github.com/TencentBlueKing/bk-ci/issues/9400)
- [修复] 选择插件历史版本修复升级后再取消版本校验优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9380)
- [修复] 开源版插件包重新上传时也需重新解析国际化信息 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9326)
