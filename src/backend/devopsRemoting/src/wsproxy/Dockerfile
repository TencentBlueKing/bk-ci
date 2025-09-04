ARG ARCH

FROM $ARCH/alpine:3.12

# FORM 后重新声明变量
ARG ARCH

# 设置时区
RUN apk --update add tzdata && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone && \
    apk del tzdata && \
    rm -rf /var/cache/apk/*

COPY bin/wsproxy-$ARCH /data/workspace/wsproxy/wsproxy

# 拷贝public文件
COPY public/ /data/workspace/wsproxy/public/

WORKDIR /data/workspace/wsproxy

ARG DEBUG

ENV DEVOPS_WSPROXY_DEBUG_ENABLE="$DEBUG"

CMD ["/data/workspace/wsproxy/wsproxy", "run", "/data/workspace/wsproxy/config/run.json"]