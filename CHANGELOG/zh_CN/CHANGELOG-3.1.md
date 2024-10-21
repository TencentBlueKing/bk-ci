<!-- BEGIN MUNGE: GENERATED_TOC -->
- [v3.1.0-rc.1](#v310-rc1)
   - [Changelog since v3.0.0](#changelog-since-v300)

<!-- END MUNGE: GENERATED_TOC -->



<!-- NEW RELEASE NOTES ENTRY -->
# v3.1.0-rc.1
## Changelog since v3.0.0
#### 新增
##### 流水线
- [新增] 流水线查看页面/编辑页面/构建详情界面 面包屑中的名字展示优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10800)
- [新增] 流水线版本管理机制 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8161)
- [新增] Job 并发支持 Code 配置 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10860)
- [新增] 草稿版本UI展示 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9861)
##### 研发商店
- [新增] sdk相关的api是否显示在申请列表中支持可配置化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10840)
##### 权限中心
- [新增] 项目成员管理优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10927)
- [新增] 活跃用户记录操作和次数 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10891)
- [新增] 项目成员支持按照过期时间/用户组名称搜索 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10892)
- [新增] issue: 修复sample鉴权下查project全表的问题 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10941)
- [新增] oauth2 增加密码模式 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10663)
##### Stream
- [新增] [stream] 优化大仓触发耗时 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10861)
- [新增] stream stage 审核的通知方式支持企业微信群 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10796)
##### 调度
- [新增] 第三方构建机 Job 间复用构建环境支持 Code 配置 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10254)
- [新增] 同一流水线多次构建时资源调度优先级优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9897)
- [新增] 把docker build插件的config ns配置给去掉 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10926)
- [新增] AgentId复用类型转换问题 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10915)
##### Worker
- [新增] 让worker支持在JDK17中运行 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10412)
##### 其他
- [新增] 新启动的POD需要热身 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10887)
- [新增] openapi filter 优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10679)
- [新增] update lerna +yarn workspace to pnpm [链接](http://github.com/TencentBlueKing/bk-ci/issues/8125)

#### 优化
##### 流水线
- [优化] 带矩阵的流水线运行矩阵分裂前的task任务无需写入记录表 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10873)
##### 研发商店
- [优化] 拉取插件task.json文件内容报错提示优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10446)
- [优化] 研发商店敏感接口签名校验优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10759)
- [优化] 应用Schema改为每个版本都可以设置不同的配置 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10929)
- [优化] 公共构建机插件缓存区路径和变量调整优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10844)

#### 修复
##### 流水线
- [修复] 非编辑页面切换Code方式时提示保存 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10933)
- [修复] 新构建详情页的失败重试展示问题 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10735)
##### 研发商店
- [修复] 上架流水线模板到研发商店，但是新建流水线的时候在“研发商店”Tab搜不出来 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10865)
- [修复] 查插件环境信息接口未正确处理插件测试分支版本号情况 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10924)
##### 权限中心
- [修复] bk-permission 项目成员管理 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9620)
##### Stream
- [修复] stream新建环境名称正则错误修复 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10939)
