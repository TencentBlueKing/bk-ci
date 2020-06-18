module github.com/Tencent/bk-ci/src/agent

go 1.13

require (
	github.com/astaxie/beego v1.12.1
	github.com/gofrs/flock v0.7.1
	github.com/influxdata/telegraf v1.14.3
	github.com/kardianos/service v1.0.0
	github.com/shiena/ansicolor v0.0.0-20151119151921-a422bbe96644 // indirect
)

replace github.com/influxdata/telegraf => github.com/ci-plugins/telegraf v1.99.0
