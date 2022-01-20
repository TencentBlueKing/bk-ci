#! /bin/sh

mkdir -p $BK_CODECC_LOGS_DIR/nginx
chmod 777 $BK_CODECC_LOGS_DIR/nginx

##初始化配置
/data/workspace/render_tpl -u -p /data/workspace -m . -e /data/workspace/codecc.env /data/workspace/templates/gateway*
/data/workspace/render_tpl -u -p /data/workspace -m . -e /data/workspace/codecc.env /data/workspace/frontend/index.html
\cp -rf /data/workspace/index.html /data/workspace/frontend

##启动程序
/usr/local/openresty/nginx/sbin/nginx -g 'daemon off;'