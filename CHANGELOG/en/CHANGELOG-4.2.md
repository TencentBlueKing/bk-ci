<!-- BEGIN MUNGE: GENERATED_TOC -->
- [v4.2.0-rc.2](#v420-rc2)
   - [Changelog since v4.2.0-rc.1](#changelog-since-v420-rc1)

- [v4.2.0-rc.1](#v420-rc1)
   - [Changelog since v4.1.0](#changelog-since-v410)

<!-- END MUNGE: GENERATED_TOC -->



<!-- NEW RELEASE NOTES ENTRY -->
# v4.2.0-rc.2
## 2026-05-15
### Changelog since v4.2.0-rc.1
#### New Features

##### Pipeline
- [New] feat: Support retrieving pipeline failure details [Link](http://github.com/TencentBlueKing/bk-ci/issues/12873)
- [New] feat: Fix encryption issue when sensitive fields are not set [Link](http://github.com/TencentBlueKing/bk-ci/issues/12871)
- [New] feat: Automatically install plugins for constraint-mode templates triggered by source template upgrades [Link](http://github.com/TencentBlueKing/bk-ci/issues/12896)
- [New] feature: Add default images for pipeline jobs, issue #1108 [Link](http://github.com/TencentBlueKing/bk-ci/issues/1265)

##### Store
- [New] feat: Support sending Store plugin comment and review notifications to groups [Link](http://github.com/TencentBlueKing/bk-ci/issues/12655)
- [New] Store: Support displaying version logs [Link](http://github.com/TencentBlueKing/bk-ci/issues/1761)

##### Permission Center
- [New] feat: Add labels and prompts for groups that cannot be proactively joined when applying for permissions [Link](http://github.com/TencentBlueKing/bk-ci/issues/12436)

##### Project Management
- [New] feat: Add an Ops API for setting the system default cluster [Link](http://github.com/TencentBlueKing/bk-ci/issues/12804)
- [New] feat: Optimize project ownership information entry [Link](http://github.com/TencentBlueKing/bk-ci/issues/12527)

##### Agent
- [New] feat: Agent reports concurrency metric data [Link](http://github.com/TencentBlueKing/bk-ci/issues/12526)
- [New] feat: Support `--network` and `--user` for third-party build machines using the Docker runtime [Link](http://github.com/TencentBlueKing/bk-ci/issues/12832)
- [New] feat: Support nohead for macOS agents [Link](http://github.com/TencentBlueKing/bk-ci/issues/12809)
- [New] feat: Support MCP for agents [Link](http://github.com/TencentBlueKing/bk-ci/issues/12653)

##### Others
- [New] feat: Remove the telegraf dependency from third-party build machines [Link](http://github.com/TencentBlueKing/bk-ci/issues/12895)
- [New] feat: Harden login debugging authentication [Link](http://github.com/TencentBlueKing/bk-ci/issues/12924)
- [New] feat: Support using user sessions under Windows services [Link](http://github.com/TencentBlueKing/bk-ci/issues/12765)
- [New] feat: Support BK-CI intelligent assistant [Link](http://github.com/TencentBlueKing/bk-ci/issues/12737)
- [New] BlueKing security governance: SAST scan fixes [Link](http://github.com/TencentBlueKing/bk-ci/issues/12884)
- [New] feat: Pipeline real-time monitoring overview page [Link](http://github.com/TencentBlueKing/bk-ci/issues/12497)
- [New] feat: Replace the Docker SDK with Docker CLI for agents to avoid dependency version issues caused by daemon upgrades [Link](http://github.com/TencentBlueKing/bk-ci/issues/12791)
- [New] Fix compilation errors, issue #171 [Link](http://github.com/TencentBlueKing/bk-ci/issues/176)

#### Improvements

##### Pipeline
- [Improved] perf: Reduce access frequency for Redis hot keys [Link](http://github.com/TencentBlueKing/bk-ci/issues/12488)

##### Store
- [Improved] perf: Redownload and overwrite damaged plugin cache files on build machines from the repository [Link](http://github.com/TencentBlueKing/bk-ci/issues/12447)

##### Permission Center
- [Improved] perf: Optimize the agent permission renewal application API [Link](http://github.com/TencentBlueKing/bk-ci/issues/12929)
- [Improved] perf: Stop sending permission renewal reminders for disabled projects [Link](http://github.com/TencentBlueKing/bk-ci/issues/12475)

##### Others
- [Improved] perf: Optimize pipeline build data cleanup [Link](http://github.com/TencentBlueKing/bk-ci/issues/12797)

#### Bug Fixes

##### Pipeline
- [Fixed] bug: PAC scheduled trigger creation reported that the PAC listener scheduled trigger configuration was invalid because PAC was not enabled for the current pipeline [Link](http://github.com/TencentBlueKing/bk-ci/issues/12946)
- [Fixed] bug: Publishing PAC pipelines should submit repository changes as the publisher [Link](http://github.com/TencentBlueKing/bk-ci/issues/12846)
- [Fixed] Parameters were not saved after modifying them and switching instances during batch template instance upgrades [Link](http://github.com/TencentBlueKing/bk-ci/issues/12939)
- [Fixed] bug: Regular pipelines could be changed to constraint-mode pipelines [Link](http://github.com/TencentBlueKing/bk-ci/issues/12933)
- [Fixed] bug: Templates copied from instantiated pipelines could not be edited after being copied as template instances [Link](http://github.com/TencentBlueKing/bk-ci/issues/12899)
- [Fixed] bug: Automatic retry did not take effect after the manual review plugin timed out [Link](http://github.com/TencentBlueKing/bk-ci/issues/11661)
- [Fixed] bug: Renaming a PAC YAML file should not delete the old pipeline and create a new one [Link](http://github.com/TencentBlueKing/bk-ci/issues/12658)
- [Fixed] bug: Webhook registration failed in SELF mode for PAC pipelines [Link](http://github.com/TencentBlueKing/bk-ci/issues/12874)
- [Fixed] bug: Editing instantiated pipelines incorrectly prompted required-value errors for constants or other variables [Link](http://github.com/TencentBlueKing/bk-ci/issues/12921)
- [Fixed] bug: Permission check details for child pipeline plugins were not surfaced during template instantiation [Link](http://github.com/TencentBlueKing/bk-ci/issues/12891)

##### Repository
- [Fixed] bug: Change the ExternalCodeccRepoResource API to service mode [Link](http://github.com/TencentBlueKing/bk-ci/issues/12892)

##### Others
- [Fixed] bugfix: Fix some issues in the new third-party build machine mode [Link](http://github.com/TencentBlueKing/bk-ci/issues/12945)

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
