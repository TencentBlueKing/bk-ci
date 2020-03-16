<template>
    <bk-dialog
        v-model="isShow"
        ext-cls="check-atom-form"
        :close-icon="false"
        :width="600"
        :position="{ top: '100' }"
        :auto-close="false"
        @confirm="startReview"
        @cancel="handleCancel">
        <div v-bkloading="{ isLoading }">
            <bk-form form-type="vertical">
                <bk-form-item :label="$t('details.checkDesc')">
                    <div style="white-space: pre-wrap;word-break:break-all;">{{$t('details.checkDescInfo', [time])}}</div>
                </bk-form-item>
                <bk-form-item :label="$t('editPage.checkResult')">
                    <bk-radio-group v-model="data.status">
                        <bk-radio class="choose-item" :value="'PROCESS'">{{ $t('editPage.agree') }}</bk-radio>
                        <bk-radio class="choose-item" :value="'ABORT'">{{ $t('details.abort') }}</bk-radio>
                    </bk-radio-group>
                </bk-form-item>
            </bk-form>
        </div>
    </bk-dialog>
</template>

<script>
    import { mapActions, mapState } from 'vuex'
    export default {
        name: 'review-dialog',
        props: {
            isShow: {
                type: Boolean,
                default: false
            },
            toggleCheck: {
                type: Function,
                required: true
            }
        },

        data () {
            return {
                isLoading: false,
                data: {
                    status: 'PROCESS',
                    suggest: '',
                    desc: '',
                    params: []
                },
                time: '123'
            }
        },
        computed: {
            ...mapState('atom', [
                'reviewInfo'
            ]),
            routerParams () {
                return this.$route.params
            }
        },
        watch: {
            'isShow': function (val) {
                if (val) {
                    // this.requestCheckData()
                }
            }
        },
        methods: {
            ...mapActions('atom', [
                'toggleReviewDialog',
                'triggerStage'
            ]),
            async requestCheckData () {
                try {
                    this.isLoading = true
                    const postData = {
                        projectId: this.routerParams.projectId,
                        pipelineId: this.routerParams.pipelineId,
                        buildId: this.routerParams.buildNo
                        // elementId: this.reviewInfo.id
                    }
                    const res = await this.getCheckAtomInfo(postData)
                    this.data = Object.assign(res, { status: 'PROCESS' })
                } catch (err) {
                    this.$showTips({
                        theme: 'error',
                        message: err.message || err
                    })
                } finally {
                    this.isLoading = false
                }
            },
            startReview () {
                this.triggerStage({
                    ...this.$route.params,
                    stageId: this.reviewInfo.id
                })

                this.$nextTick(() => {
                    this.handleCancel()
                })
            },

            handleCancel () {
                this.toggleReviewDialog({
                    isShow: false,
                    reviewInfo: null
                })
            }
        }
    }
</script>

<style lang="scss">
    .check-atom-form {
        .choose-item {
            margin-right: 30px;
        }
        .check-suggest {
            margin-top: 10px;
        }
    }
</style>
