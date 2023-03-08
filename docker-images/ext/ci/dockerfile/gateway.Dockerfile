FROM bkrepo/openrestry:0.0.1

LABEL maintainer="Tencent BlueKing Devops"

ENV INSTALL_PATH="/data/workspace/"
ENV LANG="en_US.UTF-8"

COPY ./ci/gateway /data/workspace/gateway

RUN ln -snf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo 'Asia/Shanghai' > /etc/timezone && \
    rm -rf /usr/local/openresty/nginx/conf &&\
    ln -s  /data/workspace/gateway/core /usr/local/openresty/nginx/conf &&\
    mkdir -p /usr/local/openresty/nginx/run/

WORKDIR /usr/local/openresty/nginx/

CMD mkdir -p /data/bkee/logs/ci/nginx/ /data/bkee/ci/frontend /data/bkee/codecc/frontend &&\
    chown nobody:nobody /data/bkee/logs/ci/nginx/ &&\
    sbin/nginx -g 'daemon off;'
