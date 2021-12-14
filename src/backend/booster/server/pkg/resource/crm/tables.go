/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package crm

import (
	"fmt"
	"time"

	"github.com/Tencent/bk-ci/src/booster/common/blog"
	commonMySQL "github.com/Tencent/bk-ci/src/booster/common/mysql"

	"github.com/jinzhu/gorm"
)

// Basic define the basic table columns.
type Basic struct {
	UpdatedAt time.Time `gorm:"column:update_at" json:"-"`
	Disabled  bool      `gorm:"column:disabled;default:false;index" json:"-"`
}

// TableResource describe the db columns of Resource.
// It will inherit the Basic.
type TableResource struct {
	Basic
	ResourceID string `gorm:"column:resource_id;primary_key" json:"resource_id"`
	User       string `gorm:"column:user" json:"user"`
	Param      string `gorm:"column:param" sql:"type:text" json:"param"`

	ResourceBlockKey string `gorm:"column:resource_block_key" json:"resource_block_key"`
	NoReadyInstance  int    `gorm:"column:no_ready_instance" json:"no_ready_instance"`
	RequestInstance  int    `gorm:"column:request_instance" json:"request_instance"`
	Status           int    `gorm:"column:status;index" json:"status"`

	// broker attributes
	BrokerResourceID string `gorm:"column:broker_resource_id;index" json:"broker_resource_id"`
	BrokerName       string `gorm:"column:broker_name;index" json:"broker_name"`
	BrokerSold       bool   `gorm:"column:broker_sold;index" json:"broker_sold"`

	tableName string
}

// TableName specific table name.
func (rt TableResource) TableName() string {
	return rt.tableName
}

// MySQL define the mysql operations to handle resources.
type MySQL interface {
	ListResource(opts commonMySQL.ListOptions) ([]*TableResource, int64, error)
	GetResource(resourceID string) (*TableResource, error)
	CreateResource(tr *TableResource) error
	PutResource(tr *TableResource) error
	DeleteResource(resourceID string) error
}

// MySQLConf describe the mysql connection config need by resource manager.
type MySQLConf struct {
	MySQLStorage     string
	MySQLDatabase    string
	MySQLTable       string
	MySQLUser        string
	MySQLPwd         string
	Charset          string
	MysqlTableOption string
	SkipEnsure       bool
}

// NewMySQL get a new MySQL instance.
func NewMySQL(conf MySQLConf) (MySQL, error) {
	if conf.Charset == "" {
		conf.Charset = "utf8"
	}
	if conf.MysqlTableOption == "" {
		conf.MysqlTableOption = "ENGINE=InnoDB  DEFAULT CHARSET=utf8;"
	}

	blog.Infof("crm: get a new mysql: %s, db: %s, table: %s, user: %s",
		conf.MySQLStorage, conf.MySQLDatabase, conf.MySQLTable, conf.MySQLUser)
	source := fmt.Sprintf("%s:%s@tcp(%s)/%s?charset=%s&parseTime=True&loc=Local",
		conf.MySQLUser, conf.MySQLPwd, conf.MySQLStorage, conf.MySQLDatabase, conf.Charset)
	db, err := gorm.Open("mysql", source)
	if err != nil {
		blog.Errorf("crm: connect to mysql %s failed: %v", conf.MySQLStorage, err)
		return nil, err
	}

	m := &mysql{
		db:   db.Set("gorm:table_options", conf.MysqlTableOption),
		conf: conf,
	}

	if conf.SkipEnsure {
		return m, nil
	}

	if err = m.ensureTables(&TableResource{tableName: conf.MySQLTable}); err != nil {
		blog.Errorf("crm: mysql ensure tables failed: %v", err)
		return nil, err
	}

	return m, nil
}

type mysql struct {
	db *gorm.DB

	conf MySQLConf
}

// ensureTables makes sure that the tables exist in database and runs migrations. In G-ORM, the migration
// will be save and the columns deleting will not be executed.
func (m *mysql) ensureTables(tables ...interface{}) error {
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

// ListResource list resource from db, return list and total num.
// list cut by offset and limit, but total num describe the true num.
func (m *mysql) ListResource(opts commonMySQL.ListOptions) ([]*TableResource, int64, error) {
	var rl []*TableResource
	db := opts.AddWhere(m.db.Model(&TableResource{}).Table(m.conf.MySQLTable))
	db = opts.AddOffsetLimit(db)
	db = opts.AddSelector(db)
	db = opts.AddOrder(db)
	db = db.Where("disabled = ?", false).Find(&rl)

	if err := db.Error; err != nil {
		blog.Errorf("crm: mysql list resource failed opts(%v): %v", opts, err)
		return nil, 0, err
	}

	var length int64
	if err := db.Count(&length).Error; err != nil {
		blog.Errorf("crm: mysql count resource failed opts(%v): %v", opts, err)
		return nil, 0, err
	}

	return rl, length, nil
}

// GetResource get resource.
func (m *mysql) GetResource(resourceID string) (*TableResource, error) {
	opts := commonMySQL.NewListOptions()
	opts.Limit(1)
	opts.Equal("resource_id", resourceID)
	rl, _, err := m.ListResource(opts)
	if err != nil {
		blog.Errorf("crm: mysql get resource(%s) failed: %v", resourceID, err)
		return nil, err
	}

	if len(rl) < 1 {
		err = ErrorResourceNoExist
		return nil, err
	}

	return rl[0], nil
}

// CreateResource create a new resource into database.
func (m *mysql) CreateResource(tr *TableResource) error {
	blog.Infof("crm: try to create resource(%s)", tr.ResourceID)
	tr.tableName = m.conf.MySQLTable
	if err := m.db.Model(&TableResource{}).Table(m.conf.MySQLTable).Create(tr).Error; err != nil {
		blog.Errorf("crm: mysql create resource(%s) failed: %v", tr.ResourceID, err)
		return err
	}
	blog.Infof("crm: success to create resource(%s)", tr.ResourceID)
	return nil
}

// PutResource update a existing resource with full fields into database.
func (m *mysql) PutResource(tr *TableResource) error {
	blog.Infof("crm: try to put resource(%s)", tr.ResourceID)
	tr.tableName = m.conf.MySQLTable
	if err := m.db.Model(&TableResource{}).Table(m.conf.MySQLTable).Save(tr).Error; err != nil {
		blog.Errorf("crm: mysql put resource(%s) failed: %v", tr.ResourceID, err)
		return err
	}
	blog.Infof("crm: success to put resource(%s)", tr.ResourceID)
	return nil
}

// DeleteResource delete resource from db. Just set the disabled to true instead of real deletion.
func (m *mysql) DeleteResource(resourceID string) error {
	blog.Infof("crm: try to delete resource(%s)", resourceID)
	if err := m.db.
		Model(&TableResource{}).Table(m.conf.MySQLTable).
		Where("resource_id = ?", resourceID).
		Update("disabled", true).Error; err != nil {

		blog.Errorf("crm: mysql delete resource(%s) failed: %v", resourceID, err)
		return err
	}
	blog.Infof("crm: success to delete resource(%s)", resourceID)
	return nil
}
