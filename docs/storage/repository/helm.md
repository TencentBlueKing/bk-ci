### 操作指引

______

1、新建项目和仓库    test/helm-local

2、helm添加仓库源

```powershell
$ helm repo add localhost http://localhost:10021/test/helm-local/
```

3、显示仓库列表

```powershell
$ helm repo list
```

4、上传包

```powershell
# 创建helm包
$ helm create mychart
# 打包
$ helm package ./
# 上传
$ helm push mychart-0.1.0.tgz localhost
```

5、install

```powershell
# 下载会提示先执行repo update操作
$ helm repo update
# 下载指定包
$ helm install localhost/mychart
```

6、查询相关接口

```powershell
# 通过postman测试即可

# 查询仓库所有chart 
# GET /api/charts - list all charts
$ http://localhost:10021/test/helm-local/api/charts

# 查询某个chart
# GET /api/charts/<name> - list all versions of a chart
$ http://localhost:10021/test/helm-local/api/charts/mychart

# 查询指定版本的chart
# GET /api/charts/<name>/<version> - describe a chart version
$ http://localhost:10021/test/helm-local/api/charts/mychart/0.1.0


HEAD /api/charts/<name> - check if chart exists (any versions)
HEAD /api/charts/<name>/<version> - check if chart version exists
```

