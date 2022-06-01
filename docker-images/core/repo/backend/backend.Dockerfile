FROM blueking/jdk:0.0.1

LABEL maintainer="Tencent BlueKing Devops"

ENV BK_REPO_HOME=/data/workspace \
    BK_REPO_LOGS_DIR=/data/workspace/logs \
    BK_REPO_SERVICE_PREFIX=repo- \
    BK_REPO_PROFILE=dev

COPY ./ /data/workspace/
RUN ln -snf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo 'Asia/Shanghai' > /etc/timezone && \
    chmod +x /data/workspace/startup.sh
WORKDIR /data/workspace
CMD /data/workspace/startup.sh