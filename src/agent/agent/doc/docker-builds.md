# Docker/Podman 容器构建

本文档介绍 BK-CI Agent 的容器化构建能力，包括 Docker 和 Podman 运行时支持。

## 概述

Agent 支持在 Docker 容器中执行构建任务，适用于需要隔离构建环境的场景。容器构建与宿主机构建使用独立的并发计数器，互不影响。

启用 Docker 构建需要：
1. 服务端配置 `devops.docker.enable=true`（首次使用 Docker 构建时自动启用）
2. 宿主机上安装 Docker 或 Podman

## 运行时切换

Agent 默认使用 `docker` 命令，可通过环境变量切换为 `podman`：

```bash
export DEVOPS_AGENT_CONTAINER_RUNTIME=podman
```

该变量可以通过以下方式设置：
- 操作系统环境变量
- BK-CI 服务端通过心跳下发

Agent 所有容器操作（镜像拉取、容器创建/启动/停止/删除、日志查看）均通过 CLI 命令执行，不依赖 Docker SDK，确保与各种版本的 Docker/Podman 兼容。

## 构建流程

Docker 构建的详细流程：

```
接收 Docker 构建任务
       │
       ▼
  检查 docker_init.sh (缺失则下载)
       │
       ▼
  拉取构建镜像 (需要时登录私有仓库)
       │
       ▼
  创建容器
  (挂载 JDK、Worker、Workspace、Init 脚本)
       │
       ▼
  启动容器
  (入口命令: /bin/sh -c /data/init.sh)
       │
       ▼
  等待容器退出
       │
       ▼
  收集日志并上报结果
       │
       ▼
  删除容器 (调试模式除外)
```

## 构建镜像规范

Docker 构建对镜像有一定的基础要求，且 **Linux 与 Windows/macOS 的要求不同**。

### Linux 镜像要求

Linux 上 Agent 会将宿主机的 JDK 以只读方式挂载到容器内，容器镜像本身**不需要预装 JDK**，但必须具备运行 JDK 的能力：

| 要求 | 说明 |
|------|------|
| glibc 兼容 | JDK 是原生二进制，需要 glibc（`libc.so.6`）。Alpine 等 musl-based 镜像**不兼容** |
| 基本动态库 | `libpthread`、`libdl`、`libz`、`librt` 等 JDK 运行时依赖 |
| `/bin/sh` | 容器入口脚本 `/data/init.sh` 使用 `sh` 执行 |
| 架构匹配 | 镜像的 CPU 架构必须与宿主机一致（amd64/arm64） |

JDK 挂载路径：

| 宿主机 JDK | 容器内路径 | 说明 |
|-----------|----------|------|
| JDK 17 | `/usr/local/jre` | 入口脚本使用此路径启动 Worker（`/usr/local/jre/bin/java`） |
| JDK 8 | `/usr/local/jre8` | 仅当宿主机同时存在 JDK 17 和 JDK 8 时挂载 |

**推荐基础镜像**：
- `tlinux4` / `tlinux3`

**不兼容镜像**：
- `alpine`（使用 musl libc，无法运行标准 JDK 二进制）
- `busybox`（缺少必要动态库）
- `scratch`（无任何运行时）

### Windows / macOS 镜像要求

Windows 和 macOS 上 Agent **不会挂载宿主机 JDK 到容器内**，而是使用容器自带的 Java。因此镜像必须预装 JDK 并确保可通过 `PATH` 直接访问：

| 要求 | 说明 |
|------|------|
| 预装 JDK 17 | 容器内必须安装 JDK 17 |
| `java` 命令可直接执行 | JDK 的 `bin` 目录必须在 `PATH` 中，确保 `java -version` 能正常输出 |
| `JAVA_HOME` 环境变量（建议） | 设置 `JAVA_HOME` 指向 JDK 安装目录 |


### 验证镜像兼容性

可以通过以下方式快速验证镜像是否满足要求：

```bash
# Linux：验证挂载的 JDK 能否运行
docker run --rm -v /path/to/jdk17:/usr/local/jre:ro <your-image> /usr/local/jre/bin/java -version

# Windows/macOS：验证容器自带的 java 是否在 PATH 中
docker run --rm <your-image> java -version
```

### 不同平台的差异总结

