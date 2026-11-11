<!-- BEGIN MUNGE: GENERATED_TOC -->
- [v3.2.1](#v321)
  - [Changelog since v3.2.0](#changelog-since-v320)

<!-- END MUNGE: GENERATED_TOC -->



<!-- NEW RELEASE NOTES ENTRY -->
# v3.2.1
## 2026-11-11
### Changelog since v3.2.0
### Summary
Key changes in this release:

**Features**
- Support JWT validation between services to enhance the security of internal interface calls
- Migrate Maven repository publishing from OSS to Central
- Close the service/op interface to consolidate the operations entry point
- Remove the dispatch K8s module and proxy cross-network-region code to simplify the deployment architecture
- Upgrade the Turbo version to improve build acceleration

**Bug Fixes**
- Fix the issue where the tgit trigger 2.0 version had empty content
- Fix errors in the default image initialization script
- Add project access permission validation to user-facing interfaces

#### New
- [New] Close the service/op interface [link](http://github.com/TencentBlueKing/bk-ci/issues/13843)
- [New] feat: Remove the dispatch K8s module [link](http://github.com/TencentBlueKing/bk-ci/issues/13649)
- [New] feat: Migrate Maven repository publishing from OSS to Central [link](http://github.com/TencentBlueKing/bk-ci/issues/11817)
- [New] feat: Support JWT validation between services [link](http://github.com/TencentBlueKing/bk-ci/issues/12067)
- [New] feat: Remove proxy cross-network-region code [link](http://github.com/TencentBlueKing/bk-ci/issues/12091)
- [New] feat: Upgrade the Turbo version [link](http://github.com/TencentBlueKing/bk-ci/issues/12055)

#### optimization
- [Improved] chore: Upgrade api-turbo to 0.0.7-RELEASE [link](http://github.com/TencentBlueKing/bk-ci/issues/12382)

#### Fixes

##### Pipeline
- [Fixed] bug: Fix the issue where the tgit trigger 2.0 version had empty content [link](http://github.com/TencentBlueKing/bk-ci/issues/12530)

##### Others
- [Fixed] bugfix: Fix errors in the default image initialization script [link](http://github.com/TencentBlueKing/bk-ci/issues/12216)
- [Fixed] bug: Add project access permission validation to user-facing interfaces [link](http://github.com/TencentBlueKing/bk-ci/issues/11971)
