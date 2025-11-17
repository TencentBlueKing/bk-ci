# 流水线构建取消权限配置策略 (#9233)

## 功能说明

新增流水线级别的构建取消权限策略配置，提供更细粒度的取消权限控制，防止重要构建被误操作取消。

## 需求背景

### 现状问题
- 当前拥有执行权限的用户可取消任意运行中的构建
- 缺乏细粒度的取消权限控制

### 风险场景
- 重要打包流水线可能在临近出包时被误操作取消
- 无法限制特定用户的取消权限

## 策略说明

系统提供两种构建取消权限策略：

### 1. EXECUTE_PERMISSION（执行权限策略）
**适用场景**: 普通流水线，对取消权限要求不严格

**权限要求**: 拥有流水线执行权限的用户即可取消任意构建

**特点**:
- 权限要求较低
- 适合团队协作频繁的场景
- 存量流水线默认使用此策略（保持向后兼容）

### 2. RESTRICTED（受限策略）⭐ 推荐
**适用场景**: 重要流水线（如生产环境部署、正式打包等）

**权限要求**: 仅以下两类用户可取消构建
- **触发人**: 启动该构建的用户（triggerUser 或 startUser）
- **拥有管理权限**: 拥有流水线编辑(EDIT)权限的用户

**特点**:
- 权限要求严格，安全性高
- 新建流水线默认使用此策略
- 推荐用于生产环境和重要流水线

## 技术实现

### 数据库变更

#### T_PIPELINE_SETTING 表
```sql
ALTER TABLE T_PIPELINE_SETTING 
ADD COLUMN `BUILD_CANCEL_POLICY` varchar(32) DEFAULT 'EXECUTE_PERMISSION' 
COMMENT '构建取消权限策略:EXECUTE_PERMISSION-执行权限用户可取消,RESTRICTED-仅触发人/拥有者/管理员可取消';
```

#### T_PIPELINE_SETTING_VERSION 表
```sql
ALTER TABLE T_PIPELINE_SETTING_VERSION
ADD COLUMN `BUILD_CANCEL_POLICY` varchar(32) DEFAULT 'EXECUTE_PERMISSION' 
COMMENT '构建取消权限策略:EXECUTE_PERMISSION-执行权限用户可取消,RESTRICTED-仅触发人/拥有者/管理员可取消';
```

#### T_PIPELINE_TEMPLATE_SETTING_VERSION 表（流水线模板设置）
```sql
ALTER TABLE T_PIPELINE_TEMPLATE_SETTING_VERSION
ADD COLUMN `BUILD_CANCEL_POLICY` varchar(32) DEFAULT 'EXECUTE_PERMISSION' 
COMMENT '构建取消权限策略:EXECUTE_PERMISSION-执行权限用户可取消,RESTRICTED-仅触发人/拥有者/管理员可取消';
```

### 核心代码变更

#### 新增枚举类
- `BuildCancelPolicy.kt`: 定义两种策略枚举

#### 数据模型变更
- `PipelineSetting.kt`: 添加 `buildCancelPolicy` 字段
- `PipelineSettingVersion.kt`: 添加版本支持

#### DAO 层变更
- `PipelineSettingDao.kt`: 新增字段的读写逻辑
- `PipelineSettingVersionDao.kt`: 版本表的字段支持
- `PipelineTemplateSettingDao.kt`: **流水线模板设置的字段支持**
- `PipelineTemplateSettingUpdateInfo.kt`: **模板设置更新信息**

#### 权限验证逻辑
- `PipelineBuildFacadeService.kt`:
  - 修改 `buildManualShutdown()` 方法，根据策略执行不同的权限检查
  - 新增 `validateRestrictedCancelPermission()` 私有方法，实现受限策略验证
    - 检查是否为触发人（triggerUser 或 startUser）
    - 检查是否拥有流水线编辑权限（AuthPermission.EDIT）

