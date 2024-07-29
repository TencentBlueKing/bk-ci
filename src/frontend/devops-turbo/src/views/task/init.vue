<template>
    <article
        class="list-init-home"
        v-bkloading="{ isloading }"
    >
        <main class="g-turbo-box init-main">
            <section class="init-title">
                <h3 class="title-main g-turbo-deep-black-font"> {{ $t('turbo.欢迎使用 Turbo 加速方案') }} </h3>
                <p class="title-main-content">{{ $t('turbo.welcome') }}</p>
                <h5 class="title-recommend g-turbo-deep-black-font"> {{ $t('turbo.为你推荐的加速模式') }} </h5>
                <h5
                    class="recommend-task"
                    v-for="(recommend, index) in recommendList"
                    :key="index"
                >
                    <p class="task-main">
                        <span class="task-title g-turbo-deep-black-font">{{ recommend.engineName }}</span>
                        <span class="task-desc g-turbo-gray-font">{{ recommend.recommendReason }}</span>
                    </p>
                    <p class="task-buttons">
                        <bk-button
                            text
                            class="task-doc"
                            @click="goToDoc(recommend.docUrl)"
                        >
                            {{ $t('turbo.查看文档') }}
                        </bk-button>
                        <bk-button
                            class="task-use"
                            @click="goToCreate(recommend.engineCode)"
                        >
                            {{ $t('turbo.立即使用') }}
                        </bk-button>
                    </p>
                </h5>
            </section>
            <span :class="isZH ? 'init-img' : 'init-img-en'"></span>
        </main>
    </article>
</template>

<script>
    import { getRecommendList, getPlanList } from '@/api'

    export default {
        data () {
            return {
                isloading: false,
                recommendList: []
            }
        },

        computed: {
            projectId () {
                return this.$route.params.projectId
            }
        },

        watch: {
            projectId () {
                this.judgeHasPlan()
            }
        },

        created () {
            this.isZH = ['zh-CN', 'zh', 'zh_cn'].includes(document.documentElement.lang)
            this.getRecommendList()
        },

        methods: {
            judgeHasPlan () {
                return getPlanList(this.projectId, 1).then((res) => {
                    if (res.turboPlanCount > 0) {
                        this.$router.replace({ name: 'overview' })
                    }
                }).catch((err) => {
                    if (err.code !== 2300017) {
                        this.$bkMessage({ theme: 'error', message: err.message || err })
                    }
                }).finally(() => {
                    this.isloading = false
                })
            },

            getRecommendList () {
                this.isloading = true
                getRecommendList().then((res) => {
                    this.recommendList = res || []
                }).catch((err) => {
                    this.$bkMessage({ theme: 'error', message: err.message || err })
                }).finally(() => {
                    this.isloading = false
                })
            },

            goToDoc (docUrl) {
                window.open(docUrl, '_blank')
            },

            goToCreate (engineCode) {
                this.$router.push({
                    name: 'taskCreate',
                    query: {
                        engineCode
                    }
                })
            }
        }
    }
</script>

<style lang="scss" scoped>
    .list-init-home {
        padding: .2rem;
        width: 100%;
        height: calc(100vh - 5.96vh - 50px);
        margin: 0 auto;
        .init-main {
            height: 100%;
            display: flex;
            .init-img {
                flex: 38.1;
                background: #fff;
                background-image: url('../../assets/img/task-init.png');
                background-size: contain;
                background-repeat: no-repeat;
                background-position: center;
            }
            .init-img-en {
                flex: 38.1;
                background: #fff;
                background-image: url('../../assets/img/task-init-en.png');
                background-size: contain;
                background-repeat: no-repeat;
                background-position: center;
            }
        }
    }
    .init-title {
        flex: 54.9;
        padding: 62px 64px;
        .title-main {
            font-weight: normal;
            font-size: 18px;
            line-height: 25px;
            margin-bottom: 15px;
        }
        .title-main-content {
            font-size: 14px;
            line-height: 22px;
            margin-bottom: 54px;
        }
        .title-recommend {
            font-weight: normal;
            font-size: 16px;
            line-height: 22px;
            margin-bottom: 15px;
        }
        .recommend-task {
            font-weight: normal;
            background: #f5f6fa;
            border-radius: 8px;
            display: flex;
            align-items: center;
            justify-content: space-between;
            padding: 18px 20px;
            margin-bottom: 20px;
            .task-main {
                width: calc(100% - 200px);
            }
            .task-title {
                font-size: 16px;
                line-height: 24px;
                display: block;
                margin-bottom: 4px;
            }
            .task-desc {
                font-size: 12px;
                line-height: 20px;
            }
            .task-buttons {
                display: flex;
                align-items: center;
            }
            .task-use {
                margin-left: 15px;
                border-radius: 16px;
                border: 1px solid #F0F1F5;
                color: #3a84ff;
            }
        }
    }
</style>
