# bk-ci 网关部署文档

蓝鲸ci网关服务基于Nginx+OpenResty，部署于微服务与用户设备中间层，提供用户访问验证、访问日志记录、流量控制、防爬虫、后端服务分发功能。

## 系统要求

- OpenResty 1.11.2.4版本及以上
- Consul 1.0及以上版本


## 安装说明

这里以tlinux2.2环境来对安装已经说明。

### consul安装及启动

- consul安装

将consul应用上传到服务器上,修改文件权限后，作为全局应用放到`/usr/local/sbin/`目录下。
```shell
# 修改consul程序mod
chmod 755 ./consul
# 将consul程序放到`/usr/local/sbin/`即可
cp ./consul /usr/local/sbin/
```

- consul 服务端启动
  
```shell
consul agent -server -data-dir={consul_directory} -ui -http-port={consul_http_port} -datacenter=bkdevops -domain=bkdevops -bootstrap -client=0.0.0.0
# 例子：consul server服务器IP=10.10.10.1
consul agent -server -data-dir=/data/consul -ui -http-port=8080 -datacenter=bkdevops -domain=bkdevops -bootstrap -client=0.0.0.0
```

- consul 客户端启动
  
```shell
consul agent -data-dir={consul_directory} -datacenter=bkdevops -domain=bkdevops -join={server_IP} -bind={local_IP}

# 例子：consul client服务器IP=10.10.10.2
consul agent -data-dir=/data/consul -datacenter=bkdevops -domain=bkdevops -join=10.10.10.1 -bind=10.10.10.2
```

- 参数说明

|   参数名称   |   参数说明     |
| ------------ | ---------------- |
|   consul_directory   |  consul的数据目录    |
|   consul_http_port   |  consul管理界面的访问端口 |
|   server_IP   |  服务端的IP地址 |
|   local_IP   |  当前机器的IP地址 |

- 验证
登录网页验证： http://{service_IP}:8080

### OpenResty安装及启动

- 上传安装和部署文件

在安装和部署网关之前，需要将相关的安装包和部署包上传到相应的服务器，下面是需要上传的文件。

|   文件名称   |   文件说明     |
| ------------ | ---------------- |
|   openresty-openssl-1.0.2k-1.el7.centos.x86_64.rpm   |  openresy依赖的openssl包    |
|   openresty-pcre-8.40-1.el7.centos.x86_64.rpm   |  openresy依赖的pcre包 |
|   openresty-zlib-1.2.11-3.el7.centos.x86_64.rpm   |  openresy依赖的zlib包 |
|   openresty-1.11.2.5-2.el7.centos.x86_64.rpm   |  openresy安装包 |

- 安装openresty

网关部署依赖于lua脚本做鉴权和转发，所以这里需要安装openresty，使用的版本：1.11.2.5-2。rpm安装包请查看附件，下面是安装命令。

```shell
# 修改安装包权限
chmod 644 openresty-1.11.2.5-2.el7.centos.x86_64.rpm
chmod 644 openresty-openssl-1.0.2k-1.el7.centos.x86_64.rpm
chmod 644 openresty-pcre-8.40-1.el7.centos.x86_64.rpm
chmod 644 openresty-zlib-1.2.11-3.el7.centos.x86_64.rpm
# 开始安装
rpm -ivh openresty-pcre-8.40-1.el7.centos.x86_64.rpm
rpm -ivh openresty-zlib-1.2.11-3.el7.centos.x86_64.rpm
rpm -ivh openresty-openssl-1.0.2k-1.el7.centos.x86_64.rpm  --replacefiles
rpm -ivh openresty-1.11.2.5-2.el7.centos.x86_64.rpm
# 检测安装情况
cd /usr/local/openresty/nginx && ./sbin/nginx -v
```

最后提示`"nginx version: openresty/1.11.2.5"`即安装成功。

### 部署并启动bk-ci网关

网关主要是配置文件和lua脚本，所以只需要将网关gateway的代覆盖到nginx的conf目录(`/usr/local/openresty/nginx/conf/`)，并启动nginx即可完成网关gateway的部署和启动。

#### 命令

```shell
cd /usr/local/openresty/nginx # 进入nginx安装目录
./sbin/nginx  # 启动nginx
./sbin/nginx -t  # 验证nginx的配置是否正确
./sbin/nginx -s reload # 重启nginx
```
