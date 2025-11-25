FROM bkci/openresty:0.0.2

LABEL maintainer="Tencent BlueKing Devops"

# 设置安装路径
ENV INSTALL_PATH="/data/workspace/"

# nginx配置文件
COPY ./nginx.conf /usr/local/openresty/nginx/conf/nginx.conf

# 复制脚本和模板
COPY ./scripts /data/workspace/scripts
COPY ./support-files/templates /data/workspace/templates

# 复制前端代码
COPY ./frontend /data/workspace/frontend
