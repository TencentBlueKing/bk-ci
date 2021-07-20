set -e
pwd

##初始化数据库
echo 'init mysql'
for SQL in /data/docker/bkci/codecc/configuration/support-files/sql/*.sql; do mysql -h$MYSQL_IP -P$MYSQL_PORT  -u$MYSQL_USER -p$MYSQL_PASS< $SQL; done

echo 'init mongo db: '
sh /data/docker/bkci/codecc/configuration/support-files/nosql/db_data.sh

## 初始化配置
echo 'render codecc service templates'
./codecc_render_tpl -m codecc /data/docker/bkci/codecc/configuration/support-files/templates/*.yml

## 写consul
backends=(task defect report asyncreport codeccjob schedule openapi apiquery quartz)
for var in ${backends[@]};
do
    echo "properties $var start..."
    curl --request PUT --data "$(cat /data/docker/bkci/etc/codecc/application-$var.yml)" http://${CONSUL_SERVER}:8500/v1/kv/config/${var}${BK_CI_CONSUL_DISCOVERY_TAG}:${BK_CI_ENV}/data
    echo "properties $var finish..."
done

echo "properties application start..."
curl --request PUT --data "$(cat /data/docker/bkci/etc/codecc/common.yml)" http://${CONSUL_SERVER}:8500/v1/kv/config/application:${BK_CI_ENV}/data
echo "properties application finish..."
