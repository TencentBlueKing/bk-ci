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
                    <bk-radio-group v-model="isCancel">
                        <bk-radio class="choose-item" :value="false">{{ $t('details.agree') }}</bk-radio>
                        <bk-radio class="choose-item" :value="true">{{ $t('details.abort') }}</bk-radio>
                    </bk-radio-group>
                </bk-form-item>
            </bk-form>
        </div>
    </bk-dialog>
</template>

<script>
    import { mapActions, mapState } from 'vuex'
    import { convertTime } from '@/utils/util'

    export default {
        name: 'review-dialog',
        props: {
            isShow: {
                type: Boolean,
                default: false
            }
        },

        data () {
            return {
                isLoading: false,
                isCancel: false
            }
        },
        computed: {
            ...mapState('atom', [
                'reviewInfo'
            ]),
            routerParams () {
                return this.$route.params
            },
            time () {
                try {
                    const hour2Ms = 60 * 60 * 1000
                    return convertTime(this.reviewInfo.startEpoch + this.reviewInfo.stageControlOption.timeout * hour2Ms)
                } catch (e) {
                    return 'unknow'
                }
            }
        },
        methods: {
            ...mapActions('atom', [
                'toggleReviewDialog',
                'triggerStage'
            ]),
            startReview () {
                this.triggerStage({
                    ...this.$route.params,
                    stageId: this.reviewInfo.id,
                    cancel: this.isCancel
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
