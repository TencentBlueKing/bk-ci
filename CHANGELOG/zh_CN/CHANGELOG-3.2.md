<!-- BEGIN MUNGE: GENERATED_TOC -->
- [v3.2.0-rc.4](#v320-rc4)
   - [Changelog since v3.2.0-rc.3](#changelog-since-v320-rc3)

- [v3.2.0-rc.3](#v320-rc3)
   - [Changelog since v3.2.0-rc.2](#changelog-since-v320-rc2)

- [v3.2.0-rc.2](#v320-rc2)
   - [Changelog since v3.2.0-rc.1](#changelog-since-v320-rc1)

- [v3.2.0-rc.1](#v320-rc1)
   - [Changelog since v3.1.0](#changelog-since-v310)

<!-- END MUNGE: GENERATED_TOC -->



<!-- NEW RELEASE NOTES ENTRY -->
# v3.2.0-rc.4
## 2025-03-27
### Changelog since v3.2.0-rc.3
#### 新增

##### 流水线
- [新增] feat：开启了 PAC 的流水线支持UI编辑分支版本以及手动执行分支版本 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11506)

##### 研发商店
- [新增] feat：研发商店组件上架支持自定义版本号 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11545)
- [新增] feat:OP后台访问镜像管理组件信息，去掉权限校验 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11362)

##### 环境管理
- [新增] feat：环境-节点管理列表支持按照状态等字段搜索 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11445)

##### 权限中心
- [新增] feat：支持用户主动退出项目 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11319)

##### 调度
- [新增] feat: 优化Agent超时导致的任务无法正常启动 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11546)

##### 未分类
- [新增] pnpm 10 不兼容问题 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11560)

#### 优化

##### 项目管理
- [优化] pref:完善获取服务跳转地址权限校验 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11567)
- [优化] pref: 蓝盾服务首页支持定制化跳转 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11510)

##### 未分类
- [优化] chore: 升级github action cache 版本到v4 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11566)
- [优化] Readme 错别字修正 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11531)

#### 修复

##### 流水线
- [修复] bug: 模板实例化的流水线审核记录状态不正确 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11527)
- [修复] bug: webhook触发指定流水线版本号异常 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11535)
- [修复] bug: 子流水线循环检查时，解析子流水线参数时，项目ID可能为空字符串 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11528)

##### 研发商店
- [修复] bug:研发商店组件审核发布时录入T_STORE_RELEASE表的首次发布人有误 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11366)

##### 环境管理
- [修复] bug: 导入构建机时listNew接口会因构建机过多而报错 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11553)

##### 未分类
- [修复] bug: MutableSet.toList()需线程安全 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11548)

# v3.2.0-rc.3
## 2025-03-07
### Changelog since v3.2.0-rc.2
#### 新增

##### 流水线
- [新增] feat：项目级别的变量语法风格设置增加风险提示和风险确认 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11407)
- [新增] feat：执行历史界面制品列表UI优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11012)
- [新增] feat：保存流水线时，校验是否有子流水线循环依赖 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10479)
- [新增] feat: APP的manualStartupInfo新增url处理 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11475)
- [新增] feat：流水线变量面板，上下滚动时固定顶部操作区域 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11378)

##### 环境管理
- [新增] feat：环境-节点管理列表增加执行信息并支持排序 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11444)
- [新增] feat: environment的redis迁移 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11464)
- [新增] feat：环境-节点管理列表支持按照状态等字段搜索 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11445)

##### 调度
- [新增] feat: 优化agent等待日志打印逻辑 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11504)

##### 网关
- [新增] feat: 将tag的缓存有效期从5s改为60s [链接](http://github.com/TencentBlueKing/bk-ci/issues/11508)

#### 优化

##### 流水线
- [优化] pref:流水线构建重试时增加时间限制 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11478)
- [优化] pref: 新详情页接口的stage审核状态与日志行数计算优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11507)

##### 研发商店
- [优化] pref:优化研发商店组件下架逻辑 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11308)

#### 修复

##### 流水线
- [修复] 流水线插件参数字段长度限制为1024字符问题修复 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11520)
- [修复] bug: 修复T_PIPELINE_WEBHOOK表中PROJECT_NAME字段异常的值 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11449)
- [修复] bug: 流水线版本管理与触发的并发问题 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11490)
- [修复] bug: 通过api保存的新版本可能覆盖旧版本 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11418)

