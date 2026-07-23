FROM bkci/openresty:0.0.2

LABEL maintainer="Tencent BlueKing Devops"

# nginx配置文件
COPY ./nginx.conf /usr/local/openresty/nginx/conf/nginx.conf

# 复制渲染脚本及各环境配置（render_ci + ci_env_*.properties）
COPY ./scripts /data/workspace/scripts
COPY ./support-files /data/workspace/support-files

# 复制前端代码（保留 __BK_CI_*__ 占位符，启动时渲染）
COPY ./frontend /data/workspace/frontend

# 确保渲染脚本可执行
RUN chmod +x /data/workspace/scripts/render_ci
