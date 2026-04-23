---
name: store-module-architecture
description: Store 研发商店模块架构指南。创建和发布插件（Atom）、上传和管理模板（Template）、配置镜像仓库（Image）、提交审核申请、查询组件状态、排查发布失败。当用户开发研发商店功能、发布插件、管理模板或实现扩展点时使用。
---

# Store 研发商店模块架构指南

> **模块定位**: Store 管理流水线插件（Atom）、模板（Template）、镜像（Image）的发布、审核、安装全生命周期。数据库为 `devops_ci_store`（50+ 张表）。

## 模块结构

```
src/backend/ci/core/store/
├── api-store/          # API 接口定义（atom/common/container/image/template）
├── biz-store/          # 业务逻辑（service/dao/handler/resources）
├── model-store/        # 数据模型（JOOQ 生成）
└── boot-store/         # Spring Boot 启动模块
```

分层架构：API 层（Resource）→ 业务层（Service）→ DAO 层 → 数据层（MySQL）

详细 API 接口、Service、DAO 类列表见 [reference/api-reference.md](reference/api-reference.md)。

## 组件类型

| 类型 | 枚举值 | 核心表 | 用途 |
|------|--------|--------|------|
| 插件 | `ATOM` | `T_ATOM` | 流水线可执行插件 |
| 模板 | `TEMPLATE` | `T_TEMPLATE` | 流水线模板 |
| 镜像 | `IMAGE` | `T_IMAGE` | 容器构建镜像 |

完整数据库表结构见 [reference/database-schemas.md](reference/database-schemas.md)。

## 插件状态流转

```kotlin
enum class AtomStatusEnum(val status: Int) {
    INIT(0),              // 初始化
    COMMITTING(1),        // 提交中
    BUILDING(2),          // 构建中
    BUILD_FAIL(3),        // 构建失败
    TESTING(4),           // 测试中
    AUDITING(5),          // 审核中
    AUDIT_REJECT(6),      // 审核驳回
    RELEASED(7),          // 已发布
    GROUNDING_SUSPENSION(8), // 上架中止
    UNDERCARRIAGING(9),   // 下架中
    UNDERCARRIAGED(10),   // 已下架
}
```

流转路径：`INIT → COMMITTING → BUILDING → TESTING → AUDITING → RELEASED → UNDERCARRIAGING → UNDERCARRIAGED`
失败分支：`BUILDING → BUILD_FAIL`、`AUDITING → AUDIT_REJECT`

## 插件发布流程

1. **提交发布请求** — `UserAtomReleaseResource.createAtom()`
2. **参数校验** — 校验 atomCode 唯一性、版本号格式（semver）、代码库权限
   - 验证：若校验失败返回错误码，检查 `atomCode` 是否已被占用、版本号是否符合 `x.y.z` 格式
3. **创建插件记录** — `atomDao.create()`，状态设为 `INIT`
4. **触发构建流水线** — 调用 Process 模块构建插件包
   - 验证：检查 `ATOM_STATUS` 是否变为 `BUILDING(2)`
   - 若构建失败（`BUILD_FAIL(3)`）：检查构建日志，确认 `T_ATOM_BUILD_INFO` 中 `SCRIPT` 和 `SAMPLE_PROJECT_PATH` 配置正确
5. **构建完成回调** — 状态更新为 `TESTING`，插件包上传到制品库
6. **提交审核** — 状态设为 `AUDITING`
   - 若审核驳回（`AUDIT_REJECT(6)`）：查看 `T_STORE_APPROVE` 表获取驳回原因，修正后重新提交
7. **审核通过** — 状态设为 `RELEASED(7)`，更新 `LATEST_FLAG = true`
   - 验证：`SELECT * FROM T_ATOM WHERE ATOM_CODE = 'xxx' AND LATEST_FLAG = true` 应返回且仅返回一条记录

## 插件安装流程

1. **安装请求** — `UserMarketAtomResource.installAtom()`
2. **权限校验** — 检查用户项目权限
   - 失败时：确认用户在 `T_STORE_MEMBER` 表中有对应项目角色
3. **可见性检查** — 检查 `T_ATOM_FEATURE.VISIBILITY_LEVEL`，确认项目在可见范围内
4. **创建关联** — `storeProjectRelDao.create()`，在 `T_STORE_PROJECT_REL` 表中创建关联
5. **更新统计** — `T_STORE_STATISTICS` 表安装量 +1

## 插件开发：最小可运行示例

### 目录结构

```
my-atom/
├── task.json           # 插件配置（必需）
├── src/main.py         # 入口文件
└── requirements.txt    # Python 依赖
```

### 完整 task.json 示例

```json
{
  "atomCode": "myAtom",
  "execution": {
    "language": "python",
    "packagePath": "src/",
    "target": "main.py"
  },
  "input": {
    "inputParam": {
      "label": "输入参数",
      "type": "vuex-input",
      "required": true,
      "desc": "示例输入参数"
    }
  },
  "output": {
    "outputParam": {
      "type": "string",
      "description": "示例输出参数"
    }
  }
}
```

### 入口文件示例 (src/main.py)

```python
import json
import os

# 读取插件输入参数
def get_input(key):
    """从 BK-CI 环境变量读取输入参数"""
    return os.getenv(f"bk_ci_atom_input_{key}", "")

# 设置输出参数
def set_output(data):
    """写入输出文件供 BK-CI 读取"""
    with open("output.json", "w") as f:
        json.dump({"status": "success", "data": data}, f)

if __name__ == "__main__":
    input_param = get_input("inputParam")
    print(f"[INFO] Received input: {input_param}")

    # 业务逻辑
    result = {"outputParam": f"processed: {input_param}"}
    set_output(result)
    print("[INFO] Atom execution completed successfully")
```

支持语言：Python（推荐，有完善 SDK）、NodeJS、Java、Golang

## 模块依赖关系

Store 依赖：`project`（项目信息）、`repository`（代码库）、`artifactory`（制品库）
被依赖：`process`（流水线使用插件）、`worker`（构建机执行插件）

## 常见问题排查

| 问题 | 排查方法 |
|------|----------|
| `atomCode` vs `atomId`？ | `atomCode` 是插件唯一标识（不变），`atomId` 是版本 ID（每版本不同） |
| 插件是否可用？ | `SELECT * FROM T_ATOM WHERE ATOM_CODE='xxx' AND ATOM_STATUS=7 AND LATEST_FLAG=true` |
| 插件关联到项目？ | 查 `T_STORE_PROJECT_REL` 表，`STORE_CODE` = `atomCode` |
| 获取执行环境？ | 查 `T_ATOM_ENV_INFO` 表，根据 `ATOM_ID` 获取 `PKG_PATH`、`TARGET` |
| 构建失败？ | 检查 `T_ATOM_BUILD_INFO` 中 `SCRIPT` 路径；查看构建流水线日志 |
| 审核驳回？ | 查 `T_STORE_APPROVE` 表的 `APPROVE_MSG` 字段获取驳回原因 |

---

**版本**: 1.1.0 | **更新日期**: 2026-03-13
