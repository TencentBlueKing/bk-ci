<!-- BEGIN MUNGE: GENERATED_TOC -->
- [v4.2.0-rc.1](#v420-rc1)
   - [Changelog since v4.1.0](#changelog-since-v410)

<!-- END MUNGE: GENERATED_TOC -->



<!-- NEW RELEASE NOTES ENTRY -->
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

