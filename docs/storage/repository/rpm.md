## rpm仓库使用指引

#### 创建仓库：

```bash
curl -X POST http://{bk_repo_addr}/repository/repo/create \
-H 'Content-Type: application/json' \
-d '{ 
  "projectId": "{project}",
  "name": "{repo}",
  "type": "RPM",
  "category": "COMPOSITE",
  "public": true,
  "configuration": {
    "type": "local",  
    "settings": {
            "enabledFileLists": false,
            "repodataDepth": 1,
            "groupXmlSet": [
            	"groups.xml",
      				"bkrepos.xml"
            ]
        }
  },
  "description": "for bkdevops test"
}'
```



```bash
#新增仓库配置分组文件名
curl -X PUT http://{bk_repo_addr}/{projectId}/{repoName}/rpm/configuration/{project}/{repo}/ \
-H 'Content-Type: application/json' \
-d '[
      "abc.xml",
      "bkrepo.xml"
    ]'

#移除仓库配置分组文件名
curl -X DELETE http://{bk_repo_addr}/{projectId}/{repoName}/rpm/configuration/{project}/{repo}/ \
-H 'Content-Type: application/json' \
-d '[
      "abc.xml",
      "bkrepos.xml"
    ]'
```





仓库地址配置：
=======
仓库地址配置：

配置文件目录：/etc/yum.repos.d/

全局默认配置文件：CentOS-Base.repo

或者自定义：{name}.repo

参考数据格式：

```txt
[bkrepo]        
name=bkrepo     //仓库名
baseurl=http://admin:password@{bk_repo_addr}/{projectId}/{repoName}/$releasever/os/$basearch //仓库地址，如果有开启认证，需要在请求前添加 用户名：密码
keepcache=0  //是否开启缓存，测试阶段推荐开启，否则上传后，yum install 时会优先去本地缓存找
enabled=1    //地址授信，如果非 https 环境必须设为1
gpgcheck=0   //设为0，目前还不支持gpg签名
metadata_expire=1m  //本地元数据过期时间 ，测试阶段数据量不大的话，时间越短测试越方便
```

```shell
#发布
curl -u{admin}:{password} -XPUT http://{bk_repo_addr}/{projectId}/{repoName} -T {文件路径}

#下载
yum install -y {package}
# 添加源后快速使源生效
yum check-update
#从指定源下载
yum install nginx --enablerepo={源名称}
#删除包
yum erase -y {package}
#清除缓存的包
yum clean packages --enablerepo={源名称}
#清除对应仓库的元数据
yum clean metadata --enablerepo={源名称}

#上传组文件,必须上传至仓库repodata 目录
curl -u{admin}:{password} -XPUT http://{bk_repo_addr}/{projectId}/{repoName}/{repodata}/ -T {文件路径}

#group 列表
yum grouplist
#下载组包含的包
yum groupinstall {group}
#在系统中移除组，不会移除安装的包
yum groups mark remove {group}
#移除通过组安装的包，在系统中组依然是被安装
yum groupremove {group}
#升级group下所有包
yum groupupdate {groupName}
```

<hr/>

#### repodataDepth 解释：

repodataDepth： 代表索引生成的目录的位置（对应请求的层级）,当rpm构件`deploy`请求路径参数小于repodataDepth大小时（project, repo 不包含在请求路径中）,不会计算构件索引。

##  rpm仓库属性解释：

**repodataDepth**： 代表索引生成的目录的位置（对应请求的层级）,当rpm构件`deploy`请求路径参数小于repodataDepth大小时（project, repo 不包含在请求路径中）,不会计算构件索引。

例如：repodataDepth：2 ，项目名：bkrepo ，仓库名：rpm-local。

请求1：`curl -uadmin:password -XPUT http://{host}:{port}/bkrepo/rpm-local/a/b/ -T {filePath}`

请求2：`curl -uadmin:password -XPUT http://{host}:{port}/bkrepo/rpm-local/a/ -T {filePath}`

请求3：`curl -uadmin:password -XPUT http://{host}:{port}/bkrepo/rpm-local/a/b/c/ -T {filePath}`

-- 请求1的构件将会计算索引，`yum install` 可以下载到，索引目录：`/a/b/repodata`

-- 请求2的构件不会计算索引，`yum install` 下载不到，但是包会保存在服务器上。

-- 请求3的构件将会计算索引，`yum install` 可以下载到，索引目录：`/a/b/repodata`



**enabledFileLists**:  是否启用单独的filelists.xml 索引，开启后rpm包的filelists信息会单独保存在filelists.xml中，否则部分文件信息保存在primary.xml中。



**groupXmlSet**： 仓库分组设置，添加仓库允许的分组文件列表，只有出现在列表中的分组文件才会被计算索引，客户端才可以下载到。



## rpm package

官方文档：[RPM Packaging Guide](https://rpm-packaging-guide.github.io/)

```shell
#安装打包工具
yum install gcc rpm-build rpm-devel rpmlint make python bash coreutils diffutils patch rpmdevtools

#在一个空白目录新建一个`.spec`文件
touch xxx.spec    
#打包
rpmdev-setuptree
rpmbuild -ba hello-world.spec
```



参考数据：

```txt
Name:       bkrepo-test
Version:    1.1
Release:    1
Summary:    Test same artifactUri but different content!
License:    FIXME

%description
This is my first RPM package, which does nothing.

%prep
# we have no source, so nothing here

%build
cat > bkrepo-test.sh <<EOF
#!/usr/bin/bash
echo bkrepo test
EOF

%install
mkdir -p %{buildroot}/usr/bin/
install -m 755 bkrepo-test.sh %{buildroot}/usr/bin/bkrepo-test.sh

%files
/usr/bin/bkrepo-test.sh

%changelog
# let's skip this for now
```



###  rpm group

```shell
#分组文件生成
yum-groups-manager \
-n "bkrepo" \  group name
--id=bkrepo \  group id
--save=bkrepo.xml \保存的文件名
--mandatory {包名} {包名} 
```
