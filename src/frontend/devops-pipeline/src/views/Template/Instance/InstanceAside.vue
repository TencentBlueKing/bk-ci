<template>
    <div class="instance-aside">
        <div class="operate">
            <bk-button
                v-if="isInstanceCreateType"
                icon="plus"
            >
                {{ $t('new') }}
            </bk-button>
            <i18n
                v-else
                class="select-pipeline"
                tag="div"
                path="template.selectPipelineNum"
            >
                <span class="select-num">
                    {{ 5 }}
                </span>
            </i18n>
            <div class="batch-edit-btn">
                <Logo
                    name="edit"
                    size="14"
                    style="fill:#3c96ff;position:relative;top:2px;"
                />
                <span
                    v-if="renderInstanceList.length"
                >
                    {{ $t('template.batchEdit') }}
                </span>
            </div>
        </div>
        <ul class="instance-list">
            <li
                v-for="(instance, instanceIndex) in renderInstanceList"
                :class="[
                    'item',
                    {
                        active: instanceActiveIndex === instanceIndex,
                        editing: instanceIndex === editingIndex
                    }
                ]"
                :key="instance.id"
                @click="handleInstanceClick(instanceIndex)"
            >
                <template v-if="instanceIndex === editingIndex">
                    <bk-input
                        ref="nameInputRef"
                        v-model="instance.pipelineName"
                        @blur="handleEnterChangeName(instanceIndex)"
                        @enter="handleEnterChangeName(instanceIndex)"
                    >
                    </bk-input>
                </template>
                <template v-else>
                    <div
                        :class="['pipeline-name', {
                            'upgrade': !isInstanceCreateType
                        }]"
                        v-bk-overflow-tips
                    >
                        {{ instance.pipelineName }}
                    </div>
                    <div class="instance-operate">
                        <bk-icon
                            type="edit2"
                            class="operate-icon edit-icon"
                            @click.stop="handleEditName(instance, instanceIndex)"
                        />
                        <template v-if="isInstanceCreateType">
                            <bk-icon
                                type="copy"
                                class="operate-icon copy-icon"
                                @click.stop="handleCopyInstance(instance, instanceIndex)"
                            />
                            <bk-icon
                                type="delete"
                                class="operate-icon delete-icon"
                                @click.stop="handleCopyInstance(instance, instanceIndex)"
                            />
                        </template>
                    </div>
                </template>
            </li>
        </ul>
    </div>
</template>

<script setup>
    import { ref, defineProps, computed, onMounted } from 'vue'
    import Logo from '@/components/Logo'
    import UseInstance from '@/hook/useInstance'
    const props = defineProps({
        isInstanceCreateType: Boolean
    })
    const { proxy } = UseInstance()
    const instanceActiveIndex = ref(0)
    const editingIndex = ref(null)
    const nameInputRef = ref(null)
    const instanceList = computed(() => proxy.$store?.state?.templates?.instanceList)
    const currentVersionId = computed(() => proxy?.$route.params?.version)
    const renderInstanceList = computed(() => {
        return props.isInstanceCreateType ? [] : instanceList.value
    })
    function handleInstanceClick (index) {
        instanceActiveIndex.value = index
    }
    function handleEnterChangeName (index) {
        editingIndex.value = null
    }
    function handleEditName (instance, index) {
        editingIndex.value = index
        proxy?.$nextTick(() => {
            nameInputRef.value[0].focus()
        })
    }
    function handleCopyInstance (instance, index) {

    }
    onMounted(() => {
        if (!props.isInstanceCreateType && !instanceList.value.length) {
            proxy.$router.push({
                name: 'TemplateOverview',
                params: {
                    type: 'instanceList',
                    version: currentVersionId.value
                }
            })
        }
    })
</script>

<style lang="scss" scoped>
    .instance-aside {
        height: 100%;
        font-size: 12px;
        padding: 24px;
        .operate {
            display: flex;
            align-items: center;
            justify-content: space-between;
        }
        .select-pipeline {
            color: #4d4f56;
        }
        .select-num {
            font-weight: 700;
        }
        .batch-edit-btn {
            cursor: pointer;
            color: #3A84FF;
        }
    }
    .instance-list {
        list-style: none;
        margin-top: 20px;
        .item {
            display: flex;
            align-items: center;
            justify-content: space-between;
            height: 32px;
            padding: 0 15px;
            border-radius: 2px;
            background: #F5F7FA;
            margin-bottom: 10px;
            cursor: pointer;
            &.editing {
                padding: 0;
                font-weight: 400 !important;
            }
            &.active,
            &:hover {
                color: #3A84FF;
                font-weight: 700;
                background: #E1ECFF;
            }
            &:hover {
                .instance-operate {
                    visibility: visible;
                }
            }
        }
        .pipeline-name {
            max-width: 55%;
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
            &.upgrade {
                max-width: 75%;
            }
        }
        .instance-operate {
            visibility: hidden;
            color: #989ca7;
            .edit-icon {
                position: relative;
                top: 1px;
                font-size: 20px !important;
            }
            .copy-icon,
            .delete-icon {
                font-size: 14px !important;
            }
            .operate-icon {
                &:hover {
                    color: #3A84FF;
                }
            }
        }
    }
</style>
