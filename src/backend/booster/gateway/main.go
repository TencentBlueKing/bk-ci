package main

import (
	"fmt"
	"os"

	"build-booster/common/blog"
	"build-booster/gateway/config"
	"build-booster/gateway/pkg"
)

func main() {
	c := config.NewConfig()
	c.Parse()
	blog.InitLogs(c.LogConfig)
	defer blog.CloseLogs()

	if err := pkg.Run(c); err != nil {
		_, _ = fmt.Fprintln(os.Stderr, err)
		os.Exit(1)
	}
}
