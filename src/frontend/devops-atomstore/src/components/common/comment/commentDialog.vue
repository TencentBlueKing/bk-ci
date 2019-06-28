<template>
    <article class="add-comment" @click.self="cancle">
        <section class="add-main" v-bkloading="{ isLoading }">
            <h3 class="add-title">为{{name}}评分</h3>
            <comment-rate class="add-rate" :edit="true" :rate="rate" :height="24" :width="23" @chooseRate="chooseRate"></comment-rate>
            <textarea class="add-content g-input-border" v-model="comment" placeholder="请输入你的评论内容（字数上限为500字）" ref="commentText"></textarea>
            <h3 class="g-confirm-buttom">
                <button @click="cancle">取消</button><button @click="confirm">确定</button>
            </h3>
        </section>
    </article>
</template>

<script>
    import { mapActions } from 'vuex'
    import commentRate from '../comment-rate'

    export default {
        components: {
            commentRate
        },

        props: {
            name: String,
            id: String,
            commentId: String,
            code: String
        },

        data () {
            return {
                isLoading: false,
                rate: 0,
                comment: '',
                modifyCommentGenerator: {
                    atom: (data) => this.requestAtomModifyComment(data),
                    template: (data) => this.requestTemplateModifyComment(data)
                },
                addCommentGenerator: {
                    atom: (postData) => this.requestAddAtomComment(postData),
                    template: (postData) => this.requestAddTemplateComment(postData)
                },
                getCommentGenerator: {
                    atom: () => this.requestAtomUserComment(this.commentId),
                    template: () => this.requestTemplateUserComment(this.commentId)
                }
            }
        },

        computed: {
            type () {
                return this.$route.params.type
            }
        },

        mounted () {
            this.getComment()
        },
        
        methods: {
            ...mapActions('store', [
                'requestAddAtomComment',
                'requestAddTemplateComment',
                'requestTemplateModifyComment',
                'requestTemplateUserComment',
                'requestAtomModifyComment',
                'requestAtomUserComment'
            ]),

            getComment () {
                if (this.commentId) {
                    this.isLoading = true
                    const method = this.getCommentGenerator[this.type]
                    method().then((res) => {
                        this.rate = res.score || 5
                        this.comment = res.commentContent || ''
                    }).catch((err) => {
                        this.$bkMessage({ message: (err.message || err), theme: 'error' })
                    }).finally(() => {
                        this.$refs.commentText.focus()
                        this.isLoading = false
                    })
                }
            },

            chooseRate (rate) {
                this.rate = rate
            },

            cancle () {
                this.$emit('closeDialog')
            },

            confirm () {
                if (this.rate <= 0) {
                    this.$bkMessage({ message: '请先评分，再发布评价', theme: 'warning' })
                    return
                }

                if (this.comment.length >= 500) {
                    this.$bkMessage({ message: '字数不能超过500字，请修改后再评价', theme: 'warning' })
                    return
                }

                const confirmGenerator = {
                    modify: () => this.modifyComment(),
                    add: () => this.addComment()
                }
                const type = this.commentId ? 'modify' : 'add'

                confirmGenerator[type]().then((res) => {
                    this.$bkMessage({ message: '评论成功', theme: 'success' })
                    this.$emit('freshComment', res)
                    this.$emit('closeDialog')
                }).catch((err) => {
                    this.$bkMessage({ message: (err.message || err), theme: 'error' })
                })
            },

            modifyComment () {
                const data = {
                    id: this.commentId,
                    postData: {
                        commentContent: this.comment,
                        score: this.rate
                    }
                }
                return this.modifyCommentGenerator[this.type](data).then(() => ({
                    commentId: this.commentId,
                    commentContent: this.comment,
                    score: this.rate,
                    commentTime: new Date()
                }))
            },

            addComment () {
                const data = {
                    id: this.id,
                    code: this.code,
                    postData: {
                        commentContent: this.comment,
                        score: this.rate
                    }
                }
                return this.addCommentGenerator[this.type](data)
            }
        }
    }
</script>

<style lang="scss" scoped>
    @import '@/assets/scss/conf.scss';

    .add-comment {
        position: fixed;
        left: 0;
        right: 0;
        top: 0;
        bottom: 0;
        background: rgba(0,0,0,0.6);
        .add-main {
            margin: 0 auto;
            margin-top: calc(50vh - 154px);
            width: 640px;
            height: 308px;
            background: $white;
            box-shadow: 0px 3px 7px 0px rgba(0,0,0,0.1);
            border-radius: 2px;
            .add-title {
                padding: 21px 25px 0;
                height: 29px;
                box-sizing: content-box;
                font-size: 22px;
                color: $fontBlack;
                line-height: 29px;
            }
            .add-rate {
                margin: 18px 0 0 25px;
            }
            .add-content {
                resize: none;
                margin: 8px 24px 26px 24px;
                width: 592px;
                height: 132px;
                line-height: 19px;
                font-size: 14px;
                padding: 8px 11px;
                &:focus {
                    border: 1px solid $primaryColor;
                    outline: none;
                }
            }
        }
    }
</style>
