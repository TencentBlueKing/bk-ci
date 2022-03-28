/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package sdk

import (
	"strconv"
	"time"
)

// StatsTime 是专为stats中的time.Time字段使用的时间类型, 目的是让其在JSON序列化时, 将time.Time转为UnixNano int64
type StatsTime time.Time

// MarshalJSON generate []byte from StatsTime when json encoding
func (t StatsTime) MarshalJSON() ([]byte, error) {
	return []byte(strconv.FormatInt(time.Time(t).UnixNano(), 10)), nil
}

// UnmarshalJSON parse StatsTime from []byte when json decoding
func (t *StatsTime) UnmarshalJSON(s []byte) (err error) {
	r := string(s)
	q, err := strconv.ParseInt(r, 10, 64)
	if err != nil {
		return err
	}
	*(*time.Time)(t) = time.Unix(0, q)
	return nil
}

// Time return the time.Time of StatsTime
func (t StatsTime) Time() time.Time {
	return time.Time(t).Local()
}

// UnixNano return the UnixNano of StatsTime
func (t StatsTime) UnixNano() int64 {
	return t.Time().UnixNano()
}

// Unix return the Unix of StatsTime
func (t StatsTime) Unix() int64 {
	return t.Time().Unix()
}

// String return the String of StatsTime
func (t StatsTime) String() string {
	return t.Time().String()
}

// StatsTimeNow get a StatsTime from current time
func StatsTimeNow(t *StatsTime) {
	*t = StatsTime(time.Now().Local())
}
