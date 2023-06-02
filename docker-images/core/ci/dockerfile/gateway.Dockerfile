FROM bkci/openresty:0.0.1

LABEL maintainer="Tencent BlueKing Devops"

ENV INSTALL_PATH="/data/workspace/"
ENV OPENRESTY_PATH="/usr/local/openresty/nginx"

# 复制脚本和模板
COPY ./ci-docker/scripts /data/workspace/scripts
COPY ./ci-docker/support-files/templates /data/workspace/templates

# 复制网关相关文件
COPY ./ci-docker/gateway /data/workspace/gateway
COPY ./ci-docker/agent-package/script/docker_init.sh /data/dir/gateway/files/prod/
COPY ./ci-docker/agent-package/jar /data/dir/gateway/files/prod/jar

# 准备好启动环境
RUN rm -rf ${OPENRESTY_PATH}/conf &&\
    ln -s /data/workspace/gateway/core ${OPENRESTY_PATH}/conf &&\
    chmod +x /data/workspace/scripts/render_tpl &&\
    mkdir -p ${OPENRESTY_PATH}/run/ &&\
    cp -r /data/workspace/scripts/render_tpl ${OPENRESTY_PATH}/ &&\
    touch ${OPENRESTY_PATH}/bkenv.properties

# WORKDIR ${OPENRESTY_PATH}
# CMD mkdir -p ${BK_CI_LOGS_DIR}/nginx/ ${BK_CI_HOME} &&\
#     chown nobody:nobody ${BK_CI_LOGS_DIR}/nginx/ &&\
#     ln -s /data/workspace/frontend ${BK_CI_HOME}/frontend &&\
#     ./render_tpl -m . /data/workspace/templates/gateway*
#     ./render_tpl -m . /data/workspace/templates/frontend* &&\
#     sbin/nginx -g 'daemon off;'
