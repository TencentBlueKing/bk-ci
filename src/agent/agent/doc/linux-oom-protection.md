# Linux 普通构建 OOM 保护

在 agent 安装目录的 `.env` 或平台的 agent 环境变量中设置：

```dotenv
DEVOPS_AGENT_OOM_PROTECT=true
```

默认关闭。daemon、agent 和新版 worker 各自读取环境变量，不检查彼此版本，
无需配套发布。旧 worker 忽略此开关，允许整棵构建进程树继承 agent 的 -1000。

## 分别发布时的行为

下表假设对应进程收到的开关为 true，且设置 -1000 所需的权限可用。

| 发布组合 | daemon / agent | 普通构建 worker JVM | worker 启动的用户命令 |
| --- | --- | --- | --- |
| 新 agent + 旧 worker | -1000 | 继承 -1000 | 继承 -1000 |
| 旧 agent + 新 worker | 原有策略 | 自行设置 -1000 | 执行前恢复为 0 |
| 新 agent + 新 worker | -1000 | 继承或自行设置 -1000 | 执行前恢复为 0 |

新版 worker 开关关闭时不设置自身分数，也不包装用户命令。
如果它已从父进程继承 -1000，关闭 worker 开关不会主动解除这一继承。
原有脚本启动方式和 login profile 保持不变；worker 启动前的 shell/profile
仍遵循父进程的分数，新 worker 的降级仅发生在公共命令执行器启动用户命令时。

需要 root/CAP_SYS_RESOURCE 才能降低 OOM 分数。agent 设置失败时保留心跳、
暂停普通构建接单；worker 设置失败只记录警告、继续构建，不要求升级 agent。
新版 worker 已继承 -1000 时无需再次写入，支持由 agent 切换构建用户的情况。

## 开关生效时机

安装/start 时显式导出的同名变量会保存到 `.env`。
平台配置优先级更高，agent 心跳将该开关缓存到 `.oom-protection-env.json`；
修改或删除平台变量后，先等一次成功心跳，再在空闲时重启 daemon 和 agent。

worker 使用原有环境变量透传逻辑，在每次 JVM 启动时读取并冻结自己的开关。
它不读取当前任务的变量，也不依赖 agent 的内部开关状态或版本握手。
修改 worker 环境后从下一次启动的 worker 生效；已有 JVM 不动态切换。

## Docker 与状态流转

不处理 agent 管理的 Docker 构建容器，不调整 Docker/BuildKit 的 OOM 分数或资源参数。
普通流水线脚本中的 `docker build` 客户端属于用户命令：
旧 worker 下继承 -1000，新 worker 开启后恢复为 0。
独立 Docker/BuildKit 后台执行的构建不通过客户端请求继承这一分数。

复用 worker 原有非零退出处理、任务收尾和上报逻辑。
agent 开启保护后，JDK/worker 探测被杀或网络超时改为暂停新普通构建 30 秒，
继续心跳；普通构建完成通知失败按 5～30 秒间隔重试。
缺少执行次数的旧协议保持单次通知，避免旧通知影响后续重试构建。
不新增磁盘补报队列，不承诺进程崩溃或机器重启后的通知恢复。

## 验证与边界

使用 `/proc/<pid>/oom_score_adj` 对照上表检查。
测试应覆盖双方分别升级、开关关闭、权限不足、子孙进程继承、特殊参数和退出码。
真实 OOM 压测只在隔离 Linux 测试机进行；137/SIGKILL 不是 OOM 的充分证据。

本功能不预留内存或 CPU，不防止人工杀进程、机器故障及 JVM 自身内存耗尽。
旧 worker 下用户进程继承 -1000 是接受的兼容行为，不保证这时用户进程会被内核优先回收。
