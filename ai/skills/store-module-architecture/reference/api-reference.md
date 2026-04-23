# Store 模块 API 与服务层参考

## API 接口层

| 类名 | 路径前缀 | 职责 |
|------|----------|------|
| `UserMarketAtomResource` | `/user/market/atom` | 用户插件市场操作 |
| `UserAtomReleaseResource` | `/user/market/atom/release` | 插件发布 |
| `ServiceAtomResource` | `/service/atoms` | 服务间插件查询 |
| `ServiceMarketAtomResource` | `/service/market/atom` | 服务间市场插件 |
| `OpAtomResource` | `/op/market/atom` | 运维插件管理 |
| `UserTemplateResource` | `/user/market/template` | 模板管理 |
| `UserMarketImageResource` | `/user/market/image` | 镜像管理 |
| `UserStoreMemberResource` | `/user/store/member` | 成员管理 |

## Service 层

| 类名 | 职责 |
|------|------|
| `MarketAtomService` | 插件市场核心服务 |
| `AtomReleaseService` | 插件发布流程 |
| `AtomService` | 插件基础操作 |
| `MarketAtomEnvService` | 插件执行环境 |
| `MarketAtomArchiveService` | 插件归档 |
| `AtomCooperationService` | 插件协作 |
| `AtomNotifyService` | 插件通知 |

## DAO 层

| 类名 | 文件大小 | 职责 |
|------|----------|------|
| `AtomDao` | 59KB | 插件主表访问（最大） |
| `MarketAtomDao` | 31KB | 市场插件访问 |
| `StoreProjectRelDao` | 25KB | 项目关联访问 |
| `StoreBaseQueryDao` | 20KB | 基础查询 |
| `MarketAtomEnvInfoDao` | 13KB | 插件环境访问 |

## 服务间调用示例

```kotlin
// Process 模块获取插件信息
// 注意：projectCode 是 T_PROJECT.english_name
client.get(ServiceAtomResource::class).getAtomByCode(
    atomCode = atomCode,
    username = userId
)

// 获取插件执行环境
client.get(ServiceMarketAtomEnvResource::class).getAtomEnv(
    projectCode = projectId,  // english_name
    atomCode = atomCode,
    atomVersion = version
)

// 获取项目可用的插件列表
client.get(ServiceMarketAtomResource::class).getProjectElements(
    projectCode = projectId
)
```

## 插件查询示例

```kotlin
// 根据插件代码查询最新版本
val atom = atomDao.getLatestAtomByCode(
    dslContext = dslContext,
    atomCode = atomCode
)

// 查询项目可用的插件
val atoms = atomDao.getProjectAtoms(
    dslContext = dslContext,
    projectCode = projectId,  // english_name
    classifyCode = classifyCode
)

// 查询插件执行环境
val envInfo = marketAtomEnvInfoDao.getMarketAtomEnvInfo(
    dslContext = dslContext,
    atomId = atomId
)
```

## 处理器链模式

Store 模块使用责任链模式处理组件的创建、更新、删除：

```kotlin
class StoreCreateHandlerChain {
    private val handlers = listOf(
        StoreCreateParamCheckHandler,    // 参数校验
        StoreCreatePreBusHandler,        // 前置业务处理
        StoreCreateDataPersistHandler,   // 数据持久化
        StoreCreatePostBusHandler        // 后置业务处理
    )

    fun handle(context: StoreContext) {
        handlers.forEach { it.handle(context) }
    }
}
```

新增组件类型时需要：
1. 在 `StoreTypeEnum` 添加新类型
2. 创建对应的主表和关联表
3. 创建 DAO、Service、Resource 层代码
4. 在处理器链中注册新类型的处理器
