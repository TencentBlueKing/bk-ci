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

COPY bin/registry-$ARCH /data/workspace/registry-facade/registry

WORKDIR /data/workspace/registry-facade

ARG DEBUG

ENV DEVOPS_REGISTRY_DEBUG_ENABLE="$DEBUG"

CMD ["/data/workspace/registry-facade/registry", "run", "/data/workspace/registry-facade/config/run.json"]
