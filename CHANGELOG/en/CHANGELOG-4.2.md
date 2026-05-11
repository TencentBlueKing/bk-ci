<!-- BEGIN MUNGE: GENERATED_TOC -->
- [v4.2.0-rc.1](#v420-rc1)
   - [Changelog since v4.1.0](#changelog-since-v410)

<!-- END MUNGE: GENERATED_TOC -->



<!-- NEW RELEASE NOTES ENTRY -->
# v4.2.0-rc.1
## 2026-04-21
### Changelog since v4.1.0
#### New Features

##### Pipeline
- [New] feat: Tag event triggers support action filtering [Link](http://github.com/TencentBlueKing/bk-ci/issues/12670)
- [New] feat: The review comment in the manual review plugin can now be configured as required [Link](http://github.com/TencentBlueKing/bk-ci/issues/12810)

##### Store
- [New] feat: Support signatures for Go plugin installation packages [Link](http://github.com/TencentBlueKing/bk-ci/issues/12694)

##### Project Management
- [New] feat: Support hiding project properties [Link](http://github.com/TencentBlueKing/bk-ci/issues/12796)
- [New] feat: Support enabling/disabling shared artifact capability at the project level [Link](http://github.com/TencentBlueKing/bk-ci/issues/12592)

##### Others
- [New] feat: RedisLock supports a dedicated Redis instance [Link](http://github.com/TencentBlueKing/bk-ci/issues/12508)

#### Improvements

##### Pipeline
- [Improved] perf: Adjust the built-in pipeline parameter list [Link](http://github.com/TencentBlueKing/bk-ci/issues/12859)

##### Store
- [Improved] perf: Optimize Store template installation [Link](http://github.com/TencentBlueKing/bk-ci/issues/12837)
- [Improved] perf: Optimize Store template installation [Link](http://github.com/TencentBlueKing/bk-ci/issues/12771)

##### Permission Center
- [Improved] perf: Optimize the display of member and organization statistics in user groups, and improve long user group name display [Link](http://github.com/TencentBlueKing/bk-ci/issues/12798)

##### Others
- [Improved] bug: OpenAPI lightweight pipeline build history retrieval did not sort query results [Link](http://github.com/TencentBlueKing/bk-ci/issues/12830)

#### Bug Fixes

##### Pipeline
- [Fixed] bug: Instance update could cause labels to be lost [Link](http://github.com/TencentBlueKing/bk-ci/issues/12863)
- [Fixed] bug: Deleting pipeline labels did not update dynamic pipeline groups [Link](http://github.com/TencentBlueKing/bk-ci/issues/12831)
- [Fixed] bug: Some partitioned table SQL queries lacked partition key conditions [Link](http://github.com/TencentBlueKing/bk-ci/issues/12841)
- [Fixed] bug: After deleting pipeline notifications, they could still be viewed in code mode [Link](http://github.com/TencentBlueKing/bk-ci/issues/12788)
- [Fixed] bug: The `manualCommand` field was lost for the run plugin in code mode [Link](http://github.com/TencentBlueKing/bk-ci/issues/12789)
- [Fixed] bug: Matrix-split review based on the number of reviewers caused errors when clicking approve in the manual review plugin [Link](http://github.com/TencentBlueKing/bk-ci/issues/12783)
- [Fixed] bug: When a pipeline job timed out, the plugin that failed at that time was not written to the `BK_CI_BUILD_FAIL_TASKS` variable [Link](http://github.com/TencentBlueKing/bk-ci/issues/12741)
- [Fixed] bug: After retrying the manual review plugin, variable values referenced in notification content still used the previous execution's values [Link](http://github.com/TencentBlueKing/bk-ci/issues/12723)

##### Repository
- [Fixed] bug: Compatible with 400 and 404 error codes when retrieving repository directory lists [Link](http://github.com/TencentBlueKing/bk-ci/issues/12853)

##### Dispatch
- [Fixed] bugfix: Remove the specified workspace for third-party build machine containers [Link](http://github.com/TencentBlueKing/bk-ci/issues/12814)

##### Others
- [Fixed] bug: Super administrator permission verification bug [Link](http://github.com/TencentBlueKing/bk-ci/issues/12849)
- [Fixed] bugfix: The pipeline view API could not return favorited pipelines and pipelines created by the current user [Link](http://github.com/TencentBlueKing/bk-ci/issues/12817)
- [Fixed] bug: APK packages uploaded in chunks could not be downloaded from Experience [Link](http://github.com/TencentBlueKing/bk-ci/issues/12821)
