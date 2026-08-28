# AES 密钥轮换设计与开发指南

业务表中的敏感字段（OAuth Token、凭证、证书、回调 Secret 等）使用 AES（未开 SM4 时）或 SM4 加密落库。AES 密钥更换时，必须能继续读取旧密文，并把存量数据重加密到当前密钥。

## 1. 核心约定

```
指纹字段：AES_KEY_SHA = ShaUtils.sha256Fingerprint(aesKey)  # SHA-256 前 16 位
普通解密：当前密钥 → 历史密钥
轮换解密：历史密钥 → 当前密钥，再 encryptSm4ButAes
待刷新条件：AES_KEY_SHA IS NULL OR AES_KEY_SHA <> currentKeySha()
开关：aes.refresh.enabled（默认 false）
锁：crypto:key:refresh:{spring.application.name}
```

给一张加密表接密钥轮换时，必须同时打通四条链路：

1. DDL：表增加 `AES_KEY_SHA`
2. DAO：所有写密文路径写入 `aesKeySha`
3. Helper：encrypt / decrypt（含历史密钥）/ refresh / currentKeySha
4. Writer：启动后分批重加密存量行

## 2. 核心概念

| 概念 | 说明 |
|------|------|
| 当前密钥 | 如 `aes.git`、`aes.github`，新写入一律用它加密 |
| 历史密钥 | 如 `aes.used-git-keys`（逗号分隔），轮换后把旧 key 放这里 |
| `AES_KEY_SHA` | 当前 AES 密钥指纹；`NULL` 或与当前指纹不等 → 待刷新 |
| CryptoHelper | 模块内封装 encrypt / decrypt / refresh / currentKeySha |
| CryptoKeyRefreshWriter | 按表拉取待刷新行，解密再加密后回写 |
| CryptoKeyRefreshExecutor | 启动后后台分批跑所有 Writer，带 Redis 分布式锁 |

解密顺序：

- **普通读** `decryptSm4OrAes(current, usedKeys, content)`：先当前密钥，再历史密钥
- **轮换刷新** `decryptSm4OrAesForRefresh(current, usedKeys, content)`：先历史密钥，再当前密钥

SM4 密文带前缀，加解密走 SM4，不走 AES 密钥列表。`AES_KEY_SHA` 在 SM4 开启时仍作为「是否已用当前配置迁移过」的水位标记。

## 3. 整体架构

```
OP POST /{service}/api/op/crypto/updateAesKeySha
  只补 AES_KEY_SHA（IS NULL），不重加密密文，不依赖 enabled

启动 CryptoKeyRefreshStartup          # enabled 时密钥轮换：重加密 + 写指纹
        │
        ▼
CryptoKeyRefreshExecutor
  RedisLock(crypto:key:refresh:{appName})  过期 600s
        │
        ▼  for each Writer
  fetchBatch(batchSize)
    WHERE AES_KEY_SHA IS NULL OR AES_KEY_SHA <> currentKeySha
        │
        ▼
  updateRow：refreshSm4OrAes(密文) + SET AES_KEY_SHA = current
        │
  整批 0 成功 → 停（防止死循环）
  sleepMsBetweenBatch → 下一批评
```

框架代码在 `common-security`：

- `CryptoKeyRefreshWriter` / `CryptoKeyRefreshRow`
- `CryptoKeyRefreshExecutor` / `CryptoKeyRefreshStartup`
- `OpCryptoKeyRefreshResource`（公共 OP）
- `CryptoKeyRefreshProperties`（前缀 `aes.refresh`）
- `BkCryptoUtil`（SM4/AES、多密钥尝试、refresh 专用解密）

`CryptoKeyRefreshExecutor` 和公共 OP 由 `ServiceSecurityAutoConfiguration` 自动装配。  
**启动刷新不会自动挂上**：新服务必须自己声明 `CryptoKeyRefreshStartup` Bean，否则 `aes.refresh.enabled=true` 重启也不会跑。对照 `RepositoryCryptoKeyRefreshConfiguration`。

各业务模块需要：

1. `@Service` 实现 `CryptoKeyRefreshWriter`
2. **新服务**增加 `XxxCryptoKeyRefreshConfiguration`，把 `List<CryptoKeyRefreshWriter>` 交给 `CryptoKeyRefreshStartup`

