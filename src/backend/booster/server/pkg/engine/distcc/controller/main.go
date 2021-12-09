package main

import (
	"fmt"
	"io/ioutil"
	"log"
	"os"

	"build-booster/common/blog"
	"build-booster/server/pkg/engine/distcc/controller/config"
	"build-booster/server/pkg/engine/distcc/controller/pkg"
)

func main() {
	c := config.NewConfig()
	c.Parse()
	blog.InitLogs(c.LogConfig)
	log.SetOutput(ioutil.Discard)
	defer blog.CloseLogs()

	if err := pkg.Run(c); err != nil {
		_, _ = fmt.Fprintln(os.Stderr, err)
		os.Exit(1)
	}
}
