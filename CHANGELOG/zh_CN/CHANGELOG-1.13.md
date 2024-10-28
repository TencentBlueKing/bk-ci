<!-- BEGIN MUNGE: GENERATED_TOC -->
- [v1.13.0](#v1130)
   - [Changelog since v1.12.0-rc.8](#changelog-since-v1120-rc8)
- [v1.13.0-rc.6](#v1130-rc6)
   - [Changelog since v1.13.0-rc.5](#changelog-since-v1130-rc5)
- [v1.13.0-rc.5](#v1130-rc5)
   - [Changelog since v1.13.0-rc.4](#changelog-since-v1130-rc4)
- [v1.13.0-rc.4](#v1130-rc4)
   - [Changelog since v1.13.0-rc.3](#changelog-since-v1130-rc3)
- [v1.13.0-rc.3](#v1130-rc3)
   - [Changelog since v1.13.0-rc.2](#changelog-since-v1130-rc2)
- [v1.13.0-rc.2](#v1130-rc2)
   - [Changelog since v1.12.0-rc.8](#changelog-since-v1120-rc8)

<!-- END MUNGE: GENERATED_TOC -->



<!-- NEW RELEASE NOTES ENTRY -->
# v1.13.0
## Changelog since v1.12.0-rc.8
#### 新增
- [新增] dispatch 支持上下文占位，动态赋值 [链接](http://github.com/TencentBlueKing/bk-ci/issues/6460)
- [新增] 项目支持关联运营产品和根据运营产品搜索 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9636)
- [新增] svn触发需要提供获取到触发路径的变量 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9402)
- [新增] openapi新增度量能力 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9638)
- [新增] 优化批量添加项目成员接口 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9660)
- [新增] 整合dispatch-docker, dispatch-kubernetes模块到dispatch [链接](http://github.com/TencentBlueKing/bk-ci/issues/9548)
- [新增] redis库拆分 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9621)
- [新增] 优化OP获取项目列表接口 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9666)
- [新增] 将dispatch-docker和dispatch-kubernetes统一为dispatch [链接](http://github.com/TencentBlueKing/bk-ci/issues/9658)
- [新增] undertow加上线程池监控 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9631)
- [新增] iOS重签名部分功能优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9537)
- [新增] 构建分组并发时优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9618)
- [新增] 最近使用流水线组新增删除逻辑 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9627)
- [新增] [bugfix] 默认prod集群router-tag判断有误 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9615)
- [新增] Image checkImageInspect接口优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9609)
- [新增] Revert "feat:希望支持分支进行上架测试 [链接](http://github.com/TencentBlueKing/bk-ci/issues/4780)
- [新增] 提供监控迁移service接口 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9592)
- [新增] github触发器事件补充 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9372)
- [新增] log的redisKey独立化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9599)
- [新增] redis分布式锁改造 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9499)
- [新增] 流水线插件安装包支持缓存，提高流水线执行速度 TencentBlueKing [链接](http://github.com/TencentBlueKing/bk-ci/issues/8940)
- [新增] 新增判断是否是项目成员user态接口 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9569)
- [新增] 新增获取部门员工信息接口 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9570)
- [新增] 支持开通蓝盾项目权限的同时开通对应的监控空间权限 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8935)
- [新增] 增加获取项目信息及成员信息接口 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9392)
- [新增] 接入审计中心 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9414)
- [新增] 上下文命令字支持设置备注 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9545)
- [新增] yaml 方式下，mr 触发器支持配置是否阻塞 mr 合并 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9412)
- [新增] auth服务异常信息规范 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9455)
- [新增] 研发商店插件的质量红线指标支持占位符 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9506)
- [新增] 日志组件文件名错误 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9502)
- [新增] 蓝盾权限-支持查询某单一资源的用户组人员名单 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9116)
- [新增] 修改日志请求地址 & 支持 devx 跳转路径 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9495)
- [新增] [stream] 构建结束默认消息通知内容有误 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9485)
- [新增] project的op接口支持查询remotedev类型 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9472)
- [新增] 优化Env模块的日志打印逻辑 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9470)
- [新增] user类接口传递网关token [链接](http://github.com/TencentBlueKing/bk-ci/issues/9482)
- [新增] 申请加入用户组 itsm单据内容不详细问题 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9365)
- [新增] 离岸用户登录devx.tencent.com，项目列表和服务列表过滤 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9433)
- [新增] weCheckLicense先创建release文件夹 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9474)
- [新增] 新增v4_user_pipeline_paging_search_by_name接口分页查询根据流水线名称搜索 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9466)
- [新增] 日志组件跳转功能兼容 firefox 浏览器 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9465)
- [新增] build_msg需要根据事件触发场景细化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8831)
- [新增] [GoAgent]环境共享构建机优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9437)
- [新增] 新增tencent Git Oauth 授权模式 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8845)
- [新增] bkrepo客户端新增apk加固接口 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9423)
- [新增] 流水线支持归档目录 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9320)
- [新增] 项目下的内置用户组，除了管理员组，其他组可删除 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9389)
- [新增] 权限入口优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9390)
- [新增] 迁移新版权限中心优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9306)
- [新增] 新增不用SQL检查的代码 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9346)
- [新增] 人工审核插件参数支持变量 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9373)
- [新增] 流水线产出物排序调整 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9259)
- [新增] 对接RBAC权限优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9149)

