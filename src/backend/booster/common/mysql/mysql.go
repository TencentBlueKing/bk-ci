/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package mysql

import (
	"strings"

	"github.com/jinzhu/gorm"
)

// ListOptions describe the conditions using in mysql select command.
type ListOptions struct {
	offset      int
	limit       int
	selector    []string
	order       []string
	equal       map[string]interface{}
	greaterThan map[string]interface{}
	lessThan    map[string]interface{}
	like        map[string]interface{}
	in          map[string]interface{}
}

// NewListOptions get a new, empty ListOptions.
func NewListOptions() ListOptions {
	opts := ListOptions{}
	opts.ensureInit()
	return opts
}

func (lo *ListOptions) ensureInit() {
	if lo.equal == nil {
		lo.equal = make(map[string]interface{})
	}
	if lo.greaterThan == nil {
		lo.greaterThan = make(map[string]interface{})
	}
	if lo.lessThan == nil {
		lo.lessThan = make(map[string]interface{})
	}
	if lo.like == nil {
		lo.like = make(map[string]interface{})
	}
	if lo.in == nil {
		lo.in = make(map[string]interface{})
	}
	if lo.selector == nil {
		lo.selector = make([]string, 0)
	}
	if lo.order == nil {
		lo.order = make([]string, 0)
	}
}

// Offset set the list offset.
func (lo *ListOptions) Offset(offset int) {
	lo.offset = offset
}

// Limit set the list limit.
func (lo *ListOptions) Limit(limit int) {
	lo.limit = limit
}

// Select decide the return columns, empty for all.
func (lo *ListOptions) Select(selector []string) {
	lo.selector = selector
}

// Order describe the order columns.
func (lo *ListOptions) Order(order []string) {
	lo.order = order
}

// Equal describe the condition: key equal to value.
func (lo *ListOptions) Equal(key string, value interface{}) {
	lo.ensureInit()
	lo.equal[key] = value
}

// Gt describe the condition: key greater than value.
func (lo *ListOptions) Gt(key string, value interface{}) {
	lo.ensureInit()
	lo.greaterThan[key] = value
}

// Lt describe the condition: key less than value.
func (lo *ListOptions) Lt(key string, value interface{}) {
	lo.ensureInit()
	lo.lessThan[key] = value
}

// Like describe the condition: key like value.
func (lo *ListOptions) Like(key string, value interface{}) {
	lo.ensureInit()
	lo.like[key] = value
}

// In describe the condition: key in value list.
func (lo *ListOptions) In(key string, value interface{}) {
	lo.ensureInit()
	lo.in[key] = value
}

// AddWhere receive a database operator and register all the compare conditions into it with 'WHERE'.
func (lo *ListOptions) AddWhere(db *gorm.DB) *gorm.DB {
	for k, v := range lo.equal {
		db = db.Where(k+" = ?", v)
	}
	for k, v := range lo.greaterThan {
		db = db.Where(k+" > ?", v)
	}
	for k, v := range lo.lessThan {
		db = db.Where(k+" < ?", v)
	}
	for k, v := range lo.like {
		db = db.Where(k+" LIKE ?", v)
	}
	for k, v := range lo.in {
		db = db.Where(k+" in (?)", v)
	}
	return db
}

// AddOffsetLimit receive a database operator and register offset and limit into it.
func (lo *ListOptions) AddOffsetLimit(db *gorm.DB) *gorm.DB {
	return db.Offset(lo.offset).Limit(lo.limit)
}

// AddSelector receive a database operator and register selector into it.
func (lo *ListOptions) AddSelector(db *gorm.DB) *gorm.DB {
	if lo.selector == nil || len(lo.selector) == 0 {
		return db
	}
	return db.Select(lo.selector)
}

// AddOrder receive a database operator and register order into it.
func (lo *ListOptions) AddOrder(db *gorm.DB) *gorm.DB {
	if lo.order == nil || len(lo.order) == 0 {
		return db
	}

	for _, o := range lo.order {
		if strings.HasSuffix(o, OpsOrderDESCSuffix) {
			o = strings.TrimPrefix(o, OpsOrderDESCSuffix) + " DESC"
		}
		db = db.Order(o)
	}
	return db
}

const (
	// OpsOrderDESCSuffix is order DESC suffix, "order=name" will be: select * from table order by name;
	// "order=-name" will be: select * from table order by name desc;
	OpsOrderDESCSuffix = "-"
)
