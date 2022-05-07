module github.com/Tencent/bk-ci/src/agent

go 1.16

require (
	github.com/gofrs/flock v0.8.1
	github.com/influxdata/telegraf v1.22.3
	github.com/kardianos/service v1.2.1
	github.com/pkg/errors v0.9.1
	github.com/sirupsen/logrus v1.8.1
	golang.org/x/sys v0.0.0-20220422013727-9388b58f7150 // indirect
	gopkg.in/ini.v1 v1.66.4
	gopkg.in/natefinch/lumberjack.v2 v2.0.0
)

replace golang.zx2c4.com/wireguard v0.0.20200121 => golang.zx2c4.com/wireguard v0.0.0-20200121152719-05b03c675090
