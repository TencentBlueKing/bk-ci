FROM bkci/openresty:0.0.2

LABEL maintainer="Tencent BlueKing Devops"

# 设置安装路径
ENV INSTALL_PATH="/data/workspace/"

# nginx配置文件
COPY ./nginx.conf /usr/local/openresty/nginx/conf/nginx.conf

# 复制渲染脚本及各环境配置（render_ci + ci_env_*.properties）
COPY ./scripts ${INSTALL_PATH}/scripts
COPY ./support-files/templates ${INSTALL_PATH}/templates

# 复制前端代码（保留 __BK_CI_*__ 占位符，启动时渲染）
COPY ./frontend ${INSTALL_PATH}/frontend

# 确保渲染脚本可执行
RUN chmod +x ${INSTALL_PATH}/scripts/render_ci
