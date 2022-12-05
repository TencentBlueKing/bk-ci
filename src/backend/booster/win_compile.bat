echo "windows compile"
@echo off
SET curpath=%cd%
cd ..\..
set GOPATH=%cd%

set "encryption_key=%1"
set server_cert_pwd=
set client_cert_pwd=

cd %curpath%\fastbuild\
for /F %%i in ('git describe --tags') do ( set gittag=%%i)
echo GITTAG=%gittag%

if [%BUILDTIME%] == [] (
    rem set BUILDTIME=%date:~0,4%-%date:~5,2%-%date:~8,2%
    set BUILDTIME=%date:~3,4%.%date:~8,2%.%date:~11,2%
)

for /F %%i in ('git rev-parse HEAD') do ( set githash=%%i)
echo GITHASH=%githash%

if [%CURRENTDATE%] == [] (
    rem set CURRENTDATE=%date:~0,4%.%date:~5,2%.%date:~8,2%
    set CURRENTDATE=%date:~3,4%.%date:~8,2%.%date:~11,2%
)

set VERSION=%GITTAG%-%CURRENTDATE%
echo %VERSION%

set "LDFLAG=-X github.com/Tencent/bk-ci/src/booster/common/static.EncryptionKey=%encryption_key% -X github.com/Tencent/bk-ci/src/booster/common/static.ServerCertPwd=%server_cert_pwd% -X github.com/Tencent/bk-ci/src/booster/common/static.ClientCertPwd=%client_cert_pwd% -X github.com/Tencent/bk-ci/src/booster/common/version.Version=%VERSION% -X github.com/Tencent/bk-ci/src/booster/common/version.BuildTime=%BUILDTIME% -X github.com/Tencent/bk-ci/src/booster/common/version.GitHash=%GITHASH% -X github.com/Tencent/bk-ci/src/booster/common/version.Tag=%GITTAG%"
set "BuildBooster_LDFLAG=-X github.com/Tencent/bk-ci/src/booster/bk_dist/booster/command.ProdBuildBoosterServerDomain=%distcc_server_prod_domain% -X github.com/Tencent/bk-ci/src/booster/bk_dist/booster/command.ProdBuildBoosterServerPort=%distcc_server_prod_port% -X github.com/Tencent/bk-ci/src/booster/bk_dist/booster/command.TestBuildBoosterServerDomain=%distcc_server_test_domain% -X github.com/Tencent/bk-ci/src/booster/bk_dist/booster/command.TestBuildBoosterServerPort=%distcc_server_test_port%"

cd %curpath%
set bindir=%curpath%\bin
if not exist %bindir% (
	mkdir %bindir%
)

go build -ldflags "%LDFLAG%" -o %bindir%\bk-FbMain.exe %curpath%\fastbuild\bk-fb-main\main.go

go build -ldflags "%LDFLAG%" -o %bindir%\bk-FBuild.exe %curpath%\fastbuild\bk-fb-build\main.go

go build -ldflags "%LDFLAG%" -o %bindir%\bk-bb-agent.exe %curpath%\server\pkg\resource\direct\agent\main.go

go build -ldflags "%LDFLAG% %BuildBooster_LDFLAG%" -o %bindir%\bk-booster.exe %curpath%\bk_dist\booster\main.go

go build -ldflags "%LDFLAG% %BuildBooster_LDFLAG%" -o %bindir%\bk-idle-loop.exe %curpath%\bk_dist\idleloop\main.go

go build -ldflags "%LDFLAG% %BuildBooster_LDFLAG%" -o %bindir%\bk-dist-monitor.exe %curpath%\bk_dist\monitor\main.go

go build -ldflags "%LDFLAG% %BuildBooster_LDFLAG%" -o %bindir%\bk-help-tool.exe %curpath%\bk_dist\helptool\main.go

go build -ldflags "%LDFLAG% %BuildBooster_LDFLAG%" -o %bindir%\bk-ubt-tool.exe %curpath%\bk_dist\ubttool\main.go

go build -ldflags "%LDFLAG% %BuildBooster_LDFLAG%" -o %bindir%\bk-shader-tool.exe %curpath%\bk_dist\shadertool\main.go

go build -ldflags "%LDFLAG% %BuildBooster_LDFLAG%" -o %bindir%\bk-dist-controller.exe %curpath%\bk_dist\controller\main.go

go build -ldflags "%LDFLAG% %BuildBooster_LDFLAG%" -o %bindir%\bk-dist-executor.exe %curpath%\bk_dist\executor\main.go

go build -ldflags "%LDFLAG% %BuildBooster_LDFLAG%" -o %bindir%\bk-cl-pre-deal.exe %curpath%\tools\cl_pre_deal\main.go

set "Hide_LDFLAG=-H=windowsgui"
go build -ldflags "%LDFLAG% %BuildBooster_LDFLAG% %Hide_LDFLAG%" -o %bindir%/bk-dist-worker.exe %curpath%\bk_dist\worker\main.go