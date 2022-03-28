/*
Copyright 2014 The Kubernetes Authors All rights reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package util

import (
	goflag "flag"
	"os"
	"strings"

	"github.com/Tencent/bk-ci/src/booster/common/version"

	"github.com/spf13/pflag"
)

// WordSepNormalizeFunc changes all flags that contain "_" separators
func WordSepNormalizeFunc(f *pflag.FlagSet, name string) pflag.NormalizedName {
	if strings.Contains(name, "_") {
		return pflag.NormalizedName(strings.Replace(name, "_", "-", -1))
	}
	return pflag.NormalizedName(name)
}

// WarnWordSepNormalizeFunc changes and warns for flags that contain "_" separators
func WarnWordSepNormalizeFunc(f *pflag.FlagSet, name string) pflag.NormalizedName {
	if strings.Contains(name, "_") {
		nname := strings.Replace(name, "_", "-", -1)

		return pflag.NormalizedName(nname)
	}
	return pflag.NormalizedName(name)
}

// AddCommonFlags add common flags that is needed by all modules
func AddCommonFlags(cmdline *pflag.FlagSet) *bool {
	return cmdline.Bool("version", false, "show version infomation")
}

// InitFlags normalizes and parses the command line flags
func InitFlags() {
	pflag.CommandLine.SetNormalizeFunc(WordSepNormalizeFunc)
	pflag.CommandLine.AddGoFlagSet(goflag.CommandLine)

	var help bool
	pflag.CommandLine.BoolVarP(&help, "help", "h", false, "show this help info")

	ver := AddCommonFlags(pflag.CommandLine)
	pflag.Parse()

	if help {
		pflag.PrintDefaults()
		os.Exit(0)
	}

	//add handler if flag include --version/-v
	if *ver {
		version.ShowVersion()
		os.Exit(0)
	}
}
