<!-- BEGIN MUNGE: GENERATED_TOC -->
- [v1.11.0-rc.6](#v1110-rc6)
   - [Changelog since v1.11.0-rc.2](#changelog-since-v1110-rc2)

- [v1.11.0-rc.1](#v1110-rc1)
   - [Changelog since v1.10.0-rc.17](#changelog-since-v1100-rc17)

<!-- END MUNGE: GENERATED_TOC -->



<!-- NEW RELEASE NOTES ENTRY -->
# v1.11.0-rc.6
## Changelog since v1.11.0-rc.2
#### 新增
- [新增] 蓝盾对接权限中心RBAC [链接](http://github.com/TencentBlueKing/bk-ci/issues/7794)
- [新增] 流水线国际化相关优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8683)
- [新增] 下线二进制的定时任务 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8888)
- [新增] 增加获取代码库详情openapi接口 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8850)
- [新增] 关闭jvm进程之前等待30秒 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8875)
- [新增] [stream]兼容snakeyaml解析出错 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8823)
- [新增] 网关新增强制容器化header [链接](http://github.com/TencentBlueKing/bk-ci/issues/8863)
- [新增] worker路径的tag需要去掉容器化标志 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8869)
- [新增] 流水线构建详情页重构需求 [链接](http://github.com/TencentBlueKing/bk-ci/issues/7983)
- [新增] 容器化route_tag区分ci和codecc [链接](http://github.com/TencentBlueKing/bk-ci/issues/8848)
- [新增] 主集群和子集群间构建制品网关与流量网关的配置优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8505)
- [新增] BkRepoClient请求增加X-BKREPO-PROJECT-ID请求头 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8814)
- [新增] spring 优雅停机 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8833)
- [新增] 优化首页文案 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8612)
- [新增] 新增环境页面优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8622)
- [新增] 关联代码库页面优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8621)
- [新增] callback禁用后应该发送通知 Tencent [链接](http://github.com/TencentBlueKing/bk-ci/issues/7939)
- [新增] issue: 网关支持切换整个route_tag到容器化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8827)
- [新增] 修复kubernetes构建容器ID格式兼容 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8811)
- [新增] P4事件触发支持自主选择对大小写是否敏感 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8540)
- [新增] 无编译构建机调度优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8692)
- [新增] [bug] 并发组排队异常 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8794)
- [新增] 构建详情中制品查询提供新接口 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8097)
- [新增] 修改Git触发的判断条件 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8570)
- [新增] 记录vo、v3鉴权结果 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8500)
- [新增] 修复chart的BUG [链接](http://github.com/TencentBlueKing/bk-ci/issues/8662)

#### 优化
- [优化] 蓝盾国际化方案优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8211)
- [优化] 日志单行输出超过限制（16KB）自动分行 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8856)
- [优化] 最新动态的时间展示 xxxx-xx-xx hh:mm:ss [链接](http://github.com/TencentBlueKing/bk-ci/issues/8838)

#### 修复
- [修复] 上下文的状态字段异常 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8861)
- [修复] 日志时间戳乱序导致重复查询 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8804)
- [修复] stream的CodeCC红线指标没有关联忽略问题链接 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8821)
- [修复] stream 登录调试 vmSeqId 参数 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8818)
- [修复] 第三方构建机工作空间变量解析缺少上下文 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8803)
- [修复] 并发组优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8809)
- [修复] Linux开机自动启动的agent读取到HOME 变量为空的问题 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8798)
- [修复] 运行开机内置插件时，点击取消可能取消不了构建 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8789)
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
