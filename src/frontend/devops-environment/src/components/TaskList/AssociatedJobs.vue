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
                >{{ getSeq(job) }}</span><span
                    class="job-name"
                    v-bk-overflow-tips
                >{{ getName(job) }}</span><span
                    v-if="index < visibleJobs.length - 1 || restCount > 0"
                    class="split"
                >；</span>
            </span>
            <bk-popover
                v-if="restCount > 0"
                placement="top"
                theme="light"
            >
                <span class="job-more">+{{ restCount }}</span>
                <template #content>
                    <div class="associated-jobs-popover">
                        <div
                            v-for="(job, index) in restJobs"
                            :key="index"
                            class="popover-item"
                        >
                            <span
                                v-if="getSeq(job)"
                                class="job-seq"
                            >{{ getSeq(job) }}</span><span class="job-name">{{ getName(job) }}</span><span
                                v-if="index < restJobs.length - 1"
                                class="split"
                            >；</span>
                        </div>
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
            const restJobs = computed(() => props.jobs.slice(props.maxVisible))
            const restCount = computed(() => restJobs.value.length)

            // 序号徽标：stageId 去掉 stage- 后与 vmSeqId 拼接；无 stageId 时仅展示 vmSeqId
            const getSeq = (job) => {
                const vmSeqId = job?.vmSeqId || ''
                const stageId = job?.stageId
                if (!stageId) return vmSeqId

                const stageNum = String(stageId).replace(/^stage-/, '')
                if (!stageNum) return vmSeqId
                return vmSeqId ? `${stageNum}-${vmSeqId}` : stageNum
            }
            const getName = (job) => job?.taskName || job?.jobName || '--'

            return {
                visibleJobs,
                restJobs,
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
    gap: 4px;
    min-width: 0;

    .job-tag {
        display: inline-flex;
        align-items: center;
        max-width: 180px;
        font-size: 12px;
        color: #63656e;
        min-width: 0;
    }

    .job-seq {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        height: 18px;
        padding: 0 4px;
        margin-right: 4px;
        font-size: 12px;
        line-height: 16px;
        color: #63656e;
        background: #fafbfd;
        border: 1px solid #dcdee5;
        border-radius: 2px;
        flex-shrink: 0;
        box-sizing: border-box;
    }

    .job-name {
        overflow: hidden;
        white-space: nowrap;
        text-overflow: ellipsis;
        color: #63656e;
    }

    .split {
        flex-shrink: 0;
        color: #63656e;
    }

    .job-more {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        height: 18px;
        padding: 0 6px;
        font-size: 12px;
        line-height: 16px;
        color: #63656e;
        background: #f0f1f5;
        border-radius: 2px;
        cursor: pointer;
        box-sizing: border-box;
        flex-shrink: 0;
    }
}
.associated-jobs-popover {
    max-width: 320px;
    font-size: 12px;
    line-height: 22px;
    color: #63656e;

    .popover-item {
        display: flex;
        align-items: center;

        .job-seq {
            display: inline-flex;
            align-items: center;
            justify-content: center;
            height: 18px;
            padding: 0 4px;
            margin-right: 4px;
            font-size: 12px;
            line-height: 16px;
            color: #63656e;
            background: #fafbfd;
            border: 1px solid #dcdee5;
            border-radius: 2px;
            flex-shrink: 0;
            box-sizing: border-box;
        }

        .job-name {
            color: #63656e;
        }

        .split {
            color: #63656e;
        }
    }
}
</style>
