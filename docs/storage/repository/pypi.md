## pypi仓库使用指引

#### 添加仓库：

```bash
curl -X POST http://{bk_repo_addr}/repository/repo/create \
-H 'Content-Type: application/json' \
-d '{
  "projectId": "projectName",
  "name": "repositoryName",
  "type": "PYPI",
  "category": "LOCAL|REMOTE|VIRTUAL|COMPOSITE",
  "public": true,
  "configuration": {"type": "local|remote|virtual|composite"}
}'

#remote 仓库
curl -X POST http://{bk_repo_addr}/repository/repo \
-H 'Content-Type: application/json' \
-d '{
  "projectId": "projectName",
  "name": "repositoryName",
  "type": "PYPI",
  "category": "REMOTE",
  "public": true,
  "configuration":{
        "type":"remote",
        "url":"https://pypi.org/"  
  }
}'
```

<b>默认在Python3下运行  </b>

**全局依赖源配置：**
配置文件路径：`~/.pip/pip.conf`
```conf
[bkrepo]
index-url = http://{bk_repo_addr}/{projectId}/{repoName}/simple
username = {user}
password = {password}
```

**依赖源地址需要加上`/simple`**

#### upload:

配置仓库地址和认证信息:$HOME/.pypirc

配置仓库地址和认证信息
```txt
[distutils]
index-servers = bkrepo
[bkrepo]
repository = http://{bk_repo_addr}/{projectId}/{repoName}/simple
username = {user}
password = {password}
```

```bash
#使用twine作为上传工具
python3 -m twine upload -r {bkrepo} dist/*
```

#### install

替换默认依赖源地址

- MacOS/Linux配置目录 :  $HOME/.pip/pip.conf
- Windows配置目录 :  %HOME%/pip/pip.ini
  ```txt
  [global]
  index-url = http://{admin}:{PASSWORD}@{bk_repo_addr}/{projectId}/{repoName}/simple
  [install]
  trusted-host=http://{bk_repo_addr}
  ```
- 执行下面命令：
  ```bash
  pip3 install {packageName}=={version}
  ```
  
指定依赖源下载
```bash
pip3 install -i http://{bk_repo_addr}/{projectId}/{repoName} {package}=={version}
```

#### Search

```bash
#查看已安装的包
pip3 list
#删除安装包
pip3 uninstall package
#search
pip3 search -i http://{bk_repo_addr}/{projectId}/{repoName} {package}|{summary}

```



