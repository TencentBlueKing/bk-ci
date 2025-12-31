<template>
    <bk-dialog
        v-model="isShow"
        width="1080"
        :position="dialogPositionConfig"
        render-directive="if"
        class="variable-group-preview-dialog"
        @value-change="handleHideShow"
    >
        <template slot="header">
            <div class="preview-header">
                {{ $t('publicVar.varGroupPreview') }}
                <span class="group-name">
                    | {{ groupData.groupName }}
                </span>
            </div>
        </template>
        <div class="preview-content-main">
            <div class="left-wrapper">
                <div
                    v-for="(item, index) in previewData"
                    :key="item.varName"
                    :class="{
                        'tab-item': true,
                        'active': index === activeIdx
                    }"
                    @click="handleToggle(index)"
                >
                    <span class="operate-type">{{ item.operateTitle }}</span>
                    <span
                        class="var-name"
                        v-bk-overflow-tips
                    >
                        {{ item.varName }}
                    </span>
                </div>
            </div>
            <div
                v-bkloading="{ isLoading }"
                class="right-wrapper"
            >
                <template v-if="curVarData?.content?.operate === OPERATE_TYPE.UPDATE">
                    <div class="title">
                        {{ $t('publicVar.modifiedTitle') }}
                    </div>
                    <div class="change-content">
                        <div
                            v-for="(value, key) in curVarData?.content?.changes"
                            class="row-item"
                            :key="key"
                        >
                            <span class="key">
                                <i
                                    v-if="changeFieldsNeedTips.includes(key) && (value?.oldValue !== value?.newValue)"
                                    class="bk-icon icon-exclamation-circle-shape tooltips-icon"
                                    v-bk-tooltips="changeFieldValueTips(key)"
                                />
                                {{ fieldTitleMap[key] }}
                            </span>
                            <span class="value">
                                <span class="current-value">{{ ['required', 'readOnly', 'valueNotEmpty', 'sensitive'].includes(key) ? proxy.$t(`${value.oldValue}`) : value?.oldValue || '--' }}</span>
                                <Logo
                                    class="arrow-right-icon"
                                    size="18"
                                    name="arrow-right"
                                />
                                <span class="after-value">{{ ['required', 'readOnly', 'valueNotEmpty', 'sensitive'].includes(key) ? proxy.$t(`${value.newValue}`) : value?.newValue || '--' }}</span>
                            </span>
                        </div>
                    </div>
                </template>
                <template v-else>
                    <div class="title">
                        {{ curVarData?.content?.operate === OPERATE_TYPE.CREATE
                            ? proxy.$t('publicVar.releasePreview.newContent')
                        : curVarData.type === VARIABLE ? proxy.$t('publicVar.releasePreview.variableContent')
                        : proxy.$t('publicVar.releasePreview.constantContent') }}
                    </div>
                    <div class="change-content">
                        <div
                            v-for="(value, key) in curVarData?.content?.showInfo"
                            class="row-item"
                            :key="key"
                        >
                            <span class="key">
                                {{ fieldTitleMap[key] }}
                            </span>
                            <span class="value">
                                <span class="after-value">{{ ['required', 'readOnly', 'valueNotEmpty', 'sensitive'].includes(key) ? proxy.$t(`${value}`) : value || '--' }}</span>
                            </span>
                        </div>
                    </div>
                </template>
                <div
                    v-if="referenceList.length"
                    class="references-list"
                >
                    <div class="title">
                        <div>
                            {{ $t('publicVar.referencesTitle') }}
                            <i18n
                                :path="`publicVar.releasePreview.${curVarData.type}.${curVarData?.content?.operate}`"
                                tag="span"
                                class="references-tips"
                                slot="title"
                            >
                                <template v-if="curVarData.content?.operate === OPERATE_TYPE.CREATE">
                                    <span class="red-highlight">
                                        {{ proxy.$t('publicVar.releasePreview.definedRepeatParams') }}
                                    </span>

                                    <span class="red-highlight">
                                        {{ proxy.$t('publicVar.releasePreview.CustomizeAsDesired') }}
                                    </span>
                                </template>
                                <template v-else>
                                    <span class="highlight">
                                        {{ curVarData.content?.operate === OPERATE_TYPE.UPDATE
                                            ? proxy.$t('publicVar.releasePreview.syncUpdate')
                                        : proxy.$t('publicVar.releasePreview.canNotUse') }}
                                    </span>
                                </template>
                            </i18n>
                        </div>
                    </div>
                    <render-related-table
                        v-for="item in referenceList"
                        v-bind="item"
                        :key="item.key"
                        class="related-list"
                    />
                </div>
            </div>
        </div>
        <footer slot="footer">
            <bk-button
                theme="primary"
                @click="handleRelease"
            >
                {{ $t('confirm') }}
            </bk-button>
        </footer>
    </bk-dialog>
