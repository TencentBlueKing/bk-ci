<template>
    <article class="detail-home" v-bkloading="{ isLoading }">
        <h3 class="market-home-title">
            <icon class="title-icon" name="color-logo-store" size="25" />
            <p class="title-name">
                <router-link :to="{ name: 'atomHome' }" class="back-home"> {{ $t('store.研发商店') }} </router-link>
                <i class="right-arrow banner-arrow"></i>
                <span class="back-home" @click="backToStore">{{type|typeFilter}}</span>
                <i class="right-arrow banner-arrow"></i>
                <span class="banner-des">{{detail.name}}</span>
            </p>
            <router-link :to="{ name: 'workList' }" class="title-work"> {{ $t('store.工作台') }} </router-link>
        </h3>

        <main class="store-main" v-show="!isLoading">
            <component :is="`${type}Info`" :detail="detail"></component>
            <bk-tab type="currentType" :active.sync="currentTab" class="detail-tabs">
                <bk-tab-panel name="des" :label="$t('store.概述')" class="summary-tab">
                    <mavon-editor
                        :editable="false"
                        default-open="preview"
                        :subfield="false"
                        :toolbars-flag="false"
                        :box-shadow="false"
                        :external-link="false"
                        preview-background="#fff"
                        v-model="detail.description"
                        v-if="detail.description"
                    >
                    </mavon-editor>
                    <p class="g-empty summary-empty" v-if="!detail.description"> {{ $t('store.发布者很懒，什么都没留下！') }} </p>
                </bk-tab-panel>

                <bk-tab-panel name="comment" :label="$t('store.评价')" class="detail-tab">
                    <h3 class="comment-title"> {{ $t('store.用户评分') }} </h3>
                    <section class="rate-group">
                        <h3 class="rate-title"><animated-integer :value="detail.avgScore" digits="1"></animated-integer><span>{{ $t('store.共') }}{{detail.totalNum}}{{ $t('store.份评分') }}</span></h3>
                        <hgroup class="rate-card">
                            <h3 class="rate-info" v-for="(scoreItem, index) in detail.scoreItemList" :key="index">
                                <comment-rate :rate="scoreItem.score" :width="10" :height="11"></comment-rate>
                                <p class="rate-bar">
                                    <span class="dark-gray" :style="{ flex: scoreItem.num }"></span>
                                    <span class="gray" :style="{ flex: (+detail.totalNum > 0) ? detail.totalNum - scoreItem.num : 1 }"></span>
                                </p>
                                <span class="rate-sum">{{scoreItem.num}}</span>
                            </h3>
                        </hgroup>
                        <button class="add-common" @click="showComment = true">
                            <template v-if="commentInfo.commentFlag"> {{ $t('store.修改评论') }} </template>
                            <template> {{ $t('store.撰写评论') }} </template>
                        </button>
                    </section>

                    <h3 class="comment-title"> {{ $t('store.用户评论') }} </h3>
                    <hgroup v-for="(comment, index) in commentList" :key="index">
                        <comment :comment="comment"></comment>
                    </hgroup>
                    <p class="comments-more" v-if="!isLoadEnd && commentList.length > 0" @click="getComments(true)"> {{ $t('store.阅读更多内容') }} </p>
                    <p class="g-empty comment-empty" v-if="commentList.length <= 0"> {{ $t('store.空空如洗，快来评论一下吧！') }} </p>
                </bk-tab-panel>
                <bk-tab-panel name="yaml" :label="$t('store.yaml')" v-if="type === 'atom'">
                    <section class="plugin-yaml"></section>
                </bk-tab-panel>
            </bk-tab>
        </main>
    </article>
</template>

