FROM alpine:3.16 as base_builder
RUN mkdir /ide

FROM scratch
# 确保 /ide 的正确权限
COPY --from=base_builder --chown=33333:33333 /ide/ /ide/
COPY --chown=33333:33333 devopsRemoting-ide-config.json helper/bin/vscodehelper /ide/