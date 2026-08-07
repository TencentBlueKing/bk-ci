<template>
    <div class="associated-jobs">
        <template v-if="jobs && jobs.length">
            <span
                v-for="(job, index) in visibleJobs"
                :key="index"
                class="job-tag"
            >
                <span
                    v-if="getSeq(job)"
                    class="job-seq"
                >{{ getSeq(job) }}</span>
                <span
                    class="job-name"
                    v-bk-overflow-tips
                >{{ getName(job) }}</span>
            </span>
            <bk-popover
                v-if="restCount > 0"
                placement="top"
                theme="light"
            >
                <span class="job-more">+{{ restCount }}</span>
                <template #content>
                    <div class="associated-jobs-popover">
                        <span
                            v-for="(job, index) in jobs"
                            :key="index"
                            class="popover-item"
                        >
                            <span
                                v-if="getSeq(job)"
                                class="job-seq"
                            >{{ getSeq(job) }}</span>
                            <span class="job-name">{{ getName(job) }}</span>
                            <span
                                v-if="index < jobs.length - 1"
                                class="split"
                            >；</span>
                        </span>
                    </div>
                </template>
            </bk-popover>
        </template>
        <span v-else>--</span>
    </div>
</template>

<script>
    import { computed } from 'vue'

    export default {
        name: 'associated-jobs',
        props: {
            jobs: {
                type: Array,
                default: () => []
            },
            // 内联展示的最大数量，超出折叠到气泡里
            maxVisible: {
                type: Number,
                default: 2
            }
        },
        setup (props) {
            const visibleJobs = computed(() => props.jobs.slice(0, props.maxVisible))
            const restCount = computed(() => Math.max(props.jobs.length - props.maxVisible, 0))

            // 序号徽标：优先 vmSeqId，其次 stageId
            const getSeq = (job) => job?.vmSeqId || job?.stageId || ''
            const getName = (job) => job?.taskName || job?.jobName || '--'

            return {
                visibleJobs,
                restCount,
                getSeq,
                getName
            }
        }
    }
</script>

<style lang="scss" scoped>
.associated-jobs {
    display: flex;
    align-items: center;
    flex-wrap: nowrap;
    gap: 8px;
    min-width: 0;

    .job-tag {
        display: inline-flex;
        align-items: center;
        max-width: 160px;
        font-size: 12px;
        color: #63656e;
    }

    .job-seq {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        height: 16px;
        padding: 0 4px;
        margin-right: 4px;
        font-size: 12px;
        line-height: 16px;
        color: #3a84ff;
        background: #e1ecff;
        border-radius: 2px;
        flex-shrink: 0;
    }

    .job-name {
        overflow: hidden;
        white-space: nowrap;
        text-overflow: ellipsis;
    }

    .job-more {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        height: 16px;
        padding: 0 6px;
        font-size: 12px;
        color: #63656e;
        background: #f0f1f5;
        border-radius: 2px;
        cursor: pointer;
    }
}
.associated-jobs-popover {
    max-width: 320px;
    font-size: 12px;
    line-height: 20px;
    color: #63656e;
    white-space: normal;

    .popover-item {
        .job-seq {
            display: inline-flex;
            align-items: center;
            justify-content: center;
            height: 16px;
            padding: 0 4px;
            margin-right: 4px;
            font-size: 12px;
            line-height: 16px;
            color: #3a84ff;
            background: #e1ecff;
            border-radius: 2px;
        }
        .split {
            margin: 0 2px;
        }
    }
}
</style>
