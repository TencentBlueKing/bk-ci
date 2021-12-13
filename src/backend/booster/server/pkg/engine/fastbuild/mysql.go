/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package fastbuild

import (
	"fmt"

	"github.com/Tencent/bk-ci/src/booster/common/blog"
	commonMySQL "github.com/Tencent/bk-ci/src/booster/common/mysql"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/engine"
	selfMetric "github.com/Tencent/bk-ci/src/booster/server/pkg/metric"

	"github.com/jinzhu/gorm"
)

// MySQL describe the full operations to mysql databases need by engine.
type MySQL interface {
	// get db operator
	GetDB() *gorm.DB

	ListTask(opts commonMySQL.ListOptions) ([]*TableTask, int64, error)
	GetTask(taskID string) (*TableTask, error)
	PutTask(task *TableTask) error
	UpdateTask(taskID string, task map[string]interface{}) error
	DeleteTask(taskID string) error

	ListProject(opts commonMySQL.ListOptions) ([]*CombinedProject, int64, error)

	ListProjectInfo(opts commonMySQL.ListOptions) ([]*TableProjectInfo, int64, error)
	GetProjectInfo(projectID string) (*TableProjectInfo, error)
	PutProjectInfo(projectInfo *TableProjectInfo) error
	UpdateProjectInfo(projectID string, projectInfo map[string]interface{}) error
	DeleteProjectInfo(projectID string) error
	AddProjectInfoStats(projectID string, delta DeltaInfoStats) error

	ListProjectSetting(opts commonMySQL.ListOptions) ([]*TableProjectSetting, int64, error)
	GetProjectSetting(projectID string) (*TableProjectSetting, error)
	PutProjectSetting(projectSetting *TableProjectSetting) error
	UpdateProjectSetting(projectID string, projectSetting map[string]interface{}) error
	DeleteProjectSetting(projectID string) error
	CreateOrUpdateProjectSetting(projectSetting *TableProjectSetting, projectSettingRaw map[string]interface{}) error

	ListWhitelist(opts commonMySQL.ListOptions) ([]*TableWhitelist, int64, error)
	GetWhitelist(key engine.WhiteListKey) (*TableWhitelist, error)
	PutWhitelist(wll []*TableWhitelist) error
	UpdateWhitelist(key engine.WhiteListKey, wll []map[string]interface{}) error
	DeleteWhitelist(keys []*engine.WhiteListKey) error

	ListClusterSetting(opts commonMySQL.ListOptions) ([]*TableClusterSetting, int64, error)
	GetClusterSetting(clusterID string) (*TableClusterSetting, error)
	PutClusterSetting(clusterSetting *TableClusterSetting) error
	UpdateClusterSetting(clusterID string, clusterSetting map[string]interface{}) error
	DeleteClusterSetting(clusterID string) error

	PutSubTask(task *TableSubTask) error
	ListSubTask(opts commonMySQL.ListOptions) ([]*TableSubTask, int64, error)
	GetSubTaskSummary(taskID string) (*FbSummary, error)

	ListAllSubTask(opts commonMySQL.ListOptions) ([]*TableSubTask, int64, error)
}

var (
	ErrorClusterNoFound = fmt.Errorf("cluster no found")
)

// get new mysql instance with connected orm operator.
func NewMySQL(conf engine.MySQLConf) (MySQL, error) {
	if conf.Charset == "" {
		conf.Charset = "utf8"
	}
	if conf.MysqlTableOption == "" {
		conf.MysqlTableOption = "ENGINE=InnoDB  DEFAULT CHARSET=utf8;"
	}

	blog.Info("get a new engine(%s) mysql: %s, db: %s, user: %s",
		EngineName, conf.MySQLStorage, conf.MySQLDatabase, conf.MySQLUser)
	source := fmt.Sprintf("%s:%s@tcp(%s)/%s?charset=%s&parseTime=True&loc=Local",
		conf.MySQLUser, conf.MySQLPwd, conf.MySQLStorage, conf.MySQLDatabase, conf.Charset)
	db, err := gorm.Open("mysql", source)
	if err != nil {
		blog.Errorf("engine(%s) connect to mysql %s failed: %v", EngineName, conf.MySQLStorage, err)
		return nil, err
	}

	if conf.MySQLDebug {
		db = db.Debug()
	}

	m := &mysql{
		db: db.Set("gorm:table_options", conf.MysqlTableOption),
	}
	if err = m.ensureTables(&TableTask{}, &TableSubTask{}, &TableProjectSetting{},
		&TableProjectInfo{}, &TableWhitelist{}, &TableClusterSetting{}); err != nil {
		blog.Errorf("engine(%s) mysql ensure tables failed: %v", EngineName, err)
		return nil, err
	}

	return m, nil
}

