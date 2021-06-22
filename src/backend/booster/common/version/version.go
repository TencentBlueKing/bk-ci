package version

import (
	"fmt"
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
)

// ShowVersion is the default handler which match the --version flag.
func ShowVersion() {
	fmt.Printf("%s", GetVersion())
}

// GetVersion get version message string.
func GetVersion() string {
	version := fmt.Sprintf("Version  :%s\nTag      :%s\nBuildTime:  %s\nGitHash:  %s\n", Version, Tag, BuildTime, GitHash)
	return version
}
