module helper

go 1.19

require (
	common v0.0.0-00010101000000-000000000000
	github.com/Netflix/go-env v0.0.0-20220526054621-78278af1949d
	github.com/pkg/errors v0.9.1
	gopkg.in/yaml.v3 v3.0.1
)

require (
	go.uber.org/atomic v1.7.0 // indirect
	go.uber.org/multierr v1.6.0 // indirect
	go.uber.org/zap v1.24.0 // indirect
	gopkg.in/natefinch/lumberjack.v2 v2.2.1 // indirect
)

replace common => ../../../common
