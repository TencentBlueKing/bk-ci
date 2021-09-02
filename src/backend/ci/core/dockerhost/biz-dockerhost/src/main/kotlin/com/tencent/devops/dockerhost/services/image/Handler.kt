package com.tencent.devops.dockerhost.services.image

abstract class Handler<T : HandlerContext> {
    protected var nextHandler: Handler<T>? = null

    fun setNextHandler(handler: Handler<T>): Handler<T> {
        this.nextHandler = handler
        return this
    }
    abstract fun handlerRequest(handlerContext: T)
}
