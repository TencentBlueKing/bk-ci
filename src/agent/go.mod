module github.com/Tencent/bk-ci/src/agent

go 1.16

require (
	github.com/astaxie/beego v1.12.1
	github.com/gofrs/flock v0.8.1
	github.com/influxdata/telegraf v1.14.3
	github.com/kardianos/service v1.0.0
	github.com/pkg/errors v0.9.1 // indirect
	github.com/shiena/ansicolor v0.0.0-20151119151921-a422bbe96644 // indirect
	github.com/shirou/gopsutil v3.21.11+incompatible // indirect
	github.com/tklauser/go-sysconf v0.3.10 // indirect
	github.com/yusufpapurcu/wmi v1.2.2 // indirect
	golang.org/x/sys v0.0.0-20220422013727-9388b58f7150 // indirect
)

replace github.com/influxdata/telegraf => github.com/ci-plugins/telegraf v1.99.0

replace golang.zx2c4.com/wireguard v0.0.20200121 => golang.zx2c4.com/wireguard v0.0.0-20200121152719-05b03c675090
