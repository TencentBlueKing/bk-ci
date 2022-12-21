package mysql

import (
	"database/sql"
	"disaptch-k8s-manager/pkg/config"
	// 初始化mysql driver
	_ "github.com/go-sql-driver/mysql"
	"github.com/pkg/errors"
	"time"
)

var Mysql *sql.DB

func InitMysql() error {
	s, err := sql.Open("mysql", config.Config.Mysql.DataSourceName)
	if err != nil {
		return errors.Wrap(err, "Mysql数据库配置问题")
	}

	s.SetConnMaxLifetime(time.Minute * time.Duration(config.Config.Mysql.ConnMaxLifetime))
	s.SetMaxOpenConns(config.Config.Mysql.MaxOpenConns)
	s.SetMaxIdleConns(config.Config.Mysql.MaxIdleConns)

	if err := s.Ping(); err != nil {
		return errors.Wrap(err, "无法连接Mysql数据库")
	}

	Mysql = s

	return nil
}

func sqlError(err error) error {
	return errors.Wrap(err, "sql错误")
}
