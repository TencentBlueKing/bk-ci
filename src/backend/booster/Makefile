.PHONY: distcc fastbuild alertserver bkdist

GITTAG=$(shell git describe --tags --always)
BUILDTIME = $(shell date +%Y-%m-%dT%T%z)
GITHASH=$(shell git rev-parse HEAD)
VERSION=${GITTAG}-$(shell date +%y.%m.%d)

LDFLAG=-s -w -X github.com/Tencent/bk-ci/src/booster/common/static.EncryptionKey=${encryption_key} \
 -X github.com/Tencent/bk-ci/src/booster/common/static.ServerCertPwd=${server_cert_pwd} \
 -X github.com/Tencent/bk-ci/src/booster/common/static.ClientCertPwd=${client_cert_pwd} \
 -X github.com/Tencent/bk-ci/src/booster/common/version.Version=${VERSION} \
 -X github.com/Tencent/bk-ci/src/booster/common/version.BuildTime=${BUILDTIME} \
 -X github.com/Tencent/bk-ci/src/booster/common/version.GitHash=${GITHASH} \
 -X github.com/Tencent/bk-ci/src/booster/common/version.Tag=${GITTAG} \
 -X github.com/Tencent/bk-ci/src/booster/common/encrypt.Disabled=${disable_encrypt} \
 -X github.com/Tencent/bk-ci/src/booster/server/pkg/resource/crm/operator/k8s.EnableBCSApiGw=${enable_bcs_gateway} \
 -X github.com/Tencent/bk-ci/src/booster/common/version.DisttaskRepo=${gateway_repo_url}

DISTCC_LDFLAG=-X github.com/Tencent/bk-ci/src/booster/server/pkg/engine/distcc/client/pkg.ProdDistCCServerDomain=${distcc_server_prod_domain} \
 -X github.com/Tencent/bk-ci/src/booster/server/pkg/engine/distcc/client/pkg.ProdDistCCServerPort=${distcc_server_prod_port} \
 -X github.com/Tencent/bk-ci/src/booster/server/pkg/engine/distcc/client/pkg.TestDistCCServerDomain=${distcc_server_test_domain} \
 -X github.com/Tencent/bk-ci/src/booster/server/pkg/engine/distcc/client/pkg.TestDistCCServerPort=${distcc_server_test_port}

BuildBooster_LDFLAG=-X github.com/Tencent/bk-ci/src/booster/bk_dist/booster/command.ProdBuildBoosterServerDomain=${distcc_server_prod_domain} \
 -X github.com/Tencent/bk-ci/src/booster/bk_dist/booster/command.ProdBuildBoosterServerPort=${distcc_server_prod_port} \
 -X github.com/Tencent/bk-ci/src/booster/bk_dist/booster/command.TestBuildBoosterServerDomain=${distcc_server_test_domain} \
 -X github.com/Tencent/bk-ci/src/booster/bk_dist/booster/command.TestBuildBoosterServerPort=${distcc_server_test_port} \
 -X github.com/Tencent/bk-ci/src/booster/bk_dist/booster/command.ServerNecessary=${booster_server_necessary}

BIN_PATH=./build
DISTCC_BIN_PATH=${BIN_PATH}/distcc

distcc_prepare:
	mkdir -p ${DISTCC_BIN_PATH}

distcc_cmake:distcc_prepare
	# bk-cmake
	go build -ldflags "${LDFLAG} ${DISTCC_LDFLAG}" -o ${DISTCC_BIN_PATH}/bk-cmake ./server/pkg/engine/distcc/client/cmake/main.go

distcc_make:distcc_prepare
	# bk-make
	go build -ldflags "${LDFLAG} ${DISTCC_LDFLAG}" -o ${DISTCC_BIN_PATH}/bk-make ./server/pkg/engine/distcc/client/make/main.go

distcc_bazel:distcc_prepare
	# bk-bazel
	go build -ldflags "${LDFLAG} ${DISTCC_LDFLAG}" -o ${DISTCC_BIN_PATH}/bk-bazel ./server/pkg/engine/distcc/client/bazel/main.go

distcc_blade:distcc_prepare
	# bk-blade
	go build -ldflags "${LDFLAG} ${DISTCC_LDFLAG}" -o ${DISTCC_BIN_PATH}/bk-blade ./server/pkg/engine/distcc/client/blade/main.go

distcc_ninja:distcc_prepare
    # bk-ninja
	go build -ldflags "${LDFLAG} ${DISTCC_LDFLAG}" -o ${DISTCC_BIN_PATH}/bk-ninja ./server/pkg/engine/distcc/client/ninja/main.go

distcc:distcc_cmake distcc_make distcc_bazel distcc_blade distcc_ninja

clean_distcc:
	rm -rf ${DISTCC_BIN_PATH}

clean:
	rm -rf ${BIN_PATH}
	cd tools/hook_ld_preload && make clean

FASTBUILD_BIN_PATH=${BIN_PATH}/fastbuild

fastbuild:
	mkdir -p ${FASTBUILD_BIN_PATH}
	# bk-fb-server
	go build -ldflags "${LDFLAG}" -o ${FASTBUILD_BIN_PATH}/bk-fb-server ./fastbuild/bk-fb-server/main.go
	# bk-fb-agent
	go build -ldflags "${LDFLAG}" -o ${FASTBUILD_BIN_PATH}/bk-fb-agent ./fastbuild/bk-fb-agent/main.go

