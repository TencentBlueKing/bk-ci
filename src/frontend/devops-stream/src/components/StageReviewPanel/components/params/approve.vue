<template>
    <section v-if="computedShowParam">
        <span class="review-subtitle">{{$t('pipeline.customVariables')}}</span>
        <ul>
            <li v-for="(param, index) in params" :key="index" class="review-params">
                <bk-input disabled :value="getParamKey(param)" class="review-param-item"></bk-input>
                <span :class="{ 'review-param-gap': true, 'param-require': param.required }"></span>
                <param-value :form="param" :disabled="disabled" class="review-param-item"></param-value>
                <i class="bk-icon icon-info" v-bk-tooltips="param.desc" v-if="param.desc"></i>
            </li>
        </ul>
        <span class="error-message">{{ errMessage }}</span>
    </section>
</template>

<script>
    import paramValue from './param-value'

    export default {
        components: {
            paramValue
        },

        props: {
            showReviewGroup: Object,
            reviewParams: Array,
            disabled: Boolean
        },

        data () {
            return {
                params: [],
                errMessage: ''
            }
        },

        computed: {
            computedShowParam () {
                return this.params.length && !this.$parent.$refs.flowApprove.isCancel
            }
        },

        watch: {
            showReviewGroup: {
                handler () {
                    this.updateParams()
                },
                immediate: true
            }
        },

        methods: {
            getParamKey (param) {
                return param.chineseName || (param.key || '').replace(/^variables\./, '')
            },

            updateParams () {
                const params = this.showReviewGroup.params && this.showReviewGroup.params.length ? this.showReviewGroup.params : this.reviewParams
                this.params = params || []
            },

            getApproveData () {
                return new Promise((resolve, reject) => {
                    // 校验必填
                    const errorKeys = []
                    this.params.forEach(({ required, valueType, value, key }) => {
                        if (required) {
                            key = key.replace(/^variables\./, '')
                            if (typeof value === 'undefined' || value === '') {
                                errorKeys.push(key)
                            }
                            if (valueType === 'MULTIPLE' && (!value || value.length <= 0)) {
                                errorKeys.push(key)
                            }
                        }
                    })
                    if (errorKeys.length) this.errMessage = `${errorKeys.join(',')} ` + this.$t('pipeline.requireTips')
                    else this.errMessage = ''

                    if (this.errMessage && !this.$parent.$refs.flowApprove.isCancel) reject(new Error(this.errMessage))
                    else resolve(this.params)
                })
            }
        }
    }
</script>

<style lang="postcss" scoped>
    .review-params {
        display: flex;
        align-items: center;
        margin-bottom: 12px;
        .review-param-item {
            width: 380px;
        }
        .review-param-gap {
            display: inline-block;
            min-width: 28px;
            &.param-require:after {
                height: 8px;
                line-height: 1;
                content: "*";
                color: #ea3636;
                font-size: 12px;
                position: relative;
                left: 10px;
                top: 2px;
                display: inline-block;
            }
        }
        .icon-info {
            margin-left: 9px;
        }
    }
    .error-message {
        font-size: 12px;
        color: #ea3636;
        line-height: 18px;
    }
</style>
