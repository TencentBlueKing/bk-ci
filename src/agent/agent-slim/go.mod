module github.com/TencentBlueKing/bk-ci/agentslim

go 1.19

require (
	github.com/Netflix/go-env v0.0.0-20220526054621-78278af1949d
	github.com/TencentBlueKing/bk-ci/agentcommon v0.0.0-00010101000000-000000000000
	github.com/nicksnyder/go-i18n/v2 v2.2.1
	github.com/pkg/errors v0.9.1
	golang.org/x/text v0.12.0
)

require (
	github.com/sirupsen/logrus v1.9.3 // indirect
	golang.org/x/sys v0.5.0 // indirect
	gopkg.in/natefinch/lumberjack.v2 v2.2.1 // indirect
)

replace github.com/TencentBlueKing/bk-ci/agentcommon => ../common