BUILDBOOSTER_BIN_PATH=${BIN_PATH}/buildbooster

buildbooster_prepare:
	mkdir -p ${BUILDBOOSTER_BIN_PATH}
	if [ -z "${DISABLE_PACKR2}" ]; then cd bk_dist/dashboard/pkg/dashboard && packr2; fi

buildbooster_server:buildbooster_prepare
	go build -ldflags "${LDFLAG}" -o ${BUILDBOOSTER_BIN_PATH}/bk-buildbooster-server ./server/main.go

buildbooster_gateway:buildbooster_prepare
	go build -ldflags "${LDFLAG}" -o ${BUILDBOOSTER_BIN_PATH}/bk-buildbooster-gateway ./gateway/main.go

buildbooster_agent:buildbooster_prepare
	go build -ldflags "${LDFLAG}" -o ${BUILDBOOSTER_BIN_PATH}/bk-bb-agent ./server/pkg/resource/direct/agent/main.go

buildbooster_dashboard:buildbooster_prepare
	go build -ldflags "${LDFLAG}" -o ${BUILDBOOSTER_BIN_PATH}/bk-buildbooster-dashboard ./bk_dist/dashboard/main.go

buildbooster:buildbooster_server buildbooster_gateway buildbooster_agent buildbooster_dashboard

ALERT_LDFLAG=-X github.com/Tencent/bk-ci/src/booster/alertserver/pkg/types.Sendurl=${sendurl} \
 -X github.com/Tencent/bk-ci/src/booster/alertserver/pkg/types.AppCode=${app_appcode} \
 -X github.com/Tencent/bk-ci/src/booster/alertserver/pkg/types.AppSecret=${app_secret} \
 -X github.com/Tencent/bk-ci/src/booster/alertserver/pkg/types.Operator=${operator}

ALERT_BIN_PATH=${BIN_PATH}/alertserver

alertserver:
	mkdir -p ${ALERT_BIN_PATH}

	# bk-alert-server
	go build -ldflags "${LDFLAG} ${ALERT_LDFLAG}" -o ${ALERT_BIN_PATH}/bk-prometheus-alert-server ./alertserver/main.go

DISTEXECUTOR_BIN_PATH=${BIN_PATH}/bkdist

dist_prepare:
	mkdir -p ${DISTEXECUTOR_BIN_PATH}
	go get -u github.com/gobuffalo/packr/v2/packr2
	export PATH=${GOPATH}/bin:${PATH}
	if [ -z "${DISABLE_PACKR2}" ]; then cd bk_dist/controller/pkg/dashboard && packr2; fi

dist_executor:dist_prepare
	# bk-dist-executor
	go build -ldflags "${LDFLAG}" -o ${DISTEXECUTOR_BIN_PATH}/bk-dist-executor ./bk_dist/executor/main.go

dist_booster:dist_prepare
	# bk-booster
	go build -ldflags "${LDFLAG} ${BuildBooster_LDFLAG}" -o ${DISTEXECUTOR_BIN_PATH}/bk-booster ./bk_dist/booster/main.go

dist_controller:dist_prepare
	# bk-dist-controller
	go build -ldflags "${LDFLAG}" -o ${DISTEXECUTOR_BIN_PATH}/bk-dist-controller ./bk_dist/controller/main.go

dist_worker:dist_prepare
	# bk-dist-worker
	go build -ldflags "${LDFLAG}" -o ${DISTEXECUTOR_BIN_PATH}/bk-dist-worker ./bk_dist/worker/main.go

dist_idle_loop:dist_prepare
	# bk-idle-loop
	go build -ldflags "${LDFLAG}" -o ${DISTEXECUTOR_BIN_PATH}/bk-idle-loop ./bk_dist/idleloop/main.go

dist_monitor:dist_prepare
	# bk-dist-monitor
	go build -ldflags "${LDFLAG}" -o ${DISTEXECUTOR_BIN_PATH}/bk-dist-monitor ./bk_dist/monitor/main.go

dist_help_tool:dist_prepare
	# bk-help-tool
	go build -ldflags "${LDFLAG}" -o ${DISTEXECUTOR_BIN_PATH}/bk-help-tool ./bk_dist/helptool/main.go

dist_ubt_tool:dist_prepare
	# bk-ubt-tool
	go build -ldflags "${LDFLAG}" -o ${DISTEXECUTOR_BIN_PATH}/bk-ubt-tool ./bk_dist/ubttool/main.go

dist_shader_tool:dist_prepare
	# bk-shader-tool
	go build -ldflags "${LDFLAG}" -o ${DISTEXECUTOR_BIN_PATH}/bk-shader-tool ./bk_dist/shadertool/main.go

dist_hook:dist_prepare
	cd tools/hook_ld_preload && $(MAKE) all && cp bkhook.so ../../${DISTEXECUTOR_BIN_PATH}/

dist:dist_executor dist_booster dist_controller dist_worker dist_idle_loop dist_help_tool dist_ubt_tool dist_shader_tool dist_hook dist_monitor

clean_dist:
	rm -rf ${DISTEXECUTOR_BIN_PATH}

encrypt:
	mkdir -p ${BIN_PATH}/tools
	go build -ldflags "-s -w -X main.PriKey=${encryption_key}" -o ${BIN_PATH}/tools/encrypt ./tools/encrypt/main.go
