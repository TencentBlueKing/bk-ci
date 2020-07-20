# REST接口规范

所有业务类API为了通用走HTTP协议，并用RESTful风格定义接口，需要遵循的业界标准的业务协议除外。

## 网关路由URI处理规范

### 前端请求部分

该部分只涉及到前端请求与网关**微服务路由**的部分，**该部分与后台定义路径的无关**。

- 先按子服务划分api, 方便网关识别做微服务路由，例如：
  - 仓库服务(repository) /api/repository
  - metadata服务(metadata) /api/metadata
  - api为网关判断是要向后端微服务的请求(MicroService),与静态资源请求以及其请求分离
  - docker registry微服务再做特殊处理

### 网关处理逻辑与后端定义部分

- 基于上面接下来按接口调用范围划分，分有用户态的接口与后台服务间调用 (无用户态)，还是以上面几个子服务为例子：
  - 仓库服务接口(repo)
    - 用户登录态接口 GET /api/repository/user/repos/{repoId}
    - 服务间调用接口 GET /api/repository/service/repos/{repoId}

```
  1. 示例：GET /api/repository/user/repos/{repoId}
  2. 前缀： /api/repository  
  3. 网关拿repository查到相应仓库管理微服务的IP做转发，同时把这个/api/repository微服务标识从URI中移除
  4. 此时请求URI变成  /user/repos/{repoId}
```

### 后端URI定义

后端基本上以各个子服务，所以只需要定义每个子服务中有哪些资源，并根据调用范围划分，例如: (service与user都相同，只是user替换为service而已，以及入参可能不一样)

- 一级资源操作：比如一个task就是一个资源
  - 任务列表： GET /user/tasks
  - 创建任务： POST /user/tasks
  - 查看任务： GET /user/tasks/{taskId}
  - 修改任务： PUT /user/tasks/{taskId}
  - 删除任务： DELETE /user/tasks/{taskId}
- 对资源下的二级资源操作，比如要删除某个任务下的某个规则
  - 删除某个任务下某个规则： DELETE /user/tasks/{taskId}/rules/{objectId}

如此分级可以避免路径混乱无序导致后续可能出现相同路径二义性问题。

### 错误码定义

错误码分两部分,： 接口状态码以及业务错误码

#### 接口状态码

接口是直接采用HTTP标准的状态码来区分接口调用是否成功。而到了具体接口出错信息则是由业务错误码定义输出的。

- 200 OK 服务器返回用户请求的数据
- 400 BAD REQUEST 用户发出的请求有问题，一般接口出现业务错误会用这种状态码返回
- 401 Unauthoried 表示用户没有认证，无法进行操作，需要进行登录
- 403 Forbidden 用户没有权限访问被禁止，无法进行操作
- 500 INTERNAL SERVER ERROR 服务器内部错误，用户将无法判断发出的请求是否成功,
- 502 GATEWAY_TIMEOUT 网关请求超时，网关对后台服务正在更新无法提供服务的请求直接返回502
- 503 Service Unavailable 服务不可用状态，多半是因为服务器问题，例如CPU占用率大，等等

#### 业务错误码

业务错误码是根据接口相关业务逻辑操作时出错而输出的有标识的状态码，通过封装转换为统一的错误信息再返回给用户，也方便用户根据 错误码查到相关错误信息。

```
1. 蓝鲸各平台的错误码：2位平台码 + 2位子服务系统码 + 3位子系统内自定义码
2. 例如bkci平台码为21, 流水线微服务为01,
3. 21(bkci平台)01(流水线子服务)001(内部业务错误码)
```
