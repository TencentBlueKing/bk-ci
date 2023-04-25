FROM mirrors.tencent.com/tjdk/tencentkona8-tlinux:332

LABEL maintainer="Tencent BlueKing Devops"

# yum安装软件
RUN yum install -y procps && \
    yum install -y vi && \
    yum install -y vim && \
    yum install -y less &&\
    yum install -y lrzsz &&\
    yum install -y wget

# 第三方工具
RUN mkdir -p /data/tools && \
    curl -o /data/tools/arthas.jar https://arthas.aliyun.com/arthas-boot.jar  && \
    curl -o /data/tools/ot-agent.jar https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v1.18.0/opentelemetry-javaagent.jar -L && \
    curl -o /data/tools/konaprofiler-cmder-cli.jar https://mirrors.tencent.com/repository/generic/konaprofiler/1.0.3/konaprofiler-cmder-1.0.3.4-SNAPSHOT-cli.jar

# 操作系统相关
RUN ln -snf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo 'alias ls="ls --color=auto"' >> ~/.bashrc && \
    echo 'alias ll="ls -l"' >> ~/.bashrc && \
    echo 'alias tailf="tail -f"' >> ~/.bashrc
