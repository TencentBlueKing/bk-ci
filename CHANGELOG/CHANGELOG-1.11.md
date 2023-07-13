<!-- BEGIN MUNGE: GENERATED_TOC -->
- [v1.11.0-rc.23](#v1110-rc23)
   - [Changelog since v1.11.0-rc.16](#changelog-since-v1110-rc16)

- [v1.11.0-rc.1](#v1110-rc1)
   - [Changelog since v1.10.0-rc.17](#changelog-since-v1100-rc17)

<!-- END MUNGE: GENERATED_TOC -->



<!-- NEW RELEASE NOTES ENTRY -->
# v1.11.0-rc.23
## Changelog since v1.11.0-rc.16
#### 新增
- [新增] 蓝盾对接审计中心回调接口 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9068)
- [新增] 新增arm64的JDK [链接](http://github.com/TencentBlueKing/bk-ci/issues/9063)
- [新增] bitnami的influxdb-relay修复 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9060)
- [新增] 支持开通蓝盾项目权限的同时开通对应的监控空间权限 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8935)
- [新增] 构建详情页问题优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8955)
- [新增] 支持global.imageRegistry [链接](http://github.com/TencentBlueKing/bk-ci/issues/9049)
- [新增] 日志每1GB就切割 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9048)
- [新增] 蓝盾对接权限中心RBAC优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8941)
- [新增] Rbac权限中心对接codecc迁移接口 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9001)
- [新增] websocket支持desktop路径 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9035)
- [新增] 去除agent对bktag变量依赖 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9024)
- [新增] github插件下载加速 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8938)
- [新增] 优化GIT PUSH事件触发判定逻辑 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8978)

#### 优化
- [优化] monitoring服务的查询插件监控统计数据接口性能优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9059)
- [优化] 蓝盾国际化信息优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8982)

#### 修复
- [修复] 权限迁移子流水线跨项目调用优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9086)
- [修复] linux操作系统cpu架构为aarch64应该使用arm64的node包执行node插件 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9070)
- [修复] 权限迁移策略对比优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9066)
- [修复] metrics平均耗时统计错误 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8971)
- [修复] 当stage配置了fastkill时取消后第二次重试后构建可能异常 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9053)
# v1.11.0-rc.1
## Changelog since v1.10.0-rc.17
#### 新增
- [新增] 流水线构建详情页重构需求 [链接](http://github.com/TencentBlueKing/bk-ci/issues/7983)
- [新增] 修复“服务扩展点初始化异常“问题 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8779)
- [新增] 获取第三方构建机信息使用agentId [链接](http://github.com/TencentBlueKing/bk-ci/issues/8778)
- [新增] 流水线插件支持展示获得的荣誉和SLA等信息 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8114)
- [新增] 网关backend路径新增remotedev [链接](http://github.com/TencentBlueKing/bk-ci/issues/8768)
- [新增] [stream]注册插件使用ci开启身份进行 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8767)
- [新增] 调整网关redis线程池大小 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8763)
- [新增] build_chart脚本不再依赖bkenv.properties [链接](http://github.com/TencentBlueKing/bk-ci/issues/8661)
- [新增] 优化websocket的日志, 避免日志过大 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8756)
- [新增] stream构建机传递凭据 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8466)

#### 优化
- [优化] 部分jooq逻辑优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8678)

#### 修复
- [修复] 流水线启动接口性能优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8615)
- [修复] 触发频率过高会导致排队取消积压 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8691)
- [修复] 矩阵内支持上下文缩写引用 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8641)
