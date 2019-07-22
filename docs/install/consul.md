## Consul部署

### 系统要求

- Consul 1.0及以上版本

### consul安装及启动

- consul下载
从这里选择对应的操作系统下载[consul](https://releases.hashicorp.com/consul/1.0.2/)并解压

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
consul agent -server -data-dir=/data/consul -ui -http-port=8080 -datacenter=bkci -domain=bkci -bootstrap -client=0.0.0.0
```

- consul 客户端启动
  
```shell
consul agent -data-dir={consul_directory} -datacenter=bkdevops -domain=bkdevops -join={server_IP} -bind={local_IP}

# 例子：consul client服务器IP=10.10.10.2
consul agent -data-dir=/data/consul -datacenter=bkdevops -domain=bkdevops -join=10.10.10.1 -bind=10.10.10.2
```

<b>如果你的服务是部署也是部署在consul服务端的话，那就不需要启动consul客户端，直接连接服务端即可</b>

- 参数说明

|   参数名称   |   参数说明     |
| ------------ | ---------------- |
|   consul_directory   |  consul的数据目录    |
|   consul_http_port   |  consul管理界面的访问端口 |
|   server_IP   |  服务端的IP地址 |
|   local_IP   |  当前机器的IP地址 |

- 验证
登录网页验证： http://{service_IP}:8080
