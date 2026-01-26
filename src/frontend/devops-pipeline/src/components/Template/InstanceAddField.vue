<template>
    <div>
        <section
            class="batch-edit-content"
        >
            <bk-alert
                class="mb10"
                type="warning"
                :title="$t('template.batchEditInstanceTips')"
            />
            <bk-popover
                :width="400"
                placement="bottom-start"
                theme="dot-menu light"
                trigger="click"
                :transfer="true"
                class="add-field-popover"
            >
                <div class="add-field-btn">
                    <bk-icon
                        type="plus-circle-shape"
                        class="add-icon"
                    />
                    {{ $t('template.addField') }}
                </div>
                <div
                    slot="content"
                    class="popover-container"
                >
                    <bk-input
                        clearable
                        v-model="searchKey"
                        :right-icon="'bk-icon icon-search'"
                        :placeholder="$t('template.keyword')"
                        class="search-input"
                        @right-icon-click="handlerSearch"
                    />
                    <div class="container-groups">
                        <div
                            v-for="(group, groupIndex) in fieldMap"
                            :key="groupIndex"
                            class="group"
                        >
                            <div class="group-title">
                                <p>
                                    {{ group.title }}
                                    <span
                                        v-if="group.count"
                                        class="item-count"
                                    >
                                        （{{ group.count }}）
                                    </span>
                                </p>
                                <bk-checkbox
                                    :value="group.isAll"
                                    ext-cls="item-check"
                                    @change="(selected) => handleSelectAll(selected, group.data)"
                                >
                                    {{ $t('template.selectAll') }}
                                </bk-checkbox>
                            </div>
                            <div class="group-content">
                                <div
                                    v-for="key in group.data"
                                    :key="key"
                                    @click="toggleItemSelection(key)"
                                    :class="{ 'selected': selectedData.includes(key) }"
                                >
                                    {{ key }}
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </bk-popover>
            <div
                class="config-content"
            >
                <template v-if="renderParamList.length">
                    <section class="params-content-item">
                        <header
                            :class="['params-collapse-trigger', {
                                'params-collapse-expand': activeName.has(1)
                            }]"
                            @click="toggleCollapse(1)"
                        >
                            <bk-icon
                                type="right-shape"
                                class="icon-angle-right"
                            />
        
                            {{ $t('template.pipelineBuildParams') }}
                        </header>
                        <div
                            v-if="activeName.has(1)"
                            class="params-collapse-content"
                        >
                            <!-- show-operate-btn -->
                            <pipeline-params-form
                                ref="paramsForm"
                                :handle-param-change="handleParamChange"
                                :params="renderParamList"
                                :param-values="paramsValues"
                                sort-category
                                batch-edit-flag
                                :handle-set-parma-required="handleSetParmaRequired"
                            >
                                <template
                                    slot="versionParams"
                                    v-if="isVisibleVersion"
                                >
                                    <renderSortCategoryParams
                                        :name="$t('preview.introVersion')"
                                        default-layout
                                    >
                                        <template slot="content">
                                            <pipeline-versions-form
                                                class="mb20"
                                                ref="versionParamForm"
                                                :build-no="buildNo"
                                                is-preview
                                                :handle-version-change="handleParamChange"
                                                :version-param-list="versionParams"
                                            />
                                        </template>
                                    </renderSortCategoryParams>
                                </template>
                            </pipeline-params-form>
                        </div>
                    </section>
                </template>
            </div>
        </section>
        <footer
            v-if="selectedData.length"
            class="config-footer"
        >
            <bk-button
                theme="primary"
                @click="handleConfirm"
            >
                {{ $t('confirm') }}
            </bk-button>
            <bk-button
                @click="handleCancel"
            >
                {{ $t('cancel') }}
            </bk-button>
        </footer>
    </div>
</template>

