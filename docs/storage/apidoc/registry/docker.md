## docker扩展接口说明

### 获取manifest文件

- API: GET /docker/ext/manifest/{projectId}/{repoName}/{name}/{tag}
- API 名称: get_repo_manifest
- 功能说明：
	- 中文：获取repo对应的manifest文件
	- English：get manifest by repo and tag

- input body:


``` json

```


- input 字段说明

|字段|类型|是否必须|默认值|说明|Description|
|---|---|---|---|---|---|
|projectId|string|是|无|项目id|the project id|
|repoName|string|是|无|仓库名称| name of repo|
|name|string|是|无|docker镜像名| name of docker image|
|tag|string|是|无|repo tag |tag of docker repo|



- output:

```
{
    "code":0,
    "message":null,
    "data":"{
   "schemaVersion": 2,
   "mediaType": "application/vnd.docker.distribution.manifest.v2+json",
   "config": {
      "mediaType": "application/vnd.docker.container.image.v1+json",
      "size": 9404,
      "digest": "sha256:6146596998118de36add541f9e17075a0e40be15cfc84a7fa8efe0bbe5bdc49a"
   },
   "layers": [
      {
         "mediaType": "application/vnd.docker.image.rootfs.diff.tar.gzip",
         "size": 50379708,
         "digest": "sha256:16ea0e8c887910fe167687a0169991b4c1fc165257aab6b116f6a5e61a64e7af"
      },
      {
         "mediaType": "application/vnd.docker.image.rootfs.diff.tar.gzip",
         "size": 7811508,
         "digest": "sha256:50024b0106d53dcbd29889c65bc040439b2bb8947dac16c8c670db894a2c5ba6"
      },
      {
         "mediaType": "application/vnd.docker.image.rootfs.diff.tar.gzip",
         "size": 9996013,
         "digest": "sha256:ff95660c69375e19e287b2ea87ca9b4be008cd036e95d541515262b86cc521d9"
      },
      {
         "mediaType": "application/vnd.docker.image.rootfs.diff.tar.gzip",
         "size": 51786970,
         "digest": "sha256:9c7d0e5c0bc204b3a36e3f8ff320741da0bd0225e0a67e224c6265c1e208f80a"
      },
      {
         "mediaType": "application/vnd.docker.image.rootfs.diff.tar.gzip",
         "size": 192044870,
         "digest": "sha256:29c4fb388fdfef16e8278fba2b06d46e48d152e1b40f4347c8828a04c8e2a87e"
      },
      {
         "mediaType": "application/vnd.docker.image.rootfs.diff.tar.gzip",
         "size": 11568,
         "digest": "sha256:f87845d1b3b4e4ead42cddc30dcdcf5cf06555785f8299d304cc118126bc0dd8"
      },
      {
         "mediaType": "application/vnd.docker.image.rootfs.diff.tar.gzip",
         "size": 98930042,
         "digest": "sha256:fd6d3a50a72f31eec9fe21bc3c65888df636dc32e6cb6c44b698d73258cef077"
      },
      {
         "mediaType": "application/vnd.docker.image.rootfs.diff.tar.gzip",
         "size": 11559,
         "digest": "sha256:dcfb948bad0edeb2d030e8dc230224877ffd7f5d4a93ea43d940aaceb6d95d91"
      },
      {
         "mediaType": "application/vnd.docker.image.rootfs.diff.tar.gzip",
         "size": 1943,
         "digest": "sha256:f018ac72b8e07233e71dfd9cd1d9447c5fc034d2d8f272d7324e7b276912f8db"
      }
   ]
}",
    "traceId":""
}

```

- output 字段说明

| 字段|类型|说明|Description|
|---|---|---|---|
|code|bool|错误编码。 0表示success，>0表示失败错误 |0:success, other: failure|
|message|result message|错误消息 |the failure message |
|data | bool |the manifest data |the data of manifest|
|traceId|string|请求跟踪id|the trace id|


### 根据layerId下载layer文件

- API: GET /docker/ext/layer/{projectId}/{repoName}/{name}/{Id}
- API 名称: 
- 功能说明：
	- 中文：根据layerId获取layer文件
	- English：download layer file by layerId

- input body:

``` json

```

- input 字段说明

|字段|类型|是否必须|默认值|说明|Description|
|---|---|---|---|---|---|
|projectId|string|是|无|项目id|the project id|
|repoName|string|是|无|仓库名称| name of repo|
|name|string|是|无|docker镜像名| name of docker image|
|Id|string|是|无|layer id|the  id of layer|


- output:

```
文件流

```

- output 字段说明

### 获取指定projectId和repoName下的所有镜像

- API: GET /docker/ext/repo/{projectId}/{repoName}?pageNumber=0&pageSize=10&name=docker
- API 名称: get_image_by_project_repository
- 功能说明：
	- 中文：获取指定project和仓库下的所有docker镜像
	- English：get image by project and repository

