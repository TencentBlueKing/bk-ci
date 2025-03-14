<!-- BEGIN MUNGE: GENERATED_TOC -->
- [v3.0.13](#v3013)
  - [Changelog since v3.0.12](#changelog-since-v3012)

- [v3.0.12](#v3012)
  - [Changelog since v3.0.11](#changelog-since-v3011)

- [v3.0.11](#v3011)
  - [Changelog since v3.0.0](#changelog-since-v300)
- [v3.0.1-v3.0.10]
  - 因镜像版本与仓库版本没有统一,v3.0.1-v3.0.10已有镜像版本,但没有仓库版本,所以仓库这些版本直接跳过
- [v3.0.0](#v300)
  - [Changelog since v2.1.0](#changelog-since-v210)
- [v3.0.0-rc.1](#v300-rc1)
  - [Changelog since v2.1.0](#changelog-since-v210)

<!-- END MUNGE: GENERATED_TOC -->



<!-- NEW RELEASE NOTES ENTRY -->
# v3.0.13
## 2025-02-12
### Changelog since v3.0.12
#### Added

##### Uncategorized
- [Added] feat: chart package issue that needs to be fixed [link](http://github.com/TencentBlueKing/bk-ci/issues/11105)

# v3.0.12
## 2025-01-08
### Changelog since v3.0.11
#### Fix

##### Uncategorized
- [Fixed] bug: fix the error when installing helm chart package in v3.0 version [link](http://github.com/TencentBlueKing/bk-ci/issues/11391)

# v3.0.11
## 2024-12-05
### Changelog since v3.0.0
#### New

##### Uncategorized
- [New] feat: Adjust the image of helm to support imageRegistry configuration [link](http://github.com/TencentBlueKing/bk-ci/issues/11171)
- [New] feat: Interaction optimization when dependent services are not deployed [link](http://github.com/TencentBlueKing/bk-ci/issues/10612)
- [New] feat: Support viewing version logs [link](http://github.com/TencentBlueKing/bk-ci/issues/10938)
- [New] feat: Changes in Blue Whale 7.2 version [link](http://github.com/TencentBlueKing/bk-ci/issues/10558)
- [New] Unify the style and content of the top bar drop-down box of the product [Link](http://github.com/TencentBlueKing/bk-ci/issues/10939)
- [Added] feat: Remove the config ns configuration of the docker build plugin [Link](http://github.com/TencentBlueKing/bk-ci/issues/10926)
- [Added] feat: Newly started PODs need to be warmed up [Link](http://github.com/TencentBlueKing/bk-ci/issues/10887)
- [Added] feat: Allow workers to run in JDK17 [Link](http://github.com/TencentBlueKing/bk-ci/issues/10412)

#### Fix

##### Pipeline
- [Fixed] [Devops - Review Committee has reviewed] [PAC] feat: Create/edit pipelines to support arranging pipelines in Code [Link](http://github.com/TencentBlueKing/bk-ci/issues/8125)

##### Permission Center
- [Fix] bug: Permission Management-Permission Renewal Data Synchronization [Link](http://github.com/TencentBlueKing/bk-ci/issues/11271)

##### Uncategorized
- [Fix] fix UnreachableCode [Link](http://github.com/TencentBlueKing/bk-ci/issues/11172)

# v3.0.0
## 2024-09-10
### Changelog since v2.1.0
#### New
##### Pipeline
- Pipeline as code
  - [New] feat: Draft version UI display [link](http://github.com/TencentBlueKing/bk-ci/issues/9861)
  - [New] Pipeline version management mechanism [link](http://github.com/TencentBlueKing/bk-ci/issues/8161)
  - [New] [PAC] feat: Code bases with PAC mode enabled support automatic synchronization of code base YAML changes to Devops [link](http://github.com/TencentBlueKing/bk-ci/issues/8130)
  - [New] pac ui editing pipeline [link](http://github.com/TencentBlueKing/bk-ci/issues/8125)
  - [New] Optimization of the issue that the output variables are not obtained in the variable panel of the pipeline created in Code mode [link](http://github.com/TencentBlueKing/bk-ci/issues/10755)
  - [New] Support debugging pipeline when creating/editing pipeline [link](http://github.com/TencentBlueKing/bk-ci/issues/8164)
  - [New] Context usage scope limitation [link](http://github.com/TencentBlueKing/bk-ci/issues/10655)
  - [New] [PAC] feat: Pipeline constant Code syntax and specifications [link](http://github.com/TencentBlueKing/bk-ci/issues/9971)
  - [New] Release pipeline page "static" pipeline group optimization [link](http://github.com/TencentBlueKing/bk-ci/issues/9962)
  - [New] Dynamic pipeline groups support conditional grouping based on the first-level directory under code base/.ci [link](http://github.com/TencentBlueKing/bk-ci/issues/9682)
  - [New] [PAC] feat: Support disabling pipeline in code [link](http://github.com/TencentBlueKing/bk-ci/issues/9788)
  - [New] Record operation logs during pipeline maintenance [link](http://github.com/TencentBlueKing/bk-ci/issues/8197)
  - [New] [PAC] Reuse build resource pool across projects, support Code configuration [link](http://github.com/TencentBlueKing/bk-ci/issues/10225)
  - [New] [PAC] feat: Custom build number format supports Code definition [link](http://github.com/TencentBlueKing/bk-ci/issues/10210)
  - [New] Edit variable interactive optimization [link](http://github.com/TencentBlueKing/bk-ci/issues/9652)
  - [New] The pipeline build details page supports one-click expansion/collapse of jobs [link](http://github.com/TencentBlueKing/bk-ci/issues/9775)
  - [New] Support Devops new expression running conditions [link](http://github.com/TencentBlueKing/bk-ci/issues/10467)
  - [New] Release pipeline page, add instructions for PAC mode [link](http://github.com/TencentBlueKing/bk-ci/issues/10482)
  - [New] [PAC] Impact of code conversion on API users [link](http://github.com/TencentBlueKing/bk-ci/issues/9813)
  - [New] Debug record prompts and entry optimization [link](http://github.com/TencentBlueKing/bk-ci/issues/10720)
  - [New] Pipeline variables support manual drag and drop to adjust the order [link](http://github.com/TencentBlueKing/bk-ci/issues/10458)
  - [New] Pipeline notes support contextual setting and reference [link](http://github.com/TencentBlueKing/bk-ci/issues/10459)
  - [New] Pull components to support pipeline debugging mode [link](http://github.com/TencentBlueKing/bk-ci/issues/10291)
  - [New] [PAC] feat: View pipeline [link](http://github.com/TencentBlueKing/bk-ci/issues/8195)
- [New] Support pipeline indicator monitoring [link](http://github.com/TencentBlueKing/bk-ci/issues/9860)
- [New] Refactoring of pipeline permission proxy function [link](http://github.com/TencentBlueKing/bk-ci/issues/10356)
  - [New] Added permission holder variables [link](http://github.com/TencentBlueKing/bk-ci/issues/10890)
- [New] Optimize pipeline template settings [link](http://github.com/TencentBlueKing/bk-ci/issues/10857)
- [New] Pipeline execution history supports filtering by triggerer [link](http://github.com/TencentBlueKing/bk-ci/issues/10752)
- [New] Interaction optimization when pipeline notification mode is not effective [link](http://github.com/TencentBlueKing/bk-ci/issues/10615)
- [New] Worker Bee MR trigger supports setting listening actions [link](http://github.com/TencentBlueKing/bk-ci/issues/8949)
- [New] MR event trigger support WIP [link](http://github.com/TencentBlueKing/bk-ci/issues/10683)
- [New] P4 trigger supports Code writing [link](http://github.com/TencentBlueKing/bk-ci/issues/10551)
- [New] Git event trigger custom trigger conditions support definition through Code [link](http://github.com/TencentBlueKing/bk-ci/issues/10497)
- [New] Pipeline log color optimization [link](http://github.com/TencentBlueKing/bk-ci/issues/9934)
- [New] When openapi triggers the pipeline to run, support passing in trigger materials [link](http://github.com/TencentBlueKing/bk-ci/issues/10302)
- [New] Logs need to display special characters [link](http://github.com/TencentBlueKing/bk-ci/issues/10097)
- [New] Pipeline renaming optimization [link](http://github.com/TencentBlueKing/bk-ci/issues/10399)
- [New] Added fallback logic to the path matching rules triggered by SVN events [ link ](http://github.com/TencentBlueKing/bk-ci/issues/10510)
- [New] Added "Execution time" field to the pipeline execution history list [link](http://github.com/TencentBlueKing/bk-ci/issues/10251)
- [New] [Devops-Product-Reviewed] Pipeline supports displaying running progress [link](http://github.com/TencentBlueKing/bk-ci/issues/7932)
- [New] Build history list supports displaying build information fields [link](http://github.com/TencentBlueKing/bk-ci/issues/10724)
- [New] Pipeline supports exporting POJO attributes in order [link](http://github.com/TencentBlueKing/bk-ci/issues/10728)
- [New] Optimize the variables of pipeline "file" type [link](http://github.com/TencentBlueKing/bk-ci/issues/10400)
- [New] Scheduled triggers support specifying code bases and branches [link](http://github.com/TencentBlueKing/bk-ci/issues/10300)
- [New] Pipeline template management editing and instance management optimization [link](http://github.com/TencentBlueKing/bk-ci/issues/10626)
- [New] Verify the permissions of the referenced sub-pipeline when saving the pipeline [link](http://github.com/TencentBlueKing/bk-ci/issues/10259)
- [New] Dynamic configuration management of pipeline engine [link](http://github.com/TencentBlueKing/bk-ci/issues/10647)
- [New] Support viewing the status of asynchronously executed sub-pipelines in the parent pipeline [link](http://github.com/TencentBlueKing/bk-ci/issues/10260)
- [New] When adding drop-down/checkbox type variables, predefined options support batch input and key input [link](http://github.com/TencentBlueKing/bk-ci/issues/10290)
- [New] Complete the list of built-in variables [link](http://github.com/TencentBlueKing/bk-ci/issues/10436)
- [New] On the pipeline build details page, the time consumed by each job/step is directly displayed [link](http://github.com/TencentBlueKing/bk-ci/issues/10311)
- [New] Recycle Bin supports pipeline noun search [link](http://github.com/TencentBlueKing/bk-ci/issues/10408)
- [New] Optimize the display content of the pipeline list recently executed [link](http://github.com/TencentBlueKing/bk-ci/issues/10600)
- [New] Product download no response issue [link](http://github.com/TencentBlueKing/bk-ci/issues/10555)
- [New] Optimize the parameter passing method of sub-pipeline calling plugin [link](http://github.com/TencentBlueKing/bk-ci/issues/9943)
- [New] Fix the issue of missing concurrent grouping configuration in pipeline settings view page [link](http://github.com/TencentBlueKing/bk-ci/issues/10516)
- [New] Abnormal spaces in the copied log [link](http://github.com/TencentBlueKing/bk-ci/issues/10540)
- [New] Added pipeline version description, increased length limit [link](http://github.com/TencentBlueKing/bk-ci/issues/10520)
- [New] On the build details page, hovering over the version number will display the corresponding version description [link](http://github.com/TencentBlueKing/bk-ci/issues/10524)
##### Code Repository
- [New] When associating with the WorkerBee code base, support enabling Pipeline as Code mode [ link ](http://github.com/TencentBlueKing/bk-ci/issues/8115)
- [New] Code base optimization phase 1 function points [link](http://github.com/TencentBlueKing/bk-ci/issues/9347)
- [New] github pr check output quality red line report [link](http://github.com/TencentBlueKing/bk-ci/issues/10607)
- [New] [openapi] Link the code base to Devops's API to support enabling PAC [link](http://github.com/TencentBlueKing/bk-ci/issues/10770)
- [New] PAC mode enabled code base, supports disabling PAC [link](http://github.com/TencentBlueKing/bk-ci/issues/9993)
- [New] Optimize the display of code base trigger event results [link](http://github.com/TencentBlueKing/bk-ci/issues/10307)
- [New] github check run should support pipelines for GONGFENGSCAN channel [link](http://github.com/TencentBlueKing/bk-ci/issues/10704)
##### Quality Red Line
- [New] When there are multiple CodeCC plugins in the pipeline, the quality red line jump link should be able to jump to the corresponding task [link](http://github.com/TencentBlueKing/bk-ci/issues/10605)
- [New] quality added matchRuleList app interface [link](http://github.com/TencentBlueKing/bk-ci/issues/10610)
##### Environmental Management
- [New] Support disabling/enabling nodes in the build environment [link](http://github.com/TencentBlueKing/bk-ci/issues/10258)
- [New] Clean up the online and offline records of third-party build machines [link](http://github.com/TencentBlueKing/bk-ci/issues/10237)
- [New] After installing the WINDOWS build machine and clicking install.bat to complete the installation, the refresh node does not display [link](http://github.com/TencentBlueKing/bk-ci/issues/10725)
- [New] Support batch installation of Agent [link](http://github.com/TencentBlueKing/bk-ci/issues/10024)
##### Permission Center
- [New] Support administrators to view project members [link](http://github.com/TencentBlueKing/bk-ci/issues/9620)
- [New] User group related interface optimization [link](http://github.com/TencentBlueKing/bk-ci/issues/10463)
- [New] Pull user list based on organization ID [link](http://github.com/TencentBlueKing/bk-ci/issues/10513)
- [New] Optimize the permission application page [link](http://github.com/TencentBlueKing/bk-ci/issues/10145)
##### Project management
- [New] Optimization of the problem that the name of the operational product is not displayed on the project view page [link](http://github.com/TencentBlueKing/bk-ci/issues/10668)
- [New] Added project-level event callback [link](http://github.com/TencentBlueKing/bk-ci/issues/10146)
##### Store
- [New] Support plugin developers to set the default timeout and default failure strategy [link](http://github.com/TencentBlueKing/bk-ci/issues/10019)
- [New] Added interface for modifying store component initialization project [ link ](http://github.com/TencentBlueKing/bk-ci/issues/10126)
- [New] Retry when the plugin fails to upload a file [link](http://github.com/TencentBlueKing/bk-ci/issues/10214)
- [New] Store - Workbench - Container Image, the status icon is misplaced when verification fails [link](http://github.com/TencentBlueKing/bk-ci/issues/10696)
- [New] Fixed the issue that the associated debug project information was not deleted when updating the component associated initialization project information [link](http://github.com/TencentBlueKing/bk-ci/issues/10621)
- [New] Integrate micro-extension resource scheduling capabilities [link](http://github.com/TencentBlueKing/bk-ci/issues/10122)
##### Log Service
- [New] Add subtag query conditions to the Log Service interface [link](http://github.com/TencentBlueKing/bk-ci/issues/10536)
##### Dispatch
- [New] Optimize the dockerhost dockerRun container log acquisition interface [link](http://github.com/TencentBlueKing/bk-ci/issues/10811)
- [New] kubernetes-manager supports docker inspect image [link](http://github.com/TencentBlueKing/bk-ci/issues/8862)
- [New] The build environment Agent concurrency limit is 0 and does not take effect [link](http://github.com/TencentBlueKing/bk-ci/issues/10740)
- [New] Supports specifying the number of concurrent jobs when the build resource type is a third-party build cluster [link](http://github.com/TencentBlueKing/bk-ci/issues/9810)
- [New] Adjust the default container timeout of dockerhost [link](http://github.com/TencentBlueKing/bk-ci/issues/10645)
- [New] Optimization of third-party build machine build resource locking strategy [link](http://github.com/TencentBlueKing/bk-ci/issues/10449)
- [New] Get the maximum concurrent job execution/project active user measurement data [link](http://github.com/TencentBlueKing/bk-ci/issues/10232)
##### Agent
- [New] Worker kills the parent process of the current process, causing Agent to report a false positive [link](http://github.com/TencentBlueKing/bk-ci/issues/10362)
- [New] Warning for repeated installations with the same ID but different IP addresses when Agent is started [link](http://github.com/TencentBlueKing/bk-ci/issues/10264)
- [New] Agent cleanup process to back up workers [link](http://github.com/TencentBlueKing/bk-ci/issues/10234)
##### Stream
- [New] [stream] Optimize the time consumption of large warehouse triggering [link](http://github.com/TencentBlueKing/bk-ci/issues/10861)
- [New] [stream] Optimize the trigger process and reduce the trigger duration [link](http://github.com/TencentBlueKing/bk-ci/issues/10753)
- [New] When stream starts CI, it is required to fill in the organizational structure and operation products [link](http://github.com/TencentBlueKing/bk-ci/issues/10231)
- [New] [stream] Added the function of getting group members [link](http://github.com/TencentBlueKing/bk-ci/issues/10711)
##### Gateway
- [New] The gateway can handle 302 abnormal jumps during auth_request [link](http://github.com/TencentBlueKing/bk-ci/issues/10295)
- [New] Gateway default tag is not hard-coded [link](http://github.com/TencentBlueKing/bk-ci/issues/10334)
##### Other
- [New] Compress http return json string [link](http://github.com/TencentBlueKing/bk-ci/issues/10323)
- [New] Changes in Blue Whale 7.2 version [link](http://github.com/TencentBlueKing/bk-ci/issues/10558)
- [New] SQL doc document update [link](http://github.com/TencentBlueKing/bk-ci/issues/9974)
- [New] bk-apigw interface authentication method adjustment [link](http://github.com/TencentBlueKing/bk-ci/issues/10802)
- [New] Fix swagger package scanning method [link](http://github.com/TencentBlueKing/bk-ci/issues/10806)
- [New] Global configuration title/footer/logo/favicon/product name [link](http://github.com/TencentBlueKing/bk-ci/issues/10678)
- [New] Devops Gateway trusts the cors-header of the secure domain name [link](http://github.com/TencentBlueKing/bk-ci/issues/10767)
- [New] Fix iam initialization script [link](http://github.com/TencentBlueKing/bk-ci/issues/10658)
- [New] Add text when openapi access is not authorized [link](http://github.com/TencentBlueKing/bk-ci/issues/10638)
- [New] Interaction optimization when dependent services are not deployed [link](http://github.com/TencentBlueKing/bk-ci/issues/10612)
- [New] Improve the speed of rolling release [link](http://github.com/TencentBlueKing/bk-ci/issues/10236)
- [New] Optimize audit related logic [link](http://github.com/TencentBlueKing/bk-ci/issues/10671)
- [New] Optimize open interface section verification [link](http://github.com/TencentBlueKing/bk-ci/issues/10426)

#### Optimization
##### Pipeline
- [Optimization] Optimize the pipeline execution history table [link](http://github.com/TencentBlueKing/bk-ci/issues/10769)
- [Optimization] The pipeline instance copy function does not copy the parameter values of the corresponding instance [link](http://github.com/TencentBlueKing/bk-ci/issues/10580)
- [Optimization] The expression parser adds compatibility with pipeline variable processing [link](http://github.com/TencentBlueKing/bk-ci/issues/10609)
- [Optimization] Disable pipeline function optimization [link](http://github.com/TencentBlueKing/bk-ci/issues/8190)
- [Optimization] UI mode add/edit variable page revision [link](http://github.com/TencentBlueKing/bk-ci/issues/8185)
- [Optimization] Plugin execution error code optimization [link](http://github.com/TencentBlueKing/bk-ci/issues/10326)
##### Environmental Management
- [Optimization] Add some error codes to environment management [link](http://github.com/TencentBlueKing/bk-ci/issues/10788)
- [Optimization] Optimization of some environment management codes [link](http://github.com/TencentBlueKing/bk-ci/issues/10641)
- [Optimization] er: Optimization of some code of environment management 2 [link](http://github.com/TencentBlueKing/bk-ci/issues/10263)
##### Store
- [Optimization] Support Java plugin target reference variables to set the jar package execution path [link](http://github.com/TencentBlueKing/bk-ci/issues/10643)
- [Optimization] Optimization of permission verification of sensitive interfaces in store [link](http://github.com/TencentBlueKing/bk-ci/issues/10418)
- [Optimization] The store plug-in supports specifying running parameters through the execution.target field in task.json [link](http://github.com/TencentBlueKing/bk-ci/issues/10072)
- [Optimization] store universal interface encapsulation [link](http://github.com/TencentBlueKing/bk-ci/issues/10123)
- [Optimization] store logo upload does not support svg images yet to prevent xss attacks [link](http://github.com/TencentBlueKing/bk-ci/issues/10374)
##### Agent
- [Fix] Occasional 142 issues when starting the build process in windwos [link](http://github.com/TencentBlueKing/bk-ci/issues/10179)
##### Other
- [Optimization] The method for getting the db cluster name supports configurable db cluster list [link](http://github.com/TencentBlueKing/bk-ci/issues/10372)

#### Fixes
##### Pipeline
- [Fix] Fix the slow logic that may occur when canceling a running build [link](http://github.com/TencentBlueKing/bk-ci/issues/10874)
- [Fix] Manual review should not be notified if the notification method is not checked [link](http://github.com/TencentBlueKing/bk-ci/issues/10183)
- [Fix] The matrix manually skipped by the front end still runs when triggered [link](http://github.com/TencentBlueKing/bk-ci/issues/10751)
- [Fix] New build details page plugin rendering issue fixed [link](http://github.com/TencentBlueKing/bk-ci/issues/9185)
- [Fix] The git event trigger plugin supports third-party service changeFiles value is always null [link](http://github.com/TencentBlueKing/bk-ci/issues/10255)
- [Fix] Debug record query issue of build history interface [link](http://github.com/TencentBlueKing/bk-ci/issues/10814)
- [Fix] Pipeline trigger configuration can be edited when viewing [link](http://github.com/TencentBlueKing/bk-ci/issues/10827)
- [Fix] File type variable issue fixed [link](http://github.com/TencentBlueKing/bk-ci/issues/10822)
- [Fix] After the pipeline job is started asynchronously, the user cancels the pipeline immediately, and the asynchronous startup exception causes the pipeline status refresh exception [link](http://github.com/TencentBlueKing/bk-ci/issues/10816)
- [Fix] Assigning multiple containers to a job to concurrently execute business logic will cause the build to be canceled [link](http://github.com/TencentBlueKing/bk-ci/issues/10517)
- [Fix] The artifact page of the archived component is displayed incorrectly, the path is incomplete, and the file size is missing [link](http://github.com/TencentBlueKing/bk-ci/issues/10667)
- [Fix] Fix the concurrency issue when checking the matrix code [link](http://github.com/TencentBlueKing/bk-ci/issues/10771)
- [Fix] The branch variable value is incorrect when the stream pipeline MR is triggered [link](http://github.com/TencentBlueKing/bk-ci/issues/10707)
- [Fix] Sometimes the build is not completely completed after canceling the final stage [link](http://github.com/TencentBlueKing/bk-ci/issues/10619)
- [Fix] Archive report plugin creation token is not implemented [link](http://github.com/TencentBlueKing/bk-ci/issues/10693)
- [Fix] The cooperative version of Workbee force push fails to trigger the pipeline [link](http://github.com/TencentBlueKing/bk-ci/issues/10680)
- [Fix] Permission issue when saving pipeline templates [link](http://github.com/TencentBlueKing/bk-ci/issues/10681)
- [Fix] Ignore worker bee webhook test request [link](http://github.com/TencentBlueKing/bk-ci/issues/10666)
- [Fix] After the pipeline is deleted, the executing task is not terminated [link](http://github.com/TencentBlueKing/bk-ci/issues/8483)
- [Fix] Some display issues on the new details page [link](http://github.com/TencentBlueKing/bk-ci/issues/10557)
- [Fix] The draft version returned in the front-end detail interface is incorrect [link](http://github.com/TencentBlueKing/bk-ci/issues/10545)
- [Fix] The previous cancellation status causes the finally stage to end abnormally [link](http://github.com/TencentBlueKing/bk-ci/issues/10533)
- [Fix] Delete pipeline interface exception [link](http://github.com/TencentBlueKing/bk-ci/issues/10542)
- [Fix] New details page display issue fixed [link](http://github.com/TencentBlueKing/bk-ci/issues/10395)
- [Fix] Solve the problem of inconsistent value types of stage audit parameters [link](http://github.com/TencentBlueKing/bk-ci/issues/10095)
- [Fix] Recycle Bin search is unavailable [link](http://github.com/TencentBlueKing/bk-ci/issues/8440)
- [Fix] The sub-pipeline plugin execution timed out, but the sub-pipeline was not stopped [link](http://github.com/TencentBlueKing/bk-ci/issues/10331)
- [Fix] Pipeline version save records were not cleaned up in time [link](http://github.com/TencentBlueKing/bk-ci/issues/10244)
- [Fix] Variable read-only causes inability to rewrite [link](http://github.com/TencentBlueKing/bk-ci/issues/10245)
##### Code Repository
- [Fix] The project name of the associated code library that has been associated with the pac is not cleared after closing the pop-up window [link](http://github.com/TencentBlueKing/bk-ci/issues/8146)
##### Project management
- [Fix] Open source community, the open source version of the project management interface needs to be authorized [link](http://github.com/TencentBlueKing/bk-ci/issues/10382)
- [Fix] The front end of the community version of the simple permission center should hide the maximum authorization scope [ link ](http://github.com/TencentBlueKing/bk-ci/issues/10040)
- [Fix] Serialization comparison issue of the maximum authorized scope of the project [link](http://github.com/TencentBlueKing/bk-ci/issues/10649)
- [Fix] Disabled projects should not count the number of users [link](http://github.com/TencentBlueKing/bk-ci/issues/10634)
- [Fix] Fixed the incorrect grayscale label setting on the CodeCC platform [link](http://github.com/TencentBlueKing/bk-ci/issues/10434)
##### Store
- [Fix] The first version of the store app is under testing. The query interface cannot query the app version under testing by instance ID [link](http://github.com/TencentBlueKing/bk-ci/issues/10691)
- [Fix] Lower the priority configuration of SampleFirstStoreHostDecorateImpl [link](http://github.com/TencentBlueKing/bk-ci/issues/10401)
- [Fix] [Community] Listing failure & white screen issue on pipeline execution page [v2.1.0+] [Link](http://github.com/TencentBlueKing/bk-ci/issues/10357)
- [Fix] Adjustment of international configuration of store general interface [link](http://github.com/TencentBlueKing/bk-ci/issues/10640)
- [Fix] The open source version of the plugin upgrade did not refresh the LATEST_TEST_FLAG flag status [link](http://github.com/TencentBlueKing/bk-ci/issues/10701)
##### Dispatch
- [Fix] Issue with executing matrix job with audit plugin in builder without compilation environment [link](http://github.com/TencentBlueKing/bk-ci/issues/10599)
- [Fix] Retrying rescheduling causes reuse to fail to unlock [link](http://github.com/TencentBlueKing/bk-ci/issues/10675)
##### Agent
- [Fix] Fixed the issue that arm64mac process cannot be cleaned up [link](http://github.com/TencentBlueKing/bk-ci/issues/10252)
- [Fix] Agent reuse has issues in pipeline retry scenarios [link](http://github.com/TencentBlueKing/bk-ci/issues/10877)
- [Fix] When the agent has no region information, there is no bkrepo gateway by default [link](http://github.com/TencentBlueKing/bk-ci/issues/10778)
- [Fix] Agent skips reuse lock when reusing peer nodes [link](http://github.com/TencentBlueKing/bk-ci/issues/10795)
- [Fix] Agent cannot exit the queue after cancellation during reuse [link](http://github.com/TencentBlueKing/bk-ci/issues/10589)
##### Other
- [Fix] Failed to start the process service in version 2.1 [link](http://github.com/TencentBlueKing/bk-ci/issues/10271)
- [Fix] Synchronize difference code [link](http://github.com/TencentBlueKing/bk-ci/issues/10319)
- [Fix] Fix npm dependency vulnerability [link](http://github.com/TencentBlueKing/bk-ci/issues/10604)

# v3.0.0-rc.1
## 2024-09-10
### Changelog since v2.1.0
#### New
##### Pipeline
- Pipeline as code
  - [New] feat: Draft version UI display [link](http://github.com/TencentBlueKing/bk-ci/issues/9861)
  - [New] Pipeline version management mechanism [link](http://github.com/TencentBlueKing/bk-ci/issues/8161)
  - [New] [PAC] feat: Code bases with PAC mode enabled support automatic synchronization of code base YAML changes to Devops [link](http://github.com/TencentBlueKing/bk-ci/issues/8130)
  - [New] pac ui editing pipeline [link](http://github.com/TencentBlueKing/bk-ci/issues/8125)
  - [New] Optimization of the issue that the output variables are not obtained in the variable panel of the pipeline created in Code mode [link](http://github.com/TencentBlueKing/bk-ci/issues/10755)
  - [New] Support debugging pipeline when creating/editing pipeline [link](http://github.com/TencentBlueKing/bk-ci/issues/8164)
  - [New] Context usage scope limitation [link](http://github.com/TencentBlueKing/bk-ci/issues/10655)
  - [New] [PAC] feat: Pipeline constant Code syntax and specifications [link](http://github.com/TencentBlueKing/bk-ci/issues/9971)
  - [New] Release pipeline page "static" pipeline group optimization [link](http://github.com/TencentBlueKing/bk-ci/issues/9962)
  - [New] Dynamic pipeline groups support conditional grouping based on the first-level directory under code base/.ci [link](http://github.com/TencentBlueKing/bk-ci/issues/9682)
  - [New] [PAC] feat: Support disabling pipeline in code [link](http://github.com/TencentBlueKing/bk-ci/issues/9788)
  - [New] Record operation logs during pipeline maintenance [link](http://github.com/TencentBlueKing/bk-ci/issues/8197)
  - [New] [PAC] Reuse build resource pool across projects, support Code configuration [link](http://github.com/TencentBlueKing/bk-ci/issues/10225)
  - [New] [PAC] feat: Custom build number format supports Code definition [link](http://github.com/TencentBlueKing/bk-ci/issues/10210)
  - [New] Edit variable interactive optimization [link](http://github.com/TencentBlueKing/bk-ci/issues/9652)
  - [New] The pipeline build details page supports one-click expansion/collapse of jobs [link](http://github.com/TencentBlueKing/bk-ci/issues/9775)
  - [New] Support Devops new expression running conditions [link](http://github.com/TencentBlueKing/bk-ci/issues/10467)
  - [New] Release pipeline page, add instructions for PAC mode [link](http://github.com/TencentBlueKing/bk-ci/issues/10482)
  - [New] [PAC] Impact of code conversion on API users [link](http://github.com/TencentBlueKing/bk-ci/issues/9813)
  - [New] Debug record prompts and entry optimization [link](http://github.com/TencentBlueKing/bk-ci/issues/10720)
  - [New] Pipeline variables support manual drag and drop to adjust the order [link](http://github.com/TencentBlueKing/bk-ci/issues/10458)
  - [New] Pipeline notes support contextual setting and reference [link](http://github.com/TencentBlueKing/bk-ci/issues/10459)
  - [New] Pulling components supports pipeline debugging mode [link](http://github.com/TencentBlueKing/bk-ci/issues/10291 )
  - [New] [PAC] feat: View pipeline [link](http://github.com/TencentBlueKing/bk-ci/issues/8195)
- [New] Support pipeline indicator monitoring [link](http://github.com/TencentBlueKing/bk-ci/issues/9860)
  - [New] Refactoring of pipeline permission proxy function [link](http://github.com/TencentBlueKing/bk-ci/issues/10356)
- [New] Added permission holder variables [link](http://github.com/TencentBlueKing/bk-ci/issues/10890)
- [New] Optimize pipeline template settings [link](http://github.com/TencentBlueKing/bk-ci/issues/10857)
- [New] Pipeline execution history supports filtering by triggerer [link](http://github.com/TencentBlueKing/bk-ci/issues/10752)
- [New] Interaction optimization when pipeline notification mode is not effective [link](http://github.com/TencentBlueKing/bk-ci/issues/10615)
- [New] Worker Bee MR trigger supports setting listening actions [link](http://github.com/TencentBlueKing/bk-ci/issues/8949)
- [New] MR event trigger support WIP [link](http://github.com/TencentBlueKing/bk-ci/issues/10683)
- [New] P4 trigger supports Code writing [link](http://github.com/TencentBlueKing/bk-ci/issues/10551)
- [New] Git event trigger custom trigger conditions support definition through Code [link](http://github.com/TencentBlueKing/bk-ci/issues/10497)
- [New] Pipeline log color optimization [link](http://github.com/TencentBlueKing/bk-ci/issues/9934)
- [New] When openapi triggers the pipeline to run, support passing in trigger materials [link](http://github.com/TencentBlueKing/bk-ci/issues/10302)
- [New] Logs need to display special characters [link](http://github.com/TencentBlueKing/bk-ci/issues/10097)
- [New] Pipeline renaming optimization [link](http://github.com/TencentBlueKing/bk-ci/issues/10399)
- [New] Added fallback logic to the path matching rules triggered by SVN events [link](http://github.com/TencentBlueKing/bk-ci/issues/10510)
- [New] Added "Execution time" field to the pipeline execution history list [link](http://github.com/TencentBlueKing/bk-ci/issues/10251)
- [New] [Devops-Product-Reviewed] Pipeline supports displaying running progress [link](http://github.com/TencentBlueKing/bk-ci/issues/7932)
- [New] Build history list supports displaying build information fields [link](http://github.com/TencentBlueKing/bk-ci/issues/10724)
- [New] Pipeline supports exporting POJO attributes in order [link](http://github.com/TencentBlueKing/bk-ci/issues/10728)
- [New] Optimize the variables of pipeline "file" type [link](http://github.com/TencentBlueKing/bk-ci/issues/10400)
- [New] Scheduled triggers support specifying code bases and branches [link](http://github.com/TencentBlueKing/bk-ci/issues/10300)
- [New] Pipeline template management editing and instance management optimization [link](http://github.com/TencentBlueKing/bk-ci/issues/10626)
- [New] Verify the permissions of the referenced sub-pipeline when saving the pipeline [link](http://github.com/TencentBlueKing/bk-ci/issues/10259)
- [New] Dynamic configuration management of pipeline engine [link](http://github.com/TencentBlueKing/bk-ci/issues/10647)
- [New] Support viewing the status of asynchronously executed sub-pipelines in the parent pipeline [link](http://github.com/TencentBlueKing/bk-ci/issues/10260)
- [New] When adding drop-down/checkbox type variables, predefined options support batch input and key input [link](http://github.com/TencentBlueKing/bk-ci/issues/10290)
- [New] Complete the list of built-in variables [link](http://github.com/TencentBlueKing/bk-ci/issues/10436)
- [New] On the pipeline build details page, the time consumed by each job/step is directly displayed [link](http://github.com/TencentBlueKing/bk-ci/issues/10311)
- [New] Recycle Bin supports pipeline noun search [link](http://github.com/TencentBlueKing/bk-ci/issues/10408)
- [New] Optimize the display content of the pipeline list recently executed [link](http://github.com/TencentBlueKing/bk-ci/issues/10600)
- [New] Product download no response issue [link](http://github.com/TencentBlueKing/bk-ci/issues/10555)
- [New] Optimize the parameter passing method of sub-pipeline calling plugin [link](http://github.com/TencentBlueKing/bk-ci/issues/9943)
- [New] Fix the issue of missing concurrent grouping configuration in pipeline settings view page [link](http://github.com/TencentBlueKing/bk-ci/issues/10516)
- [New] Abnormal spaces in the copied log [link](http://github.com/TencentBlueKing/bk-ci/issues/10540)
- [New] Added pipeline version description, increased length limit [link](http://github.com/TencentBlueKing/bk-ci/issues/10520)
- [New] On the build details page, hovering over the version number will display the corresponding version description [link](http://github.com/TencentBlueKing/bk-ci/issues/10524)
##### Code Repository
- [New] When associating with the WorkerBee code base, support enabling Pipeline as Code mode [link](http://github.com/TencentBlueKing/bk-ci/issues/8115)
- [New] Code base optimization phase 1 function points [link](http://github.com/TencentBlueKing/bk-ci/issues/9347)
- [New] github pr check output quality red line report [link](http://github.com/TencentBlueKing/bk-ci/issues/10607)
- [New] [openapi] Link the code base to Devops's API to support enabling PAC [link](http://github.com/TencentBlueKing/bk-ci/issues/10770)
- [New] PAC mode enabled code base, supports disabling PAC [link](http://github.com/TencentBlueKing/bk-ci/issues/9993)
- [New] Optimize the display of code base trigger event results [link](http://github.com/TencentBlueKing/bk-ci/issues/10307)
- [New] github check run should support pipelines for GONGFENGSCAN channel [link](http://github.com/TencentBlueKing/bk-ci/issues/10704)
##### Quality Red Line
- [New] When there are multiple CodeCC plugins in the pipeline, the quality red line jump link should be able to jump to the corresponding task [ link ](http://github.com/TencentBlueKing/bk-ci/issues/10605)
- [New] quality added matchRuleList app interface [link](http://github.com/TencentBlueKing/bk-ci/issues/10610)
##### Environmental Management
- [New] Support disabling/enabling nodes in the build environment [link](http://github.com/TencentBlueKing/bk-ci/issues/10258)
- [New] Clean up the online and offline records of third-party build machines [link](http://github.com/TencentBlueKing/bk-ci/issues/10237)
- [New] After installing the WINDOWS build machine and clicking install.bat to complete the installation, the refresh node does not display [link](http://github.com/TencentBlueKing/bk-ci/issues/10725)
- [New] Support batch installation of Agent [link](http://github.com/TencentBlueKing/bk-ci/issues/10024)
##### Permission Center
- [New] Support administrators to view project members [link](http://github.com/TencentBlueKing/bk-ci/issues/9620)
- [New] User group related interface optimization [link](http://github.com/TencentBlueKing/bk-ci/issues/10463)
- [New] Pull user list based on organization ID [link](http://github.com/TencentBlueKing/bk-ci/issues/10513)
- [New] Optimize the permission application page [link](http://github.com/TencentBlueKing/bk-ci/issues/10145)
##### Project management
- [New] Optimization of the problem that the name of the operational product is not displayed on the project view page [link](http://github.com/TencentBlueKing/bk-ci/issues/10668)
- [New] Added project-level event callback [link](http://github.com/TencentBlueKing/bk-ci/issues/10146)
##### Store
- [New] Support plugin developers to set the default timeout and default failure strategy [link](http://github.com/TencentBlueKing/bk-ci/issues/10019)
- [New] Added interface for modifying store component initialization project [link](http://github.com/TencentBlueKing/bk-ci/issues/10126)
- [New] Retry when the plugin fails to upload a file [link](http://github.com/TencentBlueKing/bk-ci/issues/10214)
- [New] Store - Workbench - Container Image, the status icon is misplaced when verification fails [link](http://github.com/TencentBlueKing/bk-ci/issues/10696)
- [New] Fixed the issue that the associated debug project information was not deleted when updating the component associated initialization project information [link](http://github.com/TencentBlueKing/bk-ci/issues/10621)
- [New] Integrate micro-extension resource scheduling capabilities [link](http://github.com/TencentBlueKing/bk-ci/issues/10122)
##### Log Service
- [New] Add subtag query conditions to the Log Service interface [link](http://github.com/TencentBlueKing/bk-ci/issues/10536)
##### Dispatch
- [New] Optimize the dockerhost dockerRun container log acquisition interface [link](http://github.com/TencentBlueKing/bk-ci/issues/10811)
- [New] kubernetes-manager supports docker inspect image [link](http://github.com/TencentBlueKing/bk-ci/issues/8862)
- [New] The build environment Agent concurrency limit is 0 and does not take effect [link](http://github.com/TencentBlueKing/bk-ci/issues/10740)
- [New] Supports specifying the number of concurrent jobs when the build resource type is a third-party build cluster [link](http://github.com/TencentBlueKing/bk-ci/issues/9810)
- [New] Adjust the default container timeout of dockerhost [link](http://github.com/TencentBlueKing/bk-ci/issues/10645)
- [New] Optimization of third-party build machine build resource locking strategy [link](http://github.com/TencentBlueKing/bk-ci/issues/10449)
- [New] Get the maximum concurrent job execution/project active user measurement data [link](http://github.com/TencentBlueKing/bk-ci/issues/10232)
##### Agent
- [New] Worker kills the parent process of the current process, causing Agent to report a false positive [link](http://github.com/TencentBlueKing/bk-ci/issues/10362)
- [New] Warning for repeated installations with the same ID but different IP addresses when Agent is started [link](http://github.com/TencentBlueKing/bk-ci/issues/10264)
- [New] Agent cleanup process to back up workers [link](http://github.com/TencentBlueKing/bk-ci/issues/10234)
##### Stream
- [New] [stream] Optimize the time consumption of large warehouse triggering [link](http://github.com/TencentBlueKing/bk-ci/issues/10861)
- [New] [stream] Optimize the trigger process and reduce the trigger duration [link](http://github.com/TencentBlueKing/bk-ci/issues/10753)
- [New] When stream starts CI, it is required to fill in the organizational structure and operation products [link](http://github.com/TencentBlueKing/bk-ci/issues/10231)
- [New] [stream] Added the function of getting group members [link](http://github.com/TencentBlueKing/bk-ci/issues/10711)
##### Gateway
- [New] The gateway can handle 302 abnormal jumps during auth_request [link](http://github.com/TencentBlueKing/bk-ci/issues/10295)
- [New] Gateway default tag is not hard-coded [link](http://github.com/TencentBlueKing/bk-ci/issues/10334)
##### Other
- [New] Compress http return json string [link](http://github.com/TencentBlueKing/bk-ci/issues/10323)
- [New] Changes in Blue Whale 7.2 version [link](http://github.com/TencentBlueKing/bk-ci/issues/10558)
- [New] SQL doc document update [link](http://github.com/TencentBlueKing/bk-ci/issues/9974)
- [New] bk-apigw interface authentication method adjustment [link](http://github.com/TencentBlueKing/bk-ci/issues/10802)
- [New] Fix swagger package scanning method [link](http://github.com/TencentBlueKing/bk-ci/issues/10806)
- [New] Global configuration title/footer/logo/favicon/product name [link](http://github.com/TencentBlueKing/bk-ci/issues/10678)
- [New] Devops Gateway trusts the cors-header of the secure domain name [ link ](http://github.com/TencentBlueKing/bk-ci/issues/10767)
- [New] Fix iam initialization script [link](http://github.com/TencentBlueKing/bk-ci/issues/10658)
- [New] Add text when openapi access is not authorized [link](http://github.com/TencentBlueKing/bk-ci/issues/10638)
- [New] Interaction optimization when dependent services are not deployed [link](http://github.com/TencentBlueKing/bk-ci/issues/10612)
- [New] Improve the speed of rolling release [link](http://github.com/TencentBlueKing/bk-ci/issues/10236)
- [New] Optimize audit related logic [link](http://github.com/TencentBlueKing/bk-ci/issues/10671)
- [New] Optimize open interface section verification [link](http://github.com/TencentBlueKing/bk-ci/issues/10426)

#### Optimization
##### Pipeline
- [Optimization] Optimize the pipeline execution history table [link](http://github.com/TencentBlueKing/bk-ci/issues/10769)
- [Optimization] The pipeline instance copy function does not copy the parameter values of the corresponding instance [link](http://github.com/TencentBlueKing/bk-ci/issues/10580)
- [Optimization] The expression parser adds compatibility with pipeline variable processing [link](http://github.com/TencentBlueKing/bk-ci/issues/10609)
- [Optimization] Disable pipeline function optimization [link](http://github.com/TencentBlueKing/bk-ci/issues/8190)
- [Optimization] UI mode add/edit variable page revision [link](http://github.com/TencentBlueKing/bk-ci/issues/8185)
- [Optimization] Plugin execution error code optimization [link](http://github.com/TencentBlueKing/bk-ci/issues/10326)
##### Environmental Management
- [Optimization] Add some error codes to environment management [link](http://github.com/TencentBlueKing/bk-ci/issues/10788)
- [Optimization] Optimization of some environment management codes [link](http://github.com/TencentBlueKing/bk-ci/issues/10641)
- [Optimization] er: Optimization of some code of environment management 2 [link](http://github.com/TencentBlueKing/bk-ci/issues/10263)
##### Store
- [Optimization] Support Java plugin target reference variables to set the jar package execution path [link](http://github.com/TencentBlueKing/bk-ci/issues/10643)
- [Optimization] Optimization of permission verification of sensitive interfaces in store [link](http://github.com/TencentBlueKing/bk-ci/issues/10418)
- [Optimization] The store plug-in supports specifying running parameters through the execution.target field in task.json [link](http://github.com/TencentBlueKing/bk-ci/issues/10072)
- [Optimization] store universal interface encapsulation [link](http://github.com/TencentBlueKing/bk-ci/issues/10123)
- [Optimization] store logo upload does not support svg images yet to prevent xss attacks [link](http://github.com/TencentBlueKing/bk-ci/issues/10374)
##### Agent
- [Fix] Occasional 142 issues when starting the build process in windwos [link](http://github.com/TencentBlueKing/bk-ci/issues/10179)
##### Other
- [Optimization] The method for obtaining the db cluster name supports configurable db cluster list [link](http://github.com/TencentBlueKing/bk-ci/issues/10372)

#### Fixes
##### Pipeline
- [Fix] Fix the slow logic that may occur when canceling a running build [link](http://github.com/TencentBlueKing/bk-ci/issues/10874)
- [Fix] Manual review should not be notified if the notification method is not checked [link](http://github.com/TencentBlueKing/bk-ci/issues/10183)
- [Fix] The matrix manually skipped by the front end still runs when triggered [link](http://github.com/TencentBlueKing/bk-ci/issues/10751)
- [Fix] New build details page plugin rendering issue fixed [link](http://github.com/TencentBlueKing/bk-ci/issues/9185)
- [Fix] The git event trigger plugin supports third-party service changeFiles value is always null [link](http://github.com/TencentBlueKing/bk-ci/issues/10255)
- [Fix] Debug record query issue of build history interface [link](http://github.com/TencentBlueKing/bk-ci/issues/10814)
- [Fix] Pipeline trigger configuration can be edited when viewing [link](http://github.com/TencentBlueKing/bk-ci/issues/10827)
- [Fix] File type variable issue fixed [link](http://github.com/TencentBlueKing/bk-ci/issues/10822)
- [Fix] After the pipeline job is started asynchronously, the user cancels the pipeline immediately, and the asynchronous startup exception causes the pipeline status refresh exception [link](http://github.com/TencentBlueKing/bk-ci/issues/10816)
- [Fix] Assigning multiple containers to a job to concurrently execute business logic will cause the build to be canceled [link](http://github.com/TencentBlueKing/bk-ci/issues/10517)
- [Fix] The artifact page of the archived component is displayed incorrectly, the path is incomplete, and the file size is missing [link](http://github.com/TencentBlueKing/bk-ci/issues/10667)
- [Fix] Fix the concurrency issue when checking the matrix code [link](http://github.com/TencentBlueKing/bk-ci/issues/10771)
- [Fix] The branch variable value is incorrect when the stream pipeline MR is triggered [link](http://github.com/TencentBlueKing/bk-ci/issues/10707)
- [Fix] Sometimes the build is not completely completed after canceling the final stage [link](http://github.com/TencentBlueKing/bk-ci/issues/10619)
- [Fix] Archive report plugin creation token is not implemented [link](http://github.com/TencentBlueKing/bk-ci/issues/10693)
- [Fix] The cooperative version of Workbee force push fails to trigger the pipeline [link](http://github.com/TencentBlueKing/bk-ci/issues/10680)
- [Fix] Permission issue when saving pipeline templates [link](http://github.com/TencentBlueKing/bk-ci/issues/10681)
- [Fix] Ignore worker bee webhook test request [link](http://github.com/TencentBlueKing/bk- ci/issues/10666)
- [Fix] After the pipeline is deleted, the executing task is not terminated [link](http://github.com/TencentBlueKing/bk-ci/issues/8483)
- [Fix] Some display issues on the new details page [link](http://github.com/TencentBlueKing/bk-ci/issues/10557)
- [Fix] The draft version returned in the front-end detail interface is incorrect [link](http://github.com/TencentBlueKing/bk-ci/issues/10545)
- [Fix] The previous order cancellation status causes the finally stage to end abnormally [link](http://github.com/TencentBlueKing/bk-ci/issues/10533)
- [Fix] Delete pipeline interface exception [link](http://github.com/TencentBlueKing/bk-ci/issues/10542)
- [Fix] New details page display issue fixed [link](http://github.com/TencentBlueKing/bk-ci/issues/10395)
- [Fix] Solve the problem of inconsistent value types of stage audit parameters [link](http://github.com/TencentBlueKing/bk-ci/issues/10095)
- [Fix] Recycle Bin search is unavailable [link](http://github.com/TencentBlueKing/bk-ci/issues/8440)
- [Fix] The sub-pipeline plugin execution timed out, but the sub-pipeline was not stopped [link](http://github.com/TencentBlueKing/bk-ci/issues/10331)
- [Fix] Pipeline version save records were not cleaned up in time [link](http://github.com/TencentBlueKing/bk-ci/issues/10244)
- [Fix] Variable read-only causes inability to rewrite [link](http://github.com/TencentBlueKing/bk-ci/issues/10245)
##### Code Repository
- [Fix] The project name of the associated code library that has been associated with PAC is not cleared after closing the pop-up window [link](http://github.com/TencentBlueKing/bk-ci/issues/8146)
##### Project management
- [Fix] Open source community, the permissions of the open source version of the project management interface need to be released [link](http://github.com/TencentBlueKing/bk-ci/issues/10382)
- [Fix] The front end of the community version of the simple permission center should hide the maximum authorization scope [link](http://github.com/TencentBlueKing/bk-ci/issues/10040)
- [Fix] Serialization comparison issue of the maximum authorized scope of the project [link](http://github.com/TencentBlueKing/bk-ci/issues/10649)
- [Fix] Disabled projects should not count the number of users [link](http://github.com/TencentBlueKing/bk-ci/issues/10634)
- [Fix] Fixed the incorrect grayscale label setting on the CodeCC platform [link](http://github.com/TencentBlueKing/bk-ci/issues/10434)
##### Store
- [Fix] The first version of the store app is under testing, and the query interface cannot query the app version under testing by instance ID [link](http://github.com/TencentBlueKing/bk-ci/issues/10691)
- [Fix] Lower the priority configuration of SampleFirstStoreHostDecorateImpl [link](http://github.com/TencentBlueKing/bk-ci/issues/10401)
- [Fix] [Community] Listing failure & white screen issue on pipeline execution page [v2.1.0+] [Link](http://github.com/TencentBlueKing/bk-ci/issues/10357)
- [Fix] Adjustment of international configuration of store general interface [link](http://github.com/TencentBlueKing/bk-ci/issues/10640)
- [Fix] The open source version of the plugin upgrade did not refresh the LATEST_TEST_FLAG flag status [link](http://github.com/TencentBlueKing/bk-ci/issues/10701)
##### Dispatch
- [Fix] Issue with executing matrix job with audit plugin in builder without compilation environment [link](http://github.com/TencentBlueKing/bk-ci/issues/10599)
- [Fix] Retrying rescheduling causes reuse to fail to unlock [link](http://github.com/TencentBlueKing/bk-ci/issues/10675)
##### Agent
- [Fix] Fixed the issue that arm64mac process cannot be cleaned up [link](http://github.com/TencentBlueKing/bk-ci/issues/10252)
- [Fix] Agent reuse has issues in pipeline retry scenarios [link](http://github.com/TencentBlueKing/bk-ci/issues/10877)
- [Fix] When the agent has no region information, there is no bkrepo gateway by default [link](http://github.com/TencentBlueKing/bk-ci/issues/10778)
- [Fix] Agent skips reuse lock when reusing peer nodes [link](http://github.com/TencentBlueKing/bk-ci/issues/10795)
- [Fix] Agent cannot exit the queue after cancellation during reuse [link](http://github.com/TencentBlueKing/bk-ci/issues/10589)
##### Other
- [Fix] Failed to start the process service in version 2.1 [link](http://github.com/TencentBlueKing/bk-ci/issues/10271)
- [Fix] Synchronize difference code [link](http://github.com/TencentBlueKing/bk-ci/issues/10319)
- [Fix] Fix npm dependency vulnerability [link](http://github.com/TencentBlueKing/bk-ci/issues/10604)