<script>
    import { mapActions, mapGetters } from 'vuex'
    import commentRate from '../../components/common/comment-rate'
    import comment from '../../components/common/comment'
    import commentDialog from '../../components/common/comment/commentDialog.vue'
    import animatedInteger from '../../components/common/animatedInteger'
    import ideInfo from '../../components/common/detail-info/ide'
    import atomInfo from '../../components/common/detail-info/atom'
    import templateInfo from '../../components/common/detail-info/template'
    import imageInfo from '../../components/common/detail-info/image'
    import detailScore from '../../components/common/detailTab/detailScore'
    import codeSection from '../../components/common/detailTab/codeSection'

    export default {
        components: {
            atomInfo,
            templateInfo,
            ideInfo,
            imageInfo
        },

        filters: {
            typeFilter (val) {
                const bkLocale = window.devops || {}
                let res = ''
                switch (val) {
                    case 'template':
                        res = bkLocale.$t('store.流水线模板')
                        break
                    case 'image':
                        res = bkLocale.$t('store.容器镜像')
                        break
                    default:
                        res = bkLocale.$t('store.流水线插件')
                        break
                }
                return res
            }
        },

        data () {
            return {
                detailId: '',
                pageSize: 10,
                pageIndex: 1,
                detail: {},
                isLoading: false,
                isLoadEnd: false,
                showComment: false,
                showInstallConfirm: false,
                commentInfo: {},
                codeEditor: {},
                currentTab: 'des',
                codeMirrorCon: {
                    lineNumbers: true,
                    tabMode: 'indent',
                    mode: 'yaml',
                    theme: '3024-night',
                    autoRefresh: true,
                    cursorBlinkRate: 0,
                    readOnly: true
                },
                methodsGenerator: {
                    comment: {
                        atom: (postData) => this.requestAtomComments(postData),
                        template: (postData) => this.requestTemplateComments(postData),
                        ide: (postData) => this.requestIDEComments(postData),
                        image: (postData) => this.requestImageComments(postData)
                    },
                    scoreDetail: {
                        atom: () => this.requestAtomScoreDetail(this.detailCode),
                        template: () => this.requestTemplateScoreDetail(this.detailCode),
                        ide: () => this.requestIDEScoreDetail(this.detailCode),
                        image: () => this.requestImageScoreDetail(this.detailCode)
                    }
                }
            }
        },

        computed: {
            ...mapGetters('store', { 'markerQuey': 'getMarketQuery', 'detail': 'getDetail' }),

            detailCode () {
                return this.$route.params.code
            },

            type () {
                return this.$route.params.type
            },

            tabList () {
                return {
                    atom: [
                        { componentName: 'detailScore', label: this.$t('概述'), name: 'des' },
                        { componentName: 'codeSection', label: this.$t('YAML片段'), name: 'YAML', bindData: { code: this.detail.codeSection, limitHeight: false }, hidden: (!this.detail.yamlFlag || !this.detail.recommendFlag) }
                    ],
                    template: [
                        { componentName: 'detailScore', label: this.$t('概述'), name: 'des' }
                    ],
                    image: [
                        { componentName: 'detailScore', label: this.$t('概述'), name: 'des' },
                        { componentName: 'codeSection', label: 'Dockerfile', name: 'Dockerfile', bindData: { code: this.detail.codeSection, limitHeight: false } }
                    ]
                }
            }
        },

        mounted () {
            this.getDetail()
        },

        beforeDestroy () {
            this.clearDetail()
        },
        
        methods: {
            ...mapActions('store', [
                'clearDetail',
                'setDetail',
                'requestAtom',
                'requestAtomStatistic',
                'requestTemplateDetail',
                'requestAtomComments',
                'requestAtomScoreDetail',
                'requestTemplateComments',
                'requestTemplateScoreDetail',
                'requestIDE',
                'requestIDEComments',
                'requestIDEScoreDetail',
                'requestImage',
                'getUserApprovalInfo',
                'requestImageCategorys',
                'getAtomYaml'
            ]),

            getDetail () {
                const type = this.$route.params.type
                const funObj = {
                    atom: () => this.getAtomDetail(),
                    template: () => this.getTemplateDetail(),
                    ide: () => this.getIDEDetail(),
                    image: () => this.getImageDetail()
                }
                const getDetailMethod = funObj[type]

                getDetailMethod().catch((err) => {
                    this.$bkMessage({ message: (err.message || err), theme: 'error' })
                }).finally(() => (this.isLoading = false))
            },

            getAtomDetail () {
                const atomCode = this.detailCode

                return Promise.all([
                    this.requestAtom({ atomCode }),
                    this.requestAtomStatistic({ atomCode }),
                    this.getUserApprovalInfo(atomCode),
                    this.getAtomYaml({ atomCode })
                ]).then(([atomDetail, atomStatic, userAppInfo, yaml]) => {
                    const detail = atomDetail || {}
                    detail.detailId = atomDetail.atomId
                    detail.downloads = atomStatic.downloads || 0
                    detail.approveStatus = (userAppInfo || {}).approveStatus
                    detail.codeSection = yaml
                    this.setDetail(detail)
                })
            },

            getTemplateDetail () {
                const templateCode = this.detailCode
                return this.requestTemplateDetail(templateCode).then((templateDetail) => {
                    const detail = templateDetail || {}
                    detail.detailId = templateDetail.templateId
                    detail.name = templateDetail.templateName
                    this.setDetail(detail)
                })
            },

            getIDEDetail () {
                const atomCode = this.detailCode

                return this.requestIDE({ atomCode }).then((res) => {
                    this.detail = res || {}
                    this.detailId = res.atomId
                    this.detail.name = res.atomName
                    this.commentInfo = res.userCommentInfo || {}
                })
            },

            getImageDetail () {
                const imageCode = this.detailCode

                return Promise.all([
                    this.requestImageCategorys(),
                    this.requestImage({ imageCode })
                ]).then(([categorys, res]) => {
                    this.detail = res || {}
                    this.detailId = res.imageId
                    this.detail.name = res.imageName
                    this.commentInfo = res.userCommentInfo || {}

                    const currentCategory = categorys.find((x) => (x.categoryCode === res.category))
                    const setting = currentCategory.settings || {}
                    detail.needInstallToProject = setting.needInstallToProject
                    this.setDetail(detail)
                })
            },

            backToStore () {
                Object.assign(this.markerQuey, { pipeType: this.type })
                this.$router.push({
                    name: 'atomHome',
                    query: this.markerQuey
                })
            }
        }
    }
