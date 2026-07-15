<!-- BEGIN MUNGE: GENERATED_TOC -->
- [v4.2.0-rc.3](#v420-rc3)
   - [Changelog since v4.2.0-rc.2](#changelog-since-v420-rc2)

- [v4.2.0-rc.2](#v420-rc2)
   - [Changelog since v4.2.0-rc.1](#changelog-since-v420-rc1)

- [v4.2.0-rc.1](#v420-rc1)
   - [Changelog since v4.1.0](#changelog-since-v410)

<!-- END MUNGE: GENERATED_TOC -->



<!-- NEW RELEASE NOTES ENTRY -->
# v4.2.0-rc.3
## 2026-06-08
### Changelog since v4.2.0-rc.2
#### 新增

##### 流水线
- [新增] feat：PAC流水线支持指定分支 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12635)

##### 环境管理
- [新增] feat: 环境管理重构 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12416)
- [新增] feat：创作环境/节点管理 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12389)

##### 权限中心
- [新增] feat：新增权限成员治理ai配套接口 #13019 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13019)

##### 项目管理
- [新增] feat：支持个人项目 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12852)

##### 其他
- [新增] feat：提供 build 接口支持插件获取当前步骤的原始配置 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12953)
- [新增] feat: 蓝盾智能助手支持多渠道大模型配置、用户模型配置与故障切换 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12958)
- [新增] feat：codecc规则集接入权限中心 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12981)
- [新增] feat: 自定义第三方构建机集群调度优先级 [链接](http://github.com/TencentBlueKing/bk-ci/issues/2680)
- [新增] feat: 依赖包版本升级优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12951)
- [新增] project模块缺少接口实现类造成启动失败修复 [链接](http://github.com/TencentBlueKing/bk-ci/issues/1285)

#### 优化

##### 权限中心
- [优化] pref：权限申请权限/权限交接场景优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13026)

##### 其他
- [优化] docs: 关联CodeCC代码库 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13075)
- [优化] pref: T_PIPELINE_WEBHOOK_QUEUE表间隙锁处理优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13052)
- [优化] pref:metrics数据上报优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13000)
- [优化] pref：优化版权声明 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12988)
- [优化] pref：优化 Skill 文档，采用渐进式披露，降低上下文占用并统一结构 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12948)
- [优化] pref:zip解压工具优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12960)

#### 修复

##### 流水线
- [修复] bug: 模版导出时,获取最新模版版本错误 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13049)
- [修复] bug: 模版实例化校验required合法性去掉常量参数 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13039)
- [修复] bug: 触发事件描述由前端组装 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12969)

##### 凭证管理
- [修复] bug: 密钥使用占位符配置 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12971)

##### 其他
- [修复] bug: 修复pac发布没有权限时报401错误 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13020)
- [修复] bug: 解决agent日志单例输出任务ID错乱和前端after接口问题 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12970)
- [修复] bug：AI 对话 AG-UI 流偶发缺失 RUN_FINISHED，导致前端会话长期停留运行中 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12994)
- [修复] bug：流水线构建信息通知勾选群消息转为Markdown格式时url链接未转换超链接 [链接](http://github.com/TencentBlueKing/bk-ci/issues/13044)
- [修复] bugfix: 第三方机新模式部分问题修复 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12945)
- [修复] fix: 升级前端依赖版本 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12965)
- [修复] bug: 优化callback调用http工具 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12961)

# v4.2.0-rc.2
## 2026-05-15
### Changelog since v4.2.0-rc.1
#### 新增

##### 流水线
- [新增] feat：支持获取流水线失败详情 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12873)
- [新增] feat：敏感字段未设置值错误加密问题 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12871)
- [新增] feat：源模版升级 触发约束模式模版自动安装插件 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12896)
- [新增] feature: 流水线job添加默认镜像 issue #1108 [链接](http://github.com/TencentBlueKing/bk-ci/issues/1265)

##### 研发商店
- [新增] feat：研发商店插件评论通知和审核通知支持发到群组里 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12655)
- [新增] 【研发商店】支持展示版本日志 [链接](http://github.com/TencentBlueKing/bk-ci/issues/1761)

##### 权限中心
- [新增] feat：申请权限时增加对不可主动申请加入的组的标识和提醒 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12436)

##### 项目管理
- [新增] feat: 增加设置系统默认集群的op接口 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12804)
- [新增] feat：项目归属信息填写优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12527)

