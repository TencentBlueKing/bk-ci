/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package main

import (
	"fmt"
	"io/ioutil"
	"os"
	"os/user"
	"path/filepath"
	"reflect"
	"strings"

	"github.com/Tencent/bk-ci/src/booster/common/codec"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/engine/distcc/client/pkg"
)

const (
	configFilePathEnvKey = "BK_DISTCC_CONFIG_PATH"
)

var (
	usr, _                = user.Current()
	defaultConfigFilePath = filepath.Join(usr.HomeDir, ".bk_distcc.conf")
)

type config struct {
	Debug        *bool   `json:"debug"`
	ProjectID    *string `json:"project_id"`
	Clang        *bool   `json:"clang"`
	Test         *bool   `json:"test"`
	Limit        *int    `json:"limit"`
	NoLocal      *bool   `json:"no_local"`
	SaveCode     *string `json:"save_code"`
	MaxJobs      *int    `json:"max_jobs"`
	MaxLocalJobs *int    `json:"max_local_jobs"`

	BuildID      *string `json:"build_id"`
	GccVersion   *string `json:"gcc_version"`
	CCacheEnable *string `json:"ccache_enable"`
}

func (c *config) generateFlags(configFilePath string) ([]string, error) {
	fileData, err := ioutil.ReadFile(configFilePath)
	if err != nil {
		return nil, fmt.Errorf("read configFile(%s) failed: %v", configFilePath, err)
	}

	if err = codec.DecJSON(fileData, c); err != nil {
		return nil, fmt.Errorf("decode config from configFile(%s) failed: %v", configFilePath, err)
	}

	configType := reflect.TypeOf(c).Elem()
	configValue := reflect.ValueOf(c).Elem()
	n := configType.NumField()

	flags := make([]string, 0, 100)
	for i := 0; i < n; i++ {
		field := configType.Field(i)
		fieldV := configValue.Field(i)
		if field.Type.Kind() != reflect.Ptr {
			continue
		}

		if !fieldV.Elem().IsValid() || !fieldV.Elem().CanSet() {
			continue
		}

		name, ok := field.Tag.Lookup("json")
		if !ok {
			continue
		}

		switch field.Type.Elem().Kind() {
		case reflect.String:
			flags = append(flags, fmt.Sprintf("--%s", name))
			flags = append(flags, fieldV.Elem().String())
		case reflect.Int, reflect.Int32, reflect.Int64:
			flags = append(flags, fmt.Sprintf("--%s", name))
			flags = append(flags, fmt.Sprintf("%d", fieldV.Elem().Int()))
		case reflect.Bool:
			if fieldV.Elem().Bool() {
				flags = append(flags, fmt.Sprintf("--%s", name))
			}
		default:
			continue
		}
	}
	return flags, nil
}

func main() {
	// get config path
	configPath := os.Getenv(configFilePathEnvKey)
	if configPath == "" {
		configPath = defaultConfigFilePath
	}

	config := new(config)
	flags, err := config.generateFlags(configPath)
	if err != nil {
		fmt.Printf("generate flags failed: %v\n", err)
		os.Exit(1)
	}
	flags = append([]string{pkg.ClientMake.Name()}, flags...)

	args := strings.Join(os.Args[1:], " ")
	fmt.Printf("simple call: %s --args \"%s\"\n", strings.Join(flags, " "), args)
	flags = append(flags, "--args")
	flags = append(flags, args)

	app := pkg.GetApp(pkg.ClientMake)
	app.Writer = os.Stdout
	app.Writer = os.Stderr

	if err = app.Run(flags); err != nil {
		fmt.Printf("exec error: %v\n", err)
		os.Exit(1)
	}
}
