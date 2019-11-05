<template>
    <article class="detail-home" v-bkloading="{ isLoading }">
        <h3 class="market-home-title">
            <icon class="title-icon" name="color-logo-store" size="25" />
            <p class="title-name">
                <router-link :to="{ name: 'atomHome' }" class="back-home"> {{ $t('研发商店') }} </router-link>
                <i class="right-arrow banner-arrow"></i>
                <span class="back-home" @click="backToStore">{{type|typeFilter}}</span>
                <i class="right-arrow banner-arrow"></i>
                <span class="banner-des">{{detail.name}}</span>
            </p>
            <router-link :to="{ name: 'atomList' }" class="title-work" v-if="type !== 'ide'"> {{ $t('工作台') }} </router-link>
        </h3>

        <main class="store-main" v-show="!isLoading">
            <atom v-if="type === 'atom'" :detail="detail" />
            <template-info v-if="type === 'template'" :detail="detail" />
            <bk-tab type="currentType" :active="'des'" class="detail-tabs">
                <bk-tab-panel name="des" :label="$t('概述')" class="summary-tab">
                    <mavon-editor
                        :editable="false"
                        default-open="preview"
                        :subfield="false"
                        :toolbars-flag="false"
                        :box-shadow="false"
                        :external-link="false"
                        v-model="detail.description"
                        v-if="detail.description"
                    >
                    </mavon-editor>
                    <p class="g-empty summary-empty" v-if="!detail.description"> {{ $t('发布者很懒，什么都没留下！') }} </p>
                </bk-tab-panel>

                <bk-tab-panel name="comment" :label="$t('评价')" class="detail-tab">
                    <h3 class="comment-title"> {{ $t('用户评分') }} </h3>
                    <section class="rate-group">
                        <h3 class="rate-title"><animated-integer :value="detail.avgScore" digits="1"></animated-integer><span>{{ $t('共') }}{{detail.totalNum}}{{ $t('份评分') }}</span></h3>
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
                            <template v-if="commentInfo.commentFlag"> {{ $t('修改评论') }} </template>
                            <template> {{ $t('撰写评论') }} </template>
                        </button>
                    </section>

                    <h3 class="comment-title"> {{ $t('用户评论') }} </h3>
                    <hgroup v-for="(comment, index) in commentList" :key="index">
                        <comment :comment="comment"></comment>
                    </hgroup>
                    <p class="comments-more" v-if="!isLoadEnd && commentList.length > 0" @click="getComments(true)"> {{ $t('阅读更多内容') }} </p>
                    <p class="g-empty comment-empty" v-if="commentList.length <= 0"> {{ $t('空空如洗，快来评论一下吧！') }} </p>
                </bk-tab-panel>
            </bk-tab>
            <transition name="atom-fade">
                <commentDialog v-if="showComment" @freshComment="freshComment" @closeDialog="showComment = false" :name="detail.name" :code="detailCode" :id="detailId" :comment-id="commentInfo.commentId"></commentDialog>
            </transition>
        </main>
    </article>
</template>

