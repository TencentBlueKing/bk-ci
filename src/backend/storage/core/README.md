# backend
蓝鲸制品库后端项目

## 模块说明

- common 公共模块，进行统一配置与定制化，提供微服务引用
  - common-api  接口相关，包含公共pojo类、工具类、公共常量、接口异常等
  - common-auth 认证相关，为微服务提供统一的用户认证
  - common-client 微服务客户端，提供feign client的统一配置
  - common-mongo MongoDB封装
  - common-redis Redis封装，提供常用操作以及分布式锁
  - common-service 微服务相关，提供微服务、web、swagger、日志相关的统一配置
  
- boot-assembly 统一打包模块，将所有模块打包为一个单体jar快速运行

- xxx 微服务模块
  - api-xxx 微服务提供对外的api，包含接口信息、pojo类、常量信息等。
  - biz-xxx 微服务业务实现模块
  - boot-xxx 微服务启动模块

## swagger说明

`common-service`模块中已经集成了swagger相关配置，依赖该模块则默认开启`swagger`功能。

默认已经将`/error`和`/actuator/**`相关接口排除在外。
 
### swagger开关

通过配置`swagger.enabled=false`可以关闭`swagger`功能，生产环境中可以关闭。

### swagger地址

${base-path}/v2/api-docs

配合chrome浏览器插件`Swagger UI Console`使用

## 启动说明

### idea
直接运行`xxx-boot`模块下`XXXApplication.kt`的`main`函数即可

### gradle
因为使用了`SpringBoot`的`gradle`插件，所以通过`bootRun`任务即可运行

```
运行xxx微服务
./gradlew xxx:boot-xxx:bootRun

```

## 打包说明

因为使用了`SpringBoot`的`gradle`插件，所以通过`bootJar`任务即可完成打包

```
打包单个微服务
./gradlew xxx:boot-xxx:bootJar

打包所有微服务
./gradlew bootJar

```

`SpringBoot`插件默认会关闭`jar`任务，所以在`build.gradle`中有如下配置:

```
def isBootProject = project.name.startsWith("boot-")

jar.enabled = !isBootProject
bootJar.enabled = isBootProject
bootRun.enabled = isBootProject
```
以`boot-`开头的模块才会开启`bootJar`和`bootRun`，可以打包为可执行jar，
其它模块作为依赖包，开启`jar`任务打包为jar依赖包

## 代码规范

通过`gradle`的`ktlint`任务进行扫描

```
代码格式检查
./gradlew ktlint

代码格式化
./gradlew ktlintFormat

```

## 业务类API接口定义规范说明

所有业务类API为了通用走HTTP协议，并用RESTful风格定义接口，需要遵循的业界标准的业务协议除外。

### 网关路由URI处理规范

#### 前端请求部分

该部分只涉及到前端请求与网关**微服务路由**的部分，**该部分与后台定义路径的无关**。

* 先按子服务划分api,  方便网关识别做微服务路由，例如：
  * 仓库服务(repository) /api/repository
  * metadata服务(metadata) /api/metadata
  * api为网关判断是要向后端微服务的请求(MicroService),与静态资源请求以及其请求分离
  * docker registry微服务再做特殊处理
  
#### 网关处理逻辑与后端定义部分 

- 基于上面接下来按接口调用范围划分，分有用户态的接口与后台服务间调用 (无用户态)，还是以上面几个子服务为例子：
  - 仓库服务接口(repo) 
    - 用户登录态接口  GET /api/repository/user/repos/{repoId}
    - 服务间调用接口 GET /api/repository/service/repos/{repoId}

```tex
  示例：GET /api/repository/user/repos/{repoId}
  前缀： /api/repository  
  网关拿repository查到相应仓库管理微服务的IP做转发，同时把这个/api/repository微服务标识从URI中移除
  此时请求URI变成 	/user/repos/{repoId}
```

#### 后端URI定义

后端基本上以各个子服务，所以只需要定义每个子服务中有哪些资源，并根据调用范围划分，例如: (service与user都相同，只是user替换为service而已，以及入参可能不一样)

- 一级资源操作：比如一个task就是一个资源
  - 任务列表： GET /user/repos
  - 创建任务： POST /user/repos
  - 查看任务： GET /user/repos/{taskId}
  - 修改任务： PUT /user/repos/{taskId}
  - 删除任务： DELETE /user/repos/{taskId}

- 对资源下的二级资源操作，比如要删除某个仓库下的某个资源
  - 删除某个仓库某个资源：  DELETE /user/repos/{taskId}/objects/{objectId}

如此分级可以避免路径混乱无序导致后续可能出现相同路径二义性问题。

### 错误码定义

错误码分两部分,： 接口状态码以及业务错误码

#### 接口状态码

接口是直接采用HTTP标准的状态码来区分接口调用是否成功。而到了具体接口出错信息则是由业务错误码定义输出的。

- 200 OK 服务器返回用户请求的数据
- 400 BAD REQUEST 用户发出的请求有问题，一般接口出现业务错误会用这种状态码返回
- 401 Unauthoried 表示用户没有认证，无法进行操作
- 403 Forbidden 用户访问被禁止，无法进行操作
- 500 INTERNAL SERVER ERROR 服务器内部错误，用户将无法判断发出的请求是否成功,
- 502 GATEWAY_TIMEOUT 网关请求超时，网关对后台服务正在更新无法提供服务的请求直接返回502
- 503 Service Unavailable 服务不可用状态，多半是因为服务器问题，例如CPU占用率大，等等

#### 业务错误码

业务错误码是根据接口相关业务逻辑操作时出错而输出的有标识的状态码，通过封装转换为统一的错误信息再返回给用户，也方便用户根据 错误码查到相关错误信息。

```mathematica
蓝鲸错误码：2位平台码 + 2位子服务系统码 + 3位子系统内自定义码
例如蓝鲸制品库平台码为25, 仓库子服务为01,
25(蓝鲸制品库)01(仓库子服务)001(内部业务错误码)
```