Spring 会注入本模块所有 Writer，**同一服务再新增 Writer 不用改 Configuration**。

## 4. 已接入的表与 Writer

| 模块 | 表 | Writer | Helper / 当前密钥 | 历史密钥配置 |
|------|----|--------|-------------------|--------------|
| process | `T_PROJECT_PIPELINE_CALLBACK` | `PipelineCallbackCryptoKeyRefreshWriter` | `PipelineCallbackCryptoHelper` | `project.callback.used-aes-keys` |
| repository | `T_REPOSITORY_GIT_TOKEN` | `GitTokenCryptoKeyRefreshWriter` | `GitTokenCryptoHelper` / `aes.git` | `aes.used-git-keys` |
| repository | `T_REPOSITORY_TGIT_TOKEN` | `TGitTokenCryptoKeyRefreshWriter` | 同上 | 同上 |
| repository | `T_REPOSITORY_GITHUB_TOKEN` | `GithubTokenCryptoKeyRefreshWriter` | `GithubTokenCryptoHelper` / `aes.github` | `aes.used-github-keys` |
| repository | `T_REPOSITORY_SCM_TOKEN` | `ScmTokenCryptoKeyRefreshWriter` | `GitTokenCryptoHelper` / `aes.git` | `aes.used-git-keys` |
| store | `T_STORE_SENSITIVE_CONF` | `SensitiveConfCryptoKeyRefreshWriter` | `StoreCryptoHelper` | `aes.usedAesKeys` |
| store | `T_STORE_ENV_VAR` | `StoreEnvVarCryptoKeyRefreshWriter` | 同上 | 同上 |
| ticket | `T_CREDENTIAL` | `CredentialCryptoKeyRefreshWriter` | `CredentialHelper` | `credential.used-aes-keys` |
| ticket | `T_CERT` | `CertCryptoKeyRefreshWriter` | `CertHelper` | `cert.used-aes-keys` |
| ticket | `T_CERT_ENTERPRISE` | `CertEnterpriseCryptoKeyRefreshWriter` | 同上 | 同上 |
| ticket | `T_CERT_TLS` | `CertTlsCryptoKeyRefreshWriter` | 同上 | 同上 |

`T_REPOSITORY_GIT_TOKEN` / `TGIT` / `SCM_TOKEN` 共用 `aes.git` 和 `GitTokenCryptoHelper`，GitHub 单独使用 `aes.github`。

对照实现优先看：

- Helper：`GitTokenCryptoHelper`
- DAO 写入：`GitTokenDao.saveAccessToken` / `RepositoryScmTokenDao.saveAccessToken`
- Writer：`GitTokenCryptoKeyRefreshWriter`、`ScmTokenCryptoKeyRefreshWriter`
- 配置：`RepositoryCryptoKeyRefreshConfiguration`

## 5. 给加密表接密钥轮换

### 5.1 表增加 `AES_KEY_SHA`

双轨更新（全量 DDL + 增量脚本）：

```sql
`AES_KEY_SHA` varchar(64) DEFAULT NULL COMMENT '加密密钥SHA指纹'
```

增量必须用 `IF NOT EXISTS` 判断列是否存在。改完重新生成 JOOQ。

默认 `NULL`：存量行会被 Writer 当成「未迁移」扫出来，这是预期行为。

同步更新 `support-files/templates/#etc#ci#common.yml` 中 `aes.refresh` 上方的备份表清单。

### 5.2 使用（或复用）CryptoHelper

不要在 Service 里裸调 `BkCryptoUtil` + `@Value aes.xxx`。Helper 至少提供：

```kotlin
fun currentKeySha(): String = ShaUtils.sha256Fingerprint(aesKey)
fun encryptSm4ButAes(content: String): String
fun decryptSm4OrAes(content: String): String          // 当前 key → used keys
fun refreshSm4OrAes(content: String): String          // used keys → 当前 key，再加密
```

同一把 AES 密钥的多张表应复用同一个 Helper（SCM Token 复用 `GitTokenCryptoHelper`）。

### 5.3 DAO 所有写密文路径都写指纹

`aesKeySha` 作为 DAO 参数，不要塞进 API POJO。

insert 和 `onDuplicateKeyUpdate` / update **都必须** `.set(AES_KEY_SHA, aesKeySha)`。

