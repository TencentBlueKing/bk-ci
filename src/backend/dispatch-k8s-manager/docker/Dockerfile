ARG ARCH

FROM $ARCH/alpine:3.12

ARG ARCH

# 拷贝 sql
COPY scripts/devops_kubernetes_manager.sql /data/workspace/support-files/sql/devops_kubernetes_manager.sql

COPY bin/kubernetes-manager-$ARCH /bin/kubernetes-manager

# 打包 mysql client
RUN apk add --no-cache mysql-client

WORKDIR /bin

EXPOSE 80

CMD ["kubernetes-manager"]