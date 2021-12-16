package store

import (
	"github.com/Tencent/bk-ci/src/booster/common/encrypt"
	commonMySQL "github.com/Tencent/bk-ci/src/booster/common/mysql"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/engine"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/engine/distcc"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/engine/distcc/controller/config"

	// 启用mysql driver
	_ "github.com/jinzhu/gorm/dialects/mysql"
)

type RecordTask = distcc.TableTask
type RecordProject = distcc.TableProjectInfo
type SettingProject = distcc.TableProjectSetting
type SettingWhiteList = distcc.TableWhitelist
type SettingGcc = distcc.TableGcc

type Ops = distcc.MySQL
type ListOptions = commonMySQL.ListOptions
type WhiteListKey = engine.WhiteListKey
type CombinedProject = distcc.CombinedProject

type StatsInfo = ds.StatsInfo

const (
	WhiteListAllProjectID = engine.WhiteListAllProjectID
)

var (
	NewListOptions = commonMySQL.NewListOptions
)

// NewOps get a new ops instance
func NewOps(conf *config.DistCCControllerConfig) (Ops, error) {
	pwd, err := encrypt.DesDecryptFromBase([]byte(conf.MySQLPwd))
	if err != nil {
		return nil, err
	}

	return distcc.NewMySQL(engine.MySQLConf{
		MySQLStorage:  conf.MySQLStorage,
		MySQLDatabase: conf.MySQLDatabase,
		MySQLUser:     conf.MySQLUser,
		MySQLPwd:      string(pwd),
	})
}