type mysql struct {
	db *gorm.DB
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

// get db operator.
func (m *mysql) GetDB() *gorm.DB {
	return m.db
}

// list task from db, return list and total num.
// list cut by offset and limit, but total num describe the true num.
func (m *mysql) ListTask(opts commonMySQL.ListOptions) ([]*TableTask, int64, error) {
	defer timeMetricRecord("list_task")()

	var tl []*TableTask
	db := opts.AddWhere(m.db.Model(&TableTask{})).Where("disabled = ?", false)

	var length int64
	if err := db.Count(&length).Error; err != nil {
		blog.Errorf("engine(%s) mysql count task failed opts(%v): %v", EngineName, opts, err)
		return nil, 0, err
	}

	db = opts.AddOffsetLimit(db)
	db = opts.AddSelector(db)
	db = opts.AddOrder(db)

	if err := db.Find(&tl).Error; err != nil {
		blog.Errorf("engine(%s) mysql list task failed opts(%v): %v", EngineName, opts, err)
		return nil, 0, err
	}

	return tl, length, nil
}

// get task.
func (m *mysql) GetTask(taskID string) (*TableTask, error) {
	defer timeMetricRecord("get_task")()

	opts := commonMySQL.NewListOptions()
	opts.Limit(1)
	opts.Equal("task_id", taskID)
	/*
		tl, _, err := m.ListTask(opts)
		if err != nil {
			blog.Errorf("engine(%s) mysql get task(%s) failed: %v", EngineName, taskID, err)
			return nil, err
		}
	*/
	var tl []*TableTask
	db := opts.AddWhere(m.db.Model(&TableTask{}))
	db = opts.AddOffsetLimit(db)
	db = opts.AddSelector(db)
	db = opts.AddOrder(db)
	db = db.Where("disabled = ?", false).Find(&tl)

	if err := db.Error; err != nil {
		blog.Errorf("engine(%s) mysql get task failed opts(%v): %v", EngineName, opts, err)
		return nil, err
	}

	if len(tl) < 1 {
		err := engine.ErrorTaskNoFound
		return nil, err
	}

	return tl[0], nil
}

// put task with full fields.
func (m *mysql) PutTask(task *TableTask) error {
	defer timeMetricRecord("put_task")()

	if err := m.db.Model(&TableTask{}).Save(task).Error; err != nil {
		blog.Errorf("engine(%s) mysql put task(%s) failed: %v", EngineName, task.TaskID, err)
		return err
	}
	return nil
}

// update task with given fields.
func (m *mysql) UpdateTask(taskID string, task map[string]interface{}) error {
	defer timeMetricRecord("update_task")()

	task["disabled"] = false

	if err := m.db.Model(&TableTask{}).Where("task_id = ?", taskID).Updates(task).Error; err != nil {
		blog.Errorf("engine(%s) mysql update task(%s)(%+v) failed: %v", EngineName, taskID, task, err)
		return err
	}

	return nil
}

// delete task from db. Just set the disabled to true instead of real deletion.
func (m *mysql) DeleteTask(taskID string) error {
	defer timeMetricRecord("delete_task")()

	if err := m.db.Model(&TableTask{}).Where("task_id = ?", taskID).
		Update("disabled", true).Error; err != nil {
		blog.Errorf("engine(%s) mysql delete task(%s) failed: %v", EngineName, taskID, err)
		return err
	}
	return nil
}

// ListProject join the two tables "project_settings" and "project_records".
// List all project_settings, then list project_records with those project_ids, then combine them together
// and return.
// TODO: consider to use mysql "join" command to do this
func (m *mysql) ListProject(opts commonMySQL.ListOptions) ([]*CombinedProject, int64, error) {
	defer timeMetricRecord("list_project")()

	var settingProjectList []*TableProjectSetting
	db := opts.AddWhere(m.db.Model(&TableProjectSetting{})).Where("disabled = ?", false)

	var length int64
	if err := db.Count(&length).Error; err != nil {
		blog.Errorf("engine(%s) mysql count project setting failed opts(%v): %v", EngineName, opts, err)
		return nil, 0, err
	}

	db = opts.AddOffsetLimit(db)
	db = opts.AddSelector(db)
	db = opts.AddOrder(db)

	if err := db.Find(&settingProjectList).Error; err != nil {
		blog.Errorf("engine(%s) mysql list project setting failed opts(%v): %v", EngineName, opts, err)
		return nil, 0, err
	}

	combinedProjectList := make([]*CombinedProject, 0, 1000)
	if len(settingProjectList) == 0 {
		return combinedProjectList, 0, nil
	}

	projectIDList := make([]string, 0, 1000)
	for _, settingProject := range settingProjectList {
		projectIDList = append(projectIDList, settingProject.ProjectID)
	}

	var recordProjectList []*TableProjectInfo
	secondOpts := commonMySQL.NewListOptions()
	secondOpts.In("project_id", projectIDList)
	if err := secondOpts.AddWhere(m.db.Model(&TableProjectInfo{})).Find(&recordProjectList).Error; err != nil {
		blog.Errorf("engine(%s) mysql list project info failed opts(%v): %v", EngineName, opts, err)
		return nil, 0, err
	}

	recordProjectMap := make(map[string]*TableProjectInfo, 1000)
	for _, recordProject := range recordProjectList {
		recordProjectMap[recordProject.ProjectID] = recordProject
	}

	for _, settingProject := range settingProjectList {

		combinedProjectList = append(combinedProjectList, &CombinedProject{
			TableProjectSetting: settingProject,
			TableProjectInfo:    recordProjectMap[settingProject.ProjectID],
		})

	}

	return combinedProjectList, length, nil
}

// list project info from db, return list and total num.
// list cut by offset and limit, but total num describe the true num.
func (m *mysql) ListProjectInfo(opts commonMySQL.ListOptions) ([]*TableProjectInfo, int64, error) {
	defer timeMetricRecord("list_project_info")()

	var pl []*TableProjectInfo
	db := opts.AddWhere(m.db.Model(&TableProjectInfo{})).Where("disabled = ?", false)

	var length int64
	if err := db.Count(&length).Error; err != nil {
		blog.Errorf("engine(%s) mysql count project info failed opts(%v): %v", EngineName, opts, err)
		return nil, 0, err
	}

	db = opts.AddOffsetLimit(db)
	db = opts.AddSelector(db)
	db = opts.AddOrder(db)

	if err := db.Find(&pl).Error; err != nil {
		blog.Errorf("engine(%s) mysql list project info failed opts(%v): %v", EngineName, opts, err)
		return nil, 0, err
	}

	return pl, length, nil
}

// get project info.
func (m *mysql) GetProjectInfo(projectID string) (*TableProjectInfo, error) {
	defer timeMetricRecord("get_project_info")()

	opts := commonMySQL.NewListOptions()
	opts.Limit(1)
	opts.Equal("project_id", projectID)
	/*
		pl, _, err := m.ListProjectInfo(opts)
		if err != nil {
			blog.Errorf("engine(%s) mysql get project info(%s) failed: %v", EngineName, projectID, err)
			return nil, err
		}
	*/
	var pl []*TableProjectInfo
	db := opts.AddWhere(m.db.Model(&TableProjectInfo{}))
	db = opts.AddOffsetLimit(db)
	db = opts.AddSelector(db)
	db = opts.AddOrder(db)
	db = db.Where("disabled = ?", false).Find(&pl)

	if err := db.Error; err != nil {
		blog.Errorf("engine(%s) mysql list project info failed opts(%v): %v", EngineName, opts, err)
		return nil, err
	}

	if len(pl) < 1 {
		err := engine.ErrorProjectNoFound
		blog.Errorf("engine(%s) mysql get project info(%s) failed: %v", EngineName, projectID, err)
		return nil, err
	}

	return pl[0], nil
}

// put project info with full fields.
func (m *mysql) PutProjectInfo(projectInfo *TableProjectInfo) error {
	defer timeMetricRecord("put_project_info")()

	if err := m.db.Model(&TableProjectInfo{}).Save(projectInfo).Error; err != nil {
		blog.Errorf("engine(%s) mysql put project info(%s) failed: %v", EngineName, projectInfo.ProjectID, err)
		return err
	}
	return nil
}

// update project info with given fields.
func (m *mysql) UpdateProjectInfo(projectID string, projectInfo map[string]interface{}) error {
	defer timeMetricRecord("update_project_info")()

	projectInfo["disabled"] = false

	if err := m.db.Model(&TableProjectInfo{}).Where("project_id = ?", projectID).
		Updates(projectInfo).Error; err != nil {
		blog.Errorf("engine(%s) mysql update project(%s) info(%+v) failed: %v", EngineName, projectID, projectInfo, err)
		return err
	}

	return nil
}

// delete project info from db. Just set the disabled to true instead of real deletion.
func (m *mysql) DeleteProjectInfo(projectID string) error {
	defer timeMetricRecord("delete_project_info")()

	if err := m.db.Model(&TableProjectInfo{}).Where("project_id = ?", projectID).
		Update("disabled", true).Error; err != nil {
		blog.Errorf("engine(%s) mysql delete project info(%s) failed: %v", EngineName, projectID, err)
		return err
	}
	return nil
}

// add project info stats with given delta data, will lock the row and update some data.
func (m *mysql) AddProjectInfoStats(projectID string, delta DeltaInfoStats) error {
	defer timeMetricRecord("add_project_info_stats")()

	tx := m.db.Begin()

	var pi TableProjectInfo
	pi.ProjectID = projectID
	if err := tx.Set("gorm:query_option", "FOR UPDATE").FirstOrCreate(&pi).Error; err != nil {
		tx.Rollback()
		blog.Errorf("engine(%s) mysql add project(%s) info stats, first or create failed: %v",
			EngineName, projectID, err)
		return err
	}

	pi.ServiceUnits += delta.ServiceUnits

	if err := tx.Save(&pi).Error; err != nil {
		tx.Rollback()
		blog.Errorf("engine(%s) mysql add project(%s) info stats, save failed: %v", EngineName, projectID, err)
		return err
	}

	tx.Commit()
	return nil
}

// DeltaInfoStats describe the project info delta data.
type DeltaInfoStats struct {
	ServiceUnits float64
}

// list project setting from db, return list and total num.
// list cut by offset and limit, but total num describe the true num.
func (m *mysql) ListProjectSetting(opts commonMySQL.ListOptions) ([]*TableProjectSetting, int64, error) {
	defer timeMetricRecord("list_project_setting")()

	var pl []*TableProjectSetting
	db := opts.AddWhere(m.db.Model(&TableProjectSetting{})).Where("disabled = ?", false)

	var length int64
	if err := db.Count(&length).Error; err != nil {
		blog.Errorf("engine(%s) mysql count project setting failed opts(%v): %v", EngineName, opts, err)
		return nil, 0, err
	}

	db = opts.AddOffsetLimit(db)
	db = opts.AddSelector(db)
	db = opts.AddOrder(db)

	if err := db.Find(&pl).Error; err != nil {
		blog.Errorf("engine(%s) mysql list project setting failed opts(%v): %v", EngineName, opts, err)
		return nil, 0, err
	}

	return pl, length, nil
}

// get project setting.
func (m *mysql) GetProjectSetting(projectID string) (*TableProjectSetting, error) {
	defer timeMetricRecord("get_project_setting")()

	opts := commonMySQL.NewListOptions()
	opts.Limit(1)
	opts.Equal("project_id", projectID)
	/*
		pl, _, err := m.ListProjectSetting(opts)
		if err != nil {
			blog.Errorf("engine(%s) mysql get project setting(%s) failed: %v", EngineName, projectID, err)
			return nil, err
		}
	*/
	var pl []*TableProjectSetting
	db := opts.AddWhere(m.db.Model(&TableProjectSetting{}))
	db = opts.AddOffsetLimit(db)
	db = opts.AddSelector(db)
	db = opts.AddOrder(db)
	db = db.Where("disabled = ?", false).Find(&pl)

	if err := db.Error; err != nil {
		blog.Errorf("engine(%s) mysql list project setting failed opts(%v): %v", EngineName, opts, err)
		return nil, err
	}

	if len(pl) < 1 {
		err := engine.ErrorProjectNoFound
		blog.Errorf("engine(%s) mysql get project setting(%s) failed: %v", EngineName, projectID, err)
		return nil, err
	}

	return pl[0], nil
}

// put project setting with full fields.
func (m *mysql) PutProjectSetting(projectSetting *TableProjectSetting) error {
	defer timeMetricRecord("put_project_setting")()

	if err := m.db.Model(&TableProjectSetting{}).Save(projectSetting).Error; err != nil {
		blog.Errorf("engine(%s) mysql put project setting(%s) failed: %v",
			EngineName, projectSetting.ProjectID, err)
		return err
	}
	return nil
}

// update project setting with given fields.
func (m *mysql) UpdateProjectSetting(projectID string, projectSetting map[string]interface{}) error {
	defer timeMetricRecord("update_project_setting")()

	projectSetting["disabled"] = false

	if err := m.db.Model(&TableProjectSetting{}).Where("project_id = ?", projectID).
		Updates(projectSetting).Error; err != nil {
		blog.Errorf("engine(%s) mysql update project(%s) setting(%+v) failed: %v", EngineName,
			projectID, projectSetting, err)
		return err
	}

	return nil
}

// delete project setting from db. Just set the disabled to true instead of real deletion.
func (m *mysql) DeleteProjectSetting(projectID string) error {
	defer timeMetricRecord("delete_project_setting")()

	if err := m.db.Model(&TableProjectSetting{}).Where("project_id = ?", projectID).
		Update("disabled", true).Error; err != nil {
		blog.Errorf("engine(%s) mysql delete project setting(%s) failed: %v",
			EngineName, projectID, err)
		return err
	}
	return nil
}

// create a new project with struct or update a exist project setting with given fields.
func (m *mysql) CreateOrUpdateProjectSetting(
	projectSetting *TableProjectSetting,
	projectSettingRaw map[string]interface{}) error {
	defer timeMetricRecord("create_or_update_project_setting")()

	projectID, _ := projectSettingRaw["project_id"]
	projectSettingRaw["disabled"] = false

	if projectSetting != nil {
		if err := m.db.Table(TableProjectSetting{}.TableName()).Create(projectSetting).Error; err == nil {
			return nil
		}
	}

	if err := m.db.Model(&TableProjectSetting{}).Updates(projectSettingRaw).Error; err != nil {
		blog.Errorf("engine(%s) mysql create or update project setting failed ID(%s): %v", EngineName, projectID, err)
		return err
	}

	return nil
}

// list whitelist from db, return list and total num.
// list cut by offset and limit, but total num describe the true num.
func (m *mysql) ListWhitelist(opts commonMySQL.ListOptions) ([]*TableWhitelist, int64, error) {
	defer timeMetricRecord("list_whitelist")()

	var wll []*TableWhitelist
	db := opts.AddWhere(m.db.Model(&TableWhitelist{})).Where("disabled = ?", false)

	var length int64
	if err := db.Count(&length).Error; err != nil {
		blog.Errorf("engine(%s) count whitelist failed opts(%v): %v", EngineName, opts, err)
		return nil, 0, err
	}

	db = opts.AddOffsetLimit(db)
	db = opts.AddSelector(db)
	db = opts.AddOrder(db)

	if err := db.Find(&wll).Error; err != nil {
		blog.Errorf("engine(%s) list whitelist failed opts(%v): %v", EngineName, opts, err)
		return nil, 0, err
	}

	return wll, length, nil
}

// get whitelist.
func (m *mysql) GetWhitelist(key engine.WhiteListKey) (*TableWhitelist, error) {
	defer timeMetricRecord("get_whitelist")()

	opts := commonMySQL.NewListOptions()
	opts.Limit(-1)
	opts.Equal("project_id", key.ProjectID)
	opts.Equal("ip", key.IP)
	/*
		wll, _, err := m.ListWhitelist(opts)
		if err != nil {
			blog.Errorf("engine(%s) mysql get whitelist(%+v) failed: %v", EngineName, key, err)
			return nil, err
		}
	*/
	var wll []*TableWhitelist
	db := opts.AddWhere(m.db.Model(&TableWhitelist{}))
	db = opts.AddOffsetLimit(db)
	db = opts.AddSelector(db)
	db = opts.AddOrder(db)
	db = db.Where("disabled = ?", false).Find(&wll)

	if err := db.Error; err != nil {
		blog.Errorf("engine(%s) list whitelist failed opts(%v): %v", EngineName, opts, err)
		return nil, err
	}

	if len(wll) < 1 {
		err := engine.ErrorWhitelistNoFound
		blog.Errorf("engine(%s) mysql get whitelist(%+v) failed: %v", EngineName, key, err)
		return nil, err
	}

	return wll[0], nil
}

// put whitelist with full fields.
func (m *mysql) PutWhitelist(wll []*TableWhitelist) error {
	defer timeMetricRecord("put_whitelist")()

	tx := m.db.Begin()
	for _, wl := range wll {
		if err := tx.Model(&TableWhitelist{}).Save(wl).Error; err != nil {
			blog.Errorf("engine(%s) mysql put whitelist(%+v) failed: %v", EngineName, wl, err)
			tx.Rollback()
			return err
		}
	}
	tx.Commit()
	return nil
}

// update whitelist with given fields.
func (m *mysql) UpdateWhitelist(key engine.WhiteListKey, wll []map[string]interface{}) error {
	defer timeMetricRecord("update_whitelist")()

	tx := m.db.Begin()
	for _, wl := range wll {
		wl["disabled"] = false
		if err := tx.Model(&TableWhitelist{}).Where("project_id = ?", key.ProjectID).
			Where("ip = ?", key.IP).Updates(wl).Error; err != nil {
			blog.Errorf("engine(%s) mysql update whitelist(%+v) failed: %v", EngineName, wl, err)
			tx.Rollback()
			return err
		}
	}
	tx.Commit()
	return nil
}

// delete whitelist from db. Just set the disabled to true instead of real deletion.
func (m *mysql) DeleteWhitelist(keys []*engine.WhiteListKey) error {
	defer timeMetricRecord("delete_whitelist")()

	tx := m.db.Begin()
	for _, key := range keys {
		if err := tx.Model(&TableWhitelist{}).Where("ip = ?", key.IP).
			Where("project_id = ?", key.ProjectID).Update("disabled", true).Error; err != nil {
			blog.Errorf("engine(%s) mysql delete whitelist failed IP(%s) projectID(%s): %v",
				EngineName, key.IP, key.ProjectID, err)
			tx.Rollback()
			return err
		}
	}
	tx.Commit()
	return nil
}

// list cluster setting from db, return list and total num.
// list cut by offset and limit, but total num describe the true num.
func (m *mysql) ListClusterSetting(opts commonMySQL.ListOptions) ([]*TableClusterSetting, int64, error) {
	defer timeMetricRecord("list_cluster_setting")()

	var pl []*TableClusterSetting
	db := opts.AddWhere(m.db.Model(&TableClusterSetting{})).Where("disabled = ?", false)

	var length int64
	if err := db.Count(&length).Error; err != nil {
		blog.Errorf("engine(%s) mysql count cluster setting failed opts(%v): %v", EngineName, opts, err)
		return nil, 0, err
	}

	db = opts.AddOffsetLimit(db)
	db = opts.AddSelector(db)
	db = opts.AddOrder(db)

	if err := db.Find(&pl).Error; err != nil {
		blog.Errorf("engine(%s) mysql list cluster setting failed opts(%v): %v", EngineName, opts, err)
		return nil, 0, err
	}

	return pl, length, nil
}

// get cluster setting.
func (m *mysql) GetClusterSetting(clusterID string) (*TableClusterSetting, error) {
	defer timeMetricRecord("get_cluster_setting")()

	opts := commonMySQL.NewListOptions()
	opts.Limit(1)
	opts.Equal("cluster", clusterID)
	/*
		pl, _, err := m.ListClusterSetting(opts)
		if err != nil {
			blog.Errorf("engine(%s) mysql get cluster setting(%s) failed: %v", EngineName, clusterID, err)
			return nil, err
		}
	*/
	var pl []*TableClusterSetting
	db := opts.AddWhere(m.db.Model(&TableClusterSetting{}))
	db = opts.AddOffsetLimit(db)
	db = opts.AddSelector(db)
	db = opts.AddOrder(db)
	db = db.Where("disabled = ?", false).Find(&pl)

	if err := db.Error; err != nil {
		blog.Errorf("engine(%s) mysql list cluster setting failed opts(%v): %v", EngineName, opts, err)
		return nil, err
	}

	if len(pl) < 1 {
		err := ErrorClusterNoFound
		blog.Errorf("engine(%s) mysql get cluster setting(%s) failed: %v", EngineName, clusterID, err)
		return nil, err
	}

	return pl[0], nil
}

// put cluster setting with full fields.
func (m *mysql) PutClusterSetting(clusterSetting *TableClusterSetting) error {
	defer timeMetricRecord("put_cluster_setting")()

	if err := m.db.Model(&TableClusterSetting{}).Save(clusterSetting).Error; err != nil {
		blog.Errorf("engine(%s) mysql put cluster setting(%s) failed: %v",
			EngineName, clusterSetting.Cluster, err)
		return err
	}
	return nil
}

// update cluster setting with given fields.
func (m *mysql) UpdateClusterSetting(clusterID string, clusterSetting map[string]interface{}) error {
	defer timeMetricRecord("update_cluster_setting")()

	clusterSetting["disabled"] = false

	if err := m.db.Model(&TableClusterSetting{}).Where("cluster = ?", clusterID).
		Updates(clusterSetting).Error; err != nil {
		blog.Errorf("engine(%s) mysql update cluster(%s) setting(%+v) failed: %v",
			EngineName, clusterID, clusterSetting, err)
		return err
	}

	return nil
}

// delete cluster setting from db. Just set the disabled to true instead of real deletion.
func (m *mysql) DeleteClusterSetting(clusterID string) error {
	defer timeMetricRecord("delete_cluster_setting")()

	if err := m.db.Model(&TableClusterSetting{}).Where("cluster = ?", clusterID).
		Update("disabled", true).Error; err != nil {
		blog.Errorf("engine(%s) mysql delete cluster setting(%s) failed: %v", EngineName, clusterID, err)
		return err
	}
	return nil
}

// put sub task with full fields.
func (m *mysql) PutSubTask(task *TableSubTask) error {
	defer timeMetricRecord("put_sub_task")()

	if err := m.db.Model(&TableSubTask{}).Save(task).Error; err != nil {
		blog.Errorf("engine(%s) mysql put sub task(%s) failed: %v", EngineName, task.TaskID, err)
		return err
	}
	return nil
}

// get sub task summary.
func (m *mysql) GetSubTaskSummary(taskID string) (*FbSummary, error) {
	defer timeMetricRecord("get_sub_task_summary")()

	var libraryBuilt, libraryCacheHit, libraryCPUTime = 0, 0, 0.0
	var objectBuilt, objectCacheHit, objectCPUTime = 0, 0, 0.0
	var exeBuilt, exeCacheHit, exeCPUTime = 0, 0, 0.0
	var cacheHits, cacheMisses, cacheStores = 0, 0, 0
	var realCompileTime, localCompileTime, remoteCompileTime = 0.0, 0.0, 0.0

	sql := fmt.Sprintf(`select sum(library_built),sum(library_cache_hit),sum(library_cpu_time),
sum(object_built),sum(object_cache_hit),sum(object_cpu_time),
sum(exe_built),sum(exe_cache_hit),sum(exe_cpu_time),
sum(cache_hits),sum(cache_misses),sum(cache_stores),
sum(real_compile_time),sum(local_compile_time),sum(remote_compile_time) from %s
where task_id='%s'`, mysqlTableSubTask, taskID)
	blog.Info("ready to query with sql[%s]", sql)
	rowsselect, err := m.db.Raw(sql).Rows()
	if err == nil && rowsselect != nil {
		defer func() {
			_ = rowsselect.Close()
		}()
	}
	if err != nil {
		blog.Errorf("failed to exec sql[%s],error[%v]", sql, err)
		return nil, err
	}

	for rowsselect.Next() {
		_ = rowsselect.Scan(&libraryBuilt, &libraryCacheHit, &libraryCPUTime,
			&objectBuilt, &objectCacheHit, &objectCPUTime,
			&exeBuilt, &exeCacheHit, &exeCPUTime,
			&cacheHits, &cacheMisses, &cacheStores,
			&realCompileTime, &localCompileTime, &remoteCompileTime)
		break
	}

	FbSummary := FbSummary{
		LibraryBuilt:    libraryBuilt,
		LibraryCacheHit: libraryCacheHit,
		LibraryCPUTime:  (float32)(libraryCPUTime),

		ObjectBuilt:    objectBuilt,
		ObjectCacheHit: objectCacheHit,
		ObjectCPUTime:  (float32)(objectCPUTime),

		ExeBuilt:    exeBuilt,
		ExeCacheHit: exeCacheHit,
		ExeCPUTime:  (float32)(exeCPUTime),

		CacheHits:   cacheHits,
		CacheMisses: cacheMisses,
		CacheStores: cacheStores,

		RealCompileTime:   (float32)(realCompileTime),
		LocalCompileTime:  (float32)(localCompileTime),
		RemoteCompileTime: (float32)(remoteCompileTime),
	}

	return &FbSummary, nil
}

// list sub task from db, return list and total num.
// list cut by offset and limit, but total num describe the true num.
func (m *mysql) ListSubTask(opts commonMySQL.ListOptions) ([]*TableSubTask, int64, error) {
	defer timeMetricRecord("list_sub_task")()

	var tl []*TableSubTask
	db := opts.AddWhere(m.db.Model(&TableSubTask{}))

	var length int64
	if err := db.Count(&length).Error; err != nil {
		blog.Errorf("engine(%s) mysql count task failed opts(%v): %v", EngineName, opts, err)
		return nil, 0, err
	}

	db = opts.AddOffsetLimit(db)
	db = opts.AddSelector(db)
	db = opts.AddOrder(db)

	if err := db.Find(&tl).Error; err != nil {
		blog.Errorf("engine(%s) mysql list task failed opts(%v): %v", EngineName, opts, err)
		return nil, 0, err
	}

	return tl, length, nil
}

// list all sub task.
func (m *mysql) ListAllSubTask(opts commonMySQL.ListOptions) ([]*TableSubTask, int64, error) {
	defer timeMetricRecord("list_all_sub_task")()

	var tl []*TableSubTask
	db := opts.AddWhere(m.db.Model(&TableSubTask{}))
	opts.Limit(-1)
	db = opts.AddOffsetLimit(db)
	db = opts.AddSelector(db)
	db = opts.AddOrder(db)
	db = db.Find(&tl)

	if err := db.Error; err != nil {
		blog.Errorf("engine(%s) mysql list task failed opts(%v): %v", EngineName, opts, err)
		return nil, 0, err
	}

	return tl, int64(len(tl)), nil
}

// generate project_settings and project_records
type CombinedProject struct {
	*TableProjectSetting
	*TableProjectInfo
}

func timeMetricRecord(operation string) func() {
	return selfMetric.TimeMetricRecord(fmt.Sprintf("%s_%s", EngineName, operation))
}
