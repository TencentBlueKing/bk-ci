<template>
    <h3 class="comment-main" :class="{ 'comment-reply': isReply }">
        <img :src="comment.profileUrl" class="comment-photo">
        <h5 class="commenter-info">
            <span>{{comment.commenter}}</span><span>{{comment.commenterDept}}</span>
        </h5>
        <p class="comment-content">{{comment.commentContent}}</p>
        <h5 class="comment-static">
            <p class="comment-info">
                <comment-rate :rate="comment.score" :width="11" :height="12" class="commet-rate" v-if="!isReply"></comment-rate>
                <span class="comment-replay" @click="clickReply"> {{ $t('store.回复') }} <span v-if="+comment.replyCount">({{comment.replyCount}})</span>
                </span>
                <icon class="comment-praise" :style="{ 'fill': comment.praiseFlag ? '#979BA5' : 'none' }" name="praise" size="14" @click.native="priase" v-if="!isReply" />
                <span v-if="!isReply">{{comment.praiseCount}}</span>
            </p>
            <span>{{comment.updateTime|timeFilter}}</span>
        </h5>
    </h3>
</template>

<script>
    import { mapActions, mapGetters } from 'vuex'
    import commentRate from '../comment-rate'

    export default {
        components: {
            commentRate
        },

        filters: {
            timeFilter (val) {
                const local = window.devops || {}
                const date = new Date(val)
                const year = date.getFullYear()
                const month = date.getMonth() + 1
                const day = date.getDate()
                return `${year + local.$t('store.年') + month + local.$t('store.月') + day + local.$t('store.日')}`
            }
        },

        props: {
            parentId: String,
            commentData: Object,
            isReply: Boolean
        },

        data () {
            return {
                funObj: {
                    expandReplys: {
                        atom: (id) => this.requestAtomReplyList(id),
                        template: (id) => this.requestTemplateReplyList(id),
                        ide: (id) => this.requestIDEReplyList(id),
                        image: (id) => this.requestImageReplyList(id),
                        service: (id) => this.requestServiceReplyList(id)
                    },
                    priase: {
                        atom: (id) => this.requestAtomPraiseComment(id),
                        template: (id) => this.requestTemplatePraiseComment(id),
                        ide: (id) => this.requestIDEPraiseComment(id),
                        image: (id) => this.requestImagePraiseComment(id),
                        service: (id) => this.requestServicePraiseComment(id)
                    }
                },
                hadShowMore: false
            }
        },

        computed: {
            ...mapGetters('store', { commentList: 'getCommentList' }),

            comment () {
                const data = this.commentData || {}
                if (this.isReply) {
                    const preContent = data.replyToUser ? `${this.$t('store.回复')}@${data.replyToUser}：` : ''
                    data.commentContent = preContent + data.replyContent
                    data.commentId = data.replyId
                    data.commenter = data.replyer
                    data.commenterDept = data.replyerDept
                }
                return data
            }
        },

        methods: {
            ...mapActions('store', [
                'setCommentReplay',
                'setCommentPraise',
                'requestAtomReplyList',
                'requestAtomPraiseComment',
                'requestTemplatePraiseComment',
                'requestTemplateReplyList',
                'requestIDEPraiseComment',
                'requestIDEReplyList',
                'requestImageReplyList',
                'requestImagePraiseComment',
                'requestServiceReplyList',
                'requestServicePraiseComment',
                'clearCommentReply'
            ]),

            clickReply () {
                if (this.hadShowMore) {
                    this.hadShowMore = false
                    this.$parent.showWriteReply = false
                    this.clearCommentReply(this.commentData.commentId)
                    return
                }

                this.setReplyTo()
                this.expandReplys().then(() => {
                    this.$parent.showWriteReply = true
                    this.$nextTick(() => this.$parent.$refs.replyText.focus())
                })
            },

            setReplyTo () {
                const toUser = this.isReply ? this.commentData.commenter : ''
                this.$emit('replyComment', toUser)
            },

            expandReplys () {
                if (this.isReply) return Promise.resolve()

                const type = this.$route.params.type
                const id = this.commentData.commentId

                return this.funObj.expandReplys[type](id).then((res) => {
                    this.setCommentReplay({ id, newList: res, isAdd: false })
                    this.hadShowMore = true
                }).catch(err => this.$bkMessage({ message: (err.message || err), theme: 'error' }))
            },

            priase () {
                const type = this.$route.params.type
                const id = this.commentData.commentId

                this.funObj.priase[type](id).then((count) => {
                    this.setCommentPraise({ id, count })
                }).catch(err => this.$bkMessage({ message: (err.message || err), theme: 'error' }))
            }
        }
    }
</script>

<style lang="scss" scoped>
    @import '@/assets/scss/conf.scss';

    .comment-main {
        margin-top: 35px;
        padding-bottom: 8px;
        &.comment-reply {
            border-top: 1px solid $borderWeightColor;
            padding-top: 15px;
            margin-left: 77px;
            margin-top: 0px;
            &:last-child {
                border-bottom: 1px solid $borderWeightColor;
            }
            .comment-photo {
                height: 40px;
                width: 40px;
            }
            .commenter-info, .comment-content, .comment-static {
                margin-left: 52px;
            }
        }
        .comment-photo {
            float: left;
            height: 60px;
            width: 60px;
            border-radius: 100%;
        }
        .commenter-info {
            margin-left: 77px;
            font-weight: normal;
            height: 16px;
            font-size: 12px;
            color: $fontDarkBlack;
            line-height: 16px;
            span:nth-child(2) {
                display: inline-block;
                color: #999999;
                margin-left: 10px;
            }
        }
        .comment-content {
            margin: 9px 0 7px 77px;
            font-size: 14px;
            color: $fontDarkBlack;
            line-height: 19px;
            min-height: 19px;
            font-weight: normal;
            word-wrap: break-word;
        }
        .comment-static {
            margin-left: 77px;
            display: flex;
            align-items: center;
            justify-content: space-between;
            height: 16px;
            font-size: 14px;
            font-weight: normal;
            color: #999999;
            line-height:16px;
            .comment-info {
                display: flex;
                align-items: center;
            }
            .commet-rate {
                margin-right: 23px;
            }
            .comment-replay {
                cursor: pointer;
                margin-right: 23px;
                height: 16px;
                color: $iconPrimaryColor;
                line-height: 16px;
            }
            .comment-praise {
                cursor: pointer;
                margin-right: 2px;
            }
        }
    }
</style>
