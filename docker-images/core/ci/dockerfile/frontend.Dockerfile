FROM bkci/openresty:0.0.2

LABEL maintainer="Tencent BlueKing Devops"

# 设置安装路径
ENV INSTALL_PATH="/data/workspace/"

# nginx配置文件
COPY ./dockerfile/nginx.conf /usr/local/openresty/nginx/nginx.conf

# 复制脚本和模板
COPY ./ci-docker/scripts /data/workspace/scripts
COPY ./ci-docker/support-files/templates /data/workspace/templates

# 复制前端代码
COPY ./ci-docker/frontend /data/workspace/frontend
