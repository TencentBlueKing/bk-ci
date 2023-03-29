module registry-facade

go 1.19

require (
	github.com/containerd/containerd v1.7.0
	github.com/docker/cli v23.0.1+incompatible
	github.com/docker/distribution v2.8.1+incompatible
	github.com/fsnotify/fsnotify v1.6.0
	github.com/go-test/deep v1.1.0
	github.com/golang/mock v1.4.4
	github.com/google/go-cmp v0.5.9
	github.com/gorilla/handlers v1.5.1
	github.com/gorilla/mux v1.8.0
	github.com/hashicorp/go-retryablehttp v0.7.2
	github.com/hashicorp/golang-lru v0.5.4
	github.com/opencontainers/go-digest v1.0.0
	github.com/opencontainers/image-spec v1.1.0-rc2.0.20221005185240-3a7f492d3f1b
	github.com/pkg/errors v0.9.1
	github.com/prometheus/client_golang v1.14.0
	github.com/sirupsen/logrus v1.9.0
	github.com/spf13/cobra v1.6.1
	golang.org/x/xerrors v0.0.0-20200804184101-5ec99f83aff1
	google.golang.org/protobuf v1.28.1
	k8s.io/apimachinery v0.26.2
)

require (
	github.com/AdaLogics/go-fuzz-headers v0.0.0-20230106234847-43070de90fa1 // indirect
	github.com/beorn7/perks v1.0.1 // indirect
	github.com/cespare/xxhash/v2 v2.2.0 // indirect
	github.com/cyphar/filepath-securejoin v0.2.3 // indirect
	github.com/docker/docker-credential-helpers v0.7.0 // indirect
	github.com/felixge/httpsnoop v1.0.1 // indirect
	github.com/go-logr/logr v1.2.3 // indirect
	github.com/go-logr/stdr v1.2.2 // indirect
	github.com/golang/protobuf v1.5.2 // indirect
	github.com/hashicorp/go-cleanhttp v0.5.2 // indirect
	github.com/klauspost/compress v1.16.0 // indirect
	github.com/matttproud/golang_protobuf_extensions v1.0.4 // indirect
	github.com/moby/locker v1.0.1 // indirect
	github.com/prometheus/client_model v0.3.0 // indirect
	github.com/prometheus/common v0.37.0 // indirect
	github.com/prometheus/procfs v0.8.0 // indirect
	go.opentelemetry.io/otel v1.14.0 // indirect
	go.opentelemetry.io/otel/trace v1.14.0 // indirect
	golang.org/x/sync v0.1.0 // indirect
	golang.org/x/sys v0.6.0 // indirect
	google.golang.org/genproto v0.0.0-20230306155012-7f2fa6fef1f4 // indirect
	google.golang.org/grpc v1.53.0 // indirect
	gotest.tools/v3 v3.4.0 // indirect
	k8s.io/klog/v2 v2.90.1 // indirect
	k8s.io/utils v0.0.0-20230220204549-a5ecb0141aa5 // indirect
)

require (
	common v0.0.0-00010101000000-000000000000
	github.com/inconshreveable/mousetrap v1.0.1 // indirect
	github.com/spf13/pflag v1.0.5 // indirect
	registry-facade/api v0.0.0-00010101000000-000000000000
)

replace common => ../common

replace registry-facade/api => ../registry-facade-api
