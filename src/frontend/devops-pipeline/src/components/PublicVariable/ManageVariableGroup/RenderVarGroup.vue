<template>
    <section class="render-var-group-main">
        <details :open="data.isOpen">
            <summary
                class="category-collapse-trigger"
                @click="toggleOpen"
            >
                <bk-icon
                    type="right-shape"
                    class="icon-angle-right"
                />
                <div>
                </div>
                <div>
                    <p class="group-name">{{ data.groupName }}</p>
                    <p
                        class="group-desc"
                        v-bk-overflow-tips
                    >
                        {{ data.desc || '--' }}
                    </p>
                </div>
                <template v-if="editable">
                    <!-- <div
                        v-if="hasRefNum"
                        class="delete-icon"
                    >
                        <i
                            class="devops-icon icon-minus-circle"
                            v-bk-tooltips="$t('publicVar.hasRefNumTips')"
                        />
                    </div> -->
                    <bk-popconfirm
                        trigger="click"
                        class="delete-icon-popconfirm"
                        :title="$t('publicVar.deleteGroupTitle')"
                        :content="$t('publicVar.deleteGroupTips')"
                        :confirm-text="$t('publicVar.remove')"
                        width="288"
                        @confirm="confirmRemove"
                    >
                        <i
                            class="devops-icon icon-minus-circle"
                            @click="handleDelete"
                        />
                    </bk-popconfirm>
                </template>
            </summary>
            <div
                class="collapse-content"
                v-bkloading="{ isLoading }"
            >
                <div
                    class="variable-list"
                    v-for="data in renderVariableList"
                    :key="data.key"
                >
                    {{ data[0] }}
                    <variable-table
                        show-ref
                        :data="data"
                    />
                </div>
            </div>
        </details>
    </section>
</template>

<script setup>
    import { ref, watch, computed } from 'vue'
    import UseInstance from '@/hook/useInstance'
    import variableTable from './variableTable'
    import {
        VARIABLE,
        CONSTANT
    } from '@/store/modules/publicVar/constants'

    const { proxy } = UseInstance()
    const props = defineProps({
        data: {
            type: Object,
            default: () => {}
        },
        index: {
            type: String,
            default: 0
        },
        editable: {
            type: Boolean,
            default: true
        },
        globalParams: {
            type: Array,
            default: () => []
        }
    })
    const isLoading = ref(false)
    const variableList = computed(() => {
        return props.data.variableList.map((i) => {
            return {
                ...i,
                // 是否被引用
                isCited: props.globalParams.find(param => param.id === i?.buildFormProperty?.id)
            }
        })
    })
    const hasRefNum = computed(() => variableList.value.some(i => i.referCount > 0))
    const renderVariableList = computed(() => {
        const requiredParam = variableList.value.filter(i => i.type === VARIABLE && i.buildFormProperty.required)
        const otherParam = variableList.value.filter(i => i.type === VARIABLE && !i.buildFormProperty.required)
        const constantParam = variableList.value.filter(i => i.type === CONSTANT)

        return [
            ...(
                requiredParam.length ? [
                    {
                        title: proxy.$t('newui.pipelineParam.buildParam'),
                        data: requiredParam,
                        key: VARIABLE
                    }
                ] : []
            ),
            ...(
                otherParam.length ? [
                    {
                        title: proxy.$t('newui.pipelineParam.otherVar'),
                        data: otherParam,
                        key: VARIABLE
                    }
                ] : []
            ),
            ...(
                constantParam.length ? [
                    {
                        title: proxy.$t('publicVar.constant'),
                        data: constantParam,
                        key: CONSTANT
                    }
                ] : []
            )
        ]
    })
    watch(() => props.data.isOpen,  (value) => {
        if (value && !props.data.isRequested) {
            fetchVariablesByGroupName()
        }
    }, {
        deep: true,
        immediate: true
    })
    function toggleOpen () {
        if (!props.data.isRequested) {
            fetchVariablesByGroupName()
        }
    }
    function handleDelete (event) {
        event.preventDefault()
    }
    function confirmRemove () {
        proxy.$emit('delete', props.data.groupName)
    }
    async function fetchVariablesByGroupName () {
        try {
            isLoading.value = true
            const variableList = await proxy.$store.dispatch('publicVar/getResourceVarReferenceInfo', {
                referId: proxy.$route.params.pipelineId ?? proxy.$route.params.templateId,
                params: {
                    referType: proxy.$route.name === 'pipelinesEdit' ? 'PIPELINE' : 'TEMPLATE',
                    referVersion: proxy.$route.params.version || proxy.$store.state.atom?.pipelineInfo?.version,
                    groupName: props.data.groupName
                }
            })
            proxy.$emit('updateData', {
                index: props.index,
                data: {
                    variableList,
                    isRequested: true
                }
            })
        } catch (e) {
            console.error(e)
        } finally {
            isLoading.value = false
        }
    }
</script>

<style lang="scss">
.render-var-group-main {
    &:hover {
        .category-collapse-trigger,
        .collapse-content {
            background: #F0F1F5 !important;
        }
    }
  
    .category-collapse-trigger {
        display: flex;
        position: relative;
        align-items: center;
        cursor: pointer;
        background: #F5F7FA;
        border-radius: 2px 2px 0 0;
        padding: 8px 12px;
        font-size: 12px;
        color: #4D4F56;
        font-weight: 700;
        .icon-angle-right {
            font-size: 10px;
            margin-right: 5px;
            transform: rotate(90deg);
            color: #979BA5;
            position: relative;
            top: -12px;
        }
        .group-name {
            font-weight: 400;
            font-size: 14px;
            color: #4D4F56;
        }
        .group-desc {
            width: 680px;
            font-size: 12px;
            color: #979BA5;
            margin: 8px 0 0px !important;
        }
        .delete-icon,
        .delete-icon-popconfirm {
            position: absolute;
            right: 24px;
            top: 20px;
            color: #C4C6CC;
            cursor: pointer;
            z-index: 1001;
        }
    }
    details:not([open]) .collapse-content {
        display: none;
    }

    .collapse-content {
        background: #F5F7FA;
        padding: 0 24px 16px;
    }

    details:not([open]) .category-collapse-trigger > .icon-angle-right {
        transform: rotate(0deg);
    }
    .variable-list-table {
        .is-new {
            background-color: #f2fff4 !important;
        }
    }
}
</style>