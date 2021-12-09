package main

import (
	"build-booster/server/pkg/engine/distcc/client/pkg"
)

func main() {
	pkg.Run(pkg.ClientMake)
}
