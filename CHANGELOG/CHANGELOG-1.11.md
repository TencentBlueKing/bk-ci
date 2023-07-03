<!-- BEGIN MUNGE: GENERATED_TOC -->
- [v1.11.0-rc.16](#v1110-rc16)
   - [Changelog since v1.11.0-rc.6](#changelog-since-v1110-rc6)

- [v1.11.0-rc.1](#v1110-rc1)
   - [Changelog since v1.10.0-rc.17](#changelog-since-v1100-rc17)

<!-- END MUNGE: GENERATED_TOC -->



<!-- NEW RELEASE NOTES ENTRY -->
# v1.11.0-rc.16
## Changelog since v1.11.0-rc.6
#### 新增
- [新增] 构建详情页问题优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8955)
- [新增] framework更新到0.0.7 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9027)
- [新增] 并发组排队的队列长度上限由20调整到200 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8989)
- [新增] shardingDatabase接入micrometer [链接](http://github.com/TencentBlueKing/bk-ci/issues/8999)
- [新增] 蓝盾对接权限中心RBAC优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8941)
- [新增] 优化容器化启动速度和JVM内存分配 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8960)
- [新增] 蓝盾内置插件支持国际化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8900)
- [新增] auth微服务的初始化数据需支持国际化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8898)
- [新增] JerseyConfig初始化完之后端口才可以访问 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8946)
- [新增] 支持获取构建环境的构建机列表 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8947)
- [新增] 流水线LATEST STATUS 状态取最新次构建的状态 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8907)
- [新增] 流水线构建详情页重构需求 [链接](http://github.com/TencentBlueKing/bk-ci/issues/7983)
- [新增] [stream] [链接](http://github.com/TencentBlueKing/bk-ci/issues/8910)
- [新增] hotfix:蓝盾国际化方案优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8211)
- [新增] 增加stream开启pac模式的环境配置 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8939)
- [新增] 市场插件输入参数支持插件信息相关变量的替换 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8840)
- [新增] 邮件发送后收件人顺序维持用户指定的顺序 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8902)
- [新增] 优化agent环境变量获取逻辑 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8857)
- [新增] 关联代码库页面优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8621)
- [新增] 新增环境页面优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8622)
- [新增] 优化首页文案 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8612)

#### 优化
- [优化] 进入复杂流水线详情页耗时优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8956)
- [优化] 互斥组支持重试时的占位符重新替换 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8878)

#### 修复
- [修复] 构建详情页针对矩阵场景post插件渲染修复 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9026)
- [修复] 流水线通知信息异常 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8983)
- [修复] 移动端产物二维码下载地址错误 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8966)
- [修复] 新详情页数据问题修复 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8890)
- [修复] 更新流水线模板实例的openapi接口在更新研发商店模板的实例失败 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8913)
- [修复] 国际化优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8903)
- [修复] Job启动插件收到取消请求时意外返回失败 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8879)
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
