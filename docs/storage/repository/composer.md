## composer仓库使用指引

#### 添加仓库：
```bash
#composer 不支持virtual仓库
curl -X POST http://{bk_repo_addr}/repository/repo/create \
-H 'Content-Type: application/json' \
-d '{
  "projectId": "projectName",
  "name": "repositoryName",
  "type": "COMPOSER",
  "category": "LOCAL|REMOTE",
  "public": true,
  "configuration": {"type": "local|remote"}
}'
```

**目前支持包文件压缩格式后缀：tar , zip , tar.gz , tgz**

全局换源

```bash
#首先把默认的源给禁用掉
composer config -g secure-http false
#再修改镜像源
composer config -g repo.packagist composer http://{bk_repo_addr}/{projectId}/{repoName}
#修改成功后可以先查看一下配置
composer config -g -l
#第二行repositories.packagist.org.url 
```



1、局部换源(仅对当前项目有效)

在当前项目下的composer.json中添加

```json
{
  "repositories": [
    {
    "type": "composer",
    "url": "http://{bk_repo_addr}/{projectId}/{repoName}"
    }
  ]
}
```



#### upload:

```bash
#通过curl 上传
curl -u {username}:{password} "http://{bk_repo_addr}/{projectId}/{repoName}/fileName" -T filePath
#Example
curl -u admin:password "http://{bk_repo_addr}/{projectId}/{repoName}/monolog-2.0.3.tar.gz" -T ~/Users/abc/monolog-2.0.3.tar.gz

```



#### install:

在php项目下执行：

```bash
composer.phar install
```

在项目中添加依赖：

```json
{
    "name": "canway/test",
    "repositories": [{   
            "type": "composer",
            "url": "http://{bk_repo_addr}/{projectId}/{repoName}" //添加依赖源
        }
    ],
    "config": {
        "secure-http" : false  //http支持
    },
    "require": {
        "php-http/httplug" : "2.0.0" //添加依赖的php包
    },
    "version": "1.0.0"
}
```



#### search:

```bash
composer search packageName
```