调用方一律：

```kotlin
xxxDao.saveAccessToken(..., aesKeySha = helper.currentKeySha())
```

漏掉 OAuth Token 刷新这类 upsert，会出现：密文已是新密钥，指纹仍是空/旧值，后续轮换按旧密钥去解而失败。

### 5.4 实现 `CryptoKeyRefreshWriter`

要点：

- `name` 全局唯一，用于日志
- `fetchBatch` 条件：`AES_KEY_SHA IS NULL OR AES_KEY_SHA <> currentKeySha()`（启动密钥轮换）
- `fetchMissingKeyShaBatch` 条件：`AES_KEY_SHA IS NULL`（OP 只补指纹）
- `updateAesKeySha` 只 `SET AES_KEY_SHA`，不要改密文
- 有业务过滤时一并写上（敏感配置只刷 `FIELD_TYPE = BACKEND`，回调只刷 `SECRET_PARAM IS NOT NULL`）
- `updateRow` 的 WHERE 必须是表的**业务唯一键**：
  - Git Token：`USER_ID`
  - GitHub Token：`USER_ID + TYPE`
  - SCM Token：`USER_ID + SCM_CODE + APP_TYPE`
  - Credential：`PROJECT_ID + CREDENTIAL_ID`
- 该行所有密文字段都要 `refreshSm4OrAes`（access + refresh）
- 可空字段用 `?.let(helper::refreshSm4OrAes)`
- `rowKey()` 要能唯一定位一行，便于日志排错

新增 `@Service Writer` 即可。本服务已有 `XxxCryptoKeyRefreshConfiguration` 时不用改配置类。

Writer 骨架：

```kotlin
@Service
class XxxCryptoKeyRefreshWriter(
    private val dslContext: DSLContext,
    private val helper: GitTokenCryptoHelper
) : CryptoKeyRefreshWriter {
    override val name = "repository-xxx"

    override fun fetchBatch(limit: Int): List<CryptoKeyRefreshRow> {
        return with(TXxx.T_XXX) {
            dslContext.select(PK..., CIPHER..., AES_KEY_SHA)
                .from(this)
                .where(AES_KEY_SHA.isNull.or(AES_KEY_SHA.ne(helper.currentKeySha())))
                .limit(limit)
                .fetch()
                .map(::toRow)
        }
    }

    override fun updateRow(row: CryptoKeyRefreshRow) {
        val r = row as XxxRow
        with(TXxx.T_XXX) {
            dslContext.update(this)
                .set(CIPHER, r.cipher?.let(helper::refreshSm4OrAes))
                .set(AES_KEY_SHA, helper.currentKeySha())
                .where(/* 完整唯一键 */)
                .execute()
        }
    }

    override fun fetchMissingKeyShaBatch(limit: Int): List<CryptoKeyRefreshRow> {
        return with(TXxx.T_XXX) {
            dslContext.select(PK..., CIPHER..., AES_KEY_SHA)
                .from(this)
                .where(AES_KEY_SHA.isNull)
                .limit(limit)
                .fetch()
                .map(::toRow)
        }
    }

    override fun updateAesKeySha(row: CryptoKeyRefreshRow) {
        val r = row as XxxRow
        with(TXxx.T_XXX) {
            dslContext.update(this)
                .set(AES_KEY_SHA, helper.currentKeySha())
                .where(/* 完整唯一键 */)
                .execute()
        }
    }
}
```

### 5.5 新服务必须加 `CryptoKeyRefreshStartup`

`CryptoKeyRefreshStartup` **不会**随 `common-security` 自动装配。新微服务第一次接密钥轮换时，必须在 biz 模块加一个 Configuration，否则 `aes.refresh.enabled` 开了也不跑重加密。OP 只能补空指纹，不能替代启动任务。

已有 process / repository / store / ticket 时，直接照抄对应 `*CryptoKeyRefreshConfiguration`，改 Bean 名和服务名即可：

```kotlin
@Configuration
class XxxCryptoKeyRefreshConfiguration {
    @Bean
    fun xxxCryptoKeyRefreshStartup(
        @Value("\${spring.application.name:xxx}")
        applicationName: String,
        properties: CryptoKeyRefreshProperties,
        executor: CryptoKeyRefreshExecutor,
        writers: List<CryptoKeyRefreshWriter>
    ) = CryptoKeyRefreshStartup(
        applicationName = applicationName,
        properties = properties,
        executor = executor,
        writers = writers
    )
}
```

