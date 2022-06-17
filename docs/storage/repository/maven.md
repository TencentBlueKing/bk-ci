## maven仓库使用指引

#### 添加仓库：
```bash
curl -X POST http://{bk_repo_addr}/repository/repo/create \
-H 'Content-Type: application/json' \
-d '{
  "projectId": "projectName",
  "name": "repositoryName",
  "type": "MAVEN",
  "category": "LOCAL|REMOTE|VIRTUAL",
  "public": true,
  "configuration": {
  	"type": "local|remote|virtual",
  	"settings": {
            "SNAPSHOT_BEHAVIOR": 0|1,
        }
  }
}'
```

#### deploy  jar:

```bash
mvn deploy:deploy-file  
-Dfile={filePath} 
-DgroupId={group} 
-DartifactId={artifact} 
-Dversion={version} 
-Dpackaging={packageType} 
#[-DrepositoryId=file-http]  如果仓库有鉴权需带上该参数   
-Durl={repositoryUrl}

# Example
mvn deploy:deploy-file  
-Dfile=/abc/bcd/example-1.0.0.jar 
-DgroupId=com.xxx.yyy.zzz 
-DartifactId=example 
-Dversion=1.0.0 
-Dpackaging=jar 
-DrepositoryId=file-http 
-Durl=http://{bk_repo_addr}/{projectId}/{repoName}
```

mvn `conf/settings.xml` 对应账户密码设置：

```xml
<servers>
    <server>
      <id>file-http</id>  <!--与请求中repositoryId保持一致-->
      <username>admin</username>
      <password>password</password>
    </server>
  </servers>
```