</template>

<script setup>
    import { ref, computed, watch } from 'vue'
    import Logo from '@/components/Logo'
    import RenderRelatedTable from './RenderRelatedTable'
    import UseInstance from '@/hook/useInstance'
    import { convertTime } from '@/utils/util'
    import {
        VARIABLE,
        CONSTANT,
        OPERATE_TYPE
    } from '@/store/modules/publicVar/constants'
    const { proxy } = UseInstance()
    const props = defineProps({
        isShow: Boolean,
        groupData: Object,
        previewData: Object
    })
    const activeIdx = ref(0)
    const isLoading = ref(false)
    const referenceList = ref([])
    const dialogPositionConfig = ref({
        top: window.innerHeight < 800 ? '20' :  window.innerHeight > 1000 ? '8%' : '80'
    })
    const curVarData = computed(() => props.previewData[activeIdx.value] ?? {})
    const fieldTitleMap = computed(() => {
        const isVariable = curVarData.value.type === VARIABLE
        const varAlias = proxy.$t(isVariable ? 'newui.pipelineParam.varAlias' : 'newui.pipelineParam.constAlias')
        const varDesc = proxy.$t(isVariable ? 'publicVar.varDesc' : 'publicVar.constantDesc')
        const defaultValue = proxy.$t(isVariable ? 'publicVar.defaultValue' : 'newui.pipelineParam.constValue')
    
        return {
            'alias': varAlias,
            'desc': varDesc,
            'defaultValue': defaultValue,
            'readOnly': proxy.$t('editPage.readOnlyOnRun'),
            'required': proxy.$t('newui.isBuildParam'),
            'valueNotEmpty': proxy.$t('editPage.required')
        }
    })
    // 默认值、运行时只读、是否必填、是否为入参， 属性值变更时提示
    const changeFieldsNeedTips = computed(() => ['defaultValue', 'readOnly', 'required', 'valueNotEmpty'])
    const changeFieldValueTips = (key) => {
        if (key === 'defaultValue' && curVarData.value.type === VARIABLE) {
            return proxy.$t('publicVar.changeVariableDefaultValueTips')
        }
        if (key === 'defaultValue' && curVarData.value.type === CONSTANT) {
            return proxy.$t('publicVar.changeConstDefaultValueTips')
        }
        return proxy.$t('publicVar.changeFieldValueTips')
    }
    watch(() => curVarData.value, () => {
        isLoading.value = true
        Promise.all([
            fetchReferenceList('PIPELINE'),
            fetchReferenceList('TEMPLATE')
        ]).then(([pipelineRefList, templateRefList]) => {
            const pipelineRef = {
                tabTitle: proxy.$t('pipeline'),
                data: pipelineRefList?.map(i => ({
                    ...i,
                    updateTime: convertTime(i.updateTime)
                })),
                key: 'pipeline',
                columns: getColumnsByType('pipeline')
            }
            const templateRef = {
                tabTitle: proxy.$t('template.template'),
                data: templateRefList?.map(i => ({
                    ...i,
                    updateTime: convertTime(i.updateTime)
                })),
                key: 'template',
                columns: getColumnsByType('template')
            }
            referenceList.value = [pipelineRef, templateRef]
        }).finally(() => {
            isLoading.value = false
        })
    }, {
        immediate: true,
        deep: true
    })
    function getColumnsByType (type = 'pipeline') {
        return [
            ...(type === 'pipeline'
                ? [
                    {
                        label: proxy.$t('pipelineName'),
                        prop: 'referName'
                    }
                ] : [
                    {
                        label: proxy.$t('template.name'),
                        prop: 'referName'
                    },
                    {
                        label: proxy.$t('template.type'),
                        prop: 'referType'
                    }
                ]
            ),
            {
                label: proxy.$t('creator'),
                prop: 'creator'
            },
            {
                label: proxy.$t('publicVar.lastModifiedUser'),
                prop: 'modifier'
            },
            {
                label: proxy.$t('publicVar.lastModifiedTime'),
                prop: 'updateTime'
            },
            ...(type === 'pipeline'
                ? [
                    {
                        label: proxy.$t('publicVar.execCount'),
                        prop: 'executeCount'
                    }
                ] : [
                    {
                        label: proxy.$t('publicVar.instanceNum'),
                        prop: 'instanceCount'
                    }
                ]
            )
           
        ]
    }
    async function fetchReferenceList (type) {
        if (!curVarData.value.groupName) return
        try {
            const res = await proxy.$store.dispatch('publicVar/getReferenceList', {
                groupName: curVarData.value.groupName,
                params: {
                    page:1,
                    pageSize: 100,
                    ...(curVarData.value?.content?.operate !== OPERATE_TYPE.CREATE ? {
                        varName: curVarData.value.varName,
                    } : {}),
                    referType: type
                }
            })
            return res.records
        } catch (e) {
            proxy.$bkMessage({
                theme: 'error',
                message: e.message || e
            })
        }
    }
    function handleHideShow (value) {
        if (!value) {
            proxy.$emit('update:isShow', value)
            activeIdx.value = 0
        }
    }
    function handleRelease () {
        proxy.$emit('confirm')
    }
    function handleToggle (index) {
        activeIdx.value = index
    }
