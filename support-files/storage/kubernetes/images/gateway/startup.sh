#! /bin/sh

mkdir -p $BK_REPO_LOGS_DIR/nginx
chmod 777 $BK_REPO_LOGS_DIR/nginx

##初始化配置
touch repo.env
/data/workspace/render_tpl -u -p /data/workspace -m . -e repo.env /data/workspace/templates/gateway*
/data/workspace/render_tpl -u -p /data/workspace -m . -e repo.env /data/workspace/frontend/ui/frontend#ui#index.html

##启动程序
/usr/local/openresty/nginx/sbin/nginx -g 'daemon off;'
