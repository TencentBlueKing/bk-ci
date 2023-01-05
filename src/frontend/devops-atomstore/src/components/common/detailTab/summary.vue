<template>
    <section>
        <section>
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
            <p class="g-empty summary-empty" v-if="!detail.description"> {{ $t('发布者很懒，什么都没留下！') }} </p>
        </section>
        <section>
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
        </section>
        <transition name="atom-fade">
            <commentDialog v-if="showComment" @freshComment="freshComment" @closeDialog="showComment = false" :name="detail.name" :code="detailCode" :id="detailId" :comment-id="commentInfo.commentId"></commentDialog>
        </transition>
    </section>
</template>

<script>
    export default {
        
    }
</script>

<style lang="scss" scoped>
    
</style>