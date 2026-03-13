# Auth 模块数据库表详细参考

## 用户组相关表

| 表名 | 说明 | 核心字段 |
|------|------|----------|
| `T_AUTH_RESOURCE_GROUP` | 资源用户组 | `ID`, `PROJECT_CODE`, `RESOURCE_TYPE`, `RESOURCE_CODE`, `GROUP_CODE`, `GROUP_NAME`, `IAM_GROUP_ID` |
| `T_AUTH_RESOURCE_GROUP_MEMBER` | 组成员关系 | `ID`, `PROJECT_CODE`, `IAM_GROUP_ID`, `MEMBER_ID`, `MEMBER_TYPE`, `EXPIRED_TIME` |
| `T_AUTH_RESOURCE_GROUP_PERMISSION` | 组权限 | `ID`, `PROJECT_CODE`, `RESOURCE_TYPE`, `IAM_GROUP_ID`, `ACTION`, `RESOURCE_CODE` |
| `T_AUTH_RESOURCE_GROUP_CONFIG` | 组配置 | `ID`, `RESOURCE_TYPE`, `GROUP_CODE`, `GROUP_NAME`, `ACTIONS` |

## 资源相关表

| 表名 | 说明 | 核心字段 |
|------|------|----------|
| `T_AUTH_RESOURCE` | 资源信息 | `ID`, `PROJECT_CODE`, `RESOURCE_TYPE`, `RESOURCE_CODE`, `RESOURCE_NAME`, `IAM_RESOURCE_CODE` |
| `T_AUTH_RESOURCE_TYPE` | 资源类型 | `ID`, `RESOURCE_TYPE`, `NAME`, `PARENT`, `SYSTEM` |
| `T_AUTH_ACTION` | 操作定义 | `ACTION`, `RESOURCE_TYPE`, `ACTION_NAME`, `ACTION_TYPE` |

## 授权相关表

| 表名 | 说明 | 核心字段 |
|------|------|----------|
| `T_AUTH_AUTHORIZATION` | 资源授权 | `ID`, `PROJECT_CODE`, `RESOURCE_TYPE`, `RESOURCE_CODE`, `HANDOVER_FROM`, `HANDOVER_TO` |
| `T_AUTH_IAM_CALLBACK` | IAM 回调 | `ID`, `GATEWAY`, `PATH`, `RESOURCE`, `SYSTEM` |

## OAuth2 相关表

| 表名 | 说明 |
|------|------|
| `T_AUTH_OAUTH2_CLIENT_DETAILS` | OAuth2 客户端信息 |
| `T_AUTH_OAUTH2_ACCESS_TOKEN` | 访问令牌 |
| `T_AUTH_OAUTH2_REFRESH_TOKEN` | 刷新令牌 |
| `T_AUTH_OAUTH2_CODE` | 授权码 |
| `T_AUTH_OAUTH2_SCOPE` | 授权范围 |
