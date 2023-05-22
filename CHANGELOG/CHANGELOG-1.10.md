<!-- BEGIN MUNGE: GENERATED_TOC -->
- [v1.10.0-rc.17](#v1100-rc17)
   - [Changelog since v1.10.0-rc.13](#changelog-since-v1100-rc13)

- [v1.10.0-rc.13](#v1100-rc13)
   - [Changelog since v1.10.0-rc.6](#changelog-since-v1100-rc6)

- [v1.10.0-rc.6](#v1100-rc6)
   - [Changelog since v1.10.0-rc.3](#changelog-since-v1100-rc3)

<!-- END MUNGE: GENERATED_TOC -->



<!-- NEW RELEASE NOTES ENTRY -->
# v1.10.0-rc.17
## Changelog since v1.10.0-rc.13
#### 新增
- [新增] 转发请求到容器化网关时带上token [链接](http://github.com/TencentBlueKing/bk-ci/issues/8725)
- [新增] Stream:新表达式引擎大小写不敏感bug [链接](http://github.com/TencentBlueKing/bk-ci/issues/8737)
- [新增] 环境管理 环境agent详情页 部分中文未被翻译 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8553)
- [新增] github action 的 go 版本升级 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8728)
- [新增] 【stream】concurrency 和蓝盾功能对齐 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8631)
- [新增] 修改Git触发的判断条件 Tencent [链接](http://github.com/TencentBlueKing/bk-ci/issues/8570)
- [新增] Goagent版本优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8671)
- [新增] [bug] 质量红线模块表格为空 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8685)
- [新增] 第三方构建机使用 docker 运行构建任务时，支持设置镜像拉取策略 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8635)
- [新增] 限制JVM的 metaspace 大小 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8656)
- [新增] [stream] 优化远程模板凭据引用 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8667)
- [新增] Agent: 优化因为网络问题引起的agent请求报错展示 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8603)
- [新增] 容器换环境不能调用consul [链接](http://github.com/TencentBlueKing/bk-ci/issues/8643)
- [新增] 希望流水线模板实例化流水线时能够继承分组名称的配置 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8616)
- [新增] 优化v4_user_template_get接口 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8550)
- [新增] api文档优化（2022.10-2023.04） [链接](http://github.com/TencentBlueKing/bk-ci/issues/8594)
- [新增] 矩阵支持跨库凭证引用 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8514)
- [新增] p4触发器不主动注册trigger事件 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8556)
- [新增] 【蓝盾-评审会已评审】repo-hook 支持忽略指定的代码库 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8435)
- [新增] [stream]手动触发 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8554)

#### 修复
- [修复] 研发商店工作台查询尚无统计数据组件时出现空指针异常 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8743)
- [修复] 自定义插件task.json中使用demands设置环境变量不生效 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8665)
- [修复] Stage条件判断支持新表达式 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8722)
- [修复] 调整metrics数据上报分布式锁过期时间，防止锁过期时间小于业务处理时间 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8648)
- [修复] 质量红线获取项目下可使用插件的指标缺失 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8565)
- [修复] 流水线变量版本仓库过滤器，最多仅支持1w个文件 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8675)
- [修复] 创建自定义指标报错英文名已存在 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8605)
- [修复] 流水线中配有两个相同的codecc任务时，保存指标数据偶现死锁 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8008)
# v1.10.0-rc.13
## Changelog since v1.10.0-rc.6
#### 新增
- [新增] 网关访问控制对所有路径生效 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8626)
- [新增] turbo的values修复 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8628)
- [新增] 质量红线国际化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8476)
- [新增] 流水线插件支持展示获得的荣誉和SLA等信息 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8114)
- [新增] 超时时间字段支持填写变量 [链接](http://github.com/TencentBlueKing/bk-ci/issues/7954)
- [新增] 流水线构建详情页重构需求 [链接](http://github.com/TencentBlueKing/bk-ci/issues/7983)
- [新增] 优化kubernetes部署 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8546)
- [新增] p4触发器不主动注册trigger事件 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8556)
- [新增] 将 push 事件的运行结果上报至代码库 Tencent [链接](http://github.com/TencentBlueKing/bk-ci/issues/7901)
- [新增] repo-hook 监听凭据支持 OAUTH token [链接](http://github.com/TencentBlueKing/bk-ci/issues/8216)
- [新增] 私有构建机使用镜像运行构建时，支持自定义Docker启动参数 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8461)
- [新增] 操作审计页面查询完善 [链接](http://github.com/TencentBlueKing/bk-ci/issues/4692)
- [新增] 优化websocket清理过期session逻辑 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8543)
- [新增] 流水线运转闭环保障 [链接](http://github.com/TencentBlueKing/bk-ci/issues/5110)
- [新增] 优化startbuild触发耗时 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8531)
- [新增] 归档报告时，仅入口文件添加流水线相关元数据 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8459)

