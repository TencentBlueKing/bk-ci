FROM centos:7

LABEL maintainer="Tencent BlueKing Devops"

# 安装JDK
RUN mkdir -p /data && \
    cd /data/ &&\
    curl -OL https://github.com/Tencent/TencentKona-8/releases/download/8.0.11-GA/TencentKona8.0.11.b2_jdk_linux-x86_64_8u345.tar.gz &&\
    tar -xzf TencentKona8.0.11.b2_jdk_linux-x86_64_8u345.tar.gz &&\
    rm -f TencentKona8.0.11.b2_jdk_linux-x86_64_8u345.tar.gz
ENV JAVA_HOME=/data/TencentKona-8.0.11-345
ENV PATH=${JAVA_HOME}/bin:$PATH
ENV CLASSPATH=.:${JAVA_HOME}/lib

# yum安装软件
RUN yum install -y procps && \
    yum install -y vi && \
    yum install -y vim && \
    yum install -y less && \
    yum install -y wget && \
    yum install -y lrzsz

# 第三方工具
RUN mkdir -p /data/tools && \
    curl -o /data/tools/arthas.jar https://arthas.aliyun.com/arthas-boot.jar  && \
    curl -o /data/tools/ot-agent.jar https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar -L

# 操作系统相关
RUN ln -snf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo 'alias ls="ls --color=auto"' >> ~/.bashrc && \
    echo 'alias ll="ls -l"' >> ~/.bashrc && \
    echo 'alias tailf="tail -f"' >> ~/.bashrc
