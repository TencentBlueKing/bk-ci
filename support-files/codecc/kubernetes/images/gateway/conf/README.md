# bk-repo 网关

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