- input body:


``` json

```


- input 字段说明

|字段|类型|是否必须|默认值|说明|Description|
|---|---|---|---|---|---|
|projectId|string|是|无|项目id|the project id|
|repoName|string|是|无|仓库名称| name of repo|
|pageNumber|Int|是|无|页码数| number of page|
|pageSize|Int|是|无|每页大小| limit of page|
|name|string|否|无|镜像名称| image name|



- output:

```
{
    "code":0,
    "message":null,
    "data":{
        "toatalRecords":100,
        "records":[
            {
                "name":"hello-world",
                "lastModifiedBy":"admin",
                "lastModifiedDate":"2020-09-10T14:48:22.846",
                "downloadCount":0,
                "logoUrl":"",
                "description":""
            },
            {
                "name":"mongo",
                "lastModifiedBy":"admin",
                "lastModifiedDate":"2020-08-28T12:07:12.672",
                "downloadCount":0,
                "logoUrl":"",
                "description":""
            }
        ]
    },
    "traceId":""
}

```

- output 字段说明

| 字段|类型|说明|Description|
|---|---|---|---|
|code|bool|错误编码。 0表示success，>0表示失败错误 |0:success, other: failure|
|message|result message|错误消息 |the failure message |
|data | string array |image 名称列表 |the list of image|
|traceId|string|请求跟踪id|the trace id|
|name|string|镜像ID|the id of image|
|lastModifiedBy|string|创建人|the creator of image|
|lastModifiedDate|time|创建时间|create date of image|

### 获取repo的所有tag

- API: GET /docker/ext/tag/{projectId}/{repoName}/{name}?pageNumber=0&pageSize=10&tag=v1
- API 名称: get_repo_tag_list
- 功能说明：
	- 中文：获取repo对应的manifest文件
	- English：get image tag by repo 

- input body:


``` json

```


- input 字段说明

|字段|类型|是否必须|默认值|说明|Description|
|---|---|---|---|---|---|
|projectId|string|是|无|项目id|the project id|
|repoName|string|是|无|仓库名称| name of repo|
|name|string|是|无|docker 镜像名称| name of docker image|
|pageNumber|Int|是|无|页码数| number of page|
|pageSize|Int|是|无|每页大小| limit of page|
|tag|string|否|无|tag名称| the tag name|



- output:

```
{
    "code":0,
    "message":null,
    "data":{
        "toatalRecords":100,
        "records":[
            {
                "tag":"latest",
                "stageTag":"",
                "size":524,
                "lastModifiedBy":"admin",
                "lastModifiedDate":"2020-09-10T14:48:22.846",
                "downloadCount":0,
                "registryUrl":"bkrepo.example.com/test/test/php:latest"
            },
            {
                "tag":"v1",
                "stageTag":"",
                "size":524,
                "lastModifiedBy":"admin",
                "lastModifiedDate":"2020-09-10T14:49:37.904",
                "downloadCount":0,
                "registryUrl":"bkrepo.example.com/test/test/php:v1"
            }
        ]
    },
    "traceId":""
}
```

- output 字段说明

| 字段|类型|说明|Description|
|---|---|---|---|
|code|bool|错误编码。 0表示success，>0表示失败错误 |0:success, other: failure|
|message|result message|错误消息 |the failure message |
|data | string array | image的tag列表 |tag list of the image|
|traceId|string|请求跟踪id|the trace id|
|tag|string|tag名称|the name of tag|
|stageTag|string|制品状态|the status of image|
|size|Int|镜像大小|the size of image|
|downloadCount|Int|下载次数|the download count of image|
|lastModifiedBy|date|更新人|the man upload it|
|lastModifiedDate|date|更新时间|the modified date|


### 删除指定projectId和repoName下的的镜像

- API: DELETE /docker/ext/package/delete/{projectId}/{repoName}?packageKey={packageKey}
- API 名称: delete_image_by_project_repository_name
- 功能说明：
	- 中文：删除指定project和仓库下name的镜像
	- English：delete image by project and repository and image name

- input body:


``` json

```


- input 字段说明

|字段|类型|是否必须|默认值|说明|Description|
|---|---|---|---|---|---|
|projectId|string|是|无|项目id|the project id|
|repoName|string|是|无|仓库名称| name of repo|
|packageKey|string|是|无|镜像名称| name of image|



- output:

```
{
    "code":0,
    "message":null,
    "traceId":"",
    "data":true
}

```

- output 字段说明

| 字段|类型|说明|Description|
|---|---|---|---|
|code|bool|错误编码。 0表示success，>0表示失败错误 |0:success, other: failure|
|message|result message|错误消息 |the failure message |
|data | bool |删除结果|the result of delete|
|traceId|string|请求跟踪id|the trace id|

### 删除指定projectId和repoName下的的镜像的镜像tag

