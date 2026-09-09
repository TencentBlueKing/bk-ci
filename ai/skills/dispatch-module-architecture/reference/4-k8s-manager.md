# Kubernetes Manager 与 Dispatch 对接

代码在 `src/backend/dispatch-k8s-manager`（Go）。Kotlin 侧是 `biz-dispatch-kubernetes` 的 `Kubernetes*Client`，经共享 `Devops-Token` 调 manager HTTP API。

## 什么时候读这份

- 改 K8s 构建机创建、启停、删除、调试登录
- 改身份头、debug 票据、namespace 属主
- 排查 stop/delete 403、远程登录 503、跨项目越权
- 部署或升级 Helm，动到 `kubernetes-manager-auth`

不要用这份去改 Agent 心跳或 Worker 插件。

## 调用链

1. 流水线 ACL 在 dispatch（调试路径：`DispatchBaseDebugService` 的 `AuthPermission.EDIT`）。
2. dispatch 用 `X-DEVOPS-UID` / `X-DEVOPS-PROJECT-ID` + HMAC（`X-DEVOPS-IDENTITY-TS` / `X-DEVOPS-IDENTITY-SIG`）调 manager。
3. manager 只信 Header 签名。验签失败或密钥为空则丢弃自称头。
4. 已属主对象的 stop/delete/start 必须带匹配身份。无属主存量构建仍可被生命周期接口处理。

签名 payload：`uid|pid|tid|ts|METHOD|normalizedPath`，窗口 60 秒。构建容器只有共享 Token，没有签名密钥，不能伪造身份。

BCS（`bcs.apiUrl`、`BK-Devops-Token`、`/api/v1/devops/builder`）是另一套后端，不要按 manager 协议改 `BcsBuilderClient`。

## 新增配置与留空风险

Helm 同 namespace 创建 Secret `kubernetes-manager-auth`：首装随机 32 位，升级 lookup 保值。公开串

- `bkci-k8s-manager-debug-ticket-change-in-prod`
- `bkci-k8s-manager-identity-sig-change-in-prod`

只作拒绝名单，当作未配置。

| 项 | 环境变量 | 留空 / 公开串 |
|----|----------|----------------|
| `debugTicketSecret` | `K8S_MANAGER_DEBUG_TICKET_SECRET` | `/terminal` `/debug` 503。登录不可用，不是越权。 |
| manager `identitySigningKey` | `K8S_MANAGER_IDENTITY_SIGNING_KEY` | 自称头全丢。已属主 stop/delete/start 403，池位泄漏。 |
| dispatch `kubernetes.identitySigningKey` | `KUBERNETES_IDENTITY_SIGNING_KEY` | 不发 SIG，效果同上。 |

同 namespace：dispatch Deployment `secretKeyRef` 已挂同一 Secret，不要手抄。跨 ns / 跨集群：`optional: true`，Secret 不在则静默变空，必须两侧显式同一 key。GitOps 建议把 key 写死，避免每次 rand。禁止把这两个密钥注入构建 Pod。

manager 启动时身份密钥未配置会 Warn。两侧需 NTP（60 秒窗）。共享 `Devops-Token`（默认 `landun`）不能替代身份签名。

部署细则：`docs/install/kubernetes-manager.md`。

## 改代码时先看

- `pkg/apiserver/authz/identity.go`、`identity_sig.go`
- `pkg/apiserver/authz/debug.go`、`owner.go`、`namespace.go`
- `pkg/apiserver/apis/builder.go`、`builder_start.go`
- `KubernetesClientCommon.kt`（签发身份头）
- Helm：`kubernetes-manager-secret.yaml`、`templates/dispatch/deployment.yaml`

不要在构建容器里读签名密钥。不要从 Query 取身份。不要把项目 OR 当成唯一 ACL。
