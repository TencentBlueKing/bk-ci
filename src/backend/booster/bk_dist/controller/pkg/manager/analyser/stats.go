/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package analyser

import (
	"time"
)

// NewStats provide an empty Stats
func NewStats() *Stats {
	return &Stats{}
}

// Stats describe the stats information of analyser
type Stats struct {
	TimeTotal time.Duration

	// resolve总数统计
	ResolveCount       int64
	ResolveTimeTotal   time.Duration
	ResolveTimeAvg     time.Duration
	ResolveTimeLongest time.Duration

	// resolve实际执行统计
	ResolveProcessCount       int64
	ResolveProcessTimeTotal   time.Duration
	ResolveProcessTimeAvg     time.Duration
	ResolveProcessTimeLongest time.Duration

	// resolve缓存命中统计
	ResolveHitCount       int64
	ResolveHitTimeTotal   time.Duration
	ResolveHitTimeAvg     time.Duration
	ResolveHitTimeLongest time.Duration

	// parse-file总数统计
	ParseFileCount       int64
	ParseFileTimeTotal   time.Duration
	ParseFileTimeAvg     time.Duration
	ParseFileTimeLongest time.Duration

	// parse-file实际执行统计
	ParseFileProcessCount       int64
	ParseFileProcessTimeTotal   time.Duration
	ParseFileProcessTimeAvg     time.Duration
	ParseFileProcessTimeLongest time.Duration

	// parse-file缓存命中统计
	ParseFileHitCount       int64
	ParseFileHitTimeTotal   time.Duration
	ParseFileHitTimeAvg     time.Duration
	ParseFileHitTimeLongest time.Duration

	// find-node总数统计
	FindNodeCount       int64
	FindNodeTimeTotal   time.Duration
	FindNodeTimeAvg     time.Duration
	FindNodeTimeLongest time.Duration

	// find-node实际执行统计
	FindNodeProcessCount       int64
	FindNodeProcessTimeTotal   time.Duration
	FindNodeProcessTimeAvg     time.Duration
	FindNodeProcessTimeLongest time.Duration

	// find-node缓存命中统计
	FindNodeHitCount       int64
	FindNodeHitTimeTotal   time.Duration
	FindNodeHitTimeAvg     time.Duration
	FindNodeHitTimeLongest time.Duration
}

func (s *Stats) resolve(t time.Duration) {
	s.ResolveCount++
	s.ResolveTimeTotal += t
	if s.ResolveTimeLongest < t {
		s.ResolveTimeLongest = t
	}
}

func (s *Stats) resolveProcess(t time.Duration) {
	s.resolve(t)

	s.ResolveProcessCount++
	s.ResolveProcessTimeTotal += t
	if s.ResolveProcessTimeLongest < t {
		s.ResolveProcessTimeLongest = t
	}
}

func (s *Stats) resolveHit(t time.Duration) {
	s.resolve(t)

	s.ResolveHitCount++
	s.ResolveHitTimeTotal += t
	if s.ResolveHitTimeLongest < t {
		s.ResolveHitTimeLongest = t
	}
}

func (s *Stats) resolveEx(hit bool, t time.Duration) {
	if hit {
		s.resolveHit(t)
		return
	}

	s.resolveProcess(t)
}

func (s *Stats) parseFile(t time.Duration) {
	s.ParseFileCount++
	s.ParseFileTimeTotal += t
	if s.ParseFileTimeLongest < t {
		s.ParseFileTimeLongest = t
	}
}

func (s *Stats) parseFileProcess(t time.Duration) {
	s.parseFile(t)

	s.ParseFileProcessCount++
	s.ParseFileProcessTimeTotal += t
	if s.ParseFileProcessTimeLongest < t {
		s.ParseFileProcessTimeLongest = t
	}
}

func (s *Stats) parseFileHit(t time.Duration) {
	s.parseFile(t)

	s.ParseFileHitCount++
	s.ParseFileHitTimeTotal += t
	if s.ParseFileHitTimeLongest < t {
		s.ParseFileHitTimeLongest = t
	}
}

func (s *Stats) parseFileEx(hit bool, t time.Duration) {
	if hit {
		s.parseFileHit(t)
		return
	}

	s.parseFileProcess(t)
}

func (s *Stats) findNode(t time.Duration) {
	s.FindNodeCount++

	s.FindNodeTimeTotal += t
	if s.FindNodeTimeLongest < t {
		s.FindNodeTimeLongest = t
	}
}

func (s *Stats) findNodeProcess(t time.Duration) {
	s.findNode(t)

	s.FindNodeProcessCount++

	s.FindNodeProcessTimeTotal += t
	if s.FindNodeProcessTimeLongest < t {
		s.FindNodeProcessTimeLongest = t
	}
}

func (s *Stats) findNodeHit(t time.Duration) {
	s.FindNodeHitCount++

	s.FindNodeHitTimeTotal += t
	if s.FindNodeHitTimeLongest < t {
		s.FindNodeHitTimeLongest = t
	}
}

func (s *Stats) findNodeEx(hit bool, t time.Duration) {
	if hit {
		s.findNodeHit(t)
		return
	}

	s.findNodeProcess(t)
}

// Calculate calculate the stats result, such as average time.
func (s *Stats) Calculate() {
	if s.ResolveCount > 0 {
		s.ResolveTimeAvg = s.ResolveTimeTotal / time.Duration(s.ResolveCount)
	}
	if s.ResolveProcessCount > 0 {
		s.ResolveProcessTimeAvg = s.ResolveProcessTimeTotal / time.Duration(s.ResolveProcessCount)
	}
	if s.ResolveHitCount > 0 {
		s.ResolveHitTimeAvg = s.ResolveHitTimeTotal / time.Duration(s.ResolveHitCount)
	}

	if s.ParseFileCount > 0 {
		s.ParseFileTimeAvg = s.ParseFileTimeTotal / time.Duration(s.ParseFileCount)
	}
	if s.ParseFileProcessCount > 0 {
		s.ParseFileProcessTimeAvg = s.ParseFileProcessTimeTotal / time.Duration(s.ParseFileProcessCount)
	}
	if s.ParseFileHitCount > 0 {
		s.ParseFileHitTimeAvg = s.ParseFileHitTimeTotal / time.Duration(s.ParseFileHitCount)
	}

	if s.FindNodeCount > 0 {
		s.FindNodeTimeAvg = s.FindNodeTimeTotal / time.Duration(s.FindNodeCount)
	}
	if s.FindNodeProcessCount > 0 {
		s.FindNodeProcessTimeAvg = s.FindNodeProcessTimeTotal / time.Duration(s.FindNodeProcessCount)
	}
	if s.FindNodeHitCount > 0 {
		s.FindNodeHitTimeAvg = s.FindNodeHitTimeTotal / time.Duration(s.FindNodeHitCount)
	}
}
