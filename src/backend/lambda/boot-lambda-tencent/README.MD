# artifactory微服务主工程

## 打出想要的微服务包
通过build.gradle声明依赖来决定生成的微服务是通过哪个扩展来实现的
默认是：

```groovy
    compile project(":artifactory:biz-artifactory")
    compile project(":artifactory:biz-artifactory-sample")
```

- artifactory-biz 是基础实现和抽象逻辑
- artifactory-biz-sample 是对biz的一种具体业务实现

可根据自己的实现来修改依赖

## 微服务启动类入口
service/service-artifactory/src/main/kotlin/com/tencent/devops/artifactory/Application.kt 
这个不需要去改动