</script>

<style lang="scss" scoped>
    @import '@/assets/scss/conf.scss';
    .plugin-yaml {
        height: 400px;
        background: black;
    }

    .store-main {
        height: calc(100vh - 93px);
        overflow-y: scroll;
        background: $grayBackGroundColor;
    }

    .detail-home {
        overflow: hidden;
        min-height: 100%;
    }

    .detail-tabs {
        margin: 49px auto 30px;
        width: 1200px;
        /deep/ .CodeMirror {
            font-family: Consolas, "Courier New", monospace;
            line-height: 1.5;
            margin-bottom: 20px;
            padding: 10px;
            height: auto;
        }
        .summary-tab {
            overflow: hidden;
            min-height: 360px;
        }
        .comment-title {
            margin-top: 26px;
            padding-bottom: 6px;
            height: 21px;
            font-size: 16px;
            font-weight: bold;
            color: $fontLightBlack;
            line-height: 21px;
        }
        .summary-empty {
            margin-top: 130px;
        }
        .comment-empty {
            margin-top: 70px;
        }
    }

    .detail-tab {
        padding: 14px 46px 44px;
        min-height: 360px;
    }

    .detail-tabs {
        margin: 20px auto 30px;
        width: 95vw;
        max-width: 1400px;
        background: #fff;
        padding: 10px 32px 40px;
        box-shadow: 1px 2px 3px 0px rgba(0,0,0,0.05);
    }
</style>
