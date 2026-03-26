# Tasks: Add "Reuse Parameters Execute" Feature

## 1. Implementation

### 1.1 Store Layer
- [x] 1.1.1 Add `tempParamSet: null` state field in `store/modules/atom/index.js`
- [x] 1.1.2 Add `SET_TEMP_PARAM_SET` constant in `constants.js`
- [x] 1.1.3 Add `SET_TEMP_PARAM_SET` mutation in `mutations.js`
- [x] 1.1.4 Add `setTempParamSet` action in `actions.js`

### 1.2 Constants
- [x] 1.2.1 Add `TEMP_PARAM_SET_ID` constant to `utils/pipelineConst.js`

### 1.3 Detail Header Component
- [x] 1.3.1 Add "复用参数执行" button to `DetailHeader.vue` in "more actions" dropdown (next to execute button)
- [x] 1.3.2 Add permission check using `v-perm` directive for execute permission
- [x] 1.3.3 Implement `handleReuseParamsExecute()` method:
  - Fetch build params using `fetchBuildParamsByBuildId` action
  - Create temp paramSet object with `id: TEMP_PARAM_SET_ID`, `isTemp: true`
  - Name format: `复用参数执行 - #${buildNum}`
  - Dispatch `setTempParamSet` action to store temp paramSet
  - Navigate to execute preview page
- [x] 1.3.4 Add separate `reuseParamsLoading` state for loading indicator
- [x] 1.3.5 Add styles for `.more-action-dropdown-trigger` and `.more-action-dropdown-content`

### 1.4 Execute Preview Page
- [x] 1.4.1 Map `tempParamSet` from store state
- [x] 1.4.2 Clear temp paramSet on `beforeDestroy` using `setTempParamSet(null)`

### 1.5 ParamSet Component
- [x] 1.5.1 Import `TEMP_PARAM_SET_ID` from `pipelineConst.js`
- [x] 1.5.2 Add computed `tempParamSet` to get from store's `state.atom.tempParamSet`
- [x] 1.5.3 Display temp paramSet in "最近使用" group with `disableEdit: true`
- [x] 1.5.4 Only show "最近使用" group when `useLastParams` is true OR `tempParamSet` exists
- [x] 1.5.5 Auto-select temp paramSet in `onMounted` if it exists
- [x] 1.5.6 Handle temp paramSet in `applyParamSet` function (combined with LAST_USED logic)
- [x] 1.5.7 Make `LAST_USED_SET` a computed property to always use current `props.allParams`

### 1.6 Localization
- [x] 1.6.1 Add i18n keys to `locale/pipeline/zh-CN.json`: `reuseParamsExecute`, `reuseParamsExecuteTips`, `reuseParamsExecuteFailed`
- [x] 1.6.2 Add i18n keys to `locale/pipeline/en-US.json`
- [x] 1.6.3 Add i18n keys to `locale/pipeline/ja-JP.json`

### 1.7 Cleanup (removed from StartParams.vue)
- [x] 1.7.1 Remove "复用参数执行" button from `StartParams.vue`
- [x] 1.7.2 Remove related handler, imports, and styles

## 2. Verification
- [ ] 2.1 Verify button appears in "more" dropdown for users with execute permission
- [ ] 2.2 Verify button is visible even when pipeline is running
- [ ] 2.3 Verify temp paramSet is correctly stored in separate state field
- [ ] 2.4 Verify navigation to preview page auto-selects temp paramSet
- [ ] 2.5 Verify parameter values are pre-filled correctly
- [ ] 2.6 Verify temp paramSet cleanup when leaving preview without executing
- [ ] 2.7 Verify can switch between temp paramSet and LAST_USED_SET (when available)
