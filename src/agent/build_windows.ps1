$WORK_DIR = ${PWD}.Path
go env -w GOOS = windows
go env -w GOARCH = "386"
echo $WORK_DIR\bin\
if (Test-Path -Path $WORK_DIR\bin\)
{
    Remove-Item -Force -Recurse $WORK_DIR\bin\
}
mkdir $WORK_DIR\bin
$Env:GO111MODULE = "on"
$BuildTime = Get-Date -Format "yyyy/MM/dd.HH:mm.K"
$GitCommit = git rev-parse HEAD
$BUILD_FLAGS = "-ldflags=`"-w -s -X github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/config.BuildTime=$BuildTime -X github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/config.GitCommit=$GitCommit`""

go env

go build $BUILD_FLAGS -o $WORK_DIR\bin\devopsDaemon.exe $WORK_DIR\src\cmd\daemon

go build $BUILD_FLAGS -o $WORK_DIR\bin\devopsAgent.exe $WORK_DIR\src\cmd\agent

go build $BUILD_FLAGS -o $WORK_DIR\bin\upgrader.exe $WORK_DIR\src\cmd\upgrader

go build $BUILD_FLAGS -o $WORK_DIR\bin\installer.exe $WORK_DIR\src\cmd\installer
