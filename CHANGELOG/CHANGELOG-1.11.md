<!-- BEGIN MUNGE: GENERATED_TOC -->
- [v1.11.0-rc.1](#v1110-rc1)
   - [Changelog since v1.10.0-rc.17](#changelog-since-v1100-rc17)

<!-- END MUNGE: GENERATED_TOC -->



<!-- NEW RELEASE NOTES ENTRY -->
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
