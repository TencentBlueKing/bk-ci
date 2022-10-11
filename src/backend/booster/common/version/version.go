/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package version

import (
	"bytes"
	"fmt"
	"runtime"

	"github.com/opesun/goquery"
	"github.com/opesun/goquery/exp/html"
)

var (
	// Version describes version.
	// Will be specific when compiling.
	Version = "17.03.28"

	// Tag show the git tag for this version.
	// Will be specific when compiling.
	Tag = "2017-03-28 Release"

	// BuildTime show the compile time.
	// Will be specific when compiling.
	BuildTime = "2017-03-28 19:50:00"

	// GitHash show the current commit hash.
	// Will be specific when compiling.
	GitHash = "unknown"

	// DisttaskRepo show the url of disttask repo
	// Will be specific when compiling.
	DisttaskRepo = ""
)

// ShowVersion is the default handler which match the --version flag.
func ShowVersion() {
	fmt.Printf("%s", GetVersion())
}

// GetVersion get version message string.
func GetVersion() string {
	version := fmt.Sprintf("GoVersion: %s\nVersion:   %s\nTag:       %s\nBuildTime: %s\nGitHash:   %s\n",
		runtime.Version(), Version, Tag, BuildTime, GitHash)
	return version
}

func Search(buf *bytes.Buffer, n *goquery.Node) {
	if n == nil {
		return
	}
	if n.Type == html.TextNode {
		fmt.Fprintf(buf, "%v", n.Data)
	}
	for _, v := range n.Child {
		Search(buf, &goquery.Node{v})
	}
}
