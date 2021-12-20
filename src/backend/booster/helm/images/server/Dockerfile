FROM bitnami/minideb:latest

LABEL maintainer="Tencent BlueKing Devops"

ENV BK_TBS_HOME=/data/workspace \
    BK_TBS_LOGS_DIR=/data/workspace/logs \
    BK_TBS_LOCAL_IP=127.0.0.1

COPY * /data/workspace/
RUN ln -snf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo 'Asia/Shanghai' > /etc/timezone && \
    chmod +x /data/workspace/start.sh

WORKDIR /data/workspace
CMD /data/workspace/start.sh