# Design: Reuse Parameters Execute

## Context
Users want to quickly re-run pipelines with the same parameters from previous builds without manually copying values. The existing ParamSet system already supports saving and applying parameter sets, but requires manual creation and selection.

## Goals / Non-Goals

**Goals:**
- One-click workflow to execute with previous build's parameters
- Button always visible (even when pipeline is running)
- Leverage existing ParamSet infrastructure
- Consistent UX with existing "一键填参" (One-key Fill) feature

**Non-Goals:**
- Server-side storage of temp paramSets (client-side only)
- Persisting temp paramSets across page refreshes
- Supporting reuse from archived builds

## Decisions

### 1. Separate `tempParamSet` State Field
**Decision:** Store temp paramSet in a separate `tempParamSet` state field instead of the `paramSets` array.

**Rationale:**
- `fetchParamSets` action replaces the entire `paramSets` array from API, which would wipe out any temp set added to it
- Separate state field survives the `fetchParamSets` call when preview page loads
- Cleaner separation between persisted sets (from API) and temporary sets (client-only)
- Simple cleanup by setting to `null`

### 2. Button Location: Detail Header "More" Dropdown
**Decision:** Place "复用参数执行" button in a new "more actions" dropdown in `DetailHeader.vue`, next to the execute button.

**Rationale:**
- Always visible regardless of pipeline running state (unlike rebuild dropdown which is hidden when running)
- Near execute button for logical grouping of execution-related actions
- Extensible for future "more actions" items

### 3. Data Flow
```
DetailHeader.vue                    Store (tempParamSet)             ParamSet.vue
     │                                    │                              │
     │ click "复用参数执行"               │                              │
     ├───► fetchBuildParamsByBuildId     │                              │
     │                                    │                              │
     ├───► setTempParamSet({             │                              │
     │       id: TEMP_PARAM_SET_ID,      │                              │
     │       name: '复用参数执行-#123',   │                              │
     │       params: [...buildParams],   │                              │
     │       isTemp: true                │                              │
     │     })                            │                              │
     │                                   ─┼──────────────────────────────►
     │ router.push(executePreview)       │                              │
     ├──────────────────────────────────►│                              │
     │                                   │   onMounted: tempParamSet?   │
     │                                   │◄──────────────────────────────┤
     │                                   │   → auto-select & apply      │
     │                                   │                              │
                                         │   beforeDestroy:             │
                                         │   setTempParamSet(null)      │
                                         │◄──────────────────────────────┤
                                    Preview.vue
```

### 4. Temp ParamSet Structure
```typescript
interface TempParamSet {
  id: TEMP_PARAM_SET_ID      // Constant from pipelineConst.js
  name: string               // "复用参数执行 - #<buildNum>"
  params: BuildParam[]       // Params from source build
  isTemp: true               // Flag to differentiate from saved sets
  sourceBuildNum: number     // Reference to source build
}
```

### 5. ParamSet Selector Display
Temp paramSet appears in "最近使用" (Recently Used) group:
```
最近使用
├── 上次使用的参数 (only when useLastParams=true)
└── 复用参数执行 - #123 (temp, with disableEdit)

参数组合
├── SET_ABC123
└── SET_DEF456
```

### 6. Virtual Set Handling
Both `LAST_USED_SET` and temp paramSet are "virtual sets" - not fetched from API, params attached directly.

Combined logic in `applyParamSet()`:
```javascript
const isVirtualSet = isLastUsed || isTempSet
if (isVirtualSet) {
    const sourceParams = isLastUsed ? props.allParams : (tempParamSet.value?.params ?? [])
    // ... apply params
}
```

## Risks / Trade-offs

| Risk | Mitigation |
|------|------------|
| Temp paramSet lost on refresh | Expected behavior; user can save as permanent set |
| State pollution | Clear temp on preview unmount using `setTempParamSet(null)` |
| Name collision | Use constant `TEMP_PARAM_SET_ID` from `pipelineConst.js` |
| fetchParamSets wiping temp | Separate `tempParamSet` state field survives API calls |

## Migration Plan
Not applicable - new feature, no migration needed.

## Open Questions
- [x] Should temp paramSet persist in localStorage? → No, keep it simple
- [x] Should we use paramSets array? → No, separate state to survive fetchParamSets
- [x] Where to place button? → Detail header "more" dropdown (always visible)