##### 研发商店
- [修复] bug: 当被替换插件参数发生变化时，插件替换工具不一定能兼容 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11486)

##### 未分类
- [修复] bug：文件通用下载接口的文件路径参数校验加强 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11501)

# v3.2.0-rc.2
## 2025-02-21
### Changelog since v3.2.0-rc.1
#### 新增

##### 流水线
- [新增] feat：流水线代码变更记录，支持 AI 生成摘要 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11228)
- [新增] feat: 支持流水线构建状态上报 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11413)
- [新增] feat: 流水线级回调与流水线编排解耦 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11283)
- [新增] feat: Code禁用流水线切换到使用新方式 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11420)
- [新增] feat: taskControl的loop循环太久导致Consumer线程池被占满问题 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11352)
- [新增] feat：子流水线插件支持输出触发的子流水线 build_num [链接](http://github.com/TencentBlueKing/bk-ci/issues/11373)
- [新增] feat：保存流水线时，校验是否有子流水线循环依赖 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10479)
- [新增] feat：第三方构建机Job并发排队耗时归属优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10745)
- [新增] 支持流水线模板内的输入参数需要支持“从接口获取选项”的功能 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11428)

##### 权限中心
- [新增] feat：新增 支持往资源级用户组下添加用户接口 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11403)

##### 项目管理
- [新增] feat：构建日志归档阈值支持按项目配置 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11437)
- [新增] feat：支持管理我的 OAUTH [链接](http://github.com/TencentBlueKing/bk-ci/issues/10995)

##### 调度
- [新增] 构建资源锁定支持使用只读变量 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11425)
- [新增] 第三方构建机升级优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11431)
- [新增] feat: 删除老的心跳key [链接](http://github.com/TencentBlueKing/bk-ci/issues/11430)

##### 未分类
- [新增] feat: 需要修复的chart包问题 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11105)
- [新增] sql doc 文档更新 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9974)
- [新增] feat: api文档更新 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11409)

#### 优化

##### 流水线
- [优化] pref:分表路由规则优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11406)

##### 权限中心
- [优化] pref：审计相关优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11396)

#### 修复

##### 流水线
- [修复] bug: 通过api保存的新版本可能覆盖旧版本 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11418)
- [修复] bug: MR事件触发回写检查异常 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11456)
- [修复] bug：流水线描述内容过长导致编辑后发布报错500 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11404)

##### 代码库
- [修复] bug: 修复p4变更文件太多时导致repository服务OOM [链接](http://github.com/TencentBlueKing/bk-ci/issues/11457)

##### 研发商店
- [修复] bug:插件关联的流水线数量在流水线并发保存时可能计算出错 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11419)

##### 质量红线
- [修复] bug: 质量红线生效范围选择模板保存后，再点开查看数据没有正确展示 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11470)

##### Stream
- [修复] 【Stream】selector组件下拉选项列表通过接口获取，数据为空 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11375)

##### 未分类
- [修复] bug: 修复v3.0版本打helm chart包时报错 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11391)


# v3.2.0-rc.1
## 2025-02-11
### Changelog since v3.1.0
#### 新增

##### 流水线
- [新增] feat：新增service接口用于获取流水线组下流水线 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11383)
- [新增] 插件配置支持字段间联动 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11251)

##### 权限中心
- [新增] pref：获取有权限的资源接口优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11246)

##### 未分类
- [新增] 第三方构建机Docker支持JDK17 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11421)
- [新增] sql doc 文档更新 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9974)

#### 优化

##### 日志服务
- [优化] pref: 日志模块ES存储性能优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/7091)

#### 修复

##### 流水线
- [修复] bug：更新模板实例导致 流水线模型丢失父模板ID问题 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11309)
- [修复] bug: 新增错误类型枚举导致前端解析异常 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11386)

##### 研发商店
- [修复] bug:组件latestFlag参数更新时组件所有版本修改者会全部更新为同一人 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11400)
- [修复] bug：多环境情况下流水线编辑页面插件logo缓存优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11380)

##### 权限中心
- [修复] pref：用户组续期相关逻辑优化 #11305 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11305)
- [修复] bug：修改分级管理员授权范围偶现不成功现象 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11334)

##### 未分类
- [修复] bug: 修复v3.0版本打helm chart包时报错 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11391)
