<!-- BEGIN MUNGE: GENERATED_TOC -->
- [v1.13.0-rc.4](#v1130-rc4)
   - [Changelog since v1.13.0-rc.3](#changelog-since-v1130-rc3)

- [v1.13.0-rc.2](#v1130-rc2)
   - [Changelog since v1.12.0-rc.8](#changelog-since-v1120-rc8)

<!-- END MUNGE: GENERATED_TOC -->



<!-- NEW RELEASE NOTES ENTRY -->
# v1.13.0-rc.4
## Changelog since v1.13.0-rc.3
#### 新增
- [新增] 新增判断是否是项目成员user态接口 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9569)
- [新增] 新增获取部门员工信息接口 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9570)
- [新增] 支持开通蓝盾项目权限的同时开通对应的监控空间权限 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8935)
- [新增] 增加获取项目信息及成员信息接口 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9392)
- [新增] 接入审计中心 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9414)
- [新增] fit-sec线权限升级支持 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9521)
- [新增] 优化Env模块的日志打印逻辑 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9470)
- [新增] user类接口传递网关token [链接](http://github.com/TencentBlueKing/bk-ci/issues/9482)
- [新增] build_msg需要根据事件触发场景细化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8831)
- [新增] bkrepo客户端新增apk加固接口 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9423)
- [新增] 新增不用SQL检查的代码 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9346)

#### 优化
- [优化] 静态资源文件的url地址域名支持适配特定环境 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9504)
- [优化] metrics接口优化补充，调整项目下插件信息来源 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9488)
- [优化] 增加国际化初始化配置 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9413)

#### 修复
- [修复] 获取子流水线执行状态接口不需要做权限校验 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9552)
- [修复] 查看研发商店的模版时，若插件可见范围符合要求，不应该提示项目xxx不允许使用插件xxx [链接](http://github.com/TencentBlueKing/bk-ci/issues/9531)
- [修复] 增加开源版插件的task.json的packagePath字段的非空校验 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9509)
- [修复] 删除流水线后，未删除制品库中流水线的资源 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9491)
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
