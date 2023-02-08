/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package client

import (
	"fmt"
	"io"
	"io/ioutil"
	"net"
	"time"

	"github.com/Tencent/bk-ci/src/booster/bk_dist/common/protocol"
	dcUtil "github.com/Tencent/bk-ci/src/booster/bk_dist/common/util"
	"github.com/Tencent/bk-ci/src/booster/common/blog"
)

// define const vars for http client
const (
	// TCPBUFFERLEN = 10240

	DEFAULTTIMEOUTSECS        = 300
	DEFAULTREADALLTIMEOUTSECS = 600
)

// TCPClient wrapper net.TCPConn
type TCPClient struct {
	timeout int
	conn    *net.TCPConn
}

// NewTCPClient return new TCPClient
func NewTCPClient(timeout int) *TCPClient {
	if timeout <= 0 {
		timeout = DEFAULTTIMEOUTSECS
	}

	return &TCPClient{
		timeout: timeout,
	}
}

// Connect connect to server
func (c *TCPClient) Connect(server string) error {
	// resolve server
	resolvedserver, err := net.ResolveTCPAddr("tcp", server)
	if err != nil {
		blog.Errorf("server [%s] resolve error: [%s]", server, err.Error())
		return err
	}

	t := time.Now().Local()
	c.conn, err = net.DialTCP("tcp", nil, resolvedserver)
	d := time.Now().Sub(t)
	if d > 50*time.Millisecond {
		blog.Debugf("TCP Dail to long gt50 to server(%s): %s", resolvedserver, d.String())
	}
	if d > 200*time.Millisecond {
		blog.Debugf("TCP Dail to long gt200 to server(%s): %s", resolvedserver, d.String())
	}

	if err != nil {
		blog.Errorf("connect to server error: [%s]", err.Error())
		return err
	}

	blog.Debugf("succeed to connect to server [%s] ", server)
	// blog.Infof("succeed to establish connection [%s] ", c.ConnDesc())

	// not sure whether it can imporve performance
	err = c.conn.SetNoDelay(false)
	if err != nil {
		blog.Errorf("set no delay to false error: [%s]", err.Error())
	}

	return nil
}

func (c *TCPClient) setIOTimeout(timeoutsecs int) error {
	if timeoutsecs <= 0 {
		return nil
	}

	t := time.Now()
	return c.conn.SetDeadline(t.Add(time.Duration(timeoutsecs) * time.Second))
}

// WriteData write data
func (c *TCPClient) WriteData(data []byte) error {
	if data == nil {
		return fmt.Errorf("input data is nil")
	}

	if err := c.setIOTimeout(c.timeout); err != nil {
		blog.Errorf("set io timeout error: [%s]", err.Error())
		return err
	}

	writelen := 0
	expectlen := len(data)
	for writelen < expectlen {
		ret, err := c.conn.Write((data)[writelen:])
		if err != nil {
			blog.Errorf("write token int error: [%s]", err.Error())
			return err
		}
		writelen += ret
	}

	if expectlen < 32 {
		blog.Debugf("send string '%s' ", string(data))
	} else {
		blog.Debugf("send string length [%d] ", expectlen)
	}
	return nil
}

// SendFile send file
func (c *TCPClient) SendFile(infile string, compress protocol.CompressType) error {
	blog.Debugf("ready write file [%s] with [%s]", infile, compress.String())

	data, err := ioutil.ReadFile(infile)
	if err != nil {
		blog.Debugf("failed to read file[%s]", infile)
		return err
	}

	switch compress {
	case protocol.CompressNone:
		if err := c.WriteData(data); err != nil {
			return err
		}
		return nil
	// case protocol.CompressLZO:
	// 	// compress with lzox1 firstly
	// 	outdata := golzo.Compress1X(data)
	// 	outlen := len(outdata)
	// 	blog.Debugf("compressed with lzo1x, from [%d] to [%d]", len(data), outlen)

	// 	if err := c.WriteData(outdata); err != nil {
	// 		return err
	// 	}
	// 	return nil
	case protocol.CompressLZ4:
		// compress with lz4 firstly
		outdata, _ := dcUtil.Lz4Compress(data)
		outlen := len(outdata)
		blog.Debugf("compressed with lz4, from [%d] to [%d]", len(data), outlen)

		if err := c.WriteData(outdata); err != nil {
			return err
		}
		return nil
	default:
		return fmt.Errorf("unknown compress type [%s]", compress)
	}
}

