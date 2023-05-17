module remoting

go 1.19

require (
	common v0.0.0-00010101000000-000000000000
	github.com/Netflix/go-env v0.0.0-20220526054621-78278af1949d
	github.com/creack/pty v1.1.18
	github.com/fsnotify/fsnotify v1.6.0
	github.com/gin-gonic/gin v1.8.1
	github.com/go-playground/validator/v10 v10.10.0
	github.com/google/go-cmp v0.5.9
	github.com/google/uuid v1.3.0
	github.com/pkg/errors v0.9.1
	github.com/sirupsen/logrus v1.9.0
	github.com/spf13/cobra v1.6.1
	golang.org/x/net v0.0.0-20220722155237-a158d28d115b
	golang.org/x/sync v0.1.0
	gopkg.in/yaml.v3 v3.0.1
	remoting/api v0.0.0-00010101000000-000000000000
)

require (
	github.com/gin-contrib/sse v0.1.0 // indirect
	github.com/go-playground/locales v0.14.0 // indirect
	github.com/go-playground/universal-translator v0.18.0 // indirect
	github.com/goccy/go-json v0.9.7 // indirect
	github.com/json-iterator/go v1.1.12 // indirect
	github.com/leodido/go-urn v1.2.1 // indirect
	github.com/mattn/go-isatty v0.0.14 // indirect
	github.com/modern-go/concurrent v0.0.0-20180306012644-bacd9c7ef1dd // indirect
	github.com/modern-go/reflect2 v1.0.2 // indirect
	github.com/pelletier/go-toml/v2 v2.0.1 // indirect
	github.com/ugorji/go/codec v1.2.7 // indirect
	golang.org/x/crypto v0.0.0-20220315160706-3147a52a75dd // indirect
	golang.org/x/text v0.3.7 // indirect
	google.golang.org/protobuf v1.28.0 // indirect
	gopkg.in/yaml.v2 v2.4.0 // indirect
)

require (
	github.com/golang/mock v1.6.0
	github.com/inconshreveable/mousetrap v1.0.1 // indirect
	github.com/prometheus/procfs v0.8.0
	github.com/ramr/go-reaper v0.2.1
	github.com/spf13/pflag v1.0.5 // indirect
	golang.org/x/sys v0.0.0-20220908164124-27713097b956
)

replace common => ../common

replace remoting/api => ../remoting-api
