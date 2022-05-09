# bk-ci Gateway Deployment Document

The gateway service of BlueKing ci is based on Nginx and OpenResty. It is deployed on the middle layer between microservice and user device. Its functionality includes user access verification, user access logging, flow control, anti-crawler protection and backend service distribution.

## System Requirement

- OpenResty 1.17.8.2+

## Installation Guide

Here the CentOS 7.x environment is used for illustration purpose.

### Install and Start OpenResty

- Upload Installation and Deployment Files

Before installing and deploying the gateway, upload relevant installation and deployment packages to the corresponding servers. Below are files to upload.
|   File Name   |   File Description     |
| ------------ | ---------------- |
|   [openresty-openssl111-1.1.1g-3.el7.centos.x86_64.rpm](https://openresty.org/package/centos/7/x86_64/openresty-openssl111-1.1.1g-3.el7.centos.x86_64.rpm)   |  OpenSSL package OpenResty depends on    |
|   [openresty-pcre-8.44-1.el7.x86_64.rpm](https://openresty.org/package/centos/7/x86_64/openresty-pcre-8.44-1.el7.x86_64.rpm)   | PCRE package OpenResty depends on |
|   [openresty-zlib-1.2.11-3.el7.centos.x86_64.rpm](https://openresty.org/package/centos/7/x86_64/openresty-zlib-1.2.11-3.el7.centos.x86_64.rpm)   |  zlib package OpenResty depends on |
|   [openresty-1.17.8.2-1.el7.x86_64.rpm](https://openresty.org/package/centos/7/x86_64/openresty-1.17.8.2-1.el7.x86_64.rpm)   |  OpenResty installation package |

- Install OpenResty

Since the gateway deployment depends on Lua scripts for authentication and forwarding, you need to install OpenResty 1.17.8.2+. Attached are RPM installation packages. Below are installation commands.

```shell
# Modify the permission of installation packages
chmod 644 openresty-1.17.8.2-1.el7.x86_64.rpm
chmod 644 openresty-openssl111-1.1.1g-3.el7.x86_64.rpm
chmod 644 openresty-pcre-8.44-1.el7.x86_64.rpm
chmod 644 openresty-zlib-1.2.11-3.el7.centos.x86_64.rpm
# Start installation
rpm -ivh openresty-pcre-8.44-1.el7.x86_64.rpm
rpm -ivh openresty-zlib-1.2.11-3.el7.centos.x86_64.rpm
rpm -ivh openresty-openssl111-1.1.1g-3.el7.x86_64.rpm  --replacefiles
rpm -ivh openresty-1.17.8.2-1.el7.x86_64.rpm
# Check installation status
cd /usr/local/openresty/nginx && ./sbin/nginx -v
```

`"nginx version: openresty/1.17.8.2"` means the installation is successful.

### Deploy and Start bk-ci Gateway

The gateway mainly consists of configuration files and Lua scripts, so you only need to create a symbolic link from the gateway to the Nginx conf/ directory.

- First, configure the relevant parameters in {{code}}/scripts/bkenv.properties.
- Run the render command to create template files of the gateway.

```shell
cd {{code}}/scripts
sh ./render_tpl -m ci ../support-files/templates/gateway*
```

- Copy rendered template files to the Nginx code directory.

```shell
cp -rf {{code}}/ci/gateway/core/* {{code}}/src/gateway/core/
```

- Copy Nginx configuration files to the project directory.

```shell
cp -rf {{code}}/src/gateway/core/* __INSTALL_PATH__/gateway
```

- Create a symbolic link from the Nginx configuration directory of `__INSTALL_PATH__/gateway` to the Nginx conf directory.

```shell
rm -rf /usr/local/openresty/nginx/conf
ln -s  __INSTALL_PATH__/gateway /usr/local/openresty/nginx/conf
```

#### Startup Commands

```shell
mkdir -p /usr/local/openresty/nginx/run/ # Create PID directory
cd /usr/local/openresty/nginx # Navigate to the Nginx installation directory
./sbin/nginx -t  # Verify Nginx configurations
./sbin/nginx     # Start Nginx
./sbin/nginx -s reload # Reload Nginx
```
