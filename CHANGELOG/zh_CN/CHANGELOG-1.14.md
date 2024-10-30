<!-- BEGIN MUNGE: GENERATED_TOC -->
- [v1.14.0](#v1140)
   - [Changelog since v1.13.0](#changelog-since-v1130)

- [v1.14.0-rc.2](#v1140-rc2)
   - [Changelog since v1.14.0-rc.1](#changelog-since-v1140-rc1)

- [v1.14.0-rc.1](#v1140-rc1)
   - [Changelog since v1.13.0-rc.6](#changelog-since-v1130-rc6)

<!-- END MUNGE: GENERATED_TOC -->



<!-- NEW RELEASE NOTES ENTRY -->
# v1.14.0
## Changelog since v1.13.0
#### 新增
- [新增] 流水线运营数据统计 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9735)
- [新增] 【蓝盾】【产品评审会-已评审】支持为单个流水线模板设置权限 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9574)
- [新增] [Stream]每日凌晨上报活跃项目sql优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9869)
- [新增] 重构前端保存传入的无用数据 [链接](http://github.com/TencentBlueKing/bk-ci/issues/6074)
- [新增] job 上下文增加关键字 node_alias [链接](http://github.com/TencentBlueKing/bk-ci/issues/9836)
- [新增] 缩减ticket微服务的子模块数量 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9227)
- [新增] yaml 方式下，mr 触发器支持配置是否阻塞 mr 合并 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9412)
- [新增] 新增资源时权限延迟临时优化方案 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9805)
- [新增] [openapi] 限制接口数据拉取时最大分页，提高平台稳定性 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9779)
- [新增] 【openapi】提供根据节点获取构建任务的api [链接](http://github.com/TencentBlueKing/bk-ci/issues/9829)
- [新增] 根据模板ID获取模板下插件属性数据 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9823)
- [新增] stream 限制调用历史列表接口频率 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9782)
- [新增] stream commit checker 优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9808)
- [新增] 删除dispatch相关模块冗余能力 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9699)
- [新增] [openapi]新增权限中心获取组用户接口 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9703)
- [新增] 国密处理一期 TencentBlueKing [链接](http://github.com/TencentBlueKing/bk-ci/issues/9262)
- [新增] [stream] 对slow sql专项优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9801)
- [新增] 网关强制路由支持query [链接](http://github.com/TencentBlueKing/bk-ci/issues/9807)
- [新增] 增加项目服务op操作接口 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9753)
- [新增] Worker优化表达式引擎逻辑 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9778)
- [新增] 支持从触发材料快捷访问触发源 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9092)
- [新增] [stream]手动触发支持ci.repo_id [链接](http://github.com/TencentBlueKing/bk-ci/issues/9773)
- [新增] 限制拉取history的数量为100 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9755)
- [新增] 修改apiserver获取nodes的调用方式 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9764)
- [新增] 优化网关内核参数 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9706)
- [新增] 面向用户的内置通知模版中，变量引用方式应该修改为${{}} [链接](http://github.com/TencentBlueKing/bk-ci/issues/9754)
- [新增] 【PAC】feat：支持从代码库维度查看对应的代码库事件 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8122)
- [新增] 耗时展示优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9505)
- [新增] 【PAC】feat：新增代码库详情页 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8118)
- [新增] 子流水线调用触发的运行，触发材料显示父流水线 [链接](http://github.com/TencentBlueKing/bk-ci/issues/8682)
- [新增] greysonfang bug：项目列表上的操作入口未加权限控制 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9720)
- [新增] github pr review事件输出源分支和目标分支url [链接](http://github.com/TencentBlueKing/bk-ci/issues/9716)
- [新增] 规避用户重装蓝盾因为项目数据不一致导致初始化镜像失败 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9585)
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
- [新增] 前端、网关统一走rabc [链接](http://github.com/TencentBlueKing/bk-ci/issues/9662)
- [新增] 公共构建机支持持久化构建容器调度 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9269)
- [新增] 蓝盾oauth2鉴权实现 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9110)

#### 优化
- [优化] 静态文件url中域名支持根据http请求的Referer头进行替换 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9803)
- [优化] 优化rbac权限二进制部署脚本 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9769)
- [优化] 流水线邮件通知，"耗时" 参数显示为空 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9751)
- [优化] 支持刷新研发商店多个组件内置打包流水线 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9677)
- [优化] 插件国际化properties文件value值支持引入文件 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9366)

#### 修复
- [修复] 修复支持插件使用分支开发存在的问题 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9815)
- [修复] 代码库触发事件类型错误 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9784)
- [修复] 版本体验选择流水线列表搜不到目标流水线问题修复 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9711)
# v1.14.0-rc.2
## Changelog since v1.14.0-rc.1
#### 新增
- [新增] 重构前端保存传入的无用数据 [链接](http://github.com/TencentBlueKing/bk-ci/issues/6074)
- [新增] 流水线运营数据统计 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9735)
- [新增] job 上下文增加关键字 node_alias [链接](http://github.com/TencentBlueKing/bk-ci/issues/9836)
- [新增] 缩减ticket微服务的子模块数量 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9227)
- [新增] yaml 方式下，mr 触发器支持配置是否阻塞 mr 合并 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9412)
- [新增] 新增资源时权限延迟临时优化方案 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9805)
- [新增] [openapi] 限制接口数据拉取时最大分页，提高平台稳定性 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9779)
- [新增] 【蓝盾】【产品评审会-已评审】支持为单个流水线模板设置权限 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9574)
- [新增] 【openapi】提供根据节点获取构建任务的api [链接](http://github.com/TencentBlueKing/bk-ci/issues/9829)
- [新增] 根据模板ID获取模板下插件属性数据 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9823)
- [新增] stream 限制调用历史列表接口频率 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9782)
- [新增] stream commit checker 优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9808)
- [新增] 删除dispatch相关模块冗余能力 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9699)
- [新增] [openapi]新增权限中心获取组用户接口 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9703)
- [新增] 国密处理一期 TencentBlueKing [链接](http://github.com/TencentBlueKing/bk-ci/issues/9262)
- [新增] [stream] 对slow sql专项优化 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9801)
- [新增] 网关强制路由支持query [链接](http://github.com/TencentBlueKing/bk-ci/issues/9807)
- [新增] 增加项目服务op操作接口 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9753)
- [新增] Worker优化表达式引擎逻辑 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9778)
- [新增] [stream]手动触发支持ci.repo_id [链接](http://github.com/TencentBlueKing/bk-ci/issues/9773)
- [新增] 限制拉取history的数量为100 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9755)
- [新增] 修改apiserver获取nodes的调用方式 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9764)
- [新增] 面向用户的内置通知模版中，变量引用方式应该修改为${{}} [链接](http://github.com/TencentBlueKing/bk-ci/issues/9754)
- [新增] 规避用户重装蓝盾因为项目数据不一致导致初始化镜像失败 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9585)
- [新增] 蓝盾APP Oauth2授权登录实现 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9353)
- [新增] 前端、网关统一走rabc [链接](http://github.com/TencentBlueKing/bk-ci/issues/9662)

#### 优化
- [优化] 静态文件url中域名支持根据http请求的Referer头进行替换 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9803)
- [优化] 流水线邮件通知，"耗时" 参数显示为空 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9751)

#### 修复
- [修复] 修复支持插件使用分支开发存在的问题 [链接](http://github.com/TencentBlueKing/bk-ci/issues/9815)
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
