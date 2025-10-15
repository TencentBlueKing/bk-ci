<template>
    <span
        class="param-set-selector"
    >
        <template v-if="onlySaveAsSet">
            <bk-button
                theme="primary"
                text
                @click="saveAsParamSet()"
            >
                <i class="devops-icon icon-save" />
                {{ $t("saveAsParamSet") }}
            </bk-button>
        </template>
        <template v-else>
            <bk-select
                class="param-set-selector-select"
                ref="paramSetSelector"
                v-model="paramSetId"
                searchable
                :placeholder="$t('oneKeyFill')"
            >
                <bk-option-group
                    v-for="group in paramSetGroup"
                    show-collapse
                    :name="group.name"
                    :key="group.name"
                >
                    <bk-option
                        v-for="option in group.children"
                        :key="option.id"
                        :id="option.id"
                        :name="option.name"
                    >
                        <span class="param-set-option-content">
                            {{ option.name }}
                            <i
                                class="devops-icon icon-cog"
                                @click.stop="showParamSetManageSlide(option.id)"
                            />
                        </span>
                    </bk-option>
                </bk-option-group>
            </bk-select>
            <bk-button
                class="param-set-selector-button"
                :disabled="isApplyed || !paramSetId"
                :loading="isApplying"
                @click="applyParamSet"
            >
                {{ $t('applyToYaml') }}
            </bk-button>
        </template>
        
        <bk-sideslider
            :is-show.sync="isShowParamSetManageSlide"
            :width="640"
            :close-on-click-modal="false"
            height="100%"
            :show-close="true"
            :before-close="beforeCloseSideSlider"
            @hidden="closeParamSetManageSlide"
            transfer
        >
            <header
                slot="header"
                class="param-set-manage-aside-header"
            >
                <span>
                    {{ $t('manageParamSets') }}
                </span>

                <bk-tag radius="20px">
                    {{ $t('totalParamSets', [paramSetList?.length]) }}
                </bk-tag>

                <span class="param-set-desc">
                    {{ $t('paramSetDescription') }}
                </span>

            </header>
            <div
                slot="content"
                class="param-set-manage-aside-content"
                v-bkloading="{ isLoading: isOperating }"
            >
                <aside>
                    <header class="param-set-manage-aside-left-header">
                        <bk-button
                            @click="addParamSet"
                            class="param-set-add-button"
                            icon="devops-icon icon-plus"
                            :disabled="isLoading"
                        >
                        
                        </bk-button>
                        <bk-input
                            right-icon="bk-icon icon-search"
                            :placeholder="$t('searchParamSetPlaceholder')"
                            clearable
                            :value="searchKeyword"
                            @enter="handleSearch"
                            @right-icon-click="handleSearch"
                            @clear="handleSearch"
                        />

                    </header>
                    <ul class="param-set-manage-list">
                        <li
                            v-for="(set, index) in paramSetList"
                            :class="{ active: set.id === activeSet?.id }"
                            @click="switchManageSet(index)"
                            :key="set.id"
                        >
                            <span
                                v-bk-overflow-tips
                                class="param-set-name"
                            >
                                {{ set.name }}
                            </span>
                            <span class="param-set-action-span">
                                <i
                                    class="devops-icon icon-edit-line"
                                    @click.stop="editParamSet(index)"
                                />
                                <i
                                    class="bk-icon icon-copy"
                                    @click.stop="copyParamSet(set)"
                                />
                                <i
                                    class="devops-icon icon-delete"
                                    @click.stop="deleteParamSet(index)"
                                />
                            </span>
                        </li>
                    </ul>
                </aside>
                <article v-bkloading="{ isLoading }">
                    <template v-if="isEditing">
                        <p class="param-set-edit-name">
                            <label>{{ $t('paramSetName') }}</label>
                            <bk-input
                                v-model="editingSet.name"
                            />
                        </p>

                        <h3 class="in-set-param-header">
                            {{ $t('paramList') }}
                            <bk-select

                                multiple
                                ext-cls="in-set-param-select"
                                :popover-width="300"
                                :value="editingSet.paramIds"
                                @change="handleCurrentParamSetChange"
                            >
                                <template #trigger>
                                    <span class="text-link">
                                        <i class="devops-icon icon-plus-circle" />
                                        {{ $t('addParamField') }}
                                    </span>

                                </template>
                                <bk-option-group
                                    v-for="group in allParamsGroup"
                                    :name="group.name"
                                    :key="group.name"
                                    show-collapse
                                    show-select-all
                                >
                                    <bk-option
                                        v-for="option in group.children"
                                        :key="option.id"
                                        :id="option.id"
                                        :name="option.name"
                                    >
                                    </bk-option>
                                </bk-option-group>
                            </bk-select>
                            
                        </h3>
                        <div class="param-set-form">
                            <pipeline-params-form
                                ref="paramsForm"
                                :param-values="paramsValues"
                                :handle-param-change="handleParamChange"
                                :params="editingSet.params"
                                sort-category
                                sort-category-vertical
                            >
                                <template
                                    slot="versionParams"
                                    v-if="isVisibleVersion"
                                >
                                    <renderSortCategoryParams :name="$t('preview.introVersion')">
                                        <template slot="content">
                                            <pipeline-versions-form
                                                ref="versionParamForm"
                                                :show-baseline="false"
                                                :handle-version-change="handleVersionChange"
                                                highlight-changed-param
                                                :version-param-values="versionParamValues"
                                                :version-param-list="editingSet.versionParams"
                                            />
                                        </template>
                                    </renderSortCategoryParams>
                                </template>
                            </pipeline-params-form>
                        </div>
                        <footer>
                            <bk-button
                                theme="primary"
                                @click="saveParamSet"
                            >
                                {{ $t('save') }}
                            </bk-button>
                            <bk-button @click="switchManageSet(activeSetIndex)">
                                {{ $t('cancel') }}
                            </bk-button>

                        </footer>
                    </template>
                    <div
                        class="param-set-form"
                        v-else-if="!!activeSet && !isLoading"
                    >
                        <param-group
                            v-if="activeSet?.versionParams?.length"
                            :show-header="false"
                            :editable="false"
                            v-bind="versionParamGroupObj"
                        />

                        <param-group
                            v-if="activeSet?.params?.length"
                            :show-header="false"
                            :editable="false"
                            v-bind="paramGroupObj"
                        />
                        
                        
                        <bk-exception
                            v-else
                            type="empty"
                            scene="part"
                        >
                            <p>{{ $t('noParamsAdded') }}</p>
                        </bk-exception>

                    </div>
                    
                </article>
            </div>
        </bk-sideslider>
    </span>