#### 优化
- [优化] Update CODEOWNERS [链接](http://github.com/TencentBlueKing/bk-ci/issues/6604)

#### 修复
- [修复] agent和引擎并发执行task任务时可能造成task状态出错 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8465)
- [修复] 动态变量未更新 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8585)
- [修复] 新版构建详情页因排序问题导致数据合并异常 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8558)
# v1.10.0-rc.6
## Changelog since v1.10.0-rc.3
#### 新增
- [新增] 制品库无法归档的第三方构建机日志支持保留至指定目录 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8391)
- [新增] 流水线构建详情页重构需求 [链接](http://github.com/TencentBlueKing/bk-ci/issues/7983)
- [新增] [openapi] 新增获取第三方构建机信息详情接口 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8431)
- [新增] 流水线名称的排序没有按a~Z，导致了中英混杂的情况 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8508)
- [新增] repo-hook 监听凭据支持 OAUTH token [链接](http://github.com/TencentBlueKing/bk-ci/issues/8216)
- [新增] 【蓝盾-评审会已评审】repo-hook 支持忽略指定的代码库 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8435)
- [新增] 修复TOF同步数据Bug [链接](http://github.com/TencentBlueKing/bk-ci/issues/8516)
- [新增] [stream]新增ci变量表示yaml 文件路径 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8482)
- [新增] 创建制品库报告、日志仓库时display为false [链接](http://github.com/TencentBlueKing/bk-ci/issues/8495)
- [新增] 【社区版】流水线插件上架时，「适用Job类型」选项优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8283)
- [新增] turbo 服务支持IPv6 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8488)
- [新增] stream构建机传递凭据 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8466)
- [新增] 新增github oauth app 授权 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8486)
- [新增] 复数单词服务名 jooq任务脚本优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8464)
- [新增] bkrepo的分享链接直接用bkrepo域名下载 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8467)
- [新增] 第三方构建机使用Docker功能优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8382)
- [新增] 流水线模板发布时，需检查模板里的镜像是否已发布，校验可见范围 [链接](http://github.com/TencentBlueKing/bk-ci/issues/2049)
- [新增] workflow中去掉codecc和bkrepo的编译 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8439)
- [新增] 优化chart的生成脚本 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8445)

#### 优化
- [优化] 模板异步更新任务按环境获取redis锁 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8536)
- [优化] 社区版插件初始化发布配置 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8454)
- [优化] 完善部分build类接口权限校验 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8256)
- [优化] MeasureService插件开源调整 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8287)
- [优化] metrics数据上报错误信息录入逻辑在高并发场景优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8078)

#### 修复
- [修复] 依赖job重试成功后导致矩阵插件状态异常 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8523)
- [修复] 关联SVN代码库时，若源代码地址包含具体文件夹，会导致projectName字段解析异常 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8503)
- [修复] agent升级卡主无法升级问题 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8480)
- [修复] 修复读取配置文件错误 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8477)
- [修复] metrics插件趋势错误流水线构建页面的错误码要按错误类型查询 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8103)
- [修复] ifx: 编译加速节省耗时修改为节省率 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8447)
- [修复] 模板异步更新实例时会因为更新时指定的模板版本记录被删除而卡住 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8403)
- [修复] 构建机报错未兜底上报构建日志 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8373)
- [修复] 质量红线导致的流水线失败缺少错误信息 [链接](http://github.com/TencentBlueKing/bk-ci/issues/7205)
