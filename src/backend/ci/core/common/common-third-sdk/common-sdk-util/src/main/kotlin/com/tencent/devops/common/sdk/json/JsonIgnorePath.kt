package com.tencent.devops.common.sdk.json

/**
 *  跨服务传输request,如果使用JsonIgnore注解,在服务之间传输时会出现对象序列化时值没有传输过来
 *  但是封装http请求时，如果是路径参数不希望在请求参数出现，需要忽略路径参数,所以使用自定义注解忽略
 */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class JsonIgnorePath
