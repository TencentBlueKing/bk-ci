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
