<template>
    <div class="ftp-pipeline-header">
        <div class="bread-prefix">
            <div class="back-wrap">
                <bk-button
                    variant="outline"
                    class="ftp-back-btn"
                    title="返回交付单页"
                    @click="onBack"
                >
                    <i class="devops-icon icon-arrows-left"></i>
                </bk-button>
            </div>
        </div>
        <component
            :is="ftpComponent"
        />
        <div
            class="more-action"
        >
            <bk-dropdown-menu
                trigger="click"
                align="bottom"
                :options="options"
            >
                <i slot="dropdown-trigger" class="bk-icon icon-more"></i>
                <div class="more-operation-dropmenu" slot="dropdown-content">
                    <ul>
                        <li
                            v-for="option in options"
                            :key="option.value"
                            @click="onClick(option.value)"
                        >
                            {{ option.content }}
                        </li>
                    </ul>
                </div>
            </bk-dropdown-menu>
        </div>
    </div>
</template>

<script>
    import DetailHeader from '../../components/PipelineHeader/DetailHeader.vue'
    import EditHeader from '../../components/PipelineHeader/EditHeader.vue'
    import HistoryHeader from '../../components/PipelineHeader/HistoryHeader.vue'

    const COMPONENTS = {
        ftpPipelinesDetail: DetailHeader,
        ftpPipelinesEdit: EditHeader,
        ftpPipelinesHistory: HistoryHeader
    }

    const ACTIONS = [{
        value: 'bk',
        content: '跳转到蓝盾'
    }, {
        value: 'old',
        content: '返回旧版'
    }]
    
    export default {
        components: {
            DetailHeader,
            EditHeader,
            HistoryHeader
        },
        data () {
            return {
                options: ACTIONS
            }
        },
        computed: {
            ftpComponent () {
                return COMPONENTS[this.$route.name] || 'DetailHeader'
            }
        },
        methods: {
            onClick (item) {
                switch (item) {
                    case 'old':
                        return this.sendMessage('useOldVersion')
                    case 'bk':
                        return this.toBkPipeline()
                }
            },
            onBack () {
                this.sendMessage('backToReq')
            },
            sendMessage (action, params) {
                window.parent.postMessage({
                    action,
                    params
                }, '*')
            },
            toBkPipeline () {
                const bkRoute = {
                    name: this.$route.name.replace(/^ftpP/, 'p'),
                    params: this.$route.params
                }

                const { href } = this.$router.resolve(bkRoute)

                window.open(href)
            }
        }
    }
</script>

<style lang="scss">
    .ftp-pipeline-header {
        @import "@/scss/conf";
        width: 100%;

        .bread-prefix {
            position: absolute;
            top: 7px;
            left: 20px;
            z-index: 10;
            height: 32px;
            line-height: 32px;

            .ftp-back-btn {
                width: 32px;
                height: 32px;
                min-width: 32px;
                border-radius: 50%;

                .devops-icon {
                    position: absolute;
                    left: 8px;
                    top: 7px;
                    font-size: 16px;
                    font-weight: bold;
                }
            }
        }

        .more-action {
            position: absolute;
            top: 6px;
            right: 6px;
            .bk-dropdown-menu {
                display: inline-block;
            }
            .icon-more {
                display: inline-block;
                font-size: 25px;
                padding-top: 3px;
                margin-top: 3px;
                &:hover{
                    cursor: pointer;
                    background-color: #e4e5eb;
                }
            }
        }

        .more-operation-dropmenu {
            width: 120px;
            ul {
                li {
                    font-size: 12px;
                    line-height: 32px;
                    text-align: left;
                    padding: 0 12px;
                    cursor: pointer;
                    &:hover {
                        color: $primaryColor;
                        background-color: #eaf3ff;
                        a {
                            color: $primaryColor;
                        }
                    }
                }
            }
        }
        .bkdevops-bread-crumb {
            margin-left: 40px;
        }
        .bread-crumb-item:first-child {
            display: none;
        }

        .bk-dropdown-menu {
            display: none;
        }
    }
</style>