// ReadData read data
func (c *TCPClient) ReadData(expectlen int) ([]byte, int, error) {
	if err := c.setIOTimeout(c.timeout); err != nil {
		blog.Errorf("set io timeout error: [%s]", err.Error())
		return nil, 0, err
	}

	data := make([]byte, expectlen)
	var readlen int
	for readlen < expectlen {
		ret, err := c.conn.Read(data[readlen:])
		if err != nil {
			if err != io.EOF {
				blog.Errorf("read [%d] data with error: [%s]", readlen, err.Error())
				return nil, 0, err
			}

			readlen += ret
			blog.Debugf("EOF when read [%d] data", readlen)
			break
		}

		readlen += ret
		// blog.Debugf("received [%d] data ", readlen)
	}

	// if readlen < 32 {
	// 	blog.Debugf("got string '%s'", string(data))
	// } else {
	// 	blog.Debugf("got string length [%d] ", readlen)
	// }

	blog.Debugf("finishend receive total [%d] data ", readlen)

	return data, readlen, nil
}

// TryReadData try read data, return immediately after received any data
func (c *TCPClient) TryReadData(expectlen int) ([]byte, int, error) {
	if err := c.setIOTimeout(c.timeout); err != nil {
		blog.Errorf("set io timeout error: [%s]", err.Error())
		return nil, 0, err
	}

	data := make([]byte, expectlen)
	var readlen int
	for readlen <= 0 {
		ret, err := c.conn.Read(data[readlen:])
		if err != nil {
			if err != io.EOF {
				blog.Errorf("read error: [%s]", err.Error())
				return nil, 0, err
			}

			readlen += ret
			blog.Debugf("EOF when read [%d] data", readlen)
			break
		}

		readlen += ret
	}

	if readlen < 32 {
		blog.Debugf("got string '%s'", string(data))
	} else {
		blog.Debugf("got string length [%d] ", readlen)
	}
	return data[0:readlen], readlen, nil
}

// ReadUntilEOF read data until EOF
func (c *TCPClient) ReadUntilEOF() ([]byte, int, error) {
	if err := c.setIOTimeout(DEFAULTREADALLTIMEOUTSECS); err != nil {
		blog.Errorf("set io timeout error: [%s]", err.Error())
		return nil, 0, err
	}

	data := make([]byte, 1024)
	var readlen int
	for {
		ret, err := c.conn.Read(data[readlen:])
		if err != nil {
			if err != io.EOF {
				blog.Errorf("read error: [%s]", err.Error())
				return nil, 0, err
			}

			readlen += ret
			blog.Debugf("EOF when read [%d] data", readlen)
			break
		}

		readlen += ret
		if readlen >= len(data) {
			newdata := make([]byte, len(data)*2)
			copy(newdata[0:], data[:])
			data = newdata
		}
	}

	if readlen < 32 {
		blog.Debugf("got string '%s'", string(data))
	} else {
		blog.Debugf("got string length [%d] ", readlen)
	}
	return data, readlen, nil
}

func sendMessages(client *TCPClient, messages []protocol.Message) error {
	blog.Debugf("send requests")

	if len(messages) == 0 {
		return fmt.Errorf("data to send is empty")
	}

	for _, v := range messages {
		if v.Data == nil {
			blog.Warnf("found nil data when ready send bk-common dist request")
			continue
		}

		switch v.Messagetype {
		case protocol.MessageString:
			if err := client.WriteData(v.Data); err != nil {
				return err
			}
		case protocol.MessageFile:
			if err := client.SendFile(string(v.Data), v.Compresstype); err != nil {
				return err
			}
		default:
			return fmt.Errorf("unknown message type %s", v.Messagetype.String())
		}
	}

	return nil
}

// Close close conn
func (c *TCPClient) Close() error {
	blog.Debugf("ready close connection [%v] ", c.ConnDesc())
	return c.conn.Close()
}

// ConnDesc return desc of conn
func (c *TCPClient) ConnDesc() string {
	if c.conn == nil {
		return ""
	}

	return fmt.Sprintf("%s->%s", c.conn.LocalAddr().String(), c.conn.RemoteAddr().String())
}