<script>
    import { mapActions, mapGetters } from 'vuex'
    import commentRate from '../../components/common/comment-rate'
    import comment from '../../components/common/comment'
    import commentDialog from '../../components/common/comment/commentDialog.vue'
    import animatedInteger from '../../components/common/animatedInteger'
    import atom from '../../components/common/detail-info/atom'
    import templateInfo from '../../components/common/detail-info/template'

    export default {
        components: {
            comment,
            commentRate,
            commentDialog,
            animatedInteger,
            atom,
            templateInfo
        },

        filters: {
            typeFilter (val) {
                const bkLocale = window.devops || {}
                let res = ''
                switch (val) {
                    case 'template':
                        res = bkLocale.$t('流水线模板')
                        break
                    default:
                        res = bkLocale.$t('流水线插件')
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
                methodsGenerator: {
                    comment: {
                        atom: (postData) => this.requestAtomComments(postData),
                        template: (postData) => this.requestTemplateComments(postData)
                    },
                    scoreDetail: {
                        atom: () => this.requestAtomScoreDetail(this.detailCode),
                        template: () => this.requestTemplateScoreDetail(this.detailCode)
                    }
                }
            }
        },

        computed: {
            ...mapGetters('store', { 'commentList': 'getCommentList', 'markerQuey': 'getMarketQuery' }),

            detailCode () {
                return this.$route.params.code
            },

            type () {
                return this.$route.params.type
            }
        },

        created () {
            this.getDetail()
        },
        
        methods: {
            ...mapActions('store', [
                'setCommentList',
                'requestAtom',
                'requestAtomStatistic',
                'requestTemplateDetail',
                'requestAtomComments',
                'requestAtomScoreDetail',
                'requestTemplateComments',
                'requestTemplateScoreDetail',
                'getUserApprovalInfo'
            ]),

            freshComment (comment) {
                const commentList = this.commentList
                if (this.commentInfo.commentFlag) {
                    // 修改
                    const cur = commentList.find(item => item.data.commentId === comment.commentId) || {}
                    const curData = cur.data || {}
                    Object.assign(curData, comment)
                } else {
                    // 新增
                    commentList.unshift({ data: comment, children: [] })
                }
                this.commentInfo.commentFlag = true
                this.commentInfo.commentId = comment.commentId
                this.setCommentList(commentList)
                this.getScoreDetail()
            },

            getAtomIcon (atomCode) {
                return document.getElementById(atomCode) ? atomCode : 'placeholder'
            },

            getDetail () {
                this.isLoading = true
                const type = this.$route.params.type
                const funObj = {
                    atom: () => this.getAtomDetail(),
                    template: () => this.getTemplateDetail()
                }
                const getDetailMethod = funObj[type]

                getDetailMethod().then(() => {
                    this.getComments()
                    this.getScoreDetail()
                }).catch((err) => {
                    this.$bkMessage({ message: (err.message || err), theme: 'error' })
                }).finally(() => (this.isLoading = false))
            },

            getAtomDetail () {
                const atomCode = this.detailCode

                return Promise.all([
                    this.requestAtom({ atomCode }),
                    this.requestAtomStatistic({ atomCode }),
                    this.getUserApprovalInfo(atomCode)
                ]).then(([atomDetail, atomStatic, userAppInfo]) => {
                    this.detail = atomDetail || {}
                    this.detailId = atomDetail.atomId
                    this.detail.downloads = atomStatic.downloads || 0
                    this.commentInfo = atomDetail.userCommentInfo || {}
                    this.$set(this.detail, 'approveStatus', (userAppInfo || {}).approveStatus)
                })
            },

            getTemplateDetail () {
                const templateCode = this.detailCode
                return this.requestTemplateDetail(templateCode).then((templateDetail) => {
                    this.detail = templateDetail || {}
                    this.detailId = templateDetail.templateId
                    this.detail.name = templateDetail.templateName
                    this.commentInfo = templateDetail.userCommentInfo || {}
                })
            },

            getComments (isAdd = false) {
                const postData = {
                    code: this.detailCode,
                    page: this.pageIndex,
                    pageSize: this.pageSize
                }

                const getCommentsMethod = this.methodsGenerator.comment[this.type]
                getCommentsMethod(postData).then((res) => {
                    const count = res.count || 0
                    const apiList = res.records || []
                    const commentList = isAdd ? this.commentList : []

                    apiList.forEach((comment) => {
                        commentList.push({ data: comment, children: [] })
                    })
                    this.isLoadEnd = commentList.length >= count
                    this.pageIndex++
                    this.setCommentList(commentList)
                })
            },

            getScoreDetail () {
                const getScoreDetailMethod = this.methodsGenerator.scoreDetail[this.type]
                getScoreDetailMethod().then((res) => {
                    const itemList = [
                        { score: 5, num: 0 },
                        { score: 4, num: 0 },
                        { score: 3, num: 0 },
                        { score: 2, num: 0 },
                        { score: 1, num: 0 }
                    ]
                    const apiList = res.scoreItemList || []
                    apiList.forEach((item) => {
                        const cur = itemList.find((x) => x.score === item.score)
                        if (cur) cur.num = item.num
                    })

                    this.$set(this.detail, 'score', res.avgScore)
                    this.$set(this.detail, 'avgScore', res.avgScore)
                    this.$set(this.detail, 'totalNum', res.totalNum)
                    this.$set(this.detail, 'scoreItemList', itemList)
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

    .store-main {
        height: calc(100vh - 93px);
        margin-left: calc(100vw - 100%);
        overflow-y: scroll;
    }

    .detail-home {
        overflow: hidden;
        min-height: 100%;
    }

    .detail-tabs {
        margin: 49px auto 30px;
        width: 1200px;
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

    .comments-more {
        margin: 42px auto 0;
        font-size: 14px;
        color: $primaryColor;
        line-height: 19px;
        text-align: center;
        cursor: pointer;
    }

    .rate-group {
        margin-top: 13px;
        height: 74px;
        display: flex;
        align-items: center;
        justify-content: space-between;
        .add-common {
            width: 84px;
            height: 32px;
            background: $primaryColor;
            border-radius: 2px;
            border: none;
            font-size: 14px;
            color: #ffffff;
            line-height: 32px;
            &:active {
                transform: scale(.97)
            }
        }
        .rate-title {
            margin-right: 18px;
            width: 87px;
            font-weight: normal;
            span:nth-child(1) {
                display: block;
                text-align: center;
                height: 69px;
                font-size: 52px;
                color: $fontWeightColor;
                line-height: 69px;
            }
            span:nth-child(2) {
                height: 16px;
                font-size: 12px;
                color: $fontWeightColor;
                line-height: 16px;
                text-align: center;
                display: inline-block;
                width: 100%;
            }
        }
        .rate-card {
            flex: 1;
            .rate-info {
                display: flex;
                align-items: center;
                justify-content: flex-start;
                .rate-sum {
                    height: 15px;
                    font-size: 11px;
                    font-weight: normal;
                    color: $fontWeightColor;
                    line-height: 15px;
                }
            }
            .rate-bar {
                margin: 0 6px 0 11px;
                width: 121px;
                height: 10px;
                display: flex;
                span {
                    transition: flex 200ms
                }
                .dark-gray {
                    background: $fontGray;
                }
                .gray {
                    background: $lighterGray;
                }
            }
        }
    }
</style>
