# fmt.ps1 — Windows 平台代码格式化
# 排除 linux/darwin/unix 平台文件，避免 goimports-reviser 误删平台特有的 import。

# goimports 处理全部文件（它本身支持跨平台）
goimports -w .

$files = Get-ChildItem -Recurse -Filter '*.go' |
    Where-Object { $_.Name -notmatch '_(linux|darwin|unix|linux_test|darwin_test|unix_test|darwin_cgo)\.go$' }

foreach ($f in $files) {
    gofmt -w -l $f.FullName
}

foreach ($f in $files) {
    goimports-reviser -rm-unused -set-alias -format $f.FullName
}
