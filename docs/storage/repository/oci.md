### helm oci仓库操作指引

---

1、新建项目和仓库    [{projectId}/{repoName}]

2、登录到oci仓库 (手动输入密码)
```shell script
$ helm registry login -u {userName} {helm.registry}
Password:
Login succeeded
```

3、从仓库注销登录
```shell script
$ helm registry logout {helm.registry}
Logout succeeded
```

4、helm chart创建包
```shell script
# 创建helm包
$ helm create hello-world
```

5、helm 保存chart目录到本地缓存
```shell script
# 前面项目名称和仓库名称固定，后面目录结构可自定义
$ helm chart save hello-world/ {helm.registry}/{projectId}/{repoName}/hello-world:1.0.0
```

6、helm chart push 推送chart到远程
```shell script
$ helm chart push {helm.registry}/{projectId}/{repoName}/hello-world:1.0.0
```

7、pull 从远程拉取chart
```shell script
$ helm chart pull {helm.registry}/{projectId}/{repoName}/hello-world:1.0.0
```

8、列举出所有的chart
```shell script
$ helm chart list
```