一个服务只需要这一个 Bean。`writers` 由 Spring 注入该服务全部 `CryptoKeyRefreshWriter`。

## 6. 运维轮换步骤

刷新任务配置（`common.yml` → `aes.refresh`）：

| 配置 | 默认 | 含义 |
|------|------|------|
| `enabled` | false | 启动后全量刷新开关，生产默认关 |
| `initialDelayMs` | 10000 | 启动后再跑，避开启动高峰 |
| `batchSize` | 500 | 每批行数 |
| `sleepMsBetweenBatch` | 0 | 限速，大表建议加大 |

公共 OP 挂在每个微服务上，按服务名调用（不要每个模块再写一份）：

```
POST /{service}/api/op/crypto/updateAesKeySha?writer={name}
```

| 服务 | 示例 |
|------|------|
| repository | `/repository/api/op/crypto/updateAesKeySha?writer=repository-scm-token` |
| ticket | `/ticket/api/op/crypto/updateAesKeySha` |
| process | `/process/api/op/crypto/updateAesKeySha` |
| store | `/store/api/op/crypto/updateAesKeySha` |

OP 异步触发后立刻返回，不带业务结果。它只把 `AES_KEY_SHA IS NULL` 的存量行补上当前指纹，**不会**重加密 Token/凭证。真正轮换密钥（密文用新 key 重写）仍靠 `aes.refresh.enabled` + `CryptoKeyRefreshStartup`。

操作顺序：

1. 先备份会被刷新任务改写的表（见 `common.yml` 注释）
2. 新列上线后，用 OP 给存量行补 `AES_KEY_SHA`（不改密文）
3. 轮换密钥时：把旧当前密钥追加进对应 `used-*-keys`，再改当前密钥
4. 打开 `aes.refresh.enabled=true` 并重启对应服务，由启动任务重加密刷完
5. 看日志 `Crypto key refresh writer done`，确认 success / failed
6. 抽检密文能解、`AES_KEY_SHA` 已是当前指纹
7. 确认无失败后再把 `aes.refresh.enabled` 改回 false

锁 key：启动任务 `crypto:key:refresh:{spring.application.name}`，OP 补指纹 `crypto:aes-key-sha:{spring.application.name}`，多模块互不影响。

## 7. 排障与常见坑

- 整批 `batchSuccess == 0` 会停止该 Writer，避免解密一直失败时空转。日志：`Crypto key refresh writer stopped without progress`
- 单行失败只打 error，不中断整批；下一轮还会再捞到（指纹没更新）
- 守护线程名：`crypto-key-refresh-{applicationName}`
- `parseAesKeys` 按逗号 split，**空字符串也会保留**。未配置时可能变成 `[""]`，一般无害，但不要写成只有逗号

常见坑：

1. 只改了 create，漏了 update / OAuth refresh upsert
2. Writer WHERE 唯一键不完整，误更新多行
3. 只刷了 access_token，没刷 refresh_token
4. 新表复用了错误的 AES 配置（例如 SCM Token 误用 github key）
5. 开刷新前没把旧 key 放进 `used-*-keys`，存量解不开，Writer 全失败后停止
6. `aesKeySha` 放进了 API POJO，而不是 DAO 参数
7. 新服务只加了 Writer，没加 `CryptoKeyRefreshStartup`，`aes.refresh.enabled` 开了也不跑

## 8. 自检清单

接入一张新加密表前确认：

- [ ] 全量 DDL + 增量脚本都有 `AES_KEY_SHA`
- [ ] JOOQ 已重新生成
- [ ] Helper 能 decrypt（含历史密钥）和 refresh
- [ ] 所有写密文入口都传 `currentKeySha()`
- [ ] Writer 覆盖全部密文字段、唯一键正确
- [ ] 对应 yml 有 `used-*-keys`
- [ ] `common.yml` 备份清单已加上该表
- [ ] 本服务已有 `XxxCryptoKeyRefreshConfiguration`，把 `CryptoKeyRefreshStartup` 注册成 Bean（新服务必须加）
