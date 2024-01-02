<!-- BEGIN MUNGE: GENERATED_TOC -->
- [v2.0.0](#v200)
   - [Changelog since v1.14.0](#changelog-since-v1140)

<!-- END MUNGE: GENERATED_TOC -->



<!-- NEW RELEASE NOTES ENTRY -->
# v2.0.0
## Changelog since v1.14.0
#### 新增
- [新增] 面向用户的内置通知模版中，变量引用方式应该修改为${{}} [链接](http://github.com/TencentBlueKing/bk-ci/issues/9754)
- [新增] doc: 文档链接优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9766)
- [新增] 子流水线调用触发的运行，触发材料显示父流水线 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8682)
- [新增] 规避用户重装蓝盾因为项目数据不一致导致初始化镜像失败 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9585)
- [新增] Demo项目隐藏 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9438)
- [新增] 完善权限申请单内容 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9394)
- [新增] 权限入口优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9390)
- [新增] 在bkrepo模式下默认启动bkrepo入口 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9377)
- [新增] 修复macos的jdk [链接](http://github.com/TencentBlueKing/bk-ci/issues/9362)
- [新增] 屏蔽公共构建机登录调试入口 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9349)
- [新增] 国密处理一期 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9262)
- [新增] 蓝盾国际化优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8975)
- [新增] 【rbac】流水线列表执行按钮权限校验问题 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9296)
- [新增] 处理状态码417 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9141)
- [新增] 蓝盾对接权限中心RBAC优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8941)
- [新增] 蓝盾对接权限中心RBAC [链接](http://github.com/TencentBlueKing/bk-ci/issues/7794)
- [新增] 添加项目管理前端服务 [链接](http://github.com/TencentBlueKing/bk-ci/issues/7923)
- [新增] kubernetes构建机支持专机配置 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8858)
- [新增] 修复chart的BUG [链接](http://github.com/TencentBlueKing/bk-ci/issues/8662)
- [新增] 记录vo、v3鉴权结果 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8500)
- [新增] 网关前端支持CDN配置 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8493)
- [新增] 主集群和子集群间构建制品网关与流量网关的配置优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8505)

#### 优化
- [优化] 优化rbac权限二进制部署脚本 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9769)
- [优化] 增加国际化初始化配置 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9413)
- [优化] 研发商店工作台插件管理优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9359)
- [优化] 插件统计数据来源切换至从metrics获取 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9281)

#### 修复
- [修复] 删除T_AUTH_IAM_CALLBACK表中experience资源 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9656)
- [修复] 蓝盾v2.0.0问题修复 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9343)
- [修复] 权限分组和描述都是英文的 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9420)
- [修复] 选择插件历史版本修复升级后再取消版本校验优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9380)
- [修复] 权限事务一致性优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9290)
- [修复] 【蓝盾新版权限】取消权限申请后，申请其他操作权限跳转页面筛选选择的用户组会一直复用上一次的 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9077)
- [修复] 关联代码库，添加一个仓库接口报错后页面一直出来loading状态 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9046)
- [修复] 研发商店-升级插件，切换发布类型时，清空发布包 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8049)
