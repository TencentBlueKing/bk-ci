# Agent 资源调度与容器配置

验证日期：2026-10-22。所有修改在当前工作分支，未提交、推送或部署。

## 实现范围

| 需求 | 已实现行为 |
| --- | --- |
| [13764：资源剩余调度](https://github.com/TencentBlueKing/bk-ci/issues/13764) | 构建环境自定义策略新增 CPU、可用内存、Agent 工作目录所在磁盘的剩余阈值。严格限制 1 到 99 整数，至少选一项，等于阈值通过；缺失或超过 3 分钟的数据按字段放行，不掩盖其他不足项。四条默认策略不变，后续策略仍可选择被前一策略资源条件排除的节点。 |
| [13763：Docker / Podman](https://github.com/TencentBlueKing/bk-ci/issues/13763) | Linux 第三方构建节点和创作节点可选择引擎，默认 Docker。节点设置覆盖旧环境变量开关，只影响新的容器任务。构建、调试容器和调试终端使用任务级引擎，不修改进程环境。 |
| [13748：不挂载工作空间](https://github.com/TencentBlueKing/bk-ci/issues/13748) | 编辑器新增默认开启的挂载开关，关闭时保留但禁用宿主机路径输入。只持久化 false，兼容旧 `__NO_MOUNT__`，下发时转换为该关键字，不删除目录、不自动复制到 Volumes。运行所需挂载保留，用户显式 Volumes / Mounts 不受影响，调试另建容器并沿用挂载选择。 |
| [13759：默认 CPU / 内存上限](https://github.com/TencentBlueKing/bk-ci/issues/13759) | 环境和节点按需存储默认配置。CPU、内存分别按 Job 显式值优先，再继承所选环境或直选节点；环境未配置时不回退节点。提供批量保持、不限、指定值三态，先检查全部节点编辑权限，再只修改 Linux 构建节点。创作节点仅支持引擎配置，不支持资源上限。 |

挂载关闭的 YAML 形态如下，默认开启时不输出 `mount-workspace`：

```yaml
runs-on:
  container:
    options:
      mount-workspace: false
```

编辑器和 Agent JSON 协议保留 `mountWorkspace`；实际 YAML 生成器通过局部映射使用 `mount-workspace`，避免改变已有 JSON 协议。已验证 v2、v3 解析器以及三个实际 YAML 序列化入口。

资源限制请求的三态约定：字段缺省或 null 表示保持不变，空字符串表示不限，非空字符串表示新值。CPU 必须是正数，允许小数；内存必须是正整数加小写 m/g，最低 6m。资源上限在任务入队时解析并保存，引擎在实际领取任务或创建调试会话时确定。

## 存储与性能

新增三张表：

- `T_AGENT_RESOURCE_SNAPSHOT`：每个 Agent 一行最新快照，各指标独立采样时间，不保存历史曲线。
- `T_ENV_DOCKER_CONFIG`：环境默认 CPU、内存上限，没有配置不创建记录。
- `T_NODE_DOCKER_CONFIG`：节点默认 CPU、内存和容器引擎，全部恢复默认时删除记录。

`T_ENV_DISPATCH_STRATEGY` 增加 `RESOURCE_REMAINING`。全量 DDL 与幂等升级 SQL 已同步。

监控在 core/ext 共用的 JSON HTTP 入口保存，再沿用原监控处理逻辑。Stream Agent 继续向 InfluxDB 上报，同时增加只包含相关资源指标的 JSON 副通道；副通道限时 5 秒且失败不改变主通道结果。当前顺序是先副通道再主通道，主通道有独立完整超时，但在副通道不可用时最多延后约 5 秒开始。

快照按 500 个 Agent ID 分块主键查询，随环境 Agent 列表一起返回。不引入 Redis 或本地监控缓存，避免状态恢复后旧缓存复活。调度执行器保持纯逻辑，不进行数据库或远程调用。Agent 每 5 秒轮询路径没有增加节点容器配置查询；容器配置只在环境候选查询、实际分配 Docker Job、领取 Docker Job 或启动调试时读取。

监控保存与状态变化以 Agent 行锁串行化。异常、恢复和启动会清空指标并保存短期失效水位，拒绝迟到的旧采样；物理删除 Agent 时同步删除快照。读取时仍逐字段检查 3 分钟有效性。每分钟清理最多 1000 条过期快照；两张稀疏配置表各最多清理 500 条已删除资源的记录。清理积压不会使过期指标重新参与筛选。

### 本机微基准

MySQL 5.7.44，Windows 本地开发实例，1000 个隔离 Agent 数据。写入测量 100 次，读取每种规模测量 50 次。最新一轮结果如下：

| 测量范围 | P50 | P95 |
| --- | ---: | ---: |
| 解析、Agent 行锁、快照合并及 upsert | 2.76 ms | 3.53 ms |
| 批查 50 个 Agent 快照 | 0.69 ms | 0.86 ms |
| 批查 500 个 Agent 快照 | 2.67 ms | 3.70 ms |
| 批查 1000 个 Agent 快照，拆成两次查询 | 4.32 ms | 5.99 ms |

查询执行计划使用主键。写路径通常包含锁定 Agent、读取原快照、upsert 三条语句，另有事务开销；重复或无有效新数据时不写入。微基准不包含 HTTP、真实鉴权、Kafka、InfluxDB、跨机网络或多服务实例争用，不等于生产端到端延迟或容量结论。此前一轮写入 P95 约 7.54 ms，说明本机测量也存在波动。

按每个 Agent 每分钟上报一次估算，N 个在线 Agent 新增约 N/60 次快照写事务每秒；10000 个 Agent 约 167 次/秒，而每 5 秒 ask 约 2000 次/秒。上报频率约为轮询的十二分之一。本机结果支持先使用批量主键读取而非额外缓存，但上线仍应观测数据库 CPU、锁等待、连接池、上报延迟和环境候选查询 P95/P99。

## 迁移与验证

升级 SQL：[2026_ci_environment-update_v4.2_mysql.sql](D:/projects/bk-ci-agent/support-files/sql/2026_v4.x/2026_ci_environment-update_v4.2_mysql.sql)。新安装使用 [1001_ci_environment_ddl_mysql.sql](D:/projects/bk-ci-agent/support-files/sql/1001_ci_environment_ddl_mysql.sql)。

推荐顺序：

1. 先应用 environment 数据库升级，并在构建环境生成新的 jOOQ 模型。
2. 升级 Agent，确保能读取任务级容器引擎并上报资源快照。旧服务端未下发引擎时仍使用既有 Agent 兼容逻辑。
3. 升级所有 environment 实例，提供快照和容器配置接口，再升级 dispatch 实例，最后发布前端。
4. 依赖旧环境变量选择 Podman 的节点应显式设置为 Podman。新服务端默认下发 Docker，会覆盖旧试验开关，这是本次需求定义的优先级变化。
5. 灰度检查共享环境、排队重试、异常恢复、两种引擎以及关闭挂载后的构建和调试。回退服务代码时可保留新增表列，不要通过删除用户配置作为回滚手段。

已执行：

- core environment、dispatch、common-pipeline-yaml 与 Tencent ext environment 编译通过。
- Kotlin 回归包含 45 个调度策略用例、5 个监控解析用例、3 个配置继承与校验用例、2 个 JSON/YAML 用例、1 个存储故障降级用例、6 个 MySQL 集成用例，以及 5 个既有 Agent API 用例，共 67 个。
- MySQL 集成覆盖乱序和部分上报、异常恢复失效、同状态启动失效、Agent 删除、快照有界清理、配置有界清理、批量权限原子性、混合 OS、创作节点边界、首次并发配置更新，以及迟到上报与生命周期行锁交错。
- Go 的 dockercli、job、imagedebug、monitor 四个包通过不使用结果缓存的测试，共 353 个测试/子用例事件，包含任务级引擎隔离及构建/调试 no-mount 必需文件和自定义挂载校验。
- 本次修改的 10 个前端 Vue/JS 文件静态检查通过。
- 环境管理前端完整生产模式构建通过。验证时仅在临时构建脚本中补充包级依赖查找路径，并将已安装的 js-base64 3.7.8、vuedraggable 2.24.3 映射到本地 pnpm 存储；没有改依赖清单或锁文件。
- 差异检查通过，保留原文件 CRLF 格式。

后端测试可从 `src/backend/ci` 工作目录执行，数据库参数按本地开发环境传入：

```powershell
.\gradlew.bat :core:environment:biz-environment:test --tests '*AgentFeaturesMysqlTest' --tests '*AgentResource*Test' --tests '*DockerConfigServiceTest' :core:dispatch:biz-dispatch:test --tests '*DispatchStrategyExecutorTest' :core:common:common-pipeline-yaml:test --tests '*MountWorkspaceTest' :ext:tencent:environment:biz-environment-tencent:compileKotlin "-PDB_HOST=$env:DB_HOST" "-PDB_USERNAME=$env:DB_USERNAME" "-PDB_PASSWORD=$env:DB_PASSWORD" "-PDB_PREFIX=$env:DB_PREFIX" --console=plain
```

MySQL 集成测试默认跳过，需显式设置 `BK_CI_FEATURE_TEST_URL`、`BK_CI_FEATURE_TEST_USER`、`BK_CI_FEATURE_TEST_PASSWORD`。URL 仅接受 localhost 或 127.0.0.1，源库需已有新增结构。测试只复制表结构到随机 `devops_ci_feature_test_` 库，创建和删除自己的隔离库，不修改源库业务数据。账号需有创建、删除测试库权限。

Agent 原依赖文件未改动。本机 Go 提示需要模块整理，因此本次用临时复制的 go.mod/go.sum 配合 `-modfile` 验证，没有执行全仓依赖更新。

## 待验证边界

流水线前端完整构建尚未通过：本地缺少 `@blueking/xss-filter`、`@tencent/bk-magic-vue` 的可解析安装，以及 `bkui-pipeline` Vue 2 构建产物，另有 echarts 包链接缺失。失败发生于既有依赖解析；本次改动的流水线组件已通过静态检查和构建阶段的模板处理。未为绕过问题而替换这些依赖或修改版本。

未运行真实 Linux Docker / Podman 引擎下的端到端流水线，也未进行鉴权后的浏览器交互验收或生产规模压测。仍需验证安装实际引擎的构建节点、共享环境、手动 Volumes/Mounts、调试连接、执行详情展示和用户权限页面体验。

磁盘选择依据服务端已记录的 Agent 安装/工作路径与采样挂载点。路径未知或被采集器忽略时按缺失数据处理，不猜测最大磁盘；符号链接跨盘等特殊部署应纳入灰度检查。正常 shutdown 原有状态语义未改动；启动和状态异常/恢复路径已接入失效处理。