#### 错误码
- `ProcessMessageCode.kt`: 新增 `USER_NO_CANCEL_BUILD_PERMISSION = "2101337"`
  - 错误信息：用户{0}无权取消构建{1},仅限触发人或拥有流水线管理权限的用户可取消

## 配置方式

### 流水线设置页面
在流水线设置中，可以选择构建取消权限策略：

```
[ ] 拥有执行权限的用户可取消任意构建
[√] 仅限触发人、流水线拥有者和项目管理员可取消 (推荐)
```

### API 方式
通过流水线设置 API，设置 `buildCancelPolicy` 字段：

```json
{
  "buildCancelPolicy": "RESTRICTED"  // 或 "EXECUTE_PERMISSION"
}
```

## 升级说明

### 存量数据处理
- 执行数据库升级脚本后，所有存量流水线的 `BUILD_CANCEL_POLICY` 字段值为 `EXECUTE_PERMISSION`
- 保持原有行为不变，无需手动迁移

### 新建流水线
- 默认使用 `RESTRICTED` 策略（推荐配置）
- 可在流水线设置中修改为 `EXECUTE_PERMISSION`

## 最佳实践

### 推荐配置

| 流水线类型 | 推荐策略 | 原因 |
|-----------|---------|------|
| 生产环境部署 | RESTRICTED | 防止误操作，保障生产安全 |
| 正式版本打包 | RESTRICTED | 避免构建被中断，影响发版计划 |
| 重要数据处理 | RESTRICTED | 保护关键数据流程 |
| 开发环境测试 | EXECUTE_PERMISSION | 方便协作，提高效率 |
| CI 验证流水线 | EXECUTE_PERMISSION | 频繁执行，灵活取消 |

### 迁移建议

1. **评估流水线重要性**: 识别项目中的关键流水线
2. **逐步调整策略**: 先对重要流水线启用 `RESTRICTED` 策略
3. **团队培训**: 向团队成员说明新策略的使用方式
4. **监控反馈**: 观察策略变更后的使用情况，收集反馈

## 错误处理

### 错误码 2101337
**错误信息**: 用户{0}无权取消构建{1},仅限触发人或拥有流水线管理权限的用户可取消

**出现场景**: 在 `RESTRICTED` 策略下，非授权用户尝试取消构建

**解决方案**:
1. 联系构建触发人取消
2. 联系拥有流水线编辑权限的用户取消
3. 如确需调整策略，请流水线管理员修改为 `EXECUTE_PERMISSION` 策略

## 相关文件清单

### 后端代码
- `BuildCancelPolicy.kt` - 策略枚举定义
- `PipelineSetting.kt` - 流水线设置数据模型
- `PipelineSettingVersion.kt` - 设置版本模型
- `PipelineSettingDao.kt` - 设置数据访问层
- `PipelineSettingVersionDao.kt` - 版本数据访问层
- `PipelineTemplateSettingDao.kt` - **流水线模板设置数据访问层**
- `PipelineTemplateSettingUpdateInfo.kt` - **流水线模板设置更新信息**
- `PipelineBuildFacadeService.kt` - 构建取消核心逻辑
- `ProcessMessageCode.kt` - 错误码定义

### 数据库脚本
- `2025_ci_process-update_v4.0_mysql.sql` - **主库升级脚本（已合并 v4.1 内容）**
- `2025_ci_archive_process-update_v4.0_mysql.sql` - **归档库升级脚本（已合并 v4.1 内容）**
- `1001_ci_process_ddl_mysql.sql` - **主库DDL定义（已更新表结构）**
- `1001_ci_archive_process_ddl_mysql.sql` - **归档库DDL定义（已更新表结构）**

### 测试代码
- `BuildCancelPolicyTest.kt` - 单元测试

## 版本信息

- **功能版本**: v4.1
- **Issue**: #9233
- **实现日期**: 2025-11-17
