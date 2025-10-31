<template>
    <bk-alert
        class="problem-alter"
        v-if="showProblemTips"
        type="warning"
        :closable="true"
        @close="handleClosePipelineProblemTips"
    >
        <div slot="title">
            <span>{{ $t('pipelineProblemTips.observing') }}</span>
            <span v-if="pipelineProblemDetail.failureRateCount">{{ $t('pipelineProblemTips.failureRateTips') }}</span>
            <i18n
                v-if="pipelineProblemDetail?.consecutiveFailuresCount"
                path="pipelineProblemTips.consecutiveFailuresTips"
            >
                <span class="red-highlight">{{ pipelineProblemDetail.consecutiveFailuresCount }}</span>
                <span class="red-highlight">{{ $t('pipelineProblemTips.continuousFailure') }}</span>
            </i18n>
            <i18n
                v-if="pipelineProblemDetail?.scheduledTriggerNoCodeChangeCount"
                path="pipelineProblemTips.scheduledTriggerNoCodeChangeTips"
            >
                <span class="red-highlight">{{ pipelineProblemDetail.scheduledTriggerNoCodeChangeCount }}</span>
            </i18n>
            <span>{{ $t('pipelineProblemTips.focusOnAnomalies') }}</span>
            <bk-button
                text
                class="view-btn"
                @click="handleViewProblem"
            >
                {{ $t('pipelineProblemTips.views') }}
            </bk-button>
            <span class="update-msg">
                {{ $t('pipelineProblemTips.dataUpdateT1') }}
            </span>
        </div>
    </bk-alert>
</template>

<script>
    import { mapActions } from 'vuex'
    const PIPELINE_PROBLEM_CLOSE_TIME = 'PIPELINE_PROBLEM_CLOSE_TIME'
    export default {
        props: {
            updateTableHeight: Function
        },
        data () {
            return {
                pipelineProblemDetail: {}
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            },
            showProblemTips () {
                return !!Object.keys(this.pipelineProblemDetail).length && this.checkCloseTimeWithShow()
            }
        },
        watch: {
            projectId: {
                handler () {
                    this.fetchPipelineProblemDetail()
                },
                immediate: true
            }
        },
   
        methods: {
            ...mapActions('pipelines', [
                'getPipelineProblemDetail'
            ]),
            async fetchPipelineProblemDetail () {
                try {
                    const res = await this.getPipelineProblemDetail({
                        projectId: this.projectId
                    })
                    this.pipelineProblemDetail = res ?? {}
                    this.$nextTick(() => {
                        this.updateTableHeight()
                    })
                } catch (e) {
                    console.error(e)
                }
            },
            handleClosePipelineProblemTips () {
                const currentTime = this.getCurrentTime()
                localStorage.setItem(PIPELINE_PROBLEM_CLOSE_TIME, currentTime)
            },
            handleViewProblem () {
                const { cardId } = this.pipelineProblemDetail
                this.$router.push({
                    name: 'PipelineDataBoard',
                    query: {
                        cardId
                    }
                })
            },
            getCurrentTime () {
                return new Date().getTime()
            },
            checkCloseTimeWithShow () {
                const twoWeeks = 14 * 24 * 60 * 60 * 1000
                const lastClosedTime = Number(localStorage.getItem(PIPELINE_PROBLEM_CLOSE_TIME))
                const currentTime = this.getCurrentTime()

                return isNaN(lastClosedTime) || currentTime - lastClosedTime > twoWeeks
            }
        }
    }
</script>

<style lang="scss">
    .problem-alter {
        margin-bottom: 10px;
        .bk-alert-wraper {
            align-items: baseline !important;
        }
        .red-highlight {
            color: #f44b40;
            font-weight: 700;
        }
        .view-btn {
            font-size: 12px;
        }
        .update-msg {
            margin-left: 10px;
            color: #979BA5
        }
    }
</style>
