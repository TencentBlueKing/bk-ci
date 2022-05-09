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
	"path/filepath"
	"strings"
)

// FileType 作为File文件实体的类型, 提供多个不同种类
// 用于在插入/查询索引时, 对不同的类型做不同的定制操作
type FileType int

const (
	FileTypeDir FileType = iota
	FileTypeRelative
	FileTypeCanonical
)

// NewFileWithCache 给定cache主体, 获取对应type和str的文件实体
func NewFileWithCache(cache *FileCache, fileType FileType, str string) *File {
	if cache == nil {
		cache = NewFileCache()
	}

	f := &File{
		cache:    cache,
		fileType: fileType,
		index:    -1,
		string:   str,
	}
	f.init()
	return f
}

// File 作为文件的实体, 提供从字符串到数字一一对应的索引, 用于在后续的缓存中更方便地进行hash和比较
// 该文件不一定存在, 只是一个内存中的索引, 是否真的存在还需要另外检测
type File struct {
	// 挂载的cache主体, 所有的index都在cache中获得或插入
	cache *FileCache

	// file的类型, 主要用于区分不同的cache子集, 可以视为第一层缓存分散
	fileType FileType

	// 在文件名缓存中的索引
	index int
	// 原始的文件名
	string string
}

func (f *File) init() {
	switch f.fileType {
	case FileTypeDir:
		f.index = f.cache.directoryMap.Index(f.string)
		f.string = f.cache.directoryMap.String(f.index)
	case FileTypeRelative:
		f.index = f.cache.relativeMap.Index(f.string)
		f.string = f.cache.relativeMap.String(f.index)
	case FileTypeCanonical:
		f.index = f.cache.canonicalMap.Index(f.string)
		f.string = f.cache.canonicalMap.String(f.index)
	default:
		f.index = f.cache.canonicalMap.Index(f.string)
		f.string = f.cache.canonicalMap.String(f.index)
	}
}

// Index 获取文件在缓存中对应的索引
func (f *File) Index() int {
	return f.index
}

// String 获取文件原始的名字
func (f *File) String() string {
	return f.string
}

// Dir 获取文件的所在目录
func (f *File) Dir() *File {
	if f.string == "/" || f.string == "" {
		return f
	}

	return f.cache.GetDirectoryFile(filepath.Dir(strings.TrimRight(f.string, "/")))
}

// Equal 判断文件是否与t文件是同一个文件
func (f *File) Equal(t *File) bool {
	if t == nil {
		return false
	}

	return f.fileType == t.fileType && f.index == t.index
}
