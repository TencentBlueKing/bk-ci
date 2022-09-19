FROM konajdk/konajdk:8-tlinux

LABEL maintainer="Tencent BlueKing Devops"

# yum安装软件
RUN yum install -y procps && \
    yum install -y vi && \
    yum install -y vim && \
    yum install -y less

# 第三方工具
RUN mkdir -p /data/tools && \
    curl -o /data/tools/arthas.jar https://arthas.aliyun.com/arthas-boot.jar  && \
    curl -o /data/tools/ot-agent.jar https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar -L

# 操作系统相关
RUN ln -snf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo 'alias ls="ls --color=auto"' >> ~/.bashrc && \
    echo 'alias ll="ls -l"' >> ~/.bashrc && \
    echo 'alias tailf="tail -f"' >> ~/.bashrc
