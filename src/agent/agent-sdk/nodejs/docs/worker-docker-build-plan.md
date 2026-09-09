# nodejs SDK：新增 worker 构建 + docker 构建能力

## 目标
为 `agent-sdk/nodejs` 增加两类构建执行能力，参考 Go agent `src/pkg/job`：
1. **物理机 worker 构建**：启动 `worker-agent.jar`（需 jdk17/jdk8 路径 + worker.jar）。
2. **Docker 构建**：拉起容器执行 worker（自实现 docker CLI 封装）。

保持 SDK 现有设计：零第三方依赖、building blocks + 可选参考实现，不改动 `AgentLoop`/`AgentHandler` 接口。

## 已确认的关键决策
- **Docker**：自实现 CLI 封装（`child_process` 调 `docker`/`podman`）。Node 无“封装 docker 命令行”的成熟库，dockerode 走 daemon API 且不支持 podman CLI 语义，故逐字移植 Go 的 `dockercli.Runner`。支持 `DEVOPS_AGENT_CONTAINER_RUNTIME` 切换 runtime。
- **JDK**：SDK 不下载/解压/安装 JDK。下载解压由用户自己做。worker 启动函数接收 `jdk17Path` / `jdk8Path` 参数（java 可执行文件或目录，SDK 负责拼 `bin/java`）。
- **worker.jar 下载**：SDK 实现 `downloadWorkerJar()`，移植 Go 的 eTag/md5 增量下载协议。升级编排由用户实现。
- **worker 可插拔**：提供一个默认 worker 构建实现（用官方 worker.jar 直接可用），设计上允许未来用户替换为自己的 worker。
- **交付形态**：独立可选模块 + 一个参考 `BuildRunner`，接入方在 `onBuild` 里调用。

## 参考（Go 源）
- worker 启动脚本 & 进程：`agent/src/pkg/job/do_build.go`（`writeStartBuildAgentScript` 拼 `java -jar worker-agent.jar <base64(buildInfo)>` + env；套一层 `-l` shell 脚本读 profile；进程组退出）
- Windows worker 启动：`agent/src/pkg/job/do_build_win.go`（直接 `java.exe` + args 数组，无 shell 包装，参数：`-Djava.io.tmpdir` `-Ddevops.agent.error.file` `-Dbuild.type=AGENT` `-DAGENT_LOG_PREFIX` `-Xmx2g` `-jar` jar base64）
- worker env 组装：`agent/src/pkg/job/build.go:210-233`（`DEVOPS_*` 环境变量 + jdk8/17 path）
- worker.jar 自愈/finish 上报：`build.go` `runBuild` / `workerBuildFinish`（成功后 sleep 8s 再上报）
- docker 编排：`agent/src/pkg/job/build_docker.go`（拉镜像→create→start→wait→取日志→finish）
- docker 参数/mount/env：`agent/src/pkg/job/docker_runtime.go`（mount worker.jar 到 `/data/worker-agent.jar`、jdk 到 `/usr/local/jre[8]`、init.sh 到 `/data/init.sh`、`--entrypoint /bin/sh ... -c /data/init.sh`）
- docker CLI 封装：`agent/src/pkg/dockercli/dockercli.go`（run/pull/create/start/wait/logs/rm、凭据 stdin login、日志脱敏）
- 下载协议：`agent/src/pkg/util/httputil/devops.go:136`（GET `upgrade/files/download?file=jar/worker-agent.jar&eTag=<md5>`，带 auth headers，304=not modified，校验 `X-Checksum-Md5`）
- worker 版本探测：`agent/src/third_components/worker.go`（`java -cp jar com.tencent.devops.agent.AgentVersionKt`）

## 新增文件（`agent-sdk/nodejs/src/`）

### 1. `download.ts` — worker.jar 下载
- `downloadFile(client, gateway, authHeaders, serverFile, savePath): Promise<{md5, notModified}>`
  - 若 `savePath` 已存在 → 算本地 md5 作为 `eTag` query
  - GET `<gateway>/ms/environment/api/buildAgent/agent/thirdPartyAgent/upgrade/files/download?file=<serverFile>[&eTag=<md5>]`
  - 200 → 原子写入（写临时文件再 rename）；304 → 返回旧 md5 + notModified；404 → 抛错
  - 校验响应头 `X-Checksum-Md5`
  - 用 Node 内置 `crypto` 算 md5，`http/https` 流式下载（扩展 `httpClient.ts`，新增支持流式响应 + 返回 headers 的底层请求）
