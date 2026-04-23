# 责任链模式（Chain of Responsibility）

## 研发商店创建处理链

**位置**：`store/biz-store/src/main/kotlin/com/tencent/devops/store/common/handler/`

```kotlin
interface Handler<T : HandlerRequest> {
    fun canExecute(handlerRequest: T): Boolean
    fun execute(handlerRequest: T)

    fun doExecute(handlerRequest: T, chain: HandlerChain<T>) {
        if (canExecute(handlerRequest)) {
            execute(handlerRequest)
        }
        chain.handleRequest(handlerRequest)
    }
}

interface HandlerChain<T : HandlerRequest> {
    fun nextHandler(handlerRequest: T): Handler<T>?

    fun handleRequest(handlerRequest: T) {
        val handler = nextHandler(handlerRequest)
        handler?.doExecute(handlerRequest, this)
    }
}
```

**处理器链实现**：
```kotlin
class StoreCreateHandlerChain(
    private val handlerList: MutableList<Handler<StoreCreateRequest>>
) : HandlerChain<StoreCreateRequest> {
    override fun nextHandler(handlerRequest: StoreCreateRequest): Handler<StoreCreateRequest>? {
        return handlerList.removeFirstOrNull()
    }
}
```

**具体处理器**：
```kotlin
class StoreCreateParamCheckHandler : Handler<StoreCreateRequest> {
    override fun canExecute(handlerRequest: StoreCreateRequest): Boolean = true
    override fun execute(handlerRequest: StoreCreateRequest) {
        if (handlerRequest.atomCode.isBlank()) {
            throw InvalidParamException("Atom code is blank")
        }
    }
}

class StoreCreatePreBusHandler : Handler<StoreCreateRequest> { ... }
class StoreCreateDataPersistHandler : Handler<StoreCreateRequest> { ... }
```

**使用方式**（推荐 Spring 自动注入）：
```kotlin
@Service
class StoreCreateService(
    private val handlers: List<Handler<StoreCreateRequest>>
) {
    fun create(request: StoreCreateRequest) {
        val chain = StoreCreateHandlerChain(handlers.toMutableList())
        chain.handleRequest(request)
    }
}
```

## 其他责任链

| 处理链 | 用途 |
|--------|------|
| `StoreUpdateHandlerChain` | 研发商店组件更新 |
| `StoreDeleteHandlerChain` | 研发商店组件删除 |
| `PipelineInterceptorChain` | 流水线启动拦截器链 |
| `WebhookFilterChain` | Webhook 过滤器链 |
