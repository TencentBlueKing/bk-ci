# 安装go-swagger，安装过则跳过
# go install github.com/swaggo/swag/cmd/swag

# 初始化apiserver
swag init -d ../pkg -g ./apiserver/apis/apis.go -o ./apiserver