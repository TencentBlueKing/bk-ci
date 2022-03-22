FROM bkrepo/openrestry:0.0.1

LABEL maintainer="Tencent BlueKing Devops"

ENV INSTALL_PATH="/data/workspace/"
ENV LANG="en_US.UTF-8"
ENV INNER_NAME="bk-ci"

COPY ./ci/gateway /data/workspace/gateway

RUN ln -snf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo 'Asia/Shanghai' > /etc/timezone && \
    rm -rf /usr/local/openresty/nginx/conf &&\
    ln -s  /data/workspace/gateway/core /usr/local/openresty/nginx/conf &&\
    mkdir -p /data/bkee/logs/ci/nginx/ &&\
    chown nobody:nobody /data/bkee/logs/ci/nginx/ &&\
    mkdir -p /usr/local/openresty/nginx/run/

WORKDIR /usr/local/openresty/nginx/

CMD sbin/nginx -g 'daemon off;'