- API: DELETE /docker/ext/version/delete/{projectId}/{repoName}?packageKey={packageKey}&version={version}
- API 名称: delete_image_by_project_repository_name_tag
- 功能说明：
	- 中文：删除指定project和仓库下name的镜像
	- English：delete image by project and repository and image name

- input body:


``` json

```


- input 字段说明

|字段|类型|是否必须|默认值|说明|Description|
|---|---|---|---|---|---|
|projectId|string|是|无|项目id|the project id|
|repoName|string|是|无|仓库名称| name of repo|
|packageKey|string|是|无|镜像名称| name of image|
|version|string|是|无|镜像名称| tag of image|



- output:

```
{
    "code":0,
    "message":null,
    "traceId":"",
    "data":true
}

```

- output 字段说明

| 字段|类型|说明|Description|
|---|---|---|---|
|code|bool|错误编码。 0表示success，>0表示失败错误 |0:success, other: failure|
|message|result message|错误消息 |the failure message |
|data | bool |删除结果 | the result of delete|
|traceId|string|请求跟踪id|the trace id|

### 获取repo下指定tag的详情

- API: GET /docker/ext/version/detail/{projectId}/{repoName}?packageKey={packageKey}&version={version}
- API 名称: get_repo_tag_detail
- 功能说明：
	- 中文：获取repo下tag对象的镜像相亲
	- English：get image repo tag detail

- input body:


``` json

```


- input 字段说明

|字段|类型|是否必须|默认值|说明|Description|
|---|---|---|---|---|---|
|projectId|string|是|无|项目id|the project id|
|repoName|string|是|无|仓库名称| name of repo|
|packageKey|string|是|无|镜像名称| name of image|
|version|string|是|无|镜像名称| tag of image|



- output:

```
{
    "code":0,
    "message":null,
    "data":{
        "basic":{
            "domain":"bkrepo.example.com",
            "size":2487,
            "version":"v1",
            "createdBy":"owen",
            "createdDate":"2020-09-17 03:48:42.896Z",
            "lastModifiedBy":"admin",
            "lastModifiedDate":"2020-09-10T14:49:37.904",
            "downloadCount":0,
            "sha256":"fce289e99eb9bca977dae136fbe2a82b6b7d4c372474c9235adc1741675f587e",
            "os":"linux"
        },
        "history":[
            {
                "created":"2019-01-01T01:29:27.416803627Z",
                "created_by":"/bin/sh -c #(nop) COPY file:f77490f70ce51da25bd21bfc30cb5e1a24b2b65eb37d4af0c327ddc24f0986a6 in / "
            },
            {
                "created":"2019-01-01T01:29:27.650294696Z",
                "created_by":"/bin/sh -c #(nop)  CMD ["/hello"]"
            }
        ],
        "metadata":{
            "docker.manifest":"v1",
            "sha256":"92c7f9c92844bbbb5d0a101b22f7c2a7949e40f8ea90c8b3bc396879d95e899a",
            "docker.repoName":"hello-world",
            "docker.manifest.digest":"sha256:92c7f9c92844bbbb5d0a101b22f7c2a7949e40f8ea90c8b3bc396879d95e899a",
            "docker.manifest.type":"application/vnd.docker.distribution.manifest.v2+json"
        },
        "layers":[
            {
                "mediaType":"application/vnd.docker.image.rootfs.diff.tar.gzip",
                "size":977,
                "digest":"sha256:1b930d010525941c1d56ec53b97bd057a67ae1865eebf042686d2a2d18271ced"
            }
        ]
    },
    "traceId":""
}
```

- output 字段说明

| 字段|类型|说明|Description|
|---|---|---|---|
|code|bool|错误编码。 0表示success，>0表示失败错误 |0:success, other: failure|
|message|result message|错误消息 |the failure message |
|data | object | tag详情数据 |tag list of the image|
|traceId|string|请求跟踪id|the trace id|
|basic|object|基础数据|the basic data|
|history|object array|镜像构建历史|the history of build|
|metadata|object|元数据信息|the metadata of image tag|
|layers|object array|层级信息|the layer info of image|

### 获取docker仓库地址

- API: GET /docker/ext/addr
- API 名称: get_docker_repo_addr
- 功能说明：
	- 中文：获取docker镜像仓库配置地址
	- English：get docker repo addr

- input body:


``` json

```


- input 字段说明




- output:

```
{
    "code":0,
    "message":null,
    "data":"docker.bk.com",
    "traceId":""
}
```

- output 字段说明

| 字段|类型|说明|Description|
|---|---|---|---|
|code|bool|错误编码。 0表示success，>0表示失败错误 |0:success, other: failure|
|message|result message|错误消息 |the failure message |
|data | string |docker仓库地址|the docker registry addr|
|traceId|string|请求跟踪id|the trace id|


### 鉴权示例



