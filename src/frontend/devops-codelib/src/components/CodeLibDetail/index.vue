<template>
    <section class="codelib-detail" v-bkloading="{ isLoading }">
        <div class="detail-header">
            <p class="codelib-name">{{ curRepoId }}</p>
            <a class="codelib-address">https://git.woa.com/bk-ci-test/test/yamlv2.git</a>
        </div>
        <bk-tab :active.sync="active" type="unborder-card">
            <bk-tab-panel
                v-for="(panel, index) in panels"
                v-bind="panel"
                :key="index">
                <component
                    :is="componentName">
                </component>
            </bk-tab-panel>
        </bk-tab>
    </section>
</template>
<script>
    import basicSetting from './basic-setting.vue'
    export default {
        name: 'CodeLibDetail',
        components: {
            basicSetting
        },
        props: {
            curRepoId: {
                type: String,
                default: ''
            }
        },
        data () {
            return {
                isLoading: false,
                panels: [
                    { name: 'basic', label: this.$t('codelib.basicSetting') }
                ],
                active: 'basic'
            }
        },
        computed: {
            componentName () {
                const comMap = {
                    basic: 'basicSetting'
                }
                return comMap[this.active]
            }
        },
        watch: {
            curRepoId: {
                handler (val) {
                    console.log(val, 4214)
                },
                immediate: true
            }
        },
        created () {
        },
        methods: {
           
        }
    }
</script>
<style lang='scss' scoped>
    .codelib-detail {
        height: 100%;
        .detail-header {
            display: flex;
            justify-content: space-between;
            flex: 1;
            height: 48px;
            line-height: 48px;
            background: #FAFBFD;
            padding: 0 24px;
        }
        .codelib-name {
            font-size: 16px;
            color: #313238;
        }
    }
</style>
