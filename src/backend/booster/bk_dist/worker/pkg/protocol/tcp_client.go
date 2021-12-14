/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package protocol

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
	conn *net.TCPConn
}

// NewTCPClientWithConn return new TCPClient with specified conn
func NewTCPClientWithConn(conn *net.TCPConn) *TCPClient {
	// not sure whether it can imporve performance
	err := conn.SetNoDelay(false)
	if err != nil {
		blog.Errorf("set no delay to false error: [%s]", err.Error())
	}

	return &TCPClient{conn: conn}
}

// Connect connect to server
func (c *TCPClient) Connect(server string) error {
	// resolve server
	resolvedserver, err := net.ResolveTCPAddr("tcp", server)
	if err != nil {
		blog.Errorf("server [%s] resolve error: [%s]", server, err.Error())
		return err
	}

	c.conn, err = net.DialTCP("tcp", nil, resolvedserver)
	if err != nil {
		blog.Errorf("connect to server error: [%s]", err.Error())
		return err
	}

	blog.Debugf("succeed to connect to server [%s] ", server)

	// not sure whether it can imporve performance
	err = c.conn.SetNoDelay(false)
	if err != nil {
		blog.Errorf("set no delay to false error: [%s]", err.Error())
	}

	return nil
}

// Closed check if the TCP connection is closed
func (c *TCPClient) Closed() bool {
	if c.conn == nil {
		return true
	}

	one := make([]byte, 1)
	_ = c.conn.SetReadDeadline(time.Now())
	if _, err := c.conn.Read(one); err == io.EOF {
		return true
	}

	return false
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
	blog.Debugf("ready send [%d] data to remote %s", len(data), c.RemoteAddr())

	if data == nil {
		return fmt.Errorf("input data is nil")
	}

	if err := c.setIOTimeout(DEFAULTTIMEOUTSECS); err != nil {
		blog.Errorf("set io timeout with remote %s error: [%s]", c.RemoteAddr(), err.Error())
		return err
	}
	blog.Debugf("succeed to set io timeout for remote %s", c.RemoteAddr())

	writelen := 0
	expectlen := len(data)
	for writelen < expectlen {
		ret, err := c.conn.Write(data[writelen:])
		if err != nil {
			blog.Errorf("write data to %s error: [%s]", c.RemoteAddr(), err.Error())
			return err
		}
		writelen += ret
	}

	// if expectlen < 32 {
	// 	blog.Debugf("send string '%s' ", string(data))
	// } else {
	// 	blog.Debugf("send string length [%d] ", expectlen)
	// }

	blog.Debugf("finished send [%d] data to remote %s", expectlen, c.RemoteAddr())

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
	if err := c.setIOTimeout(DEFAULTTIMEOUTSECS); err != nil {
		blog.Errorf("set io timeout error: [%s]", err.Error())
		return nil, 0, err
	}

	data := make([]byte, expectlen)
	var readlen int
	for readlen < expectlen {
		ret, err := c.conn.Read(data[readlen:])
		if err != nil {
			if err != io.EOF {
				blog.Errorf("read token int error: [%s]", err.Error())
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
	return data, readlen, nil
}

// TryReadData try read data, return immediately after received any data
func (c *TCPClient) TryReadData(expectlen int) ([]byte, int, error) {
	if err := c.setIOTimeout(DEFAULTTIMEOUTSECS); err != nil {
		blog.Errorf("set io timeout error: [%s]", err.Error())
		return nil, 0, err
	}

	data := make([]byte, expectlen)
	var readlen int
	for readlen <= 0 {
		ret, err := c.conn.Read(data[readlen:])
		if err != nil {
			if err != io.EOF {
				blog.Errorf("read token int error: [%s]", err.Error())
				return nil, 0, err
			}

			readlen += ret
			blog.Debugf("EOF when read [%d] data", readlen)
			break
		}

		readlen += ret
	}

	blog.Infof("got string length [%d] ", readlen)
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

// // ReceiveFile receive file
// func (c *TCPClient) ReceiveFile(outfile string,
// 	filesize int,
// 	compress protocol.CompressType) error {
// 	var f *os.File
// 	var err error
// 	if outfile != "" {
// 		f, err = os.Create(outfile)
// 		if err != nil {
// 			blog.Errorf("read file error: [%s]", err.Error())
// 			return err
// 		}
// 		defer f.Close()
// 	} else {
// 		f = os.Stdout
// 	}

// 	data, retlen, err := c.ReadData(filesize)
// 	if err != nil {
// 		blog.Errorf("read token int error: [%s]", err.Error())
// 		return err
// 	}

// 	if retlen != filesize {
// 		err := fmt.Errorf("filesize [%d], only read [%d]", filesize, retlen)
// 		blog.Errorf("read token int error: [%s]", err.Error())
// 		return err
// 	}

// 	switch compress {
// 	case protocol.CompressNone:
// 		_, err := f.Write(data[:retlen])
// 		if err != nil {
// 			blog.Errorf("save file [%s] error: [%s]", outfile, err.Error())
// 			return err
// 		}
// 		break
// 	case protocol.CompressLZO:
// 		// decompress with lzox1 firstly
// 		outdata, err := golzo.Decompress1X(bytes.NewReader(data), retlen, 0)
// 		if err != nil {
// 			blog.Errorf("decompress error: [%s]", err.Error())
// 			return err
// 		}
// 		outlen := len(string(outdata))
// 		blog.Debugf("decompressed with lzo1x, from [%d] to [%d]", retlen, outlen)

// 		_, err = f.Write(outdata)
// 		if err != nil {
// 			blog.Errorf("save file [%s] error: [%v]", outfile, err)
// 			return err
// 		}
// 		break
// 	default:
// 		return fmt.Errorf("unknown compress type [%s]", compress)
// 	}

// 	blog.Debugf("succeed read and save to file [%s]", outfile)
// 	return nil
// }

// Close close conn
func (c *TCPClient) Close() error {
	return c.conn.Close()
}

// RemoteAddr return RemoteAddr
func (c *TCPClient) RemoteAddr() string {
	if c.conn != nil {
		return c.conn.RemoteAddr().String()
	}

	return ""
}
