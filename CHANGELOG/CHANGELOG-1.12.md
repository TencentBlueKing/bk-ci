<!-- BEGIN MUNGE: GENERATED_TOC -->
- [v1.12.0-rc.8](#v1120-rc8)
   - [Changelog since v1.12.0-rc.7](#changelog-since-v1120-rc7)

- [v1.12.0-rc.7](#v1120-rc7)
   - [Changelog since v1.12.0-rc.3](#changelog-since-v1120-rc3)

- [v1.12.0-rc.3](#v1120-rc3)
   - [Changelog since v1.11.0-rc.23](#changelog-since-v1110-rc23)

<!-- END MUNGE: GENERATED_TOC -->



<!-- NEW RELEASE NOTES ENTRY -->
# v1.12.0-rc.8
## Changelog since v1.12.0-rc.7
#### 新增
- [新增] 修复macos的jdk [链接](http://github.com/TencentBlueKing/bk-ci/issues/9362)
- [新增] 人工审核插件支持审核提醒 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8998)
- [新增] TGit 触发器功能对齐内网工蜂 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9215)
- [新增] Github 代码库支持获取gitProjectId [链接](http://github.com/TencentBlueKing/bk-ci/issues/9329)
- [新增] 新增tencent Git Oauth 授权模式 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8845)
- [新增] JOOQ监听器检测SQL [链接](http://github.com/TencentBlueKing/bk-ci/issues/9015)
- [新增] 对接RBAC权限优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9149)
- [新增] doc：独立部署蓝盾指引文档 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9235)
- [新增] [OP需求]项目设置支持配置云研发管理员列表 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9324)
- [新增] 每天定时生成Gradle缓存 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9319)
- [新增] github支持读取私有仓库文件内容 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9218)
- [新增] 插件国际化问题优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9274)
- [新增] build_msg需要根据事件触发场景细化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8831)
- [新增] 插件日志规范 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9022)

#### 优化
- [优化] 研发商店工作台插件管理优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9359)
- [优化] 创建项目时支持设置把该项目的数据落到指定DB TencentBlueKing [链接](http://github.com/TencentBlueKing/bk-ci/issues/9140)
- [优化] 插件环境信息查询接口性能优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9322)
- [优化] 优化研发商店可信指标管理 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9204)
- [优化] 插件统计数据来源切换至从metrics获取 TencentBlueKing [链接](http://github.com/TencentBlueKing/bk-ci/issues/9281)
- [优化] 删除私有构建集群 Agent多余描述 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9209)

#### 修复
- [修复] 构建触发待审核长时间占用QUEUE_COUNT [链接](http://github.com/TencentBlueKing/bk-ci/issues/8275)
- [修复] 权限事务一致性优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9290)
- [修复] 模板实例化携带设置时参数不全 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9331)
- [修复] 构建详情页接口返回数据过滤调了插件值为空字符串的参数 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9315)
- [修复] 插件入参在开启PAC后支持凭据替换 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9310)
- [修复] 归档报告时，pdf类型的入口文件无法显示 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9250)
- [修复] 定时触发插件触发异常 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9237)
- [修复] 流水线buildMsg信息保存失败 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9213)
# v1.12.0-rc.7
## Changelog since v1.12.0-rc.3
#### 新增
- [新增] 使用v4_app_pipeline_upload和v4_app_pipeline_copy接口时，部分设置参数缺失 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9308)
- [新增] 代码库别名长度限制放开到200 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8367)
- [新增] chart对齐蓝鲸7.1 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9293)
- [新增] Jersey扫描从@RestResource改为@Path [链接](http://github.com/TencentBlueKing/bk-ci/issues/9291)
- [新增] agent初始化docker脚本去掉worker下载 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9276)
- [新增] 容器化部署等待依赖底层 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9283)
- [新增] 构建详情页交互优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9126)
- [新增] 修改第三方构建机监控指标 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8418)
- [新增] 流水线组列表支持展开/收起 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8321)
- [新增] stream sla上报触发成功率的@BKTimed修改 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9242)
- [新增] 列表模式展示的流水线字段支持用户自定义 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8326)
- [新增] 调整滚动发布的速度 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9167)
- [新增] 对接RBAC权限优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9149)
- [新增] 缩减微服务的子模块数量 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9166)
- [新增] 记忆住用户在流水线列表的排序方式/字段宽度/展示模式/左侧导航的状态 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8322)
- [新增] 【OP需求】项目设置支持“启用云研发” [链接](http://github.com/TencentBlueKing/bk-ci/issues/9178)
- [新增] 支持ESB登录逻辑 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9141)
- [新增] 增加job.retry_task_id标识当前被重试的任务ID [链接](http://github.com/TencentBlueKing/bk-ci/issues/9173)

#### 优化
- [优化] 优化标签组/标签重名时的报错提示 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9312)
- [优化] 插件首个版本支持重复一键部署 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9278)
- [优化] 日志行号计算逻辑优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9191)

#### 修复
- [修复] 当流水线权限改变后，无法取消收藏问题fix [链接](http://github.com/TencentBlueKing/bk-ci/issues/9090)
- [修复] 插件暂停时遇到重复的继续执行请求导致Job反复启动 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9113)
- [修复] 构建详情页耗时数据兜底 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9236)
- [修复] JOB 勾选显示资源别名也无法使用 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9255)
- [修复] 权限默认组名支持国际化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9254)
- [修复] 新详情页矩阵内显示自定义前端异常 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9201)
- [修复] steam流水线折叠页面不显示耗时 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9162)
# v1.12.0-rc.3
## Changelog since v1.11.0-rc.23
#### 新增
- [新增] 对接RBAC权限优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9149)
- [新增] 修复安全漏洞 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9181)
- [新增] 部署时默认不再部署插件 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9154)
- [新增] store文本溢出、空状态规范落地 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8774)
- [新增] dispatch消息重复消费时的幂等兼容 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9146)
- [新增] 实现openapi RBAC权限版本 项目下用户组添加成员 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9093)
- [新增] ci套餐 出包bug修复 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9115)
- [新增] 优化第三方构建机Docker启动脚本 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9133)
- [新增] 包含动态跳过插件任务的Job的调度逻辑优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9101)
- [新增] 第三方构建机容器化环境支持登录调试 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8915)
- [新增] 优化GIT PUSH事件触发判定逻辑 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8978)
- [新增] 网关强制路由区分codecc [链接](http://github.com/TencentBlueKing/bk-ci/issues/9081)
- [新增] Goagent新增下线能力 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8893)

#### 优化
- [优化] 蓝盾国际化信息补充及优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9074)
- [优化] 模板编辑页针对大模板校验提速 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9118)
- [优化] 调整metrics查询数量限制 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9124)
- [优化] 拉取插件包时制品库网关增加缓存 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9111)

#### 修复
- [修复] 构建详情页切换构建号时取消操作状态不正确 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9164)
- [修复] 修复错别字 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9135)
- [修复] 流水线无编辑权限时loading遮罩无法关闭 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9156)
- [修复] svn保存代码库校验用户名密码接口调用错误 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9132)
- [修复] 人工审核插件刷新变量替换推送问题 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9129)
- [修复] 权限迁移子流水线跨项目调用优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9086)
- [修复] 权限迁移策略对比查询流水线优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9130)
- [修复] 流水线组A的执行者,查看组A下流水线的日志提示无权限 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9122)
- [修复] 修复日志状态同时写入状态出现的死锁问题 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9102)
- [修复] 重试流水线后回写git的MR评论没有更新 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9073)
