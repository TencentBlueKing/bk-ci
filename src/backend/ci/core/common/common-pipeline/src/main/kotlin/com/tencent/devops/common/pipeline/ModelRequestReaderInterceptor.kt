package com.tencent.devops.common.pipeline

import com.tencent.devops.common.web.RequestFilter
import jakarta.ws.rs.ext.Provider
import jakarta.ws.rs.ext.ReaderInterceptor
import jakarta.ws.rs.ext.ReaderInterceptorContext

/**
 * 接口入参读取拦截器：对所有 HTTP 请求体的反序列化统一关闭 Model 公共变量展开。
 *
 * 语义约定：
 * - 请求传参中的 model 是用户提交的原始定义，不应在反序列化阶段被展开/同步，
 *   否则会覆盖用户编辑内容，并在后续保存时被持久化，破坏"存引用、用时展开"的设计。
 * - DB 读取 model、接口返回报文的客户端反序列化等场景不经过本拦截器（非 Jersey 服务端入口），
 *   保持默认展开，无需改动业务代码。
 */
@Provider
@RequestFilter
class ModelRequestReaderInterceptor : ReaderInterceptor {

    override fun aroundReadFrom(context: ReaderInterceptorContext): Any? {
        return ModelPublicVarExpansion.withoutExpansion {
            context.proceed()
        }
    }
}
