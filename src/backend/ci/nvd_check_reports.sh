#!/bin/bash

# 扫描工程依赖的第三方开源组件被披露的NVD漏洞报告,
# 生成后存放于dependency-check-reports目录,并按服务模块命名子目录存放各个报告, 重点可看process模块
# 大部分NVD漏洞都不会对本项目造成任何的影响,根据需要去修复即可,不强求清0所有漏洞.
#need run in src/backend/ci dir

./gradlew dependencyCheckAnalyze

r_path=dependency-check-reports
mkdir -p $r_path


loop_mv_reports() {
    service_name=$1
    dir=$2
    if [[ -d $dir ]];
        then
          # shellcheck disable=SC2231
          for file in $dir/*;
          do
            loop_mv_reports "$service_name" "$file"
          done
    elif [[ -f $dir ]] && [[ "${dir##*/}"x = "dependency-check-report.html"x ]] ;
    then
      echo "start mv $dir to $r_path/$service_name "
      mkdir -p "$r_path/$service_name"
      mv "$dir" "$r_path/$service_name/index.html"
    fi
}

cd core || exit 2

for sub_dir in ./* ;
do
    service_name=${sub_dir##*/}
    echo "Get $service_name nvd: "
    loop_mv_reports "$service_name" "$sub_dir"
done
