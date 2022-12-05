1、新建三个仓库

2、npm设置registry

```powershell
$ npm config set registry http://artifact.mac.cn:10001/test/npm-virtual/
```

3、npm下载包

```powershell
$ npm -loglevel info i underscore -registry=http://artifact.mac.cn:10001/test/npm-virtual/
```

4、npm上传包

```powershell
# 上报本地的包需要先进行登录，注意这里的地址是使用本地仓库
$ npm login -registry=http://artifact.mac.cn:10001/test/npm-local/

# 发布包
$ npm -loglevel info publish -registry=http://artifact.mac.cn:10001/test/npm-local/

# dist-tag 发布带指定tag的包
$ npm publish --tag beta

# 将该tag包转换为正式的最新版本
# 作用是将1.0.2这个版本的包作为latest的最新包
$ npm dist-tag add jfrog-npm-publish@1.0.2 latest -regsitry=http://artifact.canwaysoft.cn/api/test/npm-local/
```

5、包的废弃

```powershell
# 废弃包
npm -loglevel info deprecate helloworld-npm-publish@"< 1.0.2" "critical bug fixed in v1.0.2"    -registry=http://artifact.mac.cn:10001/test/npm-local/
```











