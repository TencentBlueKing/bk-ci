FROM bkci/openresty

LABEL maintainer="blueking"

COPY gateway /data/docker/bkci/codecc/gateway
COPY gateway_run_codecc.sh /data/docker/bkci/codecc/gateway/
COPY codecc_render_tpl /data/docker/bkci/codecc/gateway/
COPY support-files /data/docker/bkci/codecc/gateway/support-files
COPY frontend /data/docker/bkci/codecc


## lua日志目录
RUN mkdir -p /data/docker/bkci/logs/codecc/nginx/ &&\
    chown -R nobody:nobody /data/docker/bkci/logs/codecc/nginx/ &&\
    rm -rf /usr/local/openresty/nginx/conf &&\
    ln -s  /data/docker/bkci/codecc/gateway /usr/local/openresty/nginx/conf &&\
    mkdir -p /usr/local/openresty/nginx/run/ &&\
    chmod +x /data/docker/bkci/codecc/gateway/gateway_run_codecc.sh &&\
    chmod +x /data/docker/bkci/codecc/gateway/codecc_render_tpl

## 替换入口配置文件
RUN mv -f /data/docker/bkci/codecc/gateway/nginx.codecc.conf /data/docker/bkci/codecc/gateway/nginx.conf