#### 优化
- [优化] 插件国际化properties文件value值支持引入文件 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9366)
- [优化] 构建日志模块增加服务端压缩请求 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9650)
- [优化] 微服务的主机IP列表队列配置自动删除 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9663)
- [优化] 添加商店配置项优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9600)
- [优化] 静态资源文件的url地址域名支持适配特定环境 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9504)
- [优化] metrics接口优化补充，调整项目下插件信息来源 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9488)
- [优化] 支持将分区库里的项目的数据迁移至指定数据库优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9494)
- [优化] 增加国际化初始化配置 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9413)
- [优化] 支持将分区库里的项目的数据迁移至指定数据库 TencentBlueKing [链接](http://github.com/TencentBlueKing/bk-ci/issues/9188)
- [优化] metrics部分接口性能优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9147)

#### 修复
- [修复] 归档目录到根目录时，查询制品错误 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9679)
- [修复] 公共构建机的插件缓存目录挪到工作空间的上一级目录 TencentBlueKing [链接](http://github.com/TencentBlueKing/bk-ci/issues/9640)
- [修复] 点击人工审核会把流水线中同一条红线规则的两处控制点的数据都刷新，导致另一个控制点的审核失败 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9633)
- [修复] 构建日志的服务调用端增加请求熔断机制 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9602)
- [修复] stage审核取消兼容数据处理 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9611)
- [修复] 静态资源文件的url地址域名支持适配特定环境遗漏点修复 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9581)
- [修复] 获取子流水线执行状态接口不需要做权限校验 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9552)
- [修复] stream修复部分受国际化影响的错误码返回内容 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9546)
- [修复] 查看研发商店的模版时，若插件可见范围符合要求，不应该提示项目xxx不允许使用插件xxx [链接](http://github.com/TencentBlueKing/bk-ci/issues/9531)
- [修复] 申请加入组bug修复 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9512)
- [修复] 增加开源版插件的task.json的packagePath字段的非空校验 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9509)
- [修复] 调用权限中心接口增加重试 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9477)
- [修复] 接口返回对象缓存在内存时做国际化切换可能导致接口返回对象的国际化值不符合预期 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9445)
- [修复] 删除流水线后，未删除制品库中流水线的资源 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9491)
- [修复] 支持将分区库里的项目的数据迁移至指定数据库问题修复 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9464)
- [修复] openapi 判断是否项目成员没有根据项目路由 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9427)
- [修复] devcloud类型登录调试，窗口大小无法自适应 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9418)
- [修复] 获取红线拦截记录接口漏掉了拦截列表的数据 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9405)
- [修复] 修复权限迁移bug [链接](http://github.com/TencentBlueKing/bk-ci/issues/9400)
- [修复] 共享凭据不需要依赖插件敏感接口权限校验 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9398)
- [修复] 选择插件历史版本修复升级后再取消版本校验优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9380)
- [修复] 开源版插件包重新上传时也需重新解析国际化信息 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9326)
# v1.13.0-rc.6
## Changelog since v1.13.0-rc.5
#### 新增
- [新增] dispatch 支持上下文占位，动态赋值 [链接](http://github.com/TencentBlueKing/bk-ci/issues/6460)
- [新增] 项目支持关联运营产品和根据运营产品搜索 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9636)
- [新增] svn触发需要提供获取到触发路径的变量 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9402)
- [新增] openapi新增度量能力 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9638)
- [新增] 优化批量添加项目成员接口 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9660)
- [新增] 整合dispatch-docker, dispatch-kubernetes模块到dispatch [链接](http://github.com/TencentBlueKing/bk-ci/issues/9548)
- [新增] redis库拆分 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9621)
- [新增] 优化OP获取项目列表接口 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9666)
- [新增] undertow加上线程池监控 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9631)
- [新增] iOS重签名部分功能优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9537)
- [新增] 构建分组并发时优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9618)
- [新增] 最近使用流水线组新增删除逻辑 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9627)
- [新增] Revert "feat:希望支持分支进行上架测试 [链接](http://github.com/TencentBlueKing/bk-ci/issues/4780)
- [新增] log的redisKey独立化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9599)
- [新增] 上下文命令字支持设置备注 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9545)