| 特性 | Linux | Windows / macOS |
|------|-------|----------------|
| JDK 来源 | 宿主机挂载 | 容器自带 |
| 镜像是否需要预装 JDK | 否 | **是（JDK 17）** |
| `java` 需在 PATH 中 | 否（使用绝对路径 `/usr/local/jre/bin/java`） | **是** |
| 对 libc 的要求 | glibc 兼容 | 无特殊要求 |
| 入口脚本 | `/data/init.sh`（使用挂载的 JDK） | `/data/init.sh`（使用容器 `PATH` 中的 java） |

## 容器挂载

Agent 会自动将以下宿主机资源挂载到容器中：

| 宿主机路径 | 容器路径 | 模式 | 用途 |
|-----------|---------|------|------|
| `jdk17/` | `/usr/local/jre` | 只读 | JDK 17 运行时 |
| `jdk/`（如存在） | `/usr/local/jre8` | 只读 | JDK 8 运行时 |
| `worker-agent.jar` | `/data/worker-agent.jar` | 只读 | 构建 Worker |
| `docker_init.sh` | `/data/init.sh` | 只读 | 容器入口脚本 |
| `workspace/{pipeline}/` | `/data/devops/workspace/{pipeline}/` | 读写 | 构建工作空间 |
| `logs/docker/{buildId}/` | `/data/devops/logs/` | 读写 | 构建日志 |

## 容器配置

### 网络

默认使用 `bridge` 网络模式。用户可通过流水线配置中的 Docker 选项覆盖。

### Capabilities

通过 `DEVOPS_AGENT_DOCKER_CAP_ADD` 环境变量添加额外的 Linux capabilities：

```bash
export DEVOPS_AGENT_DOCKER_CAP_ADD=SYS_PTRACE,NET_ADMIN
```

### 用户自定义 Docker 选项

流水线配置中的 Docker 选项会通过 `BuildUserDockerArgs` 解析为 Docker CLI 参数，支持标准的 Docker run 选项。

## 镜像调试

Agent 支持对 Docker 构建镜像进行交互式调试，允许用户通过 WebSocket 在运行中的容器内执行命令。

### 调试流程

1. 服务端下发镜像调试任务
2. Agent 创建调试容器（基于指定镜像）
3. 用户通过 BK-CI 界面连接 WebSocket
4. Agent 执行 `docker exec -it` 进入容器，使用 PTY 提供交互式终端
5. 用户操作完成后，容器自动清理

### 端口范围

调试功能使用的端口范围由 `devops.imagedebug.portrange` 配置，默认 `30000-32767`。

### 自动清理

Agent 后台定时清理超时的调试容器，防止资源泄漏。

## Docker Init 脚本

`docker_init.sh` 是容器构建的入口脚本（对应容器内的 `/data/init.sh`），职责包括：

1. 将挂载的 `worker-agent.jar` 复制到工作目录
2. 设置 `build.type=DOCKER` 标记
3. 启动 Java Worker 进程

此脚本由服务端维护版本，Agent 通过心跳检测 MD5 变化自动更新。

## 并发控制

Docker 构建使用独立的并发计数器 `devops.docker.parallel.task.count`（默认 4），与普通构建互不影响。

| 场景 | 普通构建槽位 | Docker 构建槽位 | 结果 |
|------|------------|---------------|------|
| 普通构建已满，Docker 未满 | 4/4 | 2/4 | 只接受 Docker 构建 |
| Docker 构建已满，普通未满 | 2/4 | 4/4 | 只接受普通构建 |
| 两种都已满 | 4/4 | 4/4 | 不接受新任务 |
| 两种都有空位 | 2/4 | 2/4 | 两种都接受 |

## Podman 兼容性说明

使用 Podman 时需要注意：
- Podman 默认以 rootless 模式运行，部分操作可能需要额外配置
- Podman 的 socket 路径与 Docker 不同，Agent 会自动检测
- 镜像仓库的认证配置可能需要单独配置 `~/.config/containers/auth.json`
- `podman exec -it` 行为与 `docker exec -it` 基本一致

## 日志

Docker 构建相关操作会记录完整的 CLI 命令及其输出到 Agent 日志中：

```
[INFO] docker pull registry.example.com/builder:latest
[INFO] docker create --name bkci_build_xxx --network bridge ...
[INFO] docker start bkci_build_xxx
[INFO] docker wait bkci_build_xxx
[INFO] docker logs bkci_build_xxx
[INFO] docker rm bkci_build_xxx
```

这些日志有助于排查容器构建问题。
