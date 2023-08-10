<!-- BEGIN MUNGE: GENERATED_TOC -->
- [v1.12.0-rc.3](#v1120-rc3)
   - [Changelog since v1.11.0-rc.23](#changelog-since-v1110-rc23)

<!-- END MUNGE: GENERATED_TOC -->



<!-- NEW RELEASE NOTES ENTRY -->
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
