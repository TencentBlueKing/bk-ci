/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package pump

import (
	"fmt"
	"net"
	"os"
	"runtime"
	"strconv"

	"github.com/Tencent/bk-ci/src/booster/bk_dist/common/env"
	dcEnv "github.com/Tencent/bk-ci/src/booster/bk_dist/common/env"
)

const (
	hex = "0123456789abcdef"
)

// New get a new pump client via provided socketed
func New(socketAddr string) (*Client, error) {
	conn, err := net.Dial("unix", socketAddr)
	if err != nil {
		return nil, err
	}

	return &Client{
		conn: conn,
	}, nil
}

// Client provide a handler to do request to distcc-pump include-server
type Client struct {
	conn net.Conn
}

// Analyze do the analyze via include-server
func (c *Client) Analyze(dir string, args []string) ([]string, error) {
	if err := c.writeCwd(dir); err != nil {
		return nil, err
	}

	if err := c.write(args); err != nil {
		return nil, err
	}

	return c.read()
}

func (c *Client) writeCwd(dir string) error {
	if realDir, err := os.Readlink(dir); err == nil {
		dir = realDir
	}

	return c.writeString("CDIR", dir)
}

func (c *Client) writeInt(token string, num int) error {
	buf := []byte(token)

	for i := 28; i >= 0; i -= 4 {
		buf = append(buf, hex[num>>i&0xf])
	}

	_, err := c.conn.Write(buf)
	return err
}

func (c *Client) writeString(token string, args string) error {
	if err := c.writeInt(token, len(args)); err != nil {
		return err
	}

	_, err := c.conn.Write([]byte(args))
	return err
}

func (c *Client) write(args []string) error {
	if err := c.writeInt("ARGC", len(args)); err != nil {
		return err
	}

	for _, arg := range args {
		if err := c.writeString("ARGV", arg); err != nil {
			return err
		}
	}

	return nil
}

func (c *Client) readInt(token string) (int, error) {
	data := make([]byte, 12)
	if _, err := c.conn.Read(data); err != nil {
		return 0, err
	}

	if len(data) != 12 {
		return 0, fmt.Errorf("error data: %s", string(data))
	}

	if token != string(data[:4]) {
		return 0, fmt.Errorf("error token from data: %s", string(data))
	}

	length, err := strconv.ParseInt(string(data[4:]), 16, 32)
	return int(length), err
}

func (c *Client) readString(token string) (string, error) {
	length, err := c.readInt(token)
	if err != nil {
		return "", err
	}

	data := make([]byte, length)
	_, err = c.conn.Read(data)

	return string(data), err
}

func (c *Client) read() ([]string, error) {
	length, err := c.readInt("ARGC")
	if err != nil {
		return nil, err
	}

	files := make([]string, 0, 100)
	for i := 0; i < length; i++ {
		r, err := c.readString("ARGV")
		if err != nil {
			return nil, err
		}

		files = append(files, r)
	}

	return files, nil
}

func IsPump(env *env.Sandbox) bool {
	return env.GetEnv(dcEnv.KeyExecutorPump) != ""
}

func SupportPump(env *env.Sandbox) bool {
	// return IsPump(env) && (runtime.GOOS == "windows" || runtime.GOOS == "darwin")
	return IsPump(env) && runtime.GOOS == "windows"
}

func IsPumpCache(env *env.Sandbox) bool {
	return env.GetEnv(dcEnv.KeyExecutorPumpCache) != ""
}

func PumpCacheDir(env *env.Sandbox) string {
	return env.GetEnv(dcEnv.KeyExecutorPumpCacheDir)
}

func PumpCacheSizeMaxMB(env *env.Sandbox) int32 {
	strsize := env.GetEnv(dcEnv.KeyExecutorPumpCacheSizeMaxMB)
	if strsize != "" {
		size, err := strconv.Atoi(strsize)
		if err != nil {
			return -1
		} else {
			return int32(size)
		}
	}

	return -1
}

func PumpMinActionNum(env *env.Sandbox) int32 {
	strsize := env.GetEnv(dcEnv.KeyExecutorPumpMinActionNum)
	if strsize != "" {
		size, err := strconv.Atoi(strsize)
		if err != nil {
			return 0
		} else {
			return int32(size)
		}
	}

	return 0
}