- `downloadWorkerJar(...)`：`serverFile="jar/worker-agent.jar"`，`savePath=<dir>/worker-agent.jar`

> `httpClient.ts` 需小幅扩展：现有 `request()` 只 buffer 成字符串、不暴露响应头。新增一个 `requestStream()`（或在 download 里独立实现），返回 statusCode + headers + 可 pipe 的响应流，供下载用。

### 2. `worker.ts` — 物理机 worker 构建
- `interface WorkerBuildOptions`：`buildInfo`、`workDir`、`jdk17Path?`、`jdk8Path?`、`workerJarPath`、`gateway`、`fileGateway`、`agentVersion`、`workerVersion`、`slaveUser?`、`language?`、`extraEnv?`、`detectShell?`
- `resolveJavaBin(jdkPath)`：入参既可为 java 可执行文件也可为 JDK 目录，按平台拼 `bin/java`（mac: `Contents/Home/bin/java`），17 优先、缺失回退 8（对应 `GetJavaLatest`）
- `buildWorkerEnv(opts)`：组装 `DEVOPS_AGENT_VERSION/DEVOPS_WORKER_VERSION/DEVOPS_PROJECT_ID/DEVOPS_BUILD_ID/DEVOPS_VM_SEQ_ID/DEVOPS_GATEWAY/DEVOPS_FILE_GATEWAY/BK_CI_LOCALE_LANGUAGE/DEVOPS_AGENT_JDK_8_PATH/DEVOPS_AGENT_JDK_17_PATH` +兼容旧 key + `extraEnv`
- `runWorkerBuild(opts): Promise<{success, message}>`：
  - 校验 workerJar 存在（缺失返回错误信息，供调用方上报 finish）
  - base64(JSON(buildInfo)) 作为 worker 参数
  - 组命令：`<javaBin> -Dbuild.type=AGENT -DAGENT_LOG_PREFIX=... -Xmx2g -Djava.io.tmpdir=<tmp> -jar <workerJarPath> <base64>`
  - **默认完全对齐 Go**：Unix 下写两层脚本（`writeStartBuildAgentScript`）——`start.sh` 内含 `cd workDir` + java 命令；`prepare_start.sh` 用 `exec $SHELL -l start.sh`（tcsh 用 `exec $SHELL start.sh -l`）读取用户 profile；spawn 执行 prepare 脚本。Windows 直接 spawn `java.exe`（无 login shell、无进程组）。
  - Unix `spawn` 用 `detached:true`（`setpgid`）以便结束时按进程组清理子进程；Windows 不设进程组。`cwd=workDir`，`env=process.env + workerEnv`；`await` 退出码。
  - 生成的临时脚本 & error_msg 文件构建结束后清理（对应 Go `ToDelTmpFiles`）。
  - `getWorkerErrorMsgFile`：预写“进程被杀”提示，worker 正常结束会清空/覆盖；据此判断非正常退出（对齐 `do_build.go` #5806）。
  - 返回结构由调用方用 `api.workerBuildFinish` 上报（含成功 sleep 8s 由参考 Handler 处理）。

### 3. `dockercli.ts` — docker/podman CLI 封装（移植 `dockercli.go`）
- `runtimeBinary()`：读 `DEVOPS_AGENT_CONTAINER_RUNTIME` 环境变量，默认 `docker`
- `class DockerRunner`（workDir, binary, logFn）：
  - `serverOS()`、`imageExists(image)`、`pullImage(image,user,pass)`（有凭据时临时 config dir + `--password-stdin` login）
  - `createContainer(args)`、`startContainer`、`waitContainer`→exitCode、`removeContainer`、`containerLogs`
  - 底层 `run(stdin, ...args)` 用 `child_process.spawn`，收集 stdout/stderr，日志脱敏（`-e KEY=VALUE` 中含 secret/password/token/credential 的 mask）