</template>

<script>
    import ParamGroup from '@/components/PipelineEditTabs/components/children/param-group'
    import PipelineParamsForm from '@/components/pipelineParamsForm.vue'
    import PipelineVersionsForm from '@/components/PipelineVersionsForm.vue'
    import renderSortCategoryParams from '@/components/renderSortCategoryParams'
    import {
        getParamsGroupByLabel
    } from '@/store/modules/atom/paramsConfig'
    import { allVersionKeyList } from '@/utils/pipelineConst'
    import { randomString } from '@/utils/util'
    import { computed, defineComponent, getCurrentInstance, nextTick, onBeforeMount, onMounted, ref, watch, } from 'vue'
    const UNGROUP_NAME = 'unGrouped'
    export default defineComponent({
        name: 'ParamSet',
        components: {
            ParamGroup,
            PipelineParamsForm,
            renderSortCategoryParams,
            PipelineVersionsForm
        },
        props: {
            isStartUp: {
                type: Boolean,
                default: false
            },
            buildNum: {
                type: String,
                default: ''
            },
            onlySaveAsSet: {
                type: Boolean,
                default: false
            },
            useLastParams: {
                type: Boolean,
                default: false
            },
            isVisibleVersion: {
                type: Boolean,
                default: false
            },
            allParams: {
                type: Array,
                default: []
            }
        },
        setup (props, ctx) {
            const { proxy } = getCurrentInstance()
            const isShowParamSetManageSlide = ref(false)
            const searchKeyword = ref('')
            const paramSetId = ref()
            const isEditing = ref(false)
            const activeSetIndex = ref(0)
            const editingSet = ref(null)
            const isApplyed = ref(false)
            const isApplying = ref(false)
            const isLoading = ref(false)
            const isOperating = ref(false)
            const paramSetSelector = ref(null)
            const DEFAULT_PARAM_SET = {
                name: proxy.$t('newParamSet'),
                params: []
            }

            const LAST_USED_SET = {
                id: 'LAST_USED',
                name: proxy.$t('lastUsedParams'),
                params: props.allParams
            }

           
            const activeSet = computed(() => {
                return paramSetList.value[activeSetIndex.value]
            })
            const filteredSets = computed(() => {
                return filterParams(proxy.$store.state.atom.paramSets, searchKeyword.value)
            })
            const paramSetList = computed(() => {
                return filteredSets.value.map(set => ({
                    ...set,
                    paramIds: set.params?.filter(param => !allVersionKeyList.includes(param.id)),
                    versionParams: set.params?.filter(param => allVersionKeyList.includes(param.id)).map(version => ({
                        ...version,
                        category: proxy.$t('versionNum'),

                        isDeleted: !props.isVisibleVersion,
                        defaultValue: version.value,
                    })),
                    params: set.params?.filter(param => !allVersionKeyList.includes(param.id)).map(param => ({
                        ...param,
                        isDeleted: !Object.prototype.hasOwnProperty.call(allParamsMap.value, param.id),
                        defaultValue: param.value,
                    })),
                }))
            })
            const paramSetGroup = computed(() => {
                return paramSetList.value.reduce((acc, set) => {
                    acc[1].children.push(set)
                    return acc
                }, [
                    {
                        name: proxy.$t('recentlyUsed'),
                        children: [
                            LAST_USED_SET
                        ]
                    }, {
                        name: proxy.$t('paramValueSets'),
                        children: []
                    }
                ])

            })
            const allParamsMap = computed(() => {
                return props.allParams.reduce((acc, param) => {
                    acc[param.id] = param
                    return acc
                }, {})
            })
            const allParamsGroup = computed(() => {
                return Object.values(props.allParams.filter(param => !allVersionKeyList.includes(param.id)).reduce((acc, param) => {
                    const category = param.category || UNGROUP_NAME
                    
                    const item = {
                        id: param.id,
                        name: param.id
                    }
                    if (acc[category]) {
                        acc[category].children.push(item)
                    } else {
                        acc[category] = {
                            name: category,
                            children: [item]
                        }
                    }
                    return acc
                },{})).sort((a, b) => {
                    if (a.name === UNGROUP_NAME) {
                        return -1
                    }
                    return a.name.localeCompare(b.name)
                })
            })

            const paramsValues = computed(() => {
                return editingSet.value.params.reduce((acc, param) => {
                    acc[param.id] = param.value
                    return acc
                }, {})
            })
            const paramGroupObj = computed(() => {
                return {
                    title: proxy.$t('newui.pipelineParam.buildParam'),
                    tips: proxy.$t('newui.pipelineParam.buildParamTips'),
                    listNum: activeSet.value?.params?.length,
                    listMap: getParamsGroupByLabel(activeSet.value?.params).listMap ?? {},
                    sortedCategories: getParamsGroupByLabel(activeSet.value?.params).sortedCategories ?? []
                }
            })
            const versionParamGroupObj = computed(() => {
                return {
                    title: proxy.$t('versionNum'),

                    tips: '--',
                    listNum: activeSet.value?.versionParams?.length,
                    listMap: getParamsGroupByLabel(activeSet.value?.versionParams).listMap ?? {},
                    sortedCategories: getParamsGroupByLabel(activeSet.value?.versionParams).sortedCategories ?? []
                }
            })

            const versionParamValues = computed(() => {
                return editingSet.value.versionParams?.reduce((acc, param) => {
                    acc[param.id] = param.value
                    return acc
                }, {}) ?? {}
            })

            watch(() => paramSetId.value, (newVal, oldVal) => {
                if (!oldVal) return
                isApplyed.value = false
            })

            onBeforeMount(() => {
                dispatch('fetchParamSets', proxy.$route.params)
            })

            onMounted(() => {
                if (props.useLastParams) {
                    paramSetId.value = LAST_USED_SET.id
                    applyParamSet()
                }
            })

            ctx.expose({
                saveAsParamSet,
                clear,
                closeParamSetManageSlide
            })

            async function handleSearch (val) {
                const result = await switchManageSet(-1)
                if (result) {
                    searchKeyword.value = val
                }
            }

            async function applyParamSet () {
                const set = paramSetId.value === LAST_USED_SET.id ? LAST_USED_SET : paramSetList.value.find(item => item.id === paramSetId.value)
                if (!set) {
                    return
                }
                if (!set.params?.length) {
                    isApplying.value = true
                    const allParams = await dispatch('fetchParamSetDetail', {
                        projectId: proxy.$route.params.projectId,
                        pipelineId: proxy.$route.params.pipelineId,
                        paramSetId: set.id
                    })
                    isApplying.value = false
                    set.params = allParams?.filter(param => !allVersionKeyList.includes(param.id))
                    set.versionParams = allParams?.filter(param => allVersionKeyList.includes(param.id))
                }
                isApplyed.value = true
                ctx.emit('change', set.name, set.params.reduce((acc, param) => {
                    acc[param.id] = param.value
                    return acc
                }, {}), set.versionParams?.reduce((acc, param) => {
                    acc[param.id] = param.value
                    return acc
                }, {}))
            }
            
            function showParamSetManageSlide (setId) {
                const pos = paramSetList.value.findIndex(item => item.id === setId)
                isShowParamSetManageSlide.value = true
                console.log(paramSetSelector.value?.close())
                if (pos >= 0 && setId) {
                    switchManageSet(pos)
                }
            }

            function closeParamSetManageSlide () {
                isShowParamSetManageSlide.value = false
                activeSetIndex.value = -1
                isEditing.value = false
                editingSet.value = null
            }

            async function switchManageSet (setIndex) {
                try {
                    let result = true
                    if (isEditing.value) {
                        result = await leaveConfirm()
                    }
                    if (!result) {
                        return false
                    }
                    const originSet = filteredSets.value[setIndex]
                    if (originSet && originSet.params == null) {
                        isLoading.value = true
                        await dispatch('fetchParamSetDetail', {
                            projectId: proxy.$route.params.projectId,
                            pipelineId: proxy.$route.params.pipelineId,
                            paramSetId: paramSetList.value[setIndex].id
                        })
                    }
                    activeSetIndex.value = setIndex
                    isEditing.value = false
                    editingSet.value = null
                    return true
                } catch (error) {
                    proxy.$bkMessage({
                        message: error.message,
                        theme: 'error'
                    })
                } finally {
                    isLoading.value = false
                }
            }

            function beforeCloseSideSlider () {
                if (isEditing.value) {
                    return leaveConfirm()
                }
                return true
            }

            async function leaveConfirm () {
                return new Promise((resolve, reject) => {
                    proxy.$bkInfo({
                        title: proxy.$t('editPage.confirmTitle'),
                        subTitle: proxy.$t('editPage.confirmMsg'),
                        confirmFn: () => {
                            if (isEditing.value && editingSet.value.isNew) {
                                deleteParamSet(activeSetIndex.value)
                            }
                            resolve(true)
                        },
                        cancelFn: () => {
                            resolve(false)
                        }
                    })
                })
            }

            function saveAsParamSet (params = props.allParams, values) {
                const newSet = {
                    ...DEFAULT_PARAM_SET,
                    name: props.isStartUp ? `SET_#${props.buildNum}` : `SET_${randomString(6)}`,
                    params: params.filter(param => param.required && !param.constant).map(param => ( {
                        ...param,
                        value: values?.[param.id] ?? param.value
                    })),
                    isNew: true
                }
                dispatch('addParamSet', newSet)
                switchManageSet(0)
                editingSet.value = {
                    ...newSet,
                    paramIds: newSet.params.filter(param => !allVersionKeyList.includes(param.id)).map(param => param.id),
                    params: newSet.params.filter(param => !allVersionKeyList.includes(param.id)),
                    versionParams: newSet.params.filter(param => allVersionKeyList.includes(param.id))
                }
                isEditing.value = true
                nextTick(() => {
                    showParamSetManageSlide()
                })
            }

            function addParamSet () {
                const newSet = {
                    ...DEFAULT_PARAM_SET,
                    params: [
                        ...(props.isVisibleVersion ? props.allParams.filter(param => allVersionKeyList.includes(param.id)) : [])
                    ],
                    isNew: true
                }
                dispatch('addParamSet', newSet)
                switchManageSet(0)
                editingSet.value = {
                    ...newSet,
                    paramIds: [],
                    params: [],
                    versionParams: props.isVisibleVersion ? newSet.params.filter(param => allVersionKeyList.includes(param.id)) : []
                }
                isEditing.value = true
            }

            async function editParamSet (setIndex) {
                await switchManageSet(setIndex)
                const set = paramSetList.value[setIndex]
                if (!set) {
                    return
                }
                editingSet.value = {
                    ...set
                }
                isEditing.value = true
            }
            async function copyParamSet (set) {
                const originSet = proxy.$store.state.atom.paramSets.find(item => item.id === set.id)
                if (!originSet) {
                    return
                }
                const { id, ...restInfo } = originSet
                const newSet = {
                    ...restInfo,
                    name: `${set.name}_copy`,
                    isNew: true
                }
                if (!restInfo.params?.length) {
                    const params = await dispatch('fetchParamSetDetail', {
                        projectId: proxy.$route.params.projectId,
                        pipelineId: proxy.$route.params.pipelineId,
                        paramSetId: id
                    })
                    newSet.params = params
                }
                dispatch('addParamSet', newSet)
                
                nextTick(() => {
                    editParamSet(0)
                })
            }
            async function deleteParamSet (setIndex) {
                const set = paramSetList.value[setIndex]
                if (!set) {
                    return
                }
                try {
                    isOperating.value = true
                    await dispatch('deleteParamSet', {
                        projectId: proxy.$route.params.projectId,
                        pipelineId: proxy.$route.params.pipelineId,
                        paramSetId: set.id,
                        isNew: set.isNew
                    })
                    
                } catch (error) {
                    proxy.$bkMessage({
                        theme: 'error',
                        message: error.message
                    })
                } finally {
                    isOperating.value = false
                }
            }

            function handleCurrentParamSetChange (ids) {
                editingSet.value.paramIds = ids
                editingSet.value.params = ids.map(id => editingSet.value.params.find(param => param.id === id) ?? allParamsMap.value[id])
            }

            function handleParamChange (paramId, value) {
                editingSet.value.params.map(param => {
                    if (param.id === paramId) {
                        param.defaultValue = value
                        param.value = value
                    }
                    return param
                })
            }

            function handleVersionChange (paramId, value) {
                editingSet.value.versionParams?.map(param => {
                    if (param.id === paramId) {
                        param.defaultValue = value
                        param.value = value
                    }
                    return param
                })
            }

            async function saveParamSet () {
                try {
                    const newSet = {
                        id: editingSet.value.id,
                        name: editingSet.value.name,
                        params: [
                            ...editingSet.value.params,
                            ...editingSet.value.versionParams
                        ],
                        isNew: editingSet.value.isNew
                    }
                    isLoading.value = true
                    const id = await dispatch('saveParamSet', {
                        projectId: proxy.$route.params.projectId,
                        pipelineId: proxy.$route.params.pipelineId,
                        paramSet: newSet
                    })
                    isLoading.value = false
                    isEditing.value = false
                    
                    dispatch('updateParamSet', editingSet.value.isNew ? Object.assign(newSet, {
                        id
                    }) : newSet)
                    nextTick(() => {
                        switchManageSet(activeSetIndex.value)
                    })
                } catch (error) {
                    proxy.$bkMessage({
                        theme: 'error',
                        message: error.message
                    })
                }
            }

            function dispatch (type, ...rest) {
                return proxy.$store.dispatch(`atom/${type}`, ...rest)
            }

            function filterParams (sets, keyword) {
                if (!keyword) {
                    return sets
                }
                return sets.filter(set => {
                    if (isMatch(set.name, keyword)) {
                        return true
                    }

                    if (set.params?.some(param => isMatch(param.id, keyword))) {
                        return true
                    }
                })
            }

            function isMatch (name, keyword) {
                if (typeof keyword !== 'string' || typeof name !== 'string' || keyword === '' || name === '') {
                    return false
                }
                return name.toLowerCase().indexOf(keyword.toLowerCase()) > -1
            }

            function clear () {
                paramSetId.value = ''
            }

            return {
                paramSetSelector,
                isLoading,
                paramSetList,
                paramSetGroup,
                paramSetId,
                searchKeyword,
                isOperating,
                showParamSetManageSlide,
                closeParamSetManageSlide,
                isShowParamSetManageSlide,
                isEditing,
                activeSetIndex,
                switchManageSet,
                editParamSet,
                copyParamSet,
                deleteParamSet,
                handleParamChange,
                handleVersionChange,
                handleCurrentParamSetChange,
                paramsValues,
                addParamSet,
                allParamsGroup,
                saveParamSet,
                saveAsParamSet,
                activeSet,
                editingSet,
                getParamsGroupByLabel,
                paramGroupObj,
                versionParamGroupObj,
                applyParamSet,
                versionParamValues,
                beforeCloseSideSlider,
                isApplyed,
                isApplying,
                handleSearch
            }
        }
    })
