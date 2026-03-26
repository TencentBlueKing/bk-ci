# Auth 模块核心类速查

## API 接口层

| 类名 | 路径前缀 | 职责 |
|------|----------|------|
| `ServicePermissionAuthResource` | `/service/auth/permission` | 服务间权限校验 |
| `ServiceProjectAuthResource` | `/service/auth/project` | 服务间项目权限 |
| `ServiceResourceGroupResource` | `/service/auth/resource/group` | 用户组管理 |
| `ServiceResourceMemberResource` | `/service/auth/resource/member` | 组成员管理 |
| `UserAuthApplyResource` | `/user/auth/apply` | 用户权限申请 |
| `UserAuthAuthorizationResource` | `/user/auth/authorization` | 用户授权管理 |
| `OpenPermissionAuthResource` | `/open/auth/permission` | 开放权限接口 |
| `OpenProjectAuthResource` | `/open/auth/project` | 开放项目权限 |
| `Oauth2ServiceEndpointResource` | `/service/oauth2` | OAuth2 服务端点 |

## RBAC Provider 层（核心服务）

| 类名 | 文件大小 | 职责 |
|------|----------|------|
| `RbacPermissionService` | 32KB | 权限校验核心，对接 IAM SDK |
| `RbacPermissionResourceGroupService` | 33KB | 用户组 CRUD 管理 |
| `RbacPermissionResourceMemberService` | 29KB | 组成员管理 |
| `RbacPermissionResourceService` | 21KB | 资源注册与管理 |
| `RbacPermissionManageFacadeServiceImpl` | 117KB | 权限管理门面（最大） |
| `RbacPermissionResourceGroupPermissionService` | 39KB | 组权限管理 |
| `RbacPermissionResourceGroupSyncService` | 37KB | 组同步服务 |
| `PermissionGradeManagerService` | 27KB | 分级管理员 |
| `RbacPermissionApplyService` | 30KB | 权限申请服务 |

## 通用 Service 层

| 类名 | 职责 |
|------|------|
| `PermissionAuthorizationServiceImpl` | 资源授权（代持人）管理 |
| `AuthDeptServiceImpl` | 部门信息服务 |
| `ManagerUserService` | 超级管理员管理 |
| `StrategyService` | 权限策略管理 |
| `ThirdLoginService` | 第三方登录 |
| `AuthUserBlackListService` | 用户黑名单 |

## DAO 层

| 类名 | 文件大小 | 职责 |
|------|----------|------|
| `AuthResourceGroupMemberDao` | 34KB | 组成员数据访问（最大） |
| `AuthResourceGroupDao` | 19KB | 用户组数据访问 |
| `AuthResourceDao` | 15KB | 资源数据访问 |
| `AuthResourceGroupPermissionDao` | 11KB | 组权限数据访问 |
| `AuthAuthorizationDao` | 10KB | 授权数据访问 |

## 配置类

### RBAC 配置

```
biz-auth/provider/rbac/config/RbacAuthConfiguration.kt (37KB)
```

包含 IAM 客户端配置、权限服务 Bean 定义、缓存配置。

### MQ 配置

```
biz-auth/provider/rbac/config/RbacMQConfiguration.kt
```

定义权限相关消息队列：组创建/修改事件、ITSM 回调事件、权限同步事件。
