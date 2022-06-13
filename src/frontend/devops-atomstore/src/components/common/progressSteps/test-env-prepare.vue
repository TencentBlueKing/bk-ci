<template>
    <section class="main-body">
        <log :logs="logs" v-bkloading="{ isLoading: isLogLoading }"></log>

        <footer class="main-footer">
            <bk-button :disabled="currentStep.status !== 'success'" theme="primary" @click="nextStep"> {{ $t('store.下一步') }} </bk-button>
            <bk-button :disabled="currentStep.status !== 'fail'" :loading="isLoading" @click="rebuild"> {{ $t('store.重新构建') }} </bk-button>
        </footer>
    </section>
</template>

<script>
    import log from './log'

    export default {
        components: {
            log
        },

        props: {
            currentStep: {
                type: Object
            },
            detail: {
                type: Object
            },
            storeBuildInfo: {
                type: Object
            }
        },

        data () {
            return {
                cancelIds: [],
                logs: [],
                isLoading: false,
                isLogLoading: false
            }
        },

        mounted () {
            this.getLog()
        },

        beforeDestroy () {
            clearTimeout(this.getLog.id)
            this.cancelIds.push(this.getLog.id)
        },

        methods: {
            getLog (id) {
                if (this.getLog.start === undefined) this.isLogLoading = true
                const postData = {
                    type: 'SERVICE',
                    projectCode: this.storeBuildInfo.projectCode,
                    pipelineId: this.storeBuildInfo.pipelineId,
                    buildId: this.storeBuildInfo.buildId,
                    start: this.getLog.start > 0 ? this.getLog.start + 1 : 0,
                    executeCount: (this.getLog.executeCount || (this.getLog.executeCount = 1, 1))
                }
                this.$store.dispatch('store/requestProgressLog', postData).then((data = {}) => {
                    if (id && this.cancelIds.includes(id)) return
                    if (data.status === 0) {
                        const logs = data.logs || []
                        const lastLog = logs[logs.length - 1] || {}
                        this.getLog.start = lastLog.lineNo || this.getLog.start || 0
                        this.logs.push(...logs)
                        if (!data.finished || data.hasMore) this.getLog.id = setTimeout(() => this.getLog(this.getLog.id), 300)
                    }
                }).catch((err) => {
                    this.$bkMessage({ message: err.message || err, theme: 'error' })
                }).finally(() => {
                    this.isLogLoading = false
                })
            },

            rebuild () {
                clearTimeout(this.getLog.id)
                this.cancelIds.push(this.getLog.id)
                this.isLoading = true
                const postData = {
                    id: this.detail.serviceId,
                    projectCode: this.detail.projectCode
                }
                this.$store.dispatch('store/requestRebuildService', postData).then(() => {
                    this.logs = []
                    this.getLog.start = 0
                    this.$emit('loopProgress', this.getLog)
                }).catch((err) => {
                    this.$bkMessage({ message: err.message || err, theme: 'error' })
                }).finally(() => {
                    this.isLoading = false
                })
            },

            nextStep () {
                this.$parent.currentStepIndex++
            }
        }
    }
</script>
