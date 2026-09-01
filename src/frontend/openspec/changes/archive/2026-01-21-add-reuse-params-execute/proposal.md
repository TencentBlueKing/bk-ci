# Change: Add "Reuse Parameters Execute" Feature

## Why
Users often want to re-run a pipeline with the same parameters from a previous build. Currently, they must manually copy and enter each parameter value. This feature provides a quick way to pre-fill startup parameters from an existing build, improving workflow efficiency.

**Related GitHub Issue:** [#12418 - feat：支持快速填充启动参数](https://github.com/TencentBlueKing/bk-ci/issues/12418)

## What Changes
- Add "复用参数执行" (Reuse Parameters Execute) button in the **"more actions" dropdown** of the Build Detail page header (next to the execute button)
- Button is always visible (even when pipeline is running), unlike rebuild dropdown
- When clicked:
  1. Fetch build parameters from the current build
  2. Create a temporary `paramSet` with name format: `复用参数执行 - #<buildNum>`
  3. Navigate to Execute Preview page
  4. Auto-select and apply the temporary paramSet
  5. Auto-fill parameter values from the source build
  6. Highlight parameters that differ from the current pipeline version's defaults
- Button only visible to users with **execute** permission
- Temporary paramSet is cleared when leaving the preview page

## Impact
- **Affected components:**
  - `devops-pipeline/src/components/PipelineHeader/DetailHeader.vue` - Add "more actions" dropdown with button
  - `devops-pipeline/src/views/subpages/preview.vue` - Handle temp paramSet cleanup
  - `devops-pipeline/src/components/ParamSet.vue` - Support temp paramSet display and auto-selection
  - `devops-pipeline/src/store/modules/atom/` - Add `tempParamSet` state field
  - `devops-pipeline/src/utils/pipelineConst.js` - Add `TEMP_PARAM_SET_ID` constant
- **No breaking changes** - This is an additive feature
- **Localization:** New i18n keys for zh-CN, en-US, and ja-JP
