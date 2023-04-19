<template>
    <section class="main-body">
        <section class="progress-test">
            <img src="../../../images/progressTest.png" class="test-image">
            <span class="test-tip" v-html="$t('store.testTip')"></span>
            <bk-button theme="primary" @click="goToTest" size="large"> {{ $t('store.前往测试') }} </bk-button>
        </section>

        <footer class="main-footer">
            <bk-button theme="primary" @click="completeTest" :disabled="isRebuildLoading" :loading="isLoading"> {{ $t('store.下一步') }} </bk-button>
            <bk-button :loading="isRebuildLoading" :disabled="isLoading" @click="rebuild"> {{ $t('store.重新构建') }} </bk-button>
        </footer>
    </section>
</template>

<script>
    import { urlJoin } from '../../../utils/index.js'

    export default {
        props: {
            currentStep: {
                type: Object
            },
            detail: {
                type: Object
            }
        },

        data () {
            return {
                isLoading: false,
                isRebuildLoading: false
            }
        },

        methods: {
            goToTest () {
                const serviceIds = this.detail.bkServiceId || []
                const id = serviceIds[0]
                const serviceObject = window.serviceObject || {}
                const serviceMap = serviceObject.serviceMap || {}
                const keys = Object.keys(serviceMap)
                let link = ''
                keys.forEach((key) => {
                    const cur = serviceMap[key]
                    if (+cur.id === +id) link = cur.link_new
                })
                const url = urlJoin('/console', link)
                const projectId = this.detail.projectCode
                window.setProjectIdCookie(projectId)
                window.open(url, '_blank')
            },

            rebuild () {
                this.isRebuildLoading = true
                const postData = {
                    id: this.detail.serviceId,
                    projectCode: this.detail.projectCode
                }
                this.$store.dispatch('store/requestRebuildService', postData).then(() => {
                    this.$emit('freshProgress', () => {
                        this.isRebuildLoading = false
                    })
                }).catch((err) => {
                    this.isRebuildLoading = false
                    this.$bkMessage({ message: err.message || err, theme: 'error' })
                })
            },

            completeTest () {
                this.isLoading = true
                this.$store.dispatch('store/requestServicePassTest', this.detail.serviceId).then(() => {
                    this.$emit('freshProgress', () => {
                        this.isLoading = false
                    })
                    this.$parent.currentStepIndex++
                }).catch((err) => {
                    this.isLoading = false
                    this.$bkMessage({ message: err.message || err, theme: 'error' })
                })
            }
        }
    }
</script>

<style lang="scss" scoped>
    .progress-test {
        display: flex;
        justify-content: center;
        align-items: center;
        flex-direction: column;
        height: 100%;
        .test-image {
            width: 160px;
            height: 123px;
            margin-bottom: 16px;
        }
        .test-tip {
            color: #b0b0b0;
            margin-bottom: 16px;
        }
    }
</style>
