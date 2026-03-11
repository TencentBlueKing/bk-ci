FROM bkci/openresty:0.0.2

LABEL maintainer="Tencent BlueKing Devops"

# nginx配置文件
COPY ./nginx.conf /usr/local/openresty/nginx/conf/nginx.conf

# 复制前端代码
COPY ./frontend /data/workspace/frontend