</script>

<style lang="scss" >
    @import "@/scss/mixins/ellipsis";
    .param-set-selector {
        display: flex;
        align-items: center;
        vertical-align: middle;
        margin: 1px 0 0 8px;
        .param-set-selector-select {
            width: 300px;
        }
        .param-set-selector-button {
            margin-left: -1px;
        }
    }
    .param-set-option-content {
        display: flex;
        justify-content: space-between;
        align-items: center;
        .icon-cog {
            display: none;
        }
        &:hover {
            .icon-cog {
                display: block;
            }
        }
    }
    .bk-sideslider-header .icon-angle-right {
        color: white !important;
    }
    .param-set-manage-aside-header {
        display: flex;
        align-items: center;
        font-size: 16px;
        .param-set-desc {
            font-size: 12px;
            align-self: flex-end;
            margin-left: auto;
            color: #999;
        }
    }
    .param-set-manage-aside-content {
        display: flex;
        height: calc(100vh - 52px);
        overflow: hidden;
        > aside {
            display: flex;
            flex-direction: column;
            width: 218px;
            background-color: #F5F7FA;
            .param-set-manage-aside-left-header {
                display: flex;
                justify-content: space-between;
                align-items: center;
                gap: 8px;
                padding: 16px 24px;
                .param-set-add-button {
                    flex-shrink: 0;
                    width: 32px;
                    height: 32px;
                    background: #FFFFFF;
                    border: 1px solid #3A84FF;
                    border-radius: 2px;
                    display: flex;
                    justify-content: center;
                    align-items: center;
                    cursor: pointer;
                    i {
                        font-size: 18px;
                        font-weight: bold;
                        color: #3A84FF;
                    }
                }
            }
            .param-set-manage-list {
                flex: 1;
                overflow-y: auto;
                scrollbar-gutter: stable;
                margin-top: 8px;
                > li {
                    display: flex;
                    align-items: center;
                    height: 40px;
                    padding: 0 24px;
                    position: relative;
                    color: #4D4F56;
                    cursor: pointer;
                    &:hover {
                        background: #EAEBF0;
                        .param-set-action-span {
                            display: flex;
                        }
                    }
                    &.active {
                        background: #FFFFFF;
                        &:before {
                            position: absolute;
                            left: 0;
                            top: 0;
                            content: '';
                            width: 3px;
                            height: 40px;
                            background: #3A84FF;
                        }
                        .param-set-name {
                            color: #3A84FF;
                        }
                    }
                    .param-set-name {
                        flex: 1;
                        @include ellipsis();
                        font-size: 14px;
                        color: #4D4F56;
                        margin-right: 8px;
                    }
                    .param-set-action-span {
                        display: none;
                        flex-shrink: 0;
                        justify-content: space-between;
                        align-items: center;
                        gap: 4px;
                        color:  #979BA5;
                    }
                }
            }
        }
        > article {
            display: flex;
            flex-direction: column;
            flex: 1;
            padding: 16px;
            overflow: hidden;
            .param-set-edit-name {
                display: flex;
                flex-direction: column;
                font-size: 12px;
                gap: 6px;
                color: #4D4F56;
            }
            .in-set-param-header {
                display: flex;
                justify-content: space-between;
                align-items: center;
                font-size: 14px;
                color: #4D4F56;
                .in-set-param-select {
                    border: 0;
                    box-shadow: none;;
                }
            }
            .param-set-form {
                flex: 1;
                padding-right: 12px;
                overflow-y: auto;
                scrollbar-gutter: stable both-edges;
            }
        }
    }
</style>