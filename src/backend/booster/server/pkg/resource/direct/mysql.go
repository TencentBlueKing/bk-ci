/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package direct

import (
	"fmt"
	"time"

	"github.com/Tencent/bk-ci/src/booster/common/blog"
	commonMySQL "github.com/Tencent/bk-ci/src/booster/common/mysql"

	"github.com/jinzhu/gorm"
	// 启用mysql driver
	_ "github.com/jinzhu/gorm/dialects/mysql"
)

// AgentResource : table for agent resource
type AgentResource struct {
	Cluster     string    `gorm:"column:cluster;primary_key" sql:"type:varchar(64)" json:"cluster"`
	IP          string    `gorm:"column:ip;primary_key" sql:"type:varchar(32)" json:"ip"`
	UpdatedAt   time.Time `gorm:"column:updated_at" json:"-"`
	TotalCPU    float32   `gorm:"column:total_cpu" json:"total_cpu"`
	TotalMemory float32   `gorm:"column:total_memory" json:"total_memory"`
	TotalDisk   float32   `gorm:"column:total_disk" json:"total_disk"`
	FreeCPU     float32   `gorm:"column:free_cpu" json:"free_cpu"`
	FreeMemory  float32   `gorm:"column:free_memory" json:"free_memory"`
	FreeDisk    float32   `gorm:"column:free_disk" json:"free_disk"`
}

const (
	mysqlTableAgentResource = "direct_resource"
)

// TableName : get table name for AgentResource
func (ar AgentResource) TableName() string {
	return mysqlTableAgentResource
}

// AllocatedResource : table for allocated resource
type AllocatedResource struct {
	UpdatedAt       time.Time `gorm:"column:updated_at" json:"-"`
	UserID          string    `gorm:"column:user_id;primary_key" sql:"type:varchar(64)" json:"user_id"`
	ResourceBatchID string    `gorm:"column:resource_batch_id;primary_key" sql:"type:varchar(128)" json:"resource_batch_id"`
	Released        int32     `gorm:"column:released;index" json:"released"`
	AllocatedTime   int64     `gorm:"column:allocated_time" json:"allocated_time"`
	ReleasedTime    int64     `gorm:"column:released_time" json:"released_time"`
	AllocatedAgent  string    `gorm:"column:allocated_agent" sql:"type:text" json:"allocated_agent"`
	Message         string    `gorm:"column:message" sql:"type:text" json:"message"`
}

const (
	mysqlTableAllocatedResource = "allocated_resource"
)

// TableName : get table name for AllocatedResource
func (ar AllocatedResource) TableName() string {
	return mysqlTableAllocatedResource
}

// MySQL is used for managing mysql, all the resource will be wrap from struct into mysql row before sending.
// Also, the resource get from mysql row will be wrap into struct and return to caller.
type MySQL interface {
	PutAgentResource(ar *AgentResource) error

	ListAllocateResource(opts commonMySQL.ListOptions) ([]*AllocatedResource, int64, error)
	GetAllocatedResource(userID, resourceBatchID string) (*AllocatedResource, error)
	PutAllocatedResource(ar *AllocatedResource) error
}

// MySQLConf define mysql config
type MySQLConf struct {
	MySQLStorage     string
	MySQLDatabase    string
	MySQLUser        string
	MySQLPwd         string
	Charset          string
	MysqlTableOption string
}

// NewMySQL : return mysql object
func NewMySQL(conf MySQLConf) (MySQL, error) {
	if conf.Charset == "" {
		conf.Charset = "utf8"
	}
	if conf.MysqlTableOption == "" {
		conf.MysqlTableOption = "ENGINE=InnoDB  DEFAULT CHARSET=utf8;"
	}

	blog.Info("get a new mysql storage: %s, db: %s, user: %s", conf.MySQLStorage, conf.MySQLDatabase, conf.MySQLUser)
	source := fmt.Sprintf("%s:%s@tcp(%s)/%s?charset=utf8&parseTime=True&loc=Local",
		conf.MySQLUser, conf.MySQLPwd, conf.MySQLStorage, conf.MySQLDatabase)
	db, err := gorm.Open("mysql", source)
	if err != nil {
		blog.Errorf("connect to mysql %s failed: %v", conf.MySQLStorage, err)
		return nil, err
	}

	m := &defaultMysql{
		db: db.Set("gorm:table_options", conf.MysqlTableOption),
	}

	if err = m.ensureTables(&AgentResource{}, &AllocatedResource{}); err != nil {
		blog.Errorf("mysql ensure tables failed: %v", err)
		return nil, err
	}

	return m, nil
}

type defaultMysql struct {
	db *gorm.DB
}

func (m *defaultMysql) ensureTables(tables ...interface{}) error {
	for _, table := range tables {
		if !m.db.HasTable(table) {
			if err := m.db.CreateTable(table).Error; err != nil {
				return err
			}
		}
		if err := m.db.AutoMigrate(table).Error; err != nil {
			return err
		}
	}
	return nil
}

// PutAgentResource save agent resource into db
func (m *defaultMysql) PutAgentResource(ar *AgentResource) error {
	return m.db.Model(&AgentResource{}).Save(ar).Error
}

// ListAllocateResource list allocated resources from db
func (m *defaultMysql) ListAllocateResource(opts commonMySQL.ListOptions) ([]*AllocatedResource, int64, error) {
	var ars []*AllocatedResource
	db := opts.AddWhere(m.db.Model(&AllocatedResource{}))
	db = opts.AddOffsetLimit(db)
	db = opts.AddSelector(db)
	db = opts.AddOrder(db)
	db = db.Find(&ars)

	if err := db.Error; err != nil {
		blog.Errorf("drm: mysql list resource failed opts(%v): %v", opts, err)
		return nil, 0, err
	}

	var length int64
	if err := db.Count(&length).Error; err != nil {
		blog.Errorf("drm: mysql count resource failed opts(%v): %v", opts, err)
		return nil, 0, err
	}

	return ars, length, nil
}

// GetAllocatedResource get allocated resource from db
func (m *defaultMysql) GetAllocatedResource(userID, resourceBatchID string) (*AllocatedResource, error) {
	opts := commonMySQL.NewListOptions()
	opts.Limit(1)
	opts.Equal("user_id", userID)
	opts.Equal("resource_batch_id", resourceBatchID)
	ars, _, err := m.ListAllocateResource(opts)
	if err != nil {
		blog.Errorf("drm: mysql get resource(%s %s) failed: %v", userID, resourceBatchID, err)
		return nil, err
	}

	if len(ars) < 1 {
		err = ErrorResourceNoExist
		return nil, err
	}

	return ars[0], nil
}

// PutAllocatedResource update allocated resource into db
func (m *defaultMysql) PutAllocatedResource(ar *AllocatedResource) error {
	return m.db.Model(&AllocatedResource{}).Save(ar).Error
}