<script setup>
    import { computed, readonly, ref } from 'vue'
    import { allVersionKeyList } from '@/utils/pipelineConst'
    import UseInstance from '@/hook/useInstance'
    import PipelineVersionsForm from '@/components/PipelineVersionsForm.vue'
    import PipelineParamsForm from '@/components/pipelineParamsForm.vue'
    import renderSortCategoryParams from '@/components/renderSortCategoryParams'
    import { getParamsValuesMap } from '@/utils/util'
    const props = defineProps({
        instanceList: Array
    })
    const { proxy } = UseInstance()
    const selectedData = ref([])
    const activeName = ref(new Set([1, 2, 3]))
    const curIndex = computed(() => proxy.$route.query?.index)
    const curInstance = computed(() => props.instanceList[curIndex.value - 1])
    const params = ref(curInstance.value?.param.map(p => ({
        ...p,
        isChange: false,
        isNew: false,
        isDelete: false,
        propertyUpdates: [],
        defaultValue: ''
    })) ?? [])
    const buildNo = ref(curInstance.value?.buildNo ?? {})
    const isVisibleVersion = computed(() => {
        // buildNo.value?.required
        return false
    })
    const paramsList = computed(() => {
        return params.value.filter(p => !p.constant && p.required && !allVersionKeyList.includes(p.id) && p.propertyType !== 'BUILD').map(p => ({
            ...p,
            label: `${p.id}${p.name ? `(${p.name})` : ''}`,
            defaultValue: p.defaultValue,
            readOnlyCheck: false,
            valueNotEmpty: false,
            isRequiredParam: p.required
        }))
    })
    const paramsValues = computed(() => {
        return getParamsValuesMap(paramsList.value, 'defaultValue')
    })
    const versionParams = computed(() => {
        return params.value.filter(p => allVersionKeyList.includes(p.id)).map(p => ({
            ...p,
            defaultValue: '',
            isRequiredParam: p.required
        }))
    })
    const sortParamList = computed(() => {
        // 将参数列表按照分组进行分组,未分组的参数放到一个分组里
        const key = proxy.$t('notGrouped')
        const listMap = paramsList.value.reduce((acc, item) => {
            const categoryKey = item.category || key
            if (!acc[categoryKey]) {
                acc[categoryKey] = []
            }
            acc[categoryKey].push(item)
            return acc
        }, {})
        if (!(key in listMap)) {
            return listMap
        }
        const { [key]: value, ...rest } = listMap
        return { [key]: value, ...rest }
    })
    const paramsField = computed(() => {
        return Object.entries(sortParamList.value).map(([title, items]) => ({
            id: 'params',
            title: title,
            isAll: false,
            count: paramsList.value.length,
            data: items.map(item => item.id)
        }))
    })
    const fieldMap = computed(() => {
        return [
            ...isVisibleVersion.value
                ? [
                    {
                        id: 'version',
                        title: proxy.$t('preview.introVersion'),
                        isAll: false,
                        count: 2,
                        data: []
                    }
                ]
                : [],
            ...paramsField.value
        ]
    })
    const renderParamList = computed(() => {
        return paramsList.value.filter(p => selectedData.value.includes(p.id))
    })

    function toggleCollapse (id) {
        if (activeName.value.has(id)) {
            activeName.value.delete(id)
        } else {
            activeName.value.add(id)
        }
        activeName.value = new Set(activeName.value)
    }
    function toggleItemSelection (key) {
        const eleIdx = selectedData.value.indexOf(key)
        if (eleIdx === -1) {
            selectedData.value.push(key)
        } else {
            selectedData.value.splice(eleIdx, 1)
        }
    }
    function handleSelectAll (selected, data) {
        if (selected) {
            selectedData.value = [...new Set([...selectedData.value, ...data])]
        } else {
            selectedData.value = selectedData.value.filter(field => !data.includes(field))
        }
    }
    function handleParamChange (id, value) {
        paramsList.value.forEach(p => {
            if (p.id === id) {
                proxy.$set(p, 'defaultValue', value)
            }
        })
    }
    
    function handleSetParmaRequired (id) {
        paramsList.value.forEach(p => {
            if (p.id === id) {
                proxy.$set(p, 'isRequiredParam', !p.isRequiredParam)
            }
        })
    }
    function handleConfirm () {
        proxy.$emit('confirm', renderParamList.value)
    }
    function handleCancel () {
        proxy.$emit('cancel', renderParamList.value)
    }
    
</script>

<style lang="scss" scoped>
.add-field-popover {
    .add-field-btn {
        display: flex;
        align-items: center;
        cursor: pointer;
        color: #1768EF;
        font-size: 12px;
        .add-icon {
            font-size: 14px !important;
            margin-right: 5px;
            color: #3A84FF;
        }
    }
}
.popover-container {
    width: 400px;
    padding: 10px 12px;
    max-height: 500px;
    overflow: auto;
    .search-input {
        margin-bottom: 12px;
    }
  
    .group {
        margin-bottom: 12px;

        .group-title {
            display: flex;
            justify-content: space-between;
            align-items: center;
            font-size: 14px;
            color: #313238;
            line-height: 22px;

            p {
                font-weight: 700;

                .item-count {
                    font-weight: 500;
                    color: #63656E;
                }
            }
        }
        
        .group-content {
            display: grid;
            flex-wrap: wrap;
            gap: 8px;
            grid-template-columns: repeat(2, 1fr);
            color: #63656E;
            margin-top: 4px;
        }
        
        .group-content div {
            box-sizing: border-box;
            height: 32px;
            padding: 6px;
            font-size: 12px;
            cursor: pointer;
            border-radius: 2px;
            &:hover {
                background: #F5F7FA;
            }
        }
        
        .selected {
            background: #F5F7FA;
            color: #3A84FF;
            border-radius: 2px;
        }
    }
}
$header-height: 36px;
.config-content {
    overflow: auto !important;
    margin-top: 10px;

    .params-content-item {
        background: #FFFFFF;
        border-radius: 2px;
        margin-bottom: 20px;
    }

    .params-collapse-content {
        padding: 16px 24px;
    }

    @for $i from 1 through 6 {
        :nth-child(#{$i} of .params-collapse-trigger) {
            top: $header-height * ($i - 1);
        }
    }

    .params-collapse-trigger {
        display: flex;
        flex-shrink: 0;
        align-items: center;
        font-size: 14px;
        font-weight: 700;
        height: $header-height;
        cursor: pointer;
        top: 0;
        position: sticky;
        grid-gap: 10px;
        color: #313238;
        background-color: white;
        z-index: 6;

        &.params-collapse-expand {
            .icon-angle-right {
                transform: rotate(90deg);
            }
        }

        .icon-angle-right {
            transition: all 0.3 ease;
            color: #4D4F56;
        }
    }
}
</style>
