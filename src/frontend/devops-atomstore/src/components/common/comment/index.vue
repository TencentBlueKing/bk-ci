<template>
    <section>
        <reply :comment-data="comment.data" :parent-id="comment.commentId" @replyComment="replyComment" ref="reply"></reply>
        <reply :comment-data="child"
            :parent-id="comment.commentId"
            @replyComment="replyComment"
            :is-reply="true"
            v-for="child in comment.children"
            :key="child.commentId"
        >
        </reply>
        <div class="comment-reply-text" v-if="showWriteReply">
            <textarea :placeholder="$t('store.comment.replayPlaceholder')" v-model="reply" class="reply-content g-input-border" ref="replyText" autofocus></textarea>
            <h3 class="g-confirm-buttom reply-button">
                <button @click="cancleComment">{{ $t('store.cancel') }}</button><button @click="confirmComment">{{ $t('store.release') }}</button>
            </h3>
        </div>
    </section>
</template>

<script>
    import { mapActions, mapGetters } from 'vuex'
    import reply from './reply.vue'

    export default {
        components: {
            reply
        },

        props: {
            comment: Object
        },

        data () {
            return {
                reply: '',
                replyToUser: '',
                showWriteReply: false
            }
        },

        computed: {
            ...mapGetters('store', { 'commentList': 'getCommentList' })
        },

        watch: {
            reply (val) {
                const reg = this.replyToUser ? new RegExp(`^${this.$t('store.comment.reply')}@${this.replyToUser}：`) : /^/
                const isMatchUser = reg.test(val)
                if (!isMatchUser) this.replyToUser = ''
            }
        },

        methods: {
            ...mapActions('store', [
                'requestAtomReplyComment',
                'requestTemplateReplyComment',
                'requestIDEReplyComment',
                'requestImageReplyComment',
                'setCommentReplay',
                'clearCommentReply'
            ]),

            replyComment (user) {
                const reg = this.replyToUser ? new RegExp(`^${this.$t('store.comment.reply')}@${this.replyToUser}：`) : /^/
                const replaceStr = user ? `${this.$t('store.comment.reply')}@${user}：` : ''

                this.replyToUser = user
                this.reply = this.reply.replace(reg, replaceStr)
            },

            cancleComment () {
                this.reply = ''
                this.showWriteReply = false
                if (this.comment.children.length <= 0) this.$refs.reply.hadShowMore = false
            },

            confirmComment () {
                let replyContent = this.reply
                if (replyContent.trim() === '') {
                    this.$bkMessage({ message: this.$t('store.comment.replyContent'), theme: 'warning' })
                    return
                }

                if (replyContent.length > 500) {
                    this.$bkMessage({ message: this.$t('store.comment.contentLenLimit'), theme: 'warning' })
                    return
                }

                const reg = new RegExp(`^${this.$t('store.comment.reply')}@${this.replyToUser}：`)
                replyContent = replyContent.replace(reg, '')

                const id = this.comment.data.commentId
                const type = this.$route.params.type
                const postData = { replyContent, replyToUser: this.replyToUser }
                const funObj = {
                    atom: () => this.requestAtomReplyComment({ id, postData }),
                    template: () => this.requestTemplateReplyComment({ id, postData }),
                    ide: () => this.requestIDEReplyComment({ id, postData }),
                    image: () => this.requestImageReplyComment({ id, postData })
                }

                funObj[type]().then((res) => {
                    this.setCommentReplay({ id, newList: [res], isAdd: true })
                    this.reply = ''
                }).catch(err => this.$bkMessage({ message: (err.message || err), theme: 'error' }))
            }
        }
    }
</script>

<style lang="scss" scoped>
    @import '@/assets/scss/conf.scss';

    .comment-reply-text {
        margin: 8px 0;
        .reply-content {
            resize: none;
            width: 1050px;
            height: 56px;
            padding: 5px 5px;
            margin-left: 59px;
            &:focus {
                border: 1px solid $primaryColor;
                outline: none;
            }
        }
        .reply-button {
            border: none;
            background: none;
            padding-right: 0;
            width: 1050px;
            margin-left: 59px;
            button {
                margin: 10px 5px;
                height: 28px;
                line-height: 28px;
                font-size: 12px;
                &:first-child {
                    margin-right: 0;
                    line-height: 26px;
                }
            }
        }
    }
</style>
