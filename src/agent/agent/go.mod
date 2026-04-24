module github.com/TencentBlueKing/bk-ci/agent

go 1.19

require (
	github.com/ThinkInAIXYZ/go-mcp v0.2.24
	// 非稳定库，目前只在windows升级中简单使用且主要做对go-ole的封装简化，大规模使用前需要评估
	github.com/capnspacehook/taskmaster v0.0.0-20210519235353-1629df7c85e9
	github.com/creack/pty v1.1.24
	github.com/gofrs/flock v0.8.1
	github.com/gorilla/websocket v1.5.0
	github.com/jaypipes/ghw v0.20.0
	github.com/kardianos/service v1.2.2
	github.com/nicksnyder/go-i18n/v2 v2.2.1
	github.com/pkg/errors v0.9.1
	github.com/shirou/gopsutil/v4 v4.24.5
	github.com/sirupsen/logrus v1.9.3
	golang.org/x/net v0.25.0
	golang.org/x/sync v0.10.0
	golang.org/x/sys v0.28.0
	golang.org/x/text v0.21.0
	gopkg.in/ini.v1 v1.67.0
	gopkg.in/natefinch/lumberjack.v2 v2.2.1
)

require (
	github.com/go-ole/go-ole v1.2.6 // indirect
	github.com/google/uuid v1.6.0 // indirect
	github.com/jaypipes/pcidb v1.1.1 // indirect
	github.com/kr/pretty v0.3.0 // indirect
	github.com/lufia/plan9stats v0.0.0-20220913051719-115f729f3c8c // indirect
	github.com/orcaman/concurrent-map/v2 v2.0.1 // indirect
	github.com/power-devops/perfstat v0.0.0-20220216144756-c35f1ee13d7c // indirect
	github.com/rickb777/date v1.14.2 // indirect
	github.com/rickb777/plural v1.2.2 // indirect
	github.com/rogpeppe/go-internal v1.6.2 // indirect
	github.com/shoenig/go-m1cpu v0.1.6 // indirect
	github.com/tidwall/gjson v1.18.0 // indirect
	github.com/tidwall/match v1.1.1 // indirect
	github.com/tidwall/pretty v1.2.0 // indirect
	github.com/tklauser/go-sysconf v0.3.12 // indirect
	github.com/tklauser/numcpus v0.6.1 // indirect
	github.com/yosida95/uritemplate/v3 v3.0.2 // indirect
	github.com/yusufpapurcu/wmi v1.2.4 // indirect
	golang.org/x/xerrors v0.0.0-20220907171357-04be3eba64a2 // indirect
	gopkg.in/check.v1 v1.0.0-20201130134442-10cb98267c6c // indirect
	gopkg.in/yaml.v2 v2.4.0 // indirect
	gopkg.in/yaml.v3 v3.0.1 // indirect
	howett.net/plist v1.0.2-0.20250314012144-ee69052608d9 // indirect
)
