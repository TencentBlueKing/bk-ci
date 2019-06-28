<template>
    <hgroup class="detail-info-group">
        <template v-if="type === 'atom'">
            <h3 class="title-with-img">
                {{detail.name}}
                <h5 :title="isPublicTitle">
                    <icon v-if="isPublic" class="detail-img" name="color-git-code" @click.native="goToCode" size="17" />
                    <icon v-else class="not-public detail-img" name="gray-git-code" size="17" style="fill:#9E9E9E" />
                </h5>
            </h3>
            <h5 class="detail-info">
                <span>发布者：</span><span>{{detail.publisher || '-'}}</span>
            </h5>
            <h5 class="detail-info">
                <span>版本：</span><span>{{detail.version || '-'}}</span>
            </h5>
            <h5 class="detail-info detail-score" :title="`平均评分为${detail.score || 0}星（总分为5星），${detail.totalNum || 0}位用户评价了此项内容`">
                <span>评分：</span>
                <p class="score-group">
                    <comment-rate :rate="5" :width="14" :height="14" :style="{ width: starWidth }" class="score-real"></comment-rate>
                    <comment-rate :rate="0" :width="14" :height="14"></comment-rate>
                </p>
                <span class="rate-num">{{detail.totalNum || 0}}</span>
            </h5>
            <h5 class="detail-info">
                <span>操作系统：</span>
                <span>
                    <template v-if="detail.os && detail.os.length">
                        <i v-for="item in getJobList(detail.os)" :class="[item.icon, 'bk-icon']" :key="item" :title="item.name"></i>
                    </template>
                </span>
            </h5>
            <h5 class="detail-info">
                <span>分类：</span><span>{{detail.classifyName || '-'}}</span>
            </h5>
            <h5 class="detail-info">
                <span>热度：</span><span>{{detail.downloads || 0}}</span>
            </h5>
            <h5 class="detail-info detail-label">
                <span>功能标签：</span>
                <span v-for="(label, index) in detail.labelList" :key="index" class="info-label">{{label.labelName}}</span>
                <span v-if="!detail.labelList || detail.labelList.length <= 0 ">-</span>
            </h5>
            <h5 class="detail-info detail-maxwidth" :title="detail.summary">
                <span>简介：</span><span>{{detail.summary || '-'}}</span>
            </h5>
        </template>

        <template v-else>
            <h3>{{detail.name}}</h3>
            <h5 class="detail-info">
                <span>发布者：</span><span>{{detail.publisher || '-'}}</span>
            </h5>
            <h5 class="detail-info">
                <span>热度：</span><span>{{detail.downloads || 0}}</span>
            </h5>
            <h5 class="detail-info detail-score" :title="`平均评分为${detail.score || 0}星（总分为5星），${detail.totalNum || 0}位用户评价了此项内容`">
                <span>评分：</span>
                <p class="score-group">
                    <comment-rate :rate="5" :width="14" :height="14" :style="{ width: starWidth }" class="score-real"></comment-rate>
                    <comment-rate :rate="0" :width="14" :height="14"></comment-rate>
                </p>
                <span class="rate-num">{{detail.totalNum || 0}}</span>
            </h5>
            <h5 class="detail-info">
                <span>应用范畴：</span><span>{{detail.categoryList|templateCategory}}</span>
            </h5>
            <h5 class="detail-info">
                <span>分类：</span><span>{{detail.classifyName || '-'}}</span>
            </h5>
            <h5 class="detail-info detail-label">
                <span>功能标签：</span>
                <span v-for="(label, index) in detail.labelList" :key="index" class="info-label">{{label.labelName}}</span>
                <span v-if="!detail.labelList || detail.labelList.length <= 0 ">-</span>
            </h5>
            <h5 class="detail-info detail-maxwidth" :title="detail.summary">
                <span>简介：</span><span>{{detail.summary || '-'}}</span>
            </h5>
        </template>
    </hgroup>
</template>

<script>
    import commentRate from '../comment-rate'

    export default {
        components: {
            commentRate
        },

        filters: {
            atomJobType (val) {
                switch (val) {
                    case 'AGENT':
                        return '编译环境'
                    case 'AGENT_LESS':
                        return '无编译环境'
                }
            },

            templateCategory (list = []) {
                const nameList = list.map(item => item.categoryName) || []
                const res = nameList.join('，') || '-'
                return res
            }
        },

        props: {
            detail: Object,
            type: String
        },

        computed: {
            starWidth () {
                const integer = Math.floor(this.detail.score)
                const fixWidth = 17 * integer
                const rateWidth = 14 * (this.detail.score - integer)
                return `${fixWidth + rateWidth}px`
            },

            isPublic () {
                return this.detail.visibilityLevel === 'LOGIN_PUBLIC'
            },

            isPublicTitle () {
                if (this.isPublic) return '查看源码'
                else return '未开源'
            }
        },

        methods: {
            getJobList (os) {
                const jobList = []
                os.forEach((item) => {
                    switch (item) {
                        case 'LINUX':
                            jobList.push({ icon: 'icon-linux-view', name: 'Linux' })
                            break
                        case 'WINDOWS':
                            jobList.push({ icon: 'icon-windows', name: 'Windows' })
                            break
                        case 'MACOS':
                            jobList.push({ icon: 'icon-macos', name: 'macOS' })
                            break
                    }
                })
                return jobList
            },

            goToCode () {
                window.open(this.detail.codeSrc, '_blank')
            }
        }
    }
</script>

<style lang="scss" scoped>
    @import '@/assets/scss/conf.scss';

    .detail-info-group {
        width: 829px;
        margin: 0 76px;
        h3 {
            font-size: 22px;
            line-height: 29px;
            color: $fontBlack;
        }
        .detail-score {
            display: flex;
            align-items: center;
            .score-group {
                position: relative;
                .score-real {
                    position: absolute;
                    overflow: hidden;
                    left: 0;
                    top: 0;
                    height: 14px;
                    display: flex;
                    .yellow {
                        min-width: 14px;
                    }
                }
            }
            .rate-num {
                margin-top: 2px;
                margin-left: 6px;
                color: $fontWeightColor;
            }
        }
        .detail-info {
            float: left;
            display: flex;
            padding-top: 7px;
            width: 33.33%;
            font-size: 14px;
            font-weight: normal;
            line-height: 19px;
            color: $fontBlack;
            span:nth-child(1) {
                color: $fontWeightColor;
                display: inline-block;
                width: 70px;
                text-align: right;
            }
            span:nth-child(2) {
                overflow: hidden;
                text-overflow: ellipsis;
                white-space: nowrap;
                display: inline-block;
                width: calc(100% - 70px);
            }
        }
        .title-with-img {
            display: flex;
            align-items: center;
            .detail-img {
                margin-left: 6px;
                vertical-align: baseline;
                cursor: pointer;
            }
            .not-public {
                cursor: auto;
            }
        }
        .detail-info.detail-label {
            width: 829px;
            padding-left: 70px;
            display: inline-block;
            position: relative;
            span {
                overflow: inherit;
                margin-bottom: 7px;
            }
            span:first-child {
                position: absolute;
                left: 0;
            }
            span.info-label {
                display: inline-block;
                width: auto;
                height: 19px;
                padding: 0 7px;
                border: 1px solid $laberColor;
                border-radius: 20px;
                margin-right: 8px;
                line-height: 17px;
                text-align: center;
                font-size: 12px;
                color: $laberColor;
                background-color: $laberBackColor;
            }
        }
        .detail-maxwidth {
            max-width: 100%;
            width: auto;
            padding-top: 0;
        }
    }
</style>
