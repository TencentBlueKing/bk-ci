<template>
    <bk-dialog
        ext-cls="ai-summary-dialog"
        :width="600"
        render-directive="if"
        v-model="value"
        :title="$t('details.aiSummary')"
        @value-change="handleChangeValue"
    >
        <div
            v-if="isLoading"
            class="loading-wrapper"
        >
            <div
                class="bk-spin-loading bk-spin-loading-mini bk-spin-loading-primary ai-loading-icon"
            >
                <div class="rotate rotate1"></div>
                <div class="rotate rotate2"></div>
                <div class="rotate rotate3"></div>
                <div class="rotate rotate4"></div>
                <div class="rotate rotate5"></div>
                <div class="rotate rotate6"></div>
                <div class="rotate rotate7"></div>
                <div class="rotate rotate8"></div>
            </div>
            <p>{{ $t('details.aiSummaryLoadingTips') }}</p>
        </div>
        <div
            v-else
            class="content-wrapper"
        >
            <div
                v-if="summaryData.status === 3"
                class="summary-error-tips"
            >
                {{ $t('details.getAiSummaryErr') }}
            </div>
            <div
                v-else
                v-html="summaryData.resultHtml"
            />
        </div>
        <template slot="footer">
            <div class="footer-wrapper">
                <p>{{ $t('details.copilotTips') }}</p>
                <div
                    class="operate-buttons"
                    v-if="!isLoading"
                >
                    <div
                        class="refresh-icon"
                        @click="handleRegenerate"
                    >
                        <logo
                            name="refresh"
                            size="14"
                        />
                        <span>{{ $t('details.regenerate') }}</span>
                    </div>
                    <div
                        v-if="summaryData.status !== 3"
                        class="like-icon like-icon-1"
                        @click="handleRate('UP')"
                    >
                        <logo :name="isRateUp ? 'like' : 'notLike'" />
                    </div>
                    <div
                        v-if="summaryData.status !== 3"
                        class="like-icon like-icon-2"
                        @click="handleRate('DOWN')"
                    >
                        <logo :name="isRateDown ? 'like' : 'notLike'" />
                    </div>
                </div>
            </div>
        </template>
    </bk-dialog>
</template>

<script>
    import Logo from '@/components/Logo'
    export default {
        name: 'code-ai-summary',
        components: {
            Logo
        },
        props: {
            value: {
                type: Boolean,
                default: false
            },
            elementId: {
                type: String,
                default: ''
            }
        },
        data () {
            return {
                summaryData: {},
                isLoading: true,
                isRateUp: false,
                isRateDown: false,
                timer: null
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            },
            pipelineId () {
                return this.$route.params.pipelineId
            },
            buildNo () {
                return this.$route.params.buildNo
            }
        },
        watch: {
            value (val) {
                if (val) {
                    this.handleGetSummary()
                } else {
                    clearTimeout(this.timer)
                }
            }
        },
        methods: {
            handleChangeValue (val) {
                this.$emit('update:value', val)
            },
            async handleGetSummary (type = 'getAISummary') {
                try {
                    this.isLoading = true
                    const res = await this.$store.dispatch(`common/${type}`, {
                        projectId: this.projectId,
                        pipelineId: this.pipelineId,
                        buildId: this.buildNo,
                        elementId: this.elementId
                    })
                    if (res.status === 1) {
                        this.timer = setTimeout(() => {
                            this.handleGetSummary()
                        }, 8000)
                    } else {
                        this.summaryData = res
                        this.isLoading = false
                    }
                } catch (e) {
                    this.isLoading = false
                    this.$showTips({
                        message: e.message ? e.message : e,
                        theme: 'error'
                    })
                }
            },
            async handleRate (type) {
                try {
                    const { processId, projectName } = this.summaryData
                    await this.$store.dispatch('common/summaryRate', {
                        projectName,
                        processId,
                        type
                    })
                    this.isRateUp = type === 'UP'
                    this.isRateDown = type !== 'UP'
                } catch (e) {
                    this.$showTips({
                        message: e.message ? e.message : e,
                        theme: 'error'
                    })
                }
            },
            async handleRegenerate () {
                this.handleGetSummary('regenerateAISummary')
            }
        }
    }
</script>
<style lang="scss">
    .ai-summary-dialog {
        .bk-dialog-footer {
            text-align: left !important;
        }
        .loading-wrapper {
            min-height: 150px;
            text-align: center;
            margin-top: 50px;
            p {
                margin-top: 40px;
            }
        }
        .content-wrapper {
            margin: 20px 0;
            min-height: 160px;
            max-height: 300px;
            overflow: scroll;
            .summary-error-tips {
                margin-top: 60px;
                text-align: center;
            }
        }
        .ai-loading-icon {
            width: 32px !important;
            height: 32px !important;
            .rotate {
                width: 4px !important;
                height: 8px !important;
                transform-origin: 50% 18px !important;
            }
        }
        .footer-wrapper {
            display: flex;
            justify-content: space-between;
            height: 32px;
            line-height: 32px;
            font-size: 12px !important;
        }
        .operate-buttons {
            display: flex;
            align-items: center;
            .refresh-icon {
                cursor: pointer;
                display: flex;
                align-items: center;
                margin-right: 10px;
                span {
                    margin-left: 6px;
                }
            }
            .like-icon {
                width: 25px;
                height: 25px;
                border: 1px solid #ccc;
                margin-left: 5px;
                cursor: pointer;
            }
            .like-icon-2 {
                transform: rotate(180deg);
            }
        }
    }
</style>
