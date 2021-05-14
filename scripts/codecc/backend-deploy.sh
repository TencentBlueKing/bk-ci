#!/bin/bash

echo 'current execute path: '`pwd`

backends=(task defect report asyncreport codeccjob schedule openapi apiquery quartz)
for service in ${backends[@]};
do
  serviceFile="/etc/systemd/system/${service}.service"
  if [ -f $serviceFile ]
  then
      systemctl stop ${service}
  fi

  if [ ! -d /data/bkce/codecc/backend/${service} ]
  then
    mkdir -p /data/bkce/codecc/backend/${service}
  fi

  file=codecc/${service}/boot-${service}.jar
  echo 'the jar file is:'
  echo $file

   ## 查看服务名是不是存在的
  cat << EOF > $serviceFile
  [Unit]
  Description=$1
  Requires=network-online.target
  After=consul-client.target

  [Service]
  EnvironmentFile=-/usr/bin/java
  Restart=on-failure
  ExecStart=/usr/bin/java -server -Dspring.profiles.active=opensource -Dspring.config.location=file:/data/bkce/etc/codecc/common.yml,file:/data/bkce/etc/codecc/application-${service}.yml -Dservice.log.dir=/data/bkce/logs/codecc/${service}/ -Dsun.jnu.encoding=UTF-8 -Dfile.encoding=UTF-8 -Xms2g -Xmx4g -XX:+UseG1GC -XX:+UnlockExperimentalVMOptions -XX:G1NewSizePercent=60 -XX:G1MaxNewSizePercent=60  -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/data/bkce/codecc/logs/${service}/oom.hprof -XX:ErrorFile=/data/bkce/logs/codecc/${service}/error_sys.log -jar /data/bkce/codecc/backend/${service}/boot-${service}.jar
  ExecReload=/bin/kill -HUP \$MAINPID
  KillSignal=SIGTERM

  [Install]
  WantedBy=multi-user.target
EOF

        systemctl daemon-reload
        systemctl enable ${service}

        mv $file /data/bkce/codecc/backend/${service}
done
