# TODO: 未来换成内部ide打包过程

FROM alpine:3.16 as base_builder
# 从openvscode拷贝镜像
RUN wget https://github.com/gitpod-io/openvscode-server/releases/download/openvscode-server-v1.74.3/openvscode-server-v1.74.3-linux-x64.tar.gz && \
    tar -xzf openvscode-server-v1.74.3-linux-x64.tar.gz && \
    mv -f openvscode-server-v1.74.3-linux-x64 /ide && \
    cp /ide/bin/openvscode-server /ide/bin/devopsRemoting-vscode && \
    rm -f openvscode-server-v1.74.3-linux-x64.tar.gz

FROM scratch
COPY --from=base_builder --chown=33333:33333 /ide/ /ide/
