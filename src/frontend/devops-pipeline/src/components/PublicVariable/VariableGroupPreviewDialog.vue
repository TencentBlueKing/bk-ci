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
                    <div>
                        <span class="operate-type">{{ item.operateTitle }}</span>
                        <span class="var-name">
                            {{ item.varName }}
                        </span>
                    </div>
                </div>
            </div>
            <div
                v-bkloading="{ isLoading }"
                class="right-wrapper"
            >
                <div class="title">
                    {{ $t('publicVar.modifiedTitle') }}
                </div>
                <div class="change-content">
                    <div class="row-item">
                        <span class="key">
                            {{ curVarData.type === VARIABLE ? $t('newui.pipelineParam.varAlias') : $t('newui.pipelineParam.constAlias') }}
                        </span>
                        <template v-if="curVarData?.content?.changes?.alias">
                            <span class="value">
                                <span class="current-value">{{ curVarData.getChangesByField(curVarData, 'alias')?.oldValue }}</span>
                                <Logo
                                    class="arrow-right-icon"
                                    size="18"
                                    name="arrow-right"
                                />
                                <span class="after-value">{{ curVarData.getChangesByField(curVarData, 'alias').newValue }}</span>
                            </span>
                        </template>
                        <template v-else>
                            <span class="value">
                                <span class="after-value">{{ curVarData?.content?.alias ?? '--' }}</span>
                            </span>
                        </template>
                    </div>
                    <div class="row-item">
                        <span class="key">
                            {{ curVarData.type === VARIABLE ? $t('publicVar.varDesc') : $t('publicVar.constantDesc') }}
                        </span>
                        <template v-if="curVarData?.content?.changes?.desc">
                            <span class="value">
                                <span class="current-value">{{ curVarData.getChangesByField(curVarData, 'desc')?.oldValue }}</span>
                                <Logo
                                    class="arrow-right-icon"
                                    size="18"
                                    name="arrow-right"
                                />
                                <span class="after-value">{{ curVarData.getChangesByField(curVarData, 'desc').newValue }}</span>
                            </span>
                        </template>
                        <template v-else>
                            <span class="value">
                                <span class="after-value">{{ curVarData?.content?.desc ?? '--' }}</span>
                            </span>
                        </template>
                    </div>
                    <div class="row-item">
                        <span class="key">
                            <i
                                v-if="curVarData.type === CONSTANT"
                                class="bk-icon icon-exclamation-circle-shape tooltips-icon"
                                v-bk-tooltips="$t('publicVar.changeConstDefaultValueTips')"
                            />
                            {{ curVarData.type === VARIABLE ? $t('publicVar.defaultValue') : $t('newui.pipelineParam.constValue') }}
                        </span>
                        <template v-if="curVarData?.content?.changes?.defaultValue">
                            <span class="value">
                                <span class="current-value">{{ curVarData.getChangesByField(curVarData, 'defaultValue')?.oldValue }}</span>
                                <Logo
                                    class="arrow-right-icon"
                                    size="18"
                                    name="arrow-right"
                                />
                                <span class="after-value">{{ curVarData.getChangesByField(curVarData, 'defaultValue').newValue }}</span>
                            </span>
                        </template>
                        <template v-else>
                            <span class="value">
                                <span class="after-value">{{ curVarData?.content?.defaultValue ?? '--' }}</span>
                            </span>
                        </template>
                    </div>
                </div>
                <div class="references-list">
                    <div class="title">
                        <div>
                            {{ $t('publicVar.referencesTitle') }}
                            <span class="references-tips">
                                {{ $t('publicVar.referencesTips') }}
                            </span>
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
        CONSTANT
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
                    pageSize: 2000,
                    varName: curVarData.value.varName,
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
                color: #979BA5;
                font-size: 12px;
                margin-left: 16px;
                font-weight: 400;
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
                    display: inline-block;
                    background: #F0F1F5;
                    border-radius: 2px;
                    padding: 4px 8px;
                    margin-right: 8px;
                    min-width: 70px;
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