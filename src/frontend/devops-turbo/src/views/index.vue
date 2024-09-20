<template>
    <article class="turbo-home">
        <bk-tab
            :active.sync="active"
            type="unborder-card"
            class="home-nav g-turbo-box-without-radius"
        >
            <bk-tab-panel
                v-for="(panel, index) in panels"
                v-bind="panel"
                :key="index"
            >
                <template slot="label">
                    <section
                        @click="gotoPage(panel.name)"
                        class="home-nav-tab"
                    >
                        {{ panel.label }}
                    </section>
                </template>
            </bk-tab-panel>
        </bk-tab>
        <router-view
            class="turbo-main"
            v-bkloadng="{ isloading }"
        ></router-view>
    </article>
</template>

<script>
    import { getPlanList } from '@/api'

    export default {
        data () {
            return {
                panels: [
                    { name: 'overview', label: this.$t('turbo.总览'), count: 10 },
                    { name: 'task', label: this.$t('turbo.加速方案'), count: 20 },
                    { name: 'history', label: this.$t('turbo.加速历史'), count: 30 }
                ],
                active: 'overview',
                isloading: false
            }
        },

        computed: {
            projectId () {
                return this.$route.params.projectId
            }
        },

        watch: {
            '$route.name': {
                handler (name) {
                    this.setActive(name)
                },
                immediate: true
            },

            projectId () {
                this.goToInitPage()
            }
        },

        created () {
            window.addEventListener('resize', this.flexible, false)
            this.flexible()
        },

        beforeDestroy () {
            window.removeEventListener('resize', this.flexible, false)
            const doc = window.document
            const docEl = doc.documentElement
            docEl.style.fontSize = '14px'
        },

        methods: {
            setActive (name) {
                const activeMap = {
                    overview: 'overview',
                    task: 'task',
                    taskInit: 'task',
                    taskDetail: 'task',
                    taskCreate: 'task',
                    taskList: 'task',
                    taskSuccess: 'task',
                    history: 'history',
                    historyDetail: 'history'
                }
                const curActive = activeMap[name]
                if (curActive) this.active = curActive
                else this.goToInitPage(true)
            },

            goToInitPage (needInit) {
                this.isloading = true
                return getPlanList(this.projectId, 1).then((res) => {
                    if (res.turboPlanCount <= 0) {
                        this.$router.replace({ name: 'taskInit' })
                        this.active = 'task'
                    } else if (needInit) {
                        this.$router.replace({ name: 'overview' })
                        this.active = 'overview'
                    }
                }).catch((err) => {
                    if (err.code !== 2300017) {
                        this.$bkMessage({ theme: 'error', message: err.message || err })
                    }
                }).finally(() => {
                    this.isloading = false
                })
            },

            gotoPage (name) {
                this.$router.push({ name })
            },

            flexible () {
                const doc = window.document
                const docEl = doc.documentElement
                const designWidth = 1310 // 默认设计图宽度
                const maxRate = 2300 / designWidth
                const minRate = 1280 / designWidth
                const clientWidth = docEl.getBoundingClientRect().width || window.innerWidth
                const flexibleRem = Math.max(Math.min(clientWidth / designWidth, maxRate), minRate) * 100
                docEl.style.fontSize = flexibleRem + 'px'
            }
        }
    }
</script>

<style lang="scss" scoped>
    .turbo-main {
        height: calc(94.04vh - 50px);
        max-width: 13.1rem;
        overflow: auto;
    }
    .home-nav {
        display: flex;
        align-items: center;
        justify-content: center;
        .home-nav-tab {
            cursor: pointer;
            padding: 0 18px;
        }
        ::v-deep .bk-tab-header {
            background-color: #fff;
            height: 5.96vh !important;
            line-height: 5.96vh !important;
            background-image: none !important;
            .bk-tab-label-wrapper .bk-tab-label-list {
                height: 5.96vh !important;
                .bk-tab-label-item {
                    line-height: 5.96vh !important;
                    height: 5.96vh !important;
                    color: #63656e;
                    padding: 0;
                    &::after {
                        height: 3px;
                    }
                    &.active {
                        color: #3a84ff;
                    }
                    .bk-tab-label {
                        font-size: 16px;
                        width: 100%;
                    }
                }
            }
            .bk-tab-header-setting {
                height: 5.96vh !important;
                line-height: 5.96vh !important;
            }
        }
        ::v-deep .bk-tab-section {
            padding: 0;
        }
    }
</style>