##### Agent
- [新增] feat: agent上报并发指标数据 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12526)
- [新增] feat：第三方构建机使用 docker 运行时支持 --network 和 --user [链接](http://github.com/TencentBlueKing/bk-ci/issues/12832)
- [新增] feat: macos agent 支持 nohead [链接](http://github.com/TencentBlueKing/bk-ci/issues/12809)
- [新增] feat: agent支持mcp [链接](http://github.com/TencentBlueKing/bk-ci/issues/12653)

##### 其他
- [新增] feat: 第三方构建机移除telegraf依赖 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12895)
- [新增] feat: 加固登录调试鉴权 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12924)
- [新增] feat: windows支持服务下使用用户session [链接](http://github.com/TencentBlueKing/bk-ci/issues/12765)
- [新增] feat：支持蓝盾智能助手 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12737)
- [新增] 蓝鲸安全治理-SAST扫描修复 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12884)
- [新增] feat: 流水线实时监控概览页 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12497)
- [新增] feat: Agent使用docker cli替换sdk，避免daemon升级带来的依赖版本问题 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12791)
- [新增] 修复编译错误的问题 #171 [链接](http://github.com/TencentBlueKing/bk-ci/issues/176)

#### 优化

##### 流水线
- [优化] pref: 减少redis热key的访问频率 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12488)

##### 研发商店
- [优化] pref： 构建机上的插件缓存文件如果损坏需重新从仓库下载覆盖 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12447)

##### 权限中心
- [优化] pref：优化智能体申请续期接口 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12929)
- [优化] pref：禁用项目不再发起权限续期提醒。 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12475)

##### 其他
- [优化] pref: 流水线构建数据清理优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12797)

#### 修复

##### 流水线
- [修复] bug: 【PAC】创建定时触发器报: 定时触发器[监听PAC]配置不合法,当前流水线未开启PAC [链接](http://github.com/TencentBlueKing/bk-ci/issues/12946)
- [修复] bug: 【PAC】发布pac流水线时,应该使用发布人的身份提交代码库 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12846)
- [修复] 模板批量升级实例，修改参数后切换实例，数据未保存 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12939)
- [修复] bug: 普通流水线，不能被改成约束模式流水线 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12933)
- [修复] bug: 从实例化流水线复制为模版实例化，复制后的模版不能编辑 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12899)
- [修复] bug：人工审核插件超时后自动重试不生效问题fix [链接](http://github.com/TencentBlueKing/bk-ci/issues/11661)
- [修复] bug: 【PAC】yaml文件重命名,不应该删除旧的流水线重新创建一条新的 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12658)
- [修复] bug: PAC流水线下SELF模式下注册webhook失败 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12874)
- [修复] bug: 编辑实例化流水线,常量或其他变量会提示required值异常 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12921)
- [修复] bug: 模板实例化过程中，子流水线插件权限校验异常时没有把详情抛出 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12891)

##### 代码库
- [修复] bug: ExternalCodeccRepoResource接口改成service态 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12892)

##### 其他
- [修复] bugfix: 第三方机新模式部分问题修复 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12945)

# v4.2.0-rc.1
## 2026-04-21
### Changelog since v4.1.0
#### 新增

##### 流水线
- [新增] feat: Tag 事件触发支持动作过滤 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12670)
- [新增] feat: 人工审核插件审核意见这里可以设置为必填 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12810)

##### 研发商店
- [新增] feat: go插件的安装包文件支持签名 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12694)

##### 项目管理
- [新增] feat：支持隐藏项目属性 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12796)
- [新增] feat：支持项目级别开启/禁用「共享」制品能力 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12592)

##### 其他
- [新增] feat: RedisLock支持单独的redis实例 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12508)

#### 优化

##### 流水线
- [优化] perf: 调整流水线内置参数列表 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12859)

##### 研发商店
- [优化] perf：研发商店安装模版优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12837)
- [优化] pref ：研发商店模板安装优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12771)

##### 权限中心
- [优化] pref：用户组中成员和组织统计数据展示、以及用户组名称长时的展示优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12798)

##### 其他
- [优化] Bug: openapi获取流水线轻量构建历史 未对查询结果做排序 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12830)

#### 修复

##### 流水线
- [修复] bug: 实例化更新,会导致标签丢失 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12863)
- [修复] bug: 修复删除流水线标签,动态流水线组没有更新 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12831)
- [修复] bug:部分分区表的sql查询条件缺乏分区键 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12841)
- [修复] bug: 修复删除流水线通知但是code方式还能查看 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12788)
- [修复] bug: 修复run插件code方式丢失manualCommand字段 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12789)
- [修复] bug: 基于审核人数组做矩阵分裂审核，人工审核插件点击审核会报错 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12783)
- [修复] bug:流水线在job超时报错时，当时报错的插件不会写入到BK_CI_BUILD_FAIL_TASKS这个变量里 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12741)
- [修复] bug:人工审核插件重试后通知内容里引用的变量值还是上一次执行的值 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12723)

##### 代码库
- [修复] bug: 获取代码库目录列表兼容400和404错误码 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12853)

##### 调度
- [修复] bugfix: 第三方构建机容器去掉指定工作空间 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12814)

##### 其他
- [修复] bug：超管权限校验bug [链接](http://github.com/TencentBlueKing/bk-ci/issues/12849)
- [修复] bugfix: 流水线视图api无法返回收藏和个人创建流水线 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12817)
- [修复] bug: 分块上传的apk包无法体验下载 [链接](http://github.com/TencentBlueKing/bk-ci/issues/12821)