### 4. `dockerBuild.ts` — docker 构建编排（移植 `build_docker.go` + `docker_runtime.go`）
- `interface DockerBuildOptions`：`buildInfo`(含 `dockerBuildInfo`)、`workDir`、`jdk17Path?`、`jdk8Path?`、`workerJarPath`、`dockerInitScriptPath`、`gateway`、`agentSecret/agentId`(来自 dockerBuildInfo)、`projectId`、`extraEnv?`、`postLog?`
- `buildDockerCreateArgs(...)`：`--name`、用户 options（volumes/mounts/gpus/privileged/network/user）、默认 `--network bridge`、`-e` env（`devops_*`、`agent_build_env=DOCKER`、linux 下 jdk path env）、mount（worker.jar→`/data/worker-agent.jar`、jdk17→`/usr/local/jre`、jdk8→`/usr/local/jre8` 或单 jdk→`/usr/local/jre`、init.sh→`/data/init.sh`、workspace/data、logs）、`--entrypoint /bin/sh <image> -c /data/init.sh`
- `runDockerBuild(runner, opts): Promise<{success, message}>`：镜像存在判断→拉取→mkdir tmp→create→start→wait→退出码非 0 时读容器日志/挂载日志文件→返回结果（由调用方上报 finish）
- docker init 脚本下载：SDK 提供 `downloadDockerInitFile()`（复用 `download.ts`，serverFile 平台相关，如 linux `script/linux/agent_docker_init.sh`），是否调用交给用户/参考 Handler。

### 5. `buildRunner.ts` — 参考/默认实现（可选）
- `class DefaultBuildRunner`：并发计数（普通/docker）、`onBuild(build)` 分发到 `runWorkerBuild` 或 `runDockerBuild`、构建结束调用 `api.workerBuildFinish`（成功 sleep 8s）、维护 taskList/dockerTaskList 供心跳。
- 构造参数：`api`(AgentApi)、`config`、`workDir`、`jdk17Path`/`jdk8Path`、`workerJarPath`、`dockerInitScriptPath`、日志上报回调。
- 目的：接入方在自己的 `AgentHandler.onBuild` 里直接 `this.runner.onBuild(build)`，开箱即用；未来可替换 workerJar/自定义 worker。

### 6. `index.ts` — 导出新模块
新增 `export * from './download' / './worker' / './dockercli' / './dockerBuild' / './buildRunner'`。

## 需修改的现有文件
- `src/httpClient.ts`：新增流式请求能力（返回 statusCode + headers + 响应流），供 `download.ts` 使用。不破坏现有 `request()`。
- `src/config.ts`（可选，若走“配置传入 JDK 目录”）：本轮按“运行时传参”实现，暂不加 config 字段；若后续需要再加 `jdk8DirPath`/`jdk17DirPath`（对应 Go key `devops.agent.jdk.dir.path` / `devops.agent.jdk17.dir.path`）。
- `src/index.ts`：追加导出。
- `examples/basic.ts`（可选）：演示用 `DefaultBuildRunner` 拉起真实 worker 构建。

## 不做的事
- 不实现 JDK 下载/解压/安装（用户负责，SDK 只认路径）。
- 不实现升级编排（用户负责，SDK 只提供 `downloadWorkerJar`）。
- 不改动主循环 `AgentLoop` 与 `AgentHandler` 接口。
- 不引入任何第三方依赖。

## 平台支持（首版：Unix + Windows）
- **worker 构建**：Unix（linux/darwin）走两层 login-shell 脚本 + 进程组；Windows 直接 spawn `java.exe`（`bin/java.exe`），无 login shell、无进程组。java 路径拼接分平台：mac `Contents/Home/bin/java`、linux `bin/java`、win `bin/java.exe`（对齐 `third_components/jdk.go` `GetJava`）。
- **docker 构建**：CLI 封装跨平台通用；mount 的 jdk env（`DEVOPS_AGENT_JDK_*_PATH`）Go 仅在 linux 注入容器，mac/win 需用户自行设置——保持一致。

## 已定的小点
- worker 默认走 login shell（Unix 两层脚本，完全对齐 Go）。
- 首版支持 Unix + Windows。
