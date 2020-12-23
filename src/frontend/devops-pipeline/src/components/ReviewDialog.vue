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
            <bk-form form-type="vertical" :label-width="200">
                <bk-form-item :label="$t('details.checkDesc')">
                    <div style="white-space: pre-wrap;word-break:break-all;">{{$t('details.checkDescInfo', [time])}}</div>
                </bk-form-item>
                <bk-form-item :label="$t('stageReviewInputDesc')">
                    <div style="white-space: pre-wrap;word-break:break-all;">{{ ((reviewInfo || {}).stageControlOption || {}).reviewDesc || $t('none') }}</div>
                </bk-form-item>
                <bk-form-item :label="$t('stageReviewParams')">
                    <key-value-normal edit-value-only name="reviewParams" :handle-change="handleChangeParams" :value="reviewParams" v-if="reviewParams.length"></key-value-normal>
                    <span v-else>{{ $t('none') }}</span>
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
    import KeyValueNormal from '@/components/atomFormField/KeyValueNormal'

    export default {
        name: 'review-dialog',
        components: {
            KeyValueNormal
        },
        props: {
            isShow: {
                type: Boolean,
                default: false
            }
        },

        data () {
            return {
                isLoading: false,
                isCancel: false,
                params: []
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
            },
            reviewParams () {
                return ((this.reviewInfo || {}).stageControlOption || {}).reviewParams || []
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
                    cancel: this.isCancel,
                    reviewParams: this.params
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
            },

            handleChangeParams (name, value) {
                this.params = value
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
