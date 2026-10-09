---
name: crypto-key-rotation
description: AES 密钥轮换设计与开发指南，涵盖 AES_KEY_SHA 指纹、CryptoHelper、CryptoKeyRefreshWriter、历史密钥 used-*-keys、aes.refresh 启动刷新任务。当用户新增加密表字段、接入密钥轮换、修改 Token/凭证/证书加解密、配置 aes.refresh 或 used-git-keys 时使用。
related_skills:
  - database-design
  - common-technical-practices
  - repository-module-architecture
token_estimate: 800
---

# AES 密钥轮换

完整设计与开发指南见 [docs/dev/crypto_key_rotation.md](../../../docs/dev/crypto_key_rotation.md)。

修改加密表、`AES_KEY_SHA`、`CryptoKeyRefreshWriter` 或 `aes.refresh` 前，必须先阅读该文档，再按其中的四条链路（DDL / DAO / Helper / Writer）实现。新服务还必须加 `CryptoKeyRefreshStartup`（见文档 5.5）。
