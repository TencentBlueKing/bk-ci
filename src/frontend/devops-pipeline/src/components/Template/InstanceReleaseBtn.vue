<template>
    <bk-popover
        theme="light"
        ext-cls="instance-task-menu"
    >
        <bk-badge
            theme="danger"
            class="task-menu-badge"
            :max="99"
            :val="taskList.length"
            :visible="!!taskList.length"
        >
            <bk-button
                theme="primary"
                v-perm="permData"
                @click="handleInstance"
            >
                {{ $t('template.instantiate') }}
            </bk-button>
        </bk-badge>
        <template slot="content">
            <div class="task-card">
                <i18n
                    path="template.releaseTaskHeaderTips"
                    tag="p"
                >
                    <span style="font-weight: 700;">{{ 1 }}</span>
                </i18n>
                <ul class="task-list">
                    <li
                        class="task-item"
                        v-for="task in taskList"
                        :key="task.baseId"
                    >
                        <bk-loading
                            class="loading-icon"
                            size="mini"
                            mode="spin"
                            theme="primary"
                            is-loading
                        />
                        <span class="update-time">
                            {{ task.updateTime }}
                        </span>
                        <span class="creator">
                            {{ task.creator }}
                        </span>
                        <bk-button
                            text
                            @click="handleViewTaskDetail(task)"
                        >
                            {{ $t('template.views') }}
                        </bk-button>
                    </li>
                </ul>
            </div>
        </template>
    </bk-popover>
</template>

<script setup>
    import { ref, computed, onMounted } from 'vue'
    import UseInstance from '@/hook/useInstance'
    import { convertTime } from '@/utils/util'
    import {
        SET_INSTANCE_LIST,
        SHOW_TASK_DETAIL,
        SET_TASK_DETAIL,
        SET_RELEASE_BASE_ID,
        UPDATE_USE_TEMPLATE_SETTING
    } from '@/store/modules/templates/constants'
    defineProps({
        permData: Object
    })
    const { proxy } = UseInstance()
    const templateId = computed(() => proxy?.$route?.params.templateId)
    const projectId = computed(() => proxy?.$route?.params.projectId)
    const pipelineInfo = computed(() => proxy.$store?.state?.atom?.pipelineInfo)
    const taskList = ref([])
    async function fetchTaskList () {
        try {
            const res = await proxy.$store.dispatch('templates/fetchReleaseTaskList', {
                projectId: projectId.value,
                templateId: templateId.value
            })
            taskList.value = res.data.map(i => {
                return {
                    ...i,
                    updateTime: convertTime(i.updateTime)
                }
            })
        } catch (e) {
            console.error(e)
        }
    }
    async function handleViewTaskDetail (task) {
        try {
            const { templateId, baseId } = task
            const res = await proxy.$store.dispatch('templates/fetchTaskDetailParams', {
                projectId: projectId.value,
                templateId,
                baseId
            })
            proxy.$store.commit(`templates/${SET_INSTANCE_LIST}`, res.data?.request?.instanceReleaseInfos)
            proxy.$store.commit(`templates/${UPDATE_USE_TEMPLATE_SETTING}`, res.data?.request?.useTemplateSetting)
            proxy.$store.commit(`templates/${SET_RELEASE_BASE_ID}`, baseId)
            proxy.$store.commit(`templates/${SHOW_TASK_DETAIL}`, true)
            proxy.$store.commit(`templates/${SET_TASK_DETAIL}`, res.data?.request)
            
            proxy.$router.push({
                name: 'instanceEntry',
                params: {
                    ...proxy.$route.params,
                    version: pipelineInfo.value?.releaseVersion,
                    type: 'upgrade'
                }
            })
        } catch (e) {
            console.error(e)
        }
    }
    function handleInstance () {
        proxy.$emit('click')
    }
    onMounted(() => {
        fetchTaskList()
    })
</script>

<style lang="scss" scoped>
    .task-menu-badge {
        height: 32px;
    }
    .task-card {
        .task-item {
            position: relative;
            right: 5px;
            width: 280px;
            display: flex;
            align-items: center;
            margin-bottom: 8px;
            margin-top: 4px;
            &:last-child {
                margin-bottom: 0px;
            }
            .loading-icon {
                height: 20px;
                width: 20px;
                margin-right: 5px;
            }
            .creator {
                margin-right: 15px;
            }
            .update-time {
                display: inline-block;
                flex-shrink: 0;
                margin-right: 20px;
            }
        }
    }
</style>
