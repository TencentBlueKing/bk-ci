<!-- BEGIN MUNGE: GENERATED_TOC -->
- [v4.0.0-rc.1](#v400-rc1)
   - [Changelog since v3.2.0](#changelog-since-v320)

<!-- END MUNGE: GENERATED_TOC -->



<!-- NEW RELEASE NOTES ENTRY -->
# v4.0.0-rc.1
## 2025-05-09
### Changelog since v3.2.0
#### 新增

##### 流水线
- [新增] feat：运行中重试的展示优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10483)
- [新增] feat：执行时显示具体资源支持通过Code设置 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11588)
- [新增] feat：流水线查看和构建详情查看配置界面敏感字段展示优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11019)
- [新增] 插件的报告功能可以默认显示最新报告 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11638)
- [新增] feat：子流水线调用插件入参类型为文本框时，前端应该为textarea组件 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11605)
- [新增] feat：matrix job 的 include/exclude  语法优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11519)
- [新增] feat：支持针对构建重放事件 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11232)
- [新增] feat：文件类型变量上传的文件支持版本管理 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11467)

##### 代码库
- [新增] perf: 重构代码库服务代码结构 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9952)
- [新增] feat：平台管理-代码源管理 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11379)
- [新增] feat: 代码源管理和代码库服务下关联代码库进行联动 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11498)

##### 研发商店
- [新增] 【研发商店】支持展示版本日志 [链接](http://github.com/TencentBlueKing/bk-ci/issues/1761)
- [新增] feat:研发商店组件支持一键发布 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11543)
- [新增] bua: 修复发布者信息同步分层根据组织名称获取ID时可能出现同名组织导致ID设置错误 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11643)

##### 环境管理
- [新增] feat: node_third_part_detail 接口获取正确的节点数据 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11607)
- [新增] 优化环境管理构建机Agent的定时维护任务 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11579)

##### 项目管理
- [新增] feat: project_list 接口增加product_id过滤条件 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11610)

##### Stream
- [新增] feat: 参照push触发增加stream跨库触发分支删除场景触发参数。 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11634)

##### 未分类
- [新增] sql doc 文档更新 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9974)
- [新增] feat：新增部分openapi [链接](http://github.com/TencentBlueKing/bk-ci/issues/11655)
- [新增] feat:【PAC模板】流水线模板支持PAC特性 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11414)
- [新增] feat: 升级JDK17 [链接](http://github.com/TencentBlueKing/bk-ci/issues/10593)
- [新增] feat: 让mq在程序启动的时候正常初始化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11584)

#### 优化

##### 流水线
- [优化] pref：支持变量分组 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11590)
- [优化] pref: 日志归档下载链接支持域名自动切换 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11582)

##### 研发商店
- [优化] pref:完善研发商店组件配置文件参数校验 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11269)
- [优化] pref:研发商店组件包签名流程优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11572)
- [优化] pref:下载处于测试中状态的插件的执行包不从制品库缓存获取 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11615)

##### 权限中心
- [优化] pref:用户组加人接口优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11678)

#### 修复

##### 流水线
- [修复] bug: 模板发布时，需检查模板里的镜像是否已发布逻辑修复 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11600)
- [修复] bug: 回收站恢复的流水线名字会多一串数字需要优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11632)
- [修复] 编辑模板流水线，matchRuleList接口参数丢失问题 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11613)
- [修复] bug: 高峰期引擎打印的服务内的构建日志触发熔断 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11589)
- [修复] bug:流水线模板版本排序列表异常，最新更新版本未在最上方位置 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11495)

##### 研发商店
- [修复] bug:获取研发商店组件升级版本接口优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11669)
- [修复] bug: 修复商店首页隐藏应用查询 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11648)

##### 环境管理
- [修复] bug: 环境管理搜索CMDB节点返回为空 [链接](http://github.com/TencentBlueKing/bk-ci/issues/11645)

