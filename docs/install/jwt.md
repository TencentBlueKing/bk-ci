## JWT 校验配置指南

### 1. 生成 JWT 密钥对
运行以下脚本生成 JWT 所需的 RSA 密钥对：
```bash
./scripts/bk-ci-gen-jwt-env.sh
```

### 2. 配置 values.yaml
在 `values.yaml` 文件中，进行以下配置：
- 启用 JWT 校验：将 `bkCiJwtEnable` 设置为 `true`。
- 填入生成的密钥：将 `bkCiJwtRsaPrivateKey` 和 `bkCiJwtRsaPublicKey` 分别填入上一步生成的私钥和公钥。

### 3. 部署或更新服务
- **首次部署**：直接运行 Helm 部署命令。
- **更新服务**：运行 `helm upgrade` 后，需要重启所有 Deployment。
  **注意**：重启过程会导致服务短暂不可用，请合理安排更新时间。

---

## JWT 密钥轮换指南（密钥泄露应急处理）

### 背景说明
JWT 在 BK-CI 中的流转路径为：**网关 ➡ 微服务 ⬅➡ Turbo（如有）**
- **网关**：使用私钥签发 JWT Token
- **微服务**：使用公钥验证 JWT Token
- **Turbo**：使用公钥验证 JWT Token

系统支持多密钥配置，可以在不中断服务的情况下完成密钥轮换。

### 密钥轮换流程

#### 阶段一：添加新密钥（双密钥并存）

**目标**：让所有服务同时支持新旧两个密钥，确保平滑过渡。

1. **生成新的密钥对**
   ```bash
   ./scripts/bk-ci-gen-jwt-env.sh
   ```
   保存输出的新密钥对，记为 `NEW_PRIVATE_KEY` 和 `NEW_PUBLIC_KEY`。

2. **修改配置文件顺序**

   **2.1 修改微服务配置（support-files/templates/#etc#ci#common.yml）**
   
   在 `bkci.security.properties` 中添加新密钥配置，保留旧密钥：
   ```yaml
   bkci:
     security:
       auth-enable: "__BK_CI_JWT_ENABLE__"
       properties:
         - kid: "devops"  # 旧密钥，保持 active: true
           public-key: "__BK_CI_JWT_RSA_PUBLIC_KEY__"
           private-key: "__BK_CI_JWT_RSA_PRIVATE_KEY__"
           active: true
         - kid: "devops-new"  # 新密钥，暂时设置 active: false
           public-key: "NEW_PUBLIC_KEY"
           private-key: "NEW_PRIVATE_KEY"
           active: false
   ```

   **2.2 修改 Turbo 配置（如果使用）**
   
   同样在 Turbo 的配置中添加新密钥（参考 helm-charts/core/ci/templates/configmap/turbo-configmap.yaml）。

3. **重启服务顺序**

   **重要原则**：先重启验证方（微服务、Turbo），再重启签发方（网关）
   
   ```bash
   # 步骤 1: 重启所有微服务（按任意顺序）
   kubectl rollout restart deployment -l app.kubernetes.io/component=microservice
   
   # 步骤 2: 重启 Turbo 服务（如果使用）
   kubectl rollout restart deployment -l app.kubernetes.io/component=microservice-turbo
   
   # 步骤 3: 等待所有微服务和 Turbo 完全就绪
   kubectl rollout status deployment -l app.kubernetes.io/component=microservice
   kubectl rollout status deployment -l app.kubernetes.io/component=microservice-turbo
   
   # 此时微服务已支持新旧两个公钥验证，但网关仍使用旧私钥签发
   ```

4. **验证阶段一完成**
   - 确认所有微服务和 Turbo 正常运行
   - 确认现有业务请求正常（网关仍使用旧密钥签发，微服务可验证）

#### 阶段二：切换到新密钥

**目标**：将签发密钥切换为新密钥，同时保留旧密钥验证能力。

5. **修改配置启用新密钥**

   **5.1 修改微服务配置**
   
   将新密钥设置为 active：
   ```yaml
   bkci:
     security:
       properties:
         - kid: "devops"  # 旧密钥，改为 active: false
           public-key: "__BK_CI_JWT_RSA_PUBLIC_KEY__"
           private-key: "__BK_CI_JWT_RSA_PRIVATE_KEY__"
           active: false
         - kid: "devops-new"  # 新密钥，改为 active: true
           public-key: "NEW_PUBLIC_KEY"
           private-key: "NEW_PRIVATE_KEY"
           active: true
   ```

   **5.2 修改网关配置（support-files/templates/gateway#core#lua#init.lua）**
   
   更新网关使用的私钥：
   ```lua
   jwtPrivateKey = "NEW_PRIVATE_KEY",
   jwtKid= "devops-new",
   ```

6. **重启服务顺序**

   **重要原则**：先重启签发方（网关），再重启验证方（微服务）
   
   ```bash
   # 步骤 1: 重启网关
   kubectl rollout restart deployment gateway
   kubectl rollout status deployment gateway
   
   # 步骤 2: 重启所有微服务
   kubectl rollout restart deployment -l app.kubernetes.io/component=microservice
   kubectl rollout status deployment -l app.kubernetes.io/component=microservice
   
   # 步骤 3: 重启 Turbo（如果使用）
   kubectl rollout restart deployment -l app.kubernetes.io/component=microservice-turbo
   kubectl rollout status deployment -l app.kubernetes.io/component=microservice-turbo
   ```

7. **验证阶段二完成**
   - 确认网关使用新密钥签发 Token
   - 确认微服务使用新密钥验证 Token
   - 确认所有业务请求正常

#### 阶段三：移除旧密钥（可选）

**目标**：彻底移除已泄露的旧密钥。

**建议等待时间**：在阶段二完成后，建议等待 **24-48 小时**，确保所有使用旧 Token 的请求都已过期。

8. **移除旧密钥配置**

   **8.1 修改微服务配置**
   
   删除旧密钥配置项：
   ```yaml
   bkci:
     security:
       auth-enable: "__BK_CI_JWT_ENABLE__"
       properties:
         - kid: "devops-new"  # 仅保留新密钥,注意不要改动kid
           public-key: "NEW_PUBLIC_KEY"
           private-key: "NEW_PRIVATE_KEY"
           active: true
   ```

9. **重启服务**
   ```bash
   # 按任意顺序重启所有服务
   kubectl rollout restart deployment -l app.kubernetes.io/component=microservice
   kubectl rollout restart deployment -l app.kubernetes.io/component=microservice-turbo
   ```

### 关键注意事项

1. **回滚方案**
   - 如果阶段二出现问题，可以快速回滚：
     - 将微服务配置中旧密钥的 `active` 改回 `true`
     - 将网关配置改回旧私钥
     - 重启网关和微服务

2. **验证方法**
   - 检查网关日志，确认 JWT 签发正常
   - 检查微服务日志，确认 JWT 验证正常
   - 执行业务操作，确认端到端流程正常
