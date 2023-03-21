module helper

go 1.19

require (
	common v0.0.0-00010101000000-000000000000
	github.com/Netflix/go-env v0.0.0-20220526054621-78278af1949d
	github.com/pkg/errors v0.9.1
	gopkg.in/yaml.v3 v3.0.1
)

require (
	github.com/sirupsen/logrus v1.9.0 // indirect
	golang.org/x/sys v0.0.0-20220908164124-27713097b956 // indirect
)

replace common => ../../../common
