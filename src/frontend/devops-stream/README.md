## Stream

### 本地开发

#### 生成本地调试证书

``` bash
# 建议使用mkcert 生成可信任本地调试证书
mkcert -install
mkcert -cert-file src/conf/local-stream.com.crt -key-file src/conf/local-stream.com.key local-stream.com

# 生成不可信任证书，不推荐使用
sudo openssl req -x509 -nodes -days 365 -newkey rsa:2048 -keyout src/conf/local-stream.com.key -out src/conf/local-stream.com.crt

```

#### 安装依赖
```bash
执行 npm i 或者 frontend 工程目录下执行 yarn
```

#### 启动本地
```bash
npm run dev
```

#### 设置代理
1. 安装 [whistle](https://www.npmjs.com/package/whistle)
2. 根据部署的域名配置代理：https://{部署的域名}/static/ https://localhost:8080/static/
