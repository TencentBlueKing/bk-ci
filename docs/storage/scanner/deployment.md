# 服务部署

## scanner服务

### 配置 

```yaml
# 上报的扫描结果比较大的时候需要配置
spring:
  servlet:
    multipart:
      max-request-size: 100MB

# 用于存储详细扫描报告
scanner:
  spring:
    data:
      mongodb:
        uri: mongoUri
```

## scanner-executor服务

### 配置

```yaml
scanner:
  executor:
    docker:
      enabled: true
      host: unix:///var/run/docker.sock
      version: 1.23
      connect-timeout: 5000
      read-timeout: 1200000
```

### 依赖

- docker daemon
- 扫描器依赖容器镜像时机器上要有对应的容器镜像，不存在镜像但是可以拉取到时scanner-executor服务会自动拉取
- 运行服务器最好是8C/16G 以上的配置，因为扫描器目前还比较消耗资源
