# bkci-agent-sdk (Python)

BK-CI 第三方构建 Agent 的 Python SDK。它与同目录的 Node.js SDK 使用相同架构和后台
协议：SDK 负责 startup、心跳、轮询、并发判断、任务分发与异常隔离，接入方只实现
`AgentHandler` 约定的平台动作。

- 零运行时依赖，只使用 Python 标准库
- `asyncio` 长驻主循环，`stop()` 可立即中断轮询等待
- 与 Go/Node.js Agent 使用同一套 `X-DEVOPS-*` 鉴权头和 camelCase JSON 协议
- 自带物理机 worker、Docker/Podman 构建、升级文件下载和 worker.jar 原子升级能力
- Python 3.10+

## 安装

```bash
pip install .
```

## 快速开始

```python
import asyncio
import os
import platform
import socket

from bkci_agent_sdk import AgentConfig, AgentLoop, HeartExtra, StartupInfo


class MyHandler:
    def on_startup(self):
        return StartupInfo(
            host_name=socket.gethostname(),
            host_ip="127.0.0.1",
            detect_os=f"{platform.system()}_{platform.release()}",
            master_version="1.0.0-python-sdk",
            version="v1.0.0",
        )

    def collect_heart_extra(self, context, upgrade_enable):
        return HeartExtra(
            master_version="1.0.0-python-sdk",
            slave_version="v1.0.0",
            host_name=socket.gethostname(),
            agent_ip="127.0.0.1",
            agent_install_path=os.getcwd(),
            started_user=os.environ.get("USER", ""),
            props={
                "arch": platform.machine(),
                "jdkVersion": [],
                "dockerInitFileMd5": {"fileMd5": "", "needUpgrade": False},
                "osVersion": platform.release(),
            },
        )

    def is_upgrading(self): return False
    def check_parallel_task_count(self): return True, True
    def has_running_job(self): return False
    def pipeline_enabled(self): return False
    def docker_debug_enabled(self): return False

    async def on_build(self, build): pass
    async def on_upgrade(self, upgrade, has_build): pass
    async def on_pipeline(self, pipeline): pass
    async def on_image_debug(self, debug): pass
    async def on_heartbeat_resp(self, response): pass
    async def on_agent_deleted(self): pass


async def main():
    config = AgentConfig(
        gateway="http://your-bk-ci-gateway",
        project_id="your-project",
        agent_id="your-agent-id",
        secret_key="your-secret-key",
        parallel_task_count=4,
    )
    loop = AgentLoop(config=config, handler=MyHandler())
    await loop.run()


asyncio.run(main())
```

也可以读取现有 Agent 配置：

```python
config = AgentConfig.from_properties_file("/data/agent/.agent.properties")
```

完整的 worker 管理、构建分发和日志上报组合示例见 `examples/basic.py`。

## 架构

```text
AgentLoop
  ├─ startup（失败固定间隔重试）
  ├─ ask 轮询
  │   ├─ 能力与并发判断
  │   ├─ 心跳及升级信息组装
  │   ├─ AgentApi.ask → BK-CI
  │   └─ build / upgrade / pipeline / debug 异步分发
  └─ 可选 monitor 循环

AgentHandler                  接入方实现平台动作
DefaultBuildRunner            可选的默认物理机/Docker 构建实现
DefaultWorkerJarManager       可选的 worker.jar 检测、自愈与原子升级
```

## API 与协议

`AgentLoop.get_api()` 或独立构造的 `AgentApi` 可用于上报结果：

```python
await api.worker_build_finish({**build, "success": True, "message": "ok"})
await api.add_log_line(build_id, {
    "message": "building...",
    "timestamp": 0,
    "tag": "",
    "jobId": "",
    "logType": "LOG",
}, vm_seq_id)
await api.update_pipeline_status({"seqId": seq_id, "status": "success", "response": ""})
```

Python 配置与方法使用 snake_case；所有协议字典保留后台字段的 camelCase。`AgentLoop`
也兼容 Node.js 风格的 Handler 方法名，便于逐步迁移已有实现。

## 开发验证

```bash
python -m unittest discover -s tests -v
python -m compileall -q bkci_agent_sdk
```

## 构建与发布

SDK 构建和上传相互独立：项目负责生成 `dist/` 中的 wheel 与源码包，CI 使用 Twine
发布已经检查过的产物。上传 URL 和凭证不保存在仓库中。

发布脚本默认使用项目内独立的 `.venv-release`，避免向 Homebrew 或操作系统管理的
Python 环境安装包。可先显式初始化：

```bash
./scripts/setup_release_env.sh
```

不需要执行 `source` 激活虚拟环境；构建和上传脚本会直接使用其中的 Python。若尚未
初始化，两个脚本也会自动创建环境并安装 `requirements-release.txt` 中的工具。

构建 wheel 和源码包：

```bash
./scripts/build_dist.sh
```

脚本默认先清理 `dist/` 中本项目的旧产物，再生成：

```text
dist/bkci_agent_sdk-<version>-py3-none-any.whl
dist/bkci_agent_sdk-<version>.tar.gz
```

上传前由 CI 注入仓库与凭证：

```bash
export TWINE_REPOSITORY_URL="${PYPI_REPOSITORY_URL}"
export TWINE_USERNAME="${PYPI_USERNAME}"
export TWINE_PASSWORD="${PYPI_TOKEN}"

./scripts/upload_dist.sh
```

上传脚本会先执行 `twine check --strict`，通过后再以非交互方式上传 `dist/` 中本项目的
wheel 和源码包。`TWINE_REPOSITORY_URL` 必须配置为仓库提供的上传地址，不要默认假设
它与 `pip` 使用的 `/simple` 索引地址相同。

通用 CI 步骤示例：

```yaml
release:
  script:
    - ./scripts/setup_release_env.sh
    - ./scripts/build_dist.sh
    - ./scripts/upload_dist.sh
```

在 CI 平台的 Secret/凭证管理中，将发布地址、用户名和 Token 分别注入
`TWINE_REPOSITORY_URL`、`TWINE_USERNAME`、`TWINE_PASSWORD`。

如果遇到 `externally-managed-environment`，不要使用 `--break-system-packages` 修改受管理的
Python；直接运行 `./scripts/setup_release_env.sh` 即可。可选环境变量：`BASE_PYTHON`
指定用于创建虚拟环境的 Python，`RELEASE_VENV` 修改发布虚拟环境目录，`PYTHON_BIN`
绕过默认虚拟环境并指定现成的 Python，`DIST_DIR` 修改产物目录，`CLEAN_DIST=0` 可保留
已有产物。上传前应先更新 `pyproject.toml` 中的版本号。
