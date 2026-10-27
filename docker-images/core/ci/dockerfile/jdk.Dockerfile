FROM bkci/os:0.0.3

LABEL maintainer="Tencent BlueKing Devops"

## 安装JDK。Fedora 44 的 tar 会调用 posix_fallocate，在 Docker overlay 上解压大文件会失败。
RUN dnf install -y bsdtar && \
    mkdir -p /data && \
    cd /data/ && \
    curl -L https://github.com/Tencent/TencentKona-17/releases/download/TencentKona-17.0.8/TencentKona-17.0.8.b1-jdk_linux-x86_64.tar.gz -o kona.tar.gz && \
    bsdtar -xzf kona.tar.gz && \
    rm -f kona.tar.gz && \
    dnf remove -y bsdtar && \
    dnf clean all
ENV JAVA_HOME=/data/TencentKona-17.0.8.b1
ENV PATH=${JAVA_HOME}/bin:$PATH
ENV CLASSPATH=.:${JAVA_HOME}/lib

# 移除 eMudhra 失陷中间证书所链到的根证书。17.0.8 的 cacerts 没有 emSign 根，只有 comodoaaaca。
# 别名不存在时跳过，删完后名单里还能搜到就让构建失败。
RUN set -e; \
    for alias in \
        "emsignrootcag1 [jdk]" \
        "emsignrootcag2 [jdk]" \
        "emsigneccrootcag3 [jdk]" \
        "comodoaaaca [jdk]"; do \
        if "${JAVA_HOME}/bin/keytool" -list -cacerts -storepass changeit -alias "${alias}" >/dev/null 2>&1; then \
            "${JAVA_HOME}/bin/keytool" -delete -cacerts -storepass changeit -alias "${alias}"; \
        fi; \
    done; \
    if "${JAVA_HOME}/bin/keytool" -list -cacerts -storepass changeit | grep -iE 'emsign|emudhra|comodoaaaca'; then \
        echo "compromised CA roots still present" >&2; \
        exit 1; \
    fi

# 安装软件
RUN dnf makecache &&\
    dnf install -y mysql &&\
    dnf install -y redis &&\
    dnf install -y python3 &&\
    dnf install -y pip3 &&\
    dnf install -y openssl &&\
    pip install requests

# 安装Java工具
RUN mkdir -p /data/tools && \
    curl -o /data/tools/arthas.jar https://arthas.aliyun.com/arthas-boot.jar  && \
    curl -o /data/tools/ot-agent.jar https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar -L

# 安装第三方构建机JRE
RUN wget "https://raw.githubusercontent.com/bkdevops-projects/devops-jre/refs/heads/jdk17/linux/jdk17.zip" -P /data/workspace/agent-package/jre/linux/ &&\
    wget "https://raw.githubusercontent.com/bkdevops-projects/devops-jre/refs/heads/jdk17/windows/jdk17.zip" -P /data/workspace/agent-package/jre/windows/ &&\
    wget "https://raw.githubusercontent.com/bkdevops-projects/devops-jre/refs/heads/jdk17/macos/jdk17.zip" -P /data/workspace/agent-package/jre/macos/ &&\
    wget "https://raw.githubusercontent.com/bkdevops-projects/devops-jre/refs/heads/jdk17/linux_arm64/jdk17.zip" -P /data/workspace/agent-package/jre/linux_arm64/ &&\
    wget "https://raw.githubusercontent.com/bkdevops-projects/devops-jre/refs/heads/jdk17/macos_arm64/jdk17.zip" -P /data/workspace/agent-package/jre/macos_arm64/
