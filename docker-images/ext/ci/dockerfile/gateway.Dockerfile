FROM bkci/openresty:0.0.1

LABEL maintainer="Tencent BlueKing Devops"

ENV INSTALL_PATH="/data/workspace/"

COPY ./ci/gateway /data/workspace/gateway

RUN rm -rf /usr/local/openresty/nginx/conf &&\
    ln -s  /data/workspace/gateway/core /usr/local/openresty/nginx/conf &&\
    mkdir -p /usr/local/openresty/nginx/run/ &&\
    mkdir -p /data/bkee/logs/ci/nginx/ /data/bkee/ci/frontend /data/bkee/codecc/frontend &&\
    chown nobody:nobody /data/bkee/logs/ci/nginx/
