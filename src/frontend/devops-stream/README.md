Stream
===

``` bash
# create `Dll bundle` with webpack DllPlugin
npm run dll

# 生成本地调试证书

  # 建议使用mkcert 生成可信任本地调试证书

    mkcert -install
    mkcert -cert-file src/conf/local-stream.com.crt -key-file src/conf/local-stream.com.key local-stream.com

  # 生成不可信任证书，不推荐使用
  sudo openssl req -x509 -nodes -days 365 -newkey rsa:2048 -keyout src/conf/local-stream.com.key -out src/conf/local-stream.com.crt

#设置Host
127.0.0.1 local-stream.com

# dev
npm run dev

# build
npm run build

# build with analyzer
npm run build:analyzer

```
> 访问 https://local-stream.com:8080


## 如何安装`mkcert`工具

> **Warning**: 请勿将自动生成的 `rootCA-key.pem` 文件分享给其他人

### macOS

macOS上使用 [Homebrew](https://brew.sh/)

```
brew install mkcert
brew install nss # if you use Firefox
```

或者 [MacPorts](https://www.macports.org/).

```
sudo port selfupdate
sudo port install mkcert
sudo port install nss # if you use Firefox
```
### Windows

在Windows系统上请使用 [Chocolatey](https://chocolatey.org)工具

```
choco install mkcert
```

或者Scoop指令

```
scoop bucket add extras
scoop install mkcert
```

or build from source (requires Go 1.10+), or use [the pre-built binaries](https://github.com/FiloSottile/mkcert/releases).

如果遇到权限问题，请使用管理员权限运行`mkcert`