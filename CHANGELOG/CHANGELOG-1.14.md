<!-- BEGIN MUNGE: GENERATED_TOC -->
- [v1.14.0-rc.1](#v1140-rc1)
   - [Changelog since v1.13.0-rc.6](#changelog-since-v1130-rc6)

<!-- END MUNGE: GENERATED_TOC -->



<!-- NEW RELEASE NOTES ENTRY -->
# v1.14.0-rc.1
## Changelog since v1.13.0-rc.6
#### 新增
- [新增] 支持从触发材料快捷访问触发源 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9092)
- [新增] 优化网关内核参数 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9706)
- [新增] 【PAC】feat：支持从代码库维度查看对应的代码库事件 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8122)
- [新增] 耗时展示优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9505)
- [新增] 【PAC】feat：新增代码库详情页 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8118)
- [新增] 子流水线调用触发的运行，触发材料显示父流水线 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8682)
- [新增] greysonfang bug：项目列表上的操作入口未加权限控制 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9720)
- [新增] github pr review事件输出源分支和目标分支url [链接](http://github.com/TencentBlueKing/bk-ci/issues/9716)
- [新增] 优化GoAgent对于后台资源占用 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9597)
- [新增] 蓝盾APP Oauth2授权登录实现 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9353)
- [新增] 约束模式的模版实例，构建详情页面增加源模版版本信息 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9700)
- [新增] 流水线列表最近执行应该展示触发人 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9712)
- [新增] 希望支持分支进行上架测试 [链接](http://github.com/TencentBlueKing/bk-ci/issues/4780)
- [新增] 修复iam回调蓝盾获取项目慢问题 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9708)
- [新增] systemToken从配置文件读取，不从redis读取 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9606)
- [新增] 【PAC】feat：代码库支持重置授权 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8145)
- [新增] 接入审计中心 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9414)
- [新增] 支持语音通知 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9686)
- [新增] 容器化特殊域名处理 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9681)
- [新增] 工蜂webhook请求支持路由到灰度 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9678)
- [新增] 控制服务的accesslog数量 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9675)
- [新增] 将dispatch-docker和dispatch-kubernetes统一为dispatch [链接](http://github.com/TencentBlueKing/bk-ci/issues/9658)
- [新增] 公共构建机支持持久化构建容器调度 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9269)
- [新增] 蓝盾oauth2鉴权实现 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9110)

#### 优化
- [优化] 支持刷新研发商店多个组件内置打包流水线 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9677)
- [优化] 插件国际化properties文件value值支持引入文件 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9366)

#### 修复
- [修复] 版本体验选择流水线列表搜不到目标流水线问题修复 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9711)
