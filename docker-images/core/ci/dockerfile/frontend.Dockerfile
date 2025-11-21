FROM fedora:40

LABEL maintainer="Tencent BlueKing Devops"

# 设置安装路径
ENV INSTALL_PATH="/data/workspace/"

# 安装nginx
RUN dnf makecache --refresh && \
    dnf install -y nginx && \
    dnf clean all && \
    rm -rf /var/cache/dnf/*
COPY ./dockerfile/nginx.conf /etc/nginx/nginx.conf

# 复制脚本和模板
COPY ./ci-docker/scripts /data/workspace/scripts
COPY ./ci-docker/support-files/templates /data/workspace/templates

# 复制前端代码
COPY ./ci-docker/frontend /data/workspace/frontend

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
