# bk-ci 网关部署文档

蓝鲸ci网关服务基于Nginx+OpenResty，部署于微服务与用户设备中间层，提供用户访问验证、访问日志记录、流量控制、防爬虫、后端服务分发功能。

## 系统要求

- OpenResty 1.13.6版本及以上


## 安装说明

这里以 CentOS 7.x 环境来对安装已经说明。

### OpenResty安装及启动

- 上传安装和部署文件

在安装和部署网关之前，需要将相关的安装包和部署包上传到相应的服务器，下面是需要上传的文件。

|   文件名称   |   文件说明     |
| ------------ | ---------------- |
|   [openresty-openssl-1.1.0h-3.el7.centos.x86_64.rpm](https://openresty.org/package/centos/7/x86_64/openresty-openssl-1.1.0h-3.el7.centos.x86_64.rpm)   |  openresty依赖的openssl包    |
|   [openresty-pcre-8.42-1.el7.centos.x86_64.rpm](https://openresty.org/package/centos/7/x86_64/openresty-pcre-8.42-1.el7.centos.x86_64.rpm)   |  openresty依赖的pcre包 |
|   [openresty-zlib-1.2.11-3.el7.centos.x86_64.rpm](https://openresty.org/package/centos/7/x86_64/openresty-zlib-1.2.11-3.el7.centos.x86_64.rpm)   |  openresty依赖的zlib包 |
|   [openresty-1.13.6.2-1.el7.centos.x86_64.rpm](https://openresty.org/package/centos/7/x86_64/openresty-1.13.6.2-1.el7.centos.x86_64.rpm)   |  openresty安装包 |

- 安装openresty

网关部署依赖于lua脚本做鉴权和转发，所以这里需要安装openresty，使用的版本：1.13.6.2。rpm安装包请查看附件，下面是安装命令。

```shell
# 修改安装包权限
chmod 644 openresty-1.13.6.2-1.el7.centos.x86_64.rpm
chmod 644 openresty-openssl-1.1.0h-3.el7.centos.x86_64.rpm
chmod 644 openresty-pcre-8.42-1.el7.centos.x86_64.rpm
chmod 644 openresty-zlib-1.2.11-3.el7.centos.x86_64.rpm
# 开始安装
rpm -ivh openresty-pcre-8.42-1.el7.centos.x86_64.rpm
rpm -ivh openresty-zlib-1.2.11-3.el7.centos.x86_64.rpm
rpm -ivh openresty-openssl-1.1.0h-3.el7.centos.x86_64.rpm  --replacefiles
rpm -ivh openresty-1.13.6.2-1.el7.centos.x86_64.rpm
# 检测安装情况
cd /usr/local/openresty/nginx && ./sbin/nginx -v
```

最后提示`"nginx version: openresty/1.13.6.2"`即安装成功。

### 部署并启动bk-ci网关

网关主要是配置文件和lua脚本，所以只需要将网关gateway的外链到nginx的conf目录
```shell
1. 拷贝nginx代码到部署目录 - cp -r {{code}}/gateway __INSTALL_PATH__/__MODULE__/
2. 外链部署目录到nginx的config - rm -rf /usr/local/openresty/nginx/conf && ln -s __INSTALL_PATH__/__MODULE__//gateway /usr/local/openresty/nginx/conf
```

#### 命令

```shell
mkdir -p /usr/local/openresty/nginx/run/ # 创建PID目录
cd /usr/local/openresty/nginx # 进入nginx安装目录
./sbin/nginx  # 启动nginx
./sbin/nginx -t  # 验证nginx的配置是否正确
./sbin/nginx -s reload # 重启nginx
```