#### 优化
- [优化] 插件国际化properties文件value值支持引入文件 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9366)
- [优化] 构建日志模块增加服务端压缩请求 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9650)
- [优化] 微服务的主机IP列表队列配置自动删除 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9663)
- [优化] 添加商店配置项优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9600)

#### 修复
- [修复] 归档目录到根目录时，查询制品错误 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9679)
- [修复] 公共构建机的插件缓存目录挪到工作空间的上一级目录 TencentBlueKing [链接](http://github.com/TencentBlueKing/bk-ci/issues/9640)
- [修复] 点击人工审核会把流水线中同一条红线规则的两处控制点的数据都刷新，导致另一个控制点的审核失败 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9633)
- [修复] stage审核取消兼容数据处理 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9611)
- [修复] stream修复部分受国际化影响的错误码返回内容 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9546)
# v1.13.0-rc.5
## Changelog since v1.13.0-rc.4
#### 新增
- [新增] [bugfix] 默认prod集群router-tag判断有误 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9615)
- [新增] Image checkImageInspect接口优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9609)
- [新增] 提供监控迁移service接口 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9592)
- [新增] github触发器事件补充 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9372)
- [新增] redis分布式锁改造 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9499)
- [新增] 流水线插件安装包支持缓存，提高流水线执行速度 TencentBlueKing [链接](http://github.com/TencentBlueKing/bk-ci/issues/8940)
- [新增] 【PAC】feat：代码库支持重置授权 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8145)

#### 修复
- [修复] 构建日志的服务调用端增加请求熔断机制 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9602)
- [修复] 静态资源文件的url地址域名支持适配特定环境遗漏点修复 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9581)

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

# v1.13.0-rc.3
## Changelog since v1.13.0-rc.2
#### 新增
- [新增] yaml 方式下，mr 触发器支持配置是否阻塞 mr 合并 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9412)
- [新增] auth服务异常信息规范 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9455)
- [新增] 研发商店插件的质量红线指标支持占位符 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9506)
- [新增] 日志组件文件名错误 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9502)
- [新增] 蓝盾权限-支持查询某单一资源的用户组人员名单 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9116)
- [新增] 修改日志请求地址 & 支持 devx 跳转路径 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9495)
- [新增] [stream] 构建结束默认消息通知内容有误 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9485)
- [新增] project的op接口支持查询remotedev类型 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9472)
- [新增] 人工审核插件参数支持变量 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9373)
- [新增] 对接RBAC权限优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9149)

#### 优化
- [优化] 支持将分区库里的项目的数据迁移至指定数据库优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9494)

#### 修复
- [修复] 申请加入组bug修复 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9512)
- [修复] 调用权限中心接口增加重试 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9477)
- [修复] 共享凭据不需要依赖插件敏感接口权限校验 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9398)

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
