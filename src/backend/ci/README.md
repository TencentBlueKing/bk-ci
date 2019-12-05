# 改版说明：

为了方便版本部署，做了配置分离，将原本放在application.yml中的一些环境
相关的配置分离

```
|- support-files
    |- etc          这个目录是存放开发调试用的配置文件--不允许提交到仓库，会无意泄露敏感信息
        |- common.yml 这个是通用的配置，比如MQ，Redis，权限中心等
        |- application-xxx.yml  这个是具体微服务所要的配置，比如数据库
    |- sql          建库SQL脚本
    |- templates    使用占位符号将etc中的一些环境变量替换，在部署时会自动替换
```
## 本地开发如何启动
- 先修改support-files/etc下相应的配置文件，将你的数据库等环境配置正确。
- 在IEDA中启动时通过 增加 -Dspring.config.location 指定配置文件。比如：
 -Dspring.config.location=file:support-files/etc/common.yml,file:support-files/etc/application-log.yml
- 配置文件的加载顺序是： resources/application.yml, -Dspring.config.location指定的配置文件，最后的文件中的配置会覆盖前面文件配置有的相同的配置项


## 扩展开发说明

### 在plugin微服务中增加新REST接口