</script>

<style lang="scss">
    .variable-group-preview-dialog {
        .bk-dialog-body {
            overflow: auto;
        }
        .preview-header {
            color: #313238;
            text-align: left;
            margin-bottom: 10px;
        }
        .group-name {
            color: #979BA5;
        }
        .preview-content-main {
            display: flex;
            height: 600px;
        }
        .left-wrapper {
            display: flex;
            flex-direction: column;
            border-right: 1px solid #DCDEE5;
            width: 250px;
            .tab-item {
                position: relative;
                padding: 12px 28px;
                display: flex;
                align-items: center;
                font-size: 14px;
                height: 45px;
                cursor: pointer;
                background-color: #F5F7FA;
                margin-bottom: 8px;
                &.active {
                    background-color: #fff;
                    border: 1px solid #DCDEE5;
                    border-right: none;
                    border-radius: 5px 0 0 5px;
                    &::after {
                        position: absolute;
                        top: 0;
                        right: -1px;
                        width: 1px;
                        height: 100%;
                        background: #FFF;
                        content: '';
                        z-index: 1000;
                    }
                }
                .operate-type {
                    color: #313238;
                }
                .var-name {
                    font-size: 12px;
                    padding-left: 16px;
                    color: #979BA5;
                    max-width: 130px;
                    display: inline-block;
                    overflow: hidden;
                    text-overflow: ellipsis;
                }
            }
        }
        .right-wrapper {
            flex-grow: 1;
            padding: 14px 24px;
            border: 1px solid #DCDEE5;
            border-left: none;
            border-radius: 0 5px 5px 0;
            background-color: #fff;
            overflow: auto;
            .title {
                display: flex;
                align-items: center;
                justify-content: space-between;
                font-size: 14px;
                color: #4D4F56;
                font-weight: bold;
                margin-bottom: 8px;
            }
            .references-tips {
                display: inline-flex;
                color: #979BA5;
                font-size: 12px;
                margin-left: 16px;
                font-weight: 400;
                .highlight {
                    color:#eaa53b;
                }
                .red-highlight {
                    color: #ea3e3e;
                }
            }
            .change-content {
                font-size: 12px;
                margin-bottom: 20px;
                .row-item {
                    display: flex;
                    margin-bottom: 8px;
                    
                }
                .tooltips-icon {
                    font-size: 14px;
                    color: red;
                }
                .key {
                    display: inline-flex;
                    align-items: center;
                    justify-content: space-around;
                    background: #F0F1F5;
                    border-radius: 2px;
                    padding: 4px 8px;
                    margin-right: 8px;
                    min-width: 80px;
                    max-width: 100px;
                    overflow: hidden;
                    text-overflow: ellipsis;
                    white-space: nowrap;
                    text-align: center;
                }
                .value {
                    display: inline-flex;
                    align-items: center;
                }
                .current-value {
                    color: #C4C6CC;
                }
                .after-value {
                    color: #4D4F56;
                }
                .arrow-right-icon {
                    margin: 0 6px;
                    color: #F8B64F;
                }
            }
            .related-list {
                margin-bottom: 20px;
                &:last-child {
                    margin-bottom: 0;
                }
            }
        }
    }
</style>