<!-- BEGIN MUNGE: GENERATED_TOC -->
- [v3.1.0-rc.3](#v310-rc3)
  - [Changelog since v3.1.0-rc.2](#changelog-since-v310-rc2)

- [v3.1.0-rc.2](#v310-rc2)
  - [Changelog since v3.1.0-rc.1](#changelog-since-v310-rc1)

- [v3.1.0-rc.1](#v310-rc1)
  - [Changelog since v3.0.0](#changelog-since-v300)

<!-- END MUNGE: GENERATED_TOC -->



<!-- NEW RELEASE NOTES ENTRY -->
# v3.1.0-rc.3
## 2024-11-22
### Changelog since v3.1.0-rc.2
#### New

##### Pipeline
- [New] feat: Pipeline variable syntax supports two styles [link](http://github.com/TencentBlueKing/bk-ci/issues/10576)
- [New] [bugfix] okhttp3 Response not closing actively may cause potential memory leak [link](http://github.com/TencentBlueKing/bk-ci/issues/11234)
- [New] [Blue Shield - Reviewed by the Review Committee] [PAC] feat: Create/edit pipelines to support arranging pipelines in Code [link](http://github.com/TencentBlueKing/bk-ci/issues/8125)
- [New] feat: Optimize the editing of drop-down type variable options [link](http://github.com/TencentBlueKing/bk-ci/issues/10747)
- [New] feat: Remove CI administrator related information from pipeline group management [link](http://github.com/TencentBlueKing/bk-ci/issues/11165)
- [New] Pipeline plugin development custom UI hopes to get the jobid attribute of the container [link](http://github.com/TencentBlueKing/bk-ci/issues/11197)
- [New] feat: When the pipeline runs concurrently, support limiting the number of concurrent connections and queuing [link](http://github.com/TencentBlueKing/bk-ci/issues/10718)
- [New] feat: Recommended version number optimization [link](http://github.com/TencentBlueKing/bk-ci/issues/10958)
- [New] feat: Pipeline trigger history supports searching by trigger results [link](http://github.com/TencentBlueKing/bk-ci/issues/11006)
- [New] Added a build details view configuration item. When you click a plugin in the build details interface, the default page you enter is the log or configuration tab page. [Link](http://github.com/TencentBlueKing/bk-ci/issues/10808)
- [New] feat: When queuing in a mutually exclusive job group, the queue length supports up to 50 [link](http://github.com/TencentBlueKing/bk-ci/issues/10975)

##### Store
- [New] feat:java plugin supports running in a specified java environment [link](http://github.com/TencentBlueKing/bk-ci/issues/10978)

##### Environmental Management
- [New] feat: Environment management optimization changes [link](http://github.com/TencentBlueKing/bk-ci/issues/11003)

##### Log Service
- [New] When accessing the pipeline search log, the search bar above will disappear when querying the search log in the full-screen browser [link](http://github.com/TencentBlueKing/bk-ci/issues/11118)

###### Permission Center
- [New] feat: Optimize the acquisition of user group members under the project [link](http://github.com/TencentBlueKing/bk-ci/issues/11221)
- [New] feat: Optimize user application to join group [link](http://github.com/TencentBlueKing/bk-ci/issues/11219)
- [New] feat: Environment supports resource-level permission management entry [link](http://github.com/TencentBlueKing/bk-ci/issues/11074)
- [New] feat: pipeline list display permission control [link](http://github.com/TencentBlueKing/bk-ci/issues/10895)
- [New] feat: Provides interfaces for obtaining the user groups and renewal of users [link](http://github.com/TencentBlueKing/bk-ci/issues/11136)
- [New] feat: auth service open class interface rectification [link](http://github.com/TencentBlueKing/bk-ci/issues/10403)
- [New] bug: Changes in the returned fields of the query department information interface cause exceptions [link](http://github.com/TencentBlueKing/bk-ci/issues/11151)
- [New] feat: Optimize the synchronization logic of user groups [link](http://github.com/TencentBlueKing/bk-ci/issues/11122)

##### project management
- [New] feat: Fix the invalid change of maximum authorized scope [link](http://github.com/TencentBlueKing/bk-ci/issues/11153)

##### Dispatch
- [New] feat: Third-party build machines support running build tasks using dcoker [link](http://github.com/TencentBlueKing/bk-ci/issues/9820)
- [New] feat: No compilation resource optimization environment dependency Dispatch [link](http://github.com/TencentBlueKing/bk-ci/issues/11126)
- [New] feat: The builder triggers the user to be adjusted to the pipeline authority holder [link](http://github.com/TencentBlueKing/bk-ci/issues/11117)

##### Agent
- [New] feat: pipeline/job concurrency and queue data landing [ link ](http://github.com/TencentBlueKing/bk-ci/issues/10997)
- [New] [bugfix] The bash plugin unconfigures <archive file when script returns non-zero> and there is dirty data [link](http://github.com/TencentBlueKing/bk-ci/issues/11177)

##### Uncategorized
- [New] feat: Fix the startup problem of the open source version [link](http://github.com/TencentBlueKing/bk-ci/issues/11202)
- [New] feat: Added Hongmeng platform [link](http://github.com/TencentBlueKing/bk-ci/issues/11191)
- [New] feat: Adjust the helm image to support imageRegistry configuration [link](http://github.com/TencentBlueKing/bk-ci/issues/11171)
- [New] [feat] API documentation optimization-2024-10 batch [link](http://github.com/TencentBlueKing/bk-ci/issues/11107)
- [New] feat: Interaction optimization when dependent services are not deployed [link](http://github.com/TencentBlueKing/bk-ci/issues/10612)
- [New] SQL doc document update [link](http://github.com/TencentBlueKing/bk-ci/issues/9974)
- [New] feat: Support viewing version logs [link](http://github.com/TencentBlueKing/bk-ci/issues/10938)
- [New] feat: Changes in Blue Whale 7.2 version [link](http://github.com/TencentBlueKing/bk-ci/issues/10558)
- [New] Unify the style and content of the top bar drop-down box of the product [link](http://github.com/TencentBlueKing/bk-ci/issues/10939)

#### optimization

##### Pipeline
- [Optimization] pref : The file operator related to the pipeline is adjusted to the authority holder of the pipeline [link](http://github.com/TencentBlueKing/bk-ci/issues/11016)

##### Store
- [Optimization] perf: Optimize the default plugin query in the plugin management menu [link](http://github.com/TencentBlueKing/bk-ci/issues/11142)
- [Optimization] pref : Adjust the file storage path of the file upload interface [link](http://github.com/TencentBlueKing/bk-ci/issues/10919)

##### Uncategorized
- [Optimization] perf: The version log date is adjusted to the second-level title [link](http://github.com/TencentBlueKing/bk-ci/issues/11162)

#### Fixes

##### Pipeline
- [Fix] Bug: When clicking Skip during finally stage execution, the failed job status will be stuck in the execution [link](http://github.com/TencentBlueKing/bk-ci/issues/11143)
- [Fix] feat: Template management-list supports display fields and sorting optimization [ link ](http://github.com/TencentBlueKing/bk-ci/issues/11056)
- [Fix] bug: fix github pull request id out of bounds [link](http://github.com/TencentBlueKing/bk-ci/issues/11146)

##### Code Repository
- [Fix] bug: When PAC is enabled in the fixed code base, the git_project_id field is empty [link](http://github.com/TencentBlueKing/bk-ci/issues/11167)

##### Store
- [Fix] Bug: When different language plugins generate startup commands for the same job, there may be conflicts due to system variables [link](http://github.com/TencentBlueKing/bk-ci/issues/11229)
- [Fix] bug: Optimize the log permissions for viewing store components [link](http://github.com/TencentBlueKing/bk-ci/issues/11208)
- [Fix] Bug: Optimize the paging data query of the plugin installed in the project, and exclude the built-in pipeline associated with the plugin [link](http://github.com/TencentBlueKing/bk-ci/issues/11210)

##### Quality Red Line
- [Fix] Bug: When using pipeline variables to pass in multiple reviewers, the approval does not take effect [link](http://github.com/TencentBlueKing/bk-ci/issues/11127)

###### Permission Center
- [Fix] Synchronize data when adding personnel to permission management user group template [link](http://github.com/TencentBlueKing/bk-ci/issues/11217)

##### Uncategorized
- [Fix] bug: Remove illegal placeholder information in international description information [link](http://github.com/TencentBlueKing/bk-ci/issues/11182)
- [Fix] fix UnreachableCode [link](http://github.com/TencentBlueKing/bk-ci/issues/11172)
- [Fix] Bug: Optimize cluster name obtained based on Profile [link](http://github.com/TencentBlueKing/bk-ci/issues/11137)
# v3.1.0-rc.2
## 2024-10-26
## Changelog since v3.1.0-rc.1
#### New
##### Pipeline
- [New] Recommended version number optimization [link](http://github.com/TencentBlueKing/bk-ci/issues/10958)
- [New] Support pipeline indicator monitoring [link](http://github.com/TencentBlueKing/bk-ci/issues/9860)
- [New] Added label display to pipeline list [link](http://github.com/TencentBlueKing/bk-ci/issues/11054)
- [New] Template management - list supports display fields and sorting optimization [link](http://github.com/TencentBlueKing/bk-ci/issues/11056)
- [New] Source material display optimization [link](http://github.com/TencentBlueKing/bk-ci/issues/10733)
- [New] Support obtaining parent task ID in Post action [link](http://github.com/TencentBlueKing/bk-ci/issues/10968)
- [New] Stage review supports checklist confirmation scenarios [link](http://github.com/TencentBlueKing/bk-ci/issues/10920)
- [New] AI big model integration [link](http://github.com/TencentBlueKing/bk-ci/issues/10825)
- [New] Enrich the audit function of pipeline-stage admission, support configuring roles or user groups as auditors [link](http://github.com/TencentBlueKing/bk-ci/issues/10689)
- [New] Pipeline log supports AI repair [link](http://github.com/TencentBlueKing/bk-ci/issues/10913)
- [New] When the pipeline runs concurrently, support limiting the number of concurrent connections and queuing [link](http://github.com/TencentBlueKing/bk-ci/issues/10718)
- [New] Added default public plugin display in plugin list in plugin management menu [link](http://github.com/TencentBlueKing/bk-ci/issues/10472)
- [New] When the policy is "Lock Build Number", the execution interface can modify the current value [link](http://github.com/TencentBlueKing/bk-ci/issues/11089)
- [New] Community version pipeline completion notification, support notification group [link](http://github.com/TencentBlueKing/bk-ci/issues/10976)
- [New] When queuing in a mutually exclusive job group, the queue length supports up to 50 [link](http://github.com/TencentBlueKing/bk-ci/issues/10975)
- [New] Trigger event replay operation permission control [link](http://github.com/TencentBlueKing/bk-ci/issues/11052)
- [New] Support retries for executions triggered by sub-pipeline calls [link](http://github.com/TencentBlueKing/bk-ci/issues/11015)
- [New] "Execute when all parameters meet the conditions" and "Do not execute when all parameters meet the conditions" in UI mode are converted to Code optimization [link](http://github.com/TencentBlueKing/bk-ci/issues/10930)
- [New] MR event trigger support WIP [link](http://github.com/TencentBlueKing/bk-ci/issues/10683)
- [New] Worker Bee MR trigger adds action=edit [link](http://github.com/TencentBlueKing/bk-ci/issues/11024)
##### Code Repository
- [New] Added access to worker bees and github oauth url build interface [link](http://github.com/TencentBlueKing/bk-ci/issues/10826)
###### Permission Center
- [New] Synchronize and store resource group permission data in separate tables [link](http://github.com/TencentBlueKing/bk-ci/issues/10964)
- [New] Create custom groups and grant group permissions [link](http://github.com/TencentBlueKing/bk-ci/issues/11026)
##### Environmental Management
- [New] Third-party build machines support running build tasks using dcoker [link](http://github.com/TencentBlueKing/bk-ci/issues/9820)
- [New] Support for third-party build machine DockerUi interface [link](http://github.com/TencentBlueKing/bk-ci/issues/10962)
##### Openapi
- [New] OpenApi provides an interface for forwarding Turbo compilation to accelerate reporting of resource statistics data [link](http://github.com/TencentBlueKing/bk-ci/issues/10508)
##### other
- [New] Support viewing version logs [link](http://github.com/TencentBlueKing/bk-ci/issues/10938)
- [New] Optimize AESUtil [link](http://github.com/TencentBlueKing/bk-ci/issues/11084)
- [New] SQL doc document update [link](http://github.com/TencentBlueKing/bk-ci/issues/9974)
- [New] Engine and other MQ scenarios are connected to the SCS framework [link](http://github.com/TencentBlueKing/bk-ci/issues/7443)

#### optimization
##### Store
- [Optimization] Optimization of store component indicator data fields [link](http://github.com/TencentBlueKing/bk-ci/issues/10219)
##### Stream
- [Optimization] [stream] Optimization of retention issues [link](http://github.com/TencentBlueKing/bk-ci/issues/11045)

#### Fixes
##### Pipeline
- [Fix] The CONTAINER_ID field value inserted into the T_PIPELINE_BUILD_RECORD_TASK table in some build scenarios is incorrect [link](http://github.com/TencentBlueKing/bk-ci/issues/11029)
- [Fix] The trigger condition introduces ${{ variables.xxx }} variables and cannot trigger the pipeline [link](http://github.com/TencentBlueKing/bk-ci/issues/10987)
- [Fix] Trigger variable supplement [link](http://github.com/TencentBlueKing/bk-ci/issues/11002)
##### Store
- [Fix] Optimize the upload and download of store component package files [link](http://github.com/TencentBlueKing/bk-ci/issues/11115)
- [Fix] When updating component associated initialization project information, the old debug project records were not successfully cleared when adding debug project records [link](http://github.com/TencentBlueKing/bk-ci/issues/11011)
##### Product Library
- [Fix] Archive report plugin creation token is not implemented [link](http://github.com/TencentBlueKing/bk-ci/issues/10693)


# v3.1.0-rc.1
## 2024-10-15
## Changelog since v3.0.0
#### New
##### Pipeline
- [New] Optimize the display of names in the breadcrumbs of pipeline view page/edit page/build details interface [link](http://github.com/TencentBlueKing/bk-ci/issues/10800)
- [New] Pipeline version management mechanism [link](http://github.com/TencentBlueKing/bk-ci/issues/8161)
- [New] Job concurrency supports Code configuration [link](http://github.com/TencentBlueKing/bk-ci/issues/10860)
- [New] Draft version UI display [link](http://github.com/TencentBlueKing/bk-ci/issues/9861)
##### Store
- [New] Whether the SDK- related API is displayed in the application list and supports configurability [ link ](http://github.com/TencentBlueKing/bk-ci/issues/10840)
###### Permission Center
- [New] Project member management optimization [link](http://github.com/TencentBlueKing/bk-ci/issues/10927)
- [New] Active user record operations and times [link](http://github.com/TencentBlueKing/bk-ci/issues/10891)
- [New] Support searching project members by expiration date/user group name [link](http://github.com/TencentBlueKing/bk-ci/issues/10892)
- [New] Issue: Fix the problem of querying the entire project table under sample authentication [link](http://github.com/TencentBlueKing/bk-ci/issues/10941)
- [New] oauth2 adds password mode [link](http://github.com/TencentBlueKing/bk-ci/issues/10663)
##### Stream
- [New] [stream] Optimize the triggering time of large warehouse [link](http://github.com/TencentBlueKing/bk-ci/issues/10861)
- [New] Notification method for stream stage review supports enterprise WeChat groups [link](http://github.com/TencentBlueKing/bk-ci/issues/10796)
##### Dispatch
- [New] Support Code configuration for reusing build environment between jobs of third-party build machines [link](http://github.com/TencentBlueKing/bk-ci/issues/10254)
- [New] Optimize resource Dispatch priority when building the same pipeline multiple times [link](http://github.com/TencentBlueKing/bk-ci/issues/9897)
- [New] Remove the config ns configuration of the docker build plugin [link](http://github.com/TencentBlueKing/bk-ci/issues/10926)
- [New] AgentId reuse type conversion issue [link](http://github.com/TencentBlueKing/bk-ci/issues/10915)
##### Worker
- [New] Support worker running in JDK17 [link](http://github.com/TencentBlueKing/bk-ci/issues/10412)
##### other
- [New] Newly started POD needs to be warmed up [link](http://github.com/TencentBlueKing/bk-ci/issues/10887)
- [New] OpenAPI filter optimization [link](http://github.com/TencentBlueKing/bk-ci/issues/10679)
- [New] update lerna +yarn workspace to pnpm [link](http://github.com/TencentBlueKing/bk-ci/issues/8125)

#### optimization
##### Pipeline
- [Optimization] When running a pipeline with a matrix, the task before the matrix split does not need to be written into the record table [link](http://github.com/TencentBlueKing/bk-ci/issues/10873)
##### Store
Optimize the error message when pulling the plugin task.json file content [link](http://github.com/TencentBlueKing/bk-ci/issues/10446)
- [Optimization] Optimization of signature verification of sensitive interfaces in stores [link](http://github.com/TencentBlueKing/bk-ci/issues/10759)
- [Optimization] Change the application schema so that each version can have different configurations [link](http://github.com/TencentBlueKing/bk-ci/issues/10929)
- [Optimization] Optimization of the public build machine plugin cache path and variable adjustment [link](http://github.com/TencentBlueKing/bk-ci/issues/10844)

#### Fixes
##### Pipeline
- [Fix] Prompt to save when switching to Code mode on non-editing pages [link](http://github.com/TencentBlueKing/bk-ci/issues/10933)
- [Fix] Failed retry display issue on new build details page [link](http://github.com/TencentBlueKing/bk-ci/issues/10735)
##### Store
- [Fix] The pipeline template was uploaded to the store, but it could not be found in the "store" tab when creating a new pipeline [link](http://github.com/TencentBlueKing/bk-ci/issues/10865)
- [Fix] The plugin environment information query interface does not correctly handle the plugin test branch version number [link](http://github.com/TencentBlueKing/bk-ci/issues/10924)
###### Permission Center
- [Fix] bk-permission project member management [link](http://github.com/TencentBlueKing/bk-ci/issues/9620)
##### Stream
- [Fix] Fixed the regular expression error of stream new environment name [link](http://github.com/TencentBlueKing/bk-ci/issues/10939)
