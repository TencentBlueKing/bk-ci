## Consul Deployment

### System Requirement

- Consul 1.0+

### Consul Installation & Startup

- Download Consul

Download the appropriate [Consul](https://releases.hashicorp.com/consul/1.0.2/) package for your operating system and unzip it.

- Install Consul

Upload Consul to the server. After modifying its permission, put it in the /usr/local/sbin/ directory as global application.

```shell
# Modify the permission of Consul
chmod 755 ./consul
# Put Consul in /usr/local/sbin/
cp ./consul /usr/local/sbin/
```

- Start Consul Agent in Server Mode

```shell
consul agent -server -data-dir={consul_directory} -ui -http-port={consul_http_port} -datacenter=bkdevops -domain=bkdevops -bootstrap -client=0.0.0.0
# For example, Consul server IP=CONSOL_SERVER_IP
consul agent -server -data-dir=/data/consul -ui -http-port=8080 -datacenter=dc -domain=ci -bootstrap -client=0.0.0.0
```

- Start Consul Agent in Client Mode

```shell
consul agent -data-dir={consul_directory} -datacenter=dc -domain=ci -join={server_IP} -bind={local_IP}
# For example, Consul client IP=CONSUL_CLIENT_IP
consul agent -data-dir=/data/consul -datacenter=dc -domain=ci -join=CONSOL_SERVER_IP -bind=CONSUL_CLIENT_IP
```

<b>If your service is also deployed on Consul server agent, you can connect to the server directly without starting Consul client agent.</b>

- Parameter Description

|   Parameter Name   |   Parameter Description     |
| ------------ | ---------------- |
|   consul_directory   |  Data directory of Consul   |
|   consul_http_port   |  Port for Consul’s web UI  |
|   server_IP   |  IP address of the server |
|   local_IP   |  IP address of the current machine |

- Verification

Go to this website for verification： http://{service_IP}:8080
