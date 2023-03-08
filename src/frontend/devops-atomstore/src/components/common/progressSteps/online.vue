<template>
    <section class="main-body">
        <log :logs="logs" v-bkloading="{ isLoading: isLogLoading }"></log>

        <footer class="main-footer">
            <bk-button :disabled="currentStep.status !== 'fail'" :loading="isLoading" @click="reDeploy"> {{ $t('store.重新部署') }} </bk-button>
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
                this.$store.dispatch('store/requestDeployInfo', this.detail.serviceCode).then((data = {}) => {
                    if (id && this.cancelIds.includes(id)) return
                    const conditions = data.conditions || []
                    conditions.forEach((condition) => {
                        let message = ''
                        Object.keys(condition).forEach((key) => {
                            message += `${key}: ${condition[key]}; `
                        })
                        this.logs.push({ message })
                    })
                    this.getLog.id = setTimeout(() => this.getLog(this.getLog.id), 1000)
                }).catch((err) => {
                    this.$bkMessage({ message: err.message || err, theme: 'error' })
                }).finally(() => {
                    this.getLog.start = 1
                    this.isLogLoading = false
                })
            },

            reDeploy () {
                clearTimeout(this.getLog.id)
                this.cancelIds.push(this.getLog.id)
                this.isLoading = true
                const postData = {
                    serviceCode: this.detail.serviceCode,
                    version: this.detail.version
                }
                this.$store.dispatch('store/requestRedeploy', postData).then(() => {
                    this.logs = []
                    this.$emit('loopProgress', this.getLog)
                }).catch((err) => {
                    this.$bkMessage({ message: err.message || err, theme: 'error' })
                }).finally(() => {
                    this.isLoading = false
                })
            }
        }
    }
</script>
