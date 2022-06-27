<template>
    <article class="detail-config">
        <bk-tab :active.sync="active">
            <bk-tab-panel
                v-for="(panel, index) in panels"
                v-bind="panel"
                :key="index">
            </bk-tab-panel>
        </bk-tab>

        <code-section :code="ymlObj[active]" limit-height :height="`${height}px`" v-bkloading="{ isLoading, opacity: 0, color: '#090300' }"></code-section>
    </article>
</template>

<script>
    import { mapState } from 'vuex'
    import { pipelines } from '@/http'
    import codeSection from '@/components/code-section'

    export default {
        components: {
            codeSection
        },

        data () {
            return {
                panels: [
                    { label: this.$t('pipeline.originYaml'), name: 'originYaml' },
                    { label: this.$t('pipeline.parsedYaml'), name: 'parsedYaml' }
                ],
                active: 'originYaml',
                ymlObj: {},
                isLoading: true
            }
        },

        computed: {
            ...mapState(['projectId', 'appHeight']),

            height () {
                return this.appHeight - 267
            }
        },

        watch: {
            '$route.params.buildId' () {
                this.getYaml()
            }
        },

        created () {
            this.getYaml()
        },

        methods: {
            getYaml () {
                this.isLoading = true
                pipelines.getPipelineBuildYaml(this.projectId, this.$route.params.buildId).then((res) => {
                    this.ymlObj = res
                }).catch((err) => {
                    this.$bkMessage({ theme: 'error', message: err.message || err })
                }).finally(() => {
                    this.isLoading = false
                })
            }
        }
    }
</script>

<style lang="postcss" scoped>
    .detail-config {
        overflow: hidden;
        /deep/ .bk-tab-section {
            padding: 0;
            border: none;
        }
    }
</style>
