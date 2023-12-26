<template>
    <section class="detail-title">
        <img class="detail-pic atom-logo" :src="detail.logoUrl">
        <hgroup class="store-item-detail detail-info-group">
            <h3>{{detail.name}}</h3>
            <div class="detail-info-row">
                <h5 class="detail-info">
                    <span> {{ $t('store.发布者：') }} </span>
                    <span>{{detail.publisher || '-'}}</span>
                </h5>
                <h5 class="detail-info">
                    <span> {{ $t('store.热度：') }} </span>
                    <span>{{detail.downloads || 0}}</span>
                </h5>
                
                <h5 class="detail-info detail-score" :title="$t('store.rateTips', [(detail.score || 0), (detail.totalNum || 0)])">
                    <span> {{ $t('store.评分：') }} </span>
                    <p class="score-group">
                        <comment-rate :rate="5" :width="14" :height="14" :style="{ width: starWidth }" class="score-real"></comment-rate>
                        <comment-rate :rate="0" :width="14" :height="14"></comment-rate>
                    </p>
                    <span class="rate-num">{{detail.totalNum || 0}}</span>
                </h5>
            </div>
            <div class="detail-info-row">
                <h5 class="detail-info">
                    <span> {{ $t('store.应用范畴：') }} </span>
                    <span>{{detail.categoryList|templateCategory}}</span>
                </h5>
                <h5 class="detail-info">
                    <span> {{ $t('store.分类：') }} </span>
                    <span>{{detail.classifyName || '-'}}</span>
                </h5>
            </div>
            <h5 class="detail-info detail-label">
                <span> {{ $t('store.功能标签：') }} </span>
                <p>
                    <bk-tag v-for="(label, index) in detail.labelList" :key="index">{{label.labelName}}</bk-tag>
                    <span v-if="!detail.labelList || detail.labelList.length <= 0 ">--</span>
                </p>
            </h5>
            <h5 class="detail-info detail-maxwidth" :title="detail.summary">
                <span> {{ $t('store.简介：') }} </span><span>{{detail.summary || '-'}}</span>
            </h5>
        </hgroup>

        <bk-popover placement="top" v-if="buttonInfo.disable">
            <button class="bk-button bk-primary" type="button" disabled> {{ $t('store.安装') }} </button>
            <template slot="content">
                <p>{{buttonInfo.des}}</p>
            </template>
        </bk-popover>
        <button class="detail-install" @click="goToInstall" v-else> {{ $t('store.安装') }} </button>
    </section>
</template>

<script>
    import commentRate from '../comment-rate'

    export default {
        components: {
            commentRate
        },

        filters: {
            templateCategory (list = []) {
                const nameList = list.map(item => item.categoryName) || []
                const res = nameList.join('，') || '-'
                return res
            }
        },

        props: {
            detail: Object
        },

        computed: {
            starWidth () {
                const integer = Math.floor(this.detail.score)
                const fixWidth = 17 * integer
                const rateWidth = 14 * (this.detail.score - integer)
                return `${fixWidth + rateWidth}px`
            },

            buttonInfo () {
                const info = {}
                info.disable = this.detail.defaultFlag || !this.detail.flag
                if (this.detail.defaultFlag) info.des = `${this.$t('store.通用流水线模板，所有项目默认可用，无需安装')}`
                if (!this.detail.flag) info.des = `${this.$t('store.你没有该流水线模板的安装权限，请联系流水线模板发布者')}`
                return info
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

            goToInstall () {
                this.$router.push({
                    name: 'install',
                    query: {
                        code: this.detail.templateCode,
                        type: 'template',
                        from: 'details'
                    }
                })
            }
        }
    }
</script>

<style lang="scss" scoped>
    @import '@/assets/scss/conf.scss';

    .detail-title {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin: 26px auto 0;
        width: 95vw;
        background: $white;
        box-shadow: 1px 2px 3px 0px rgba(0,0,0,0.05);
        padding: 32px;
        .detail-pic {
            width: 130px;
        }
        .atom-icon {
            height: 160px;
            width: 160px;
        }
        button {
            border-radius: 4px;
            width: 120px;
            height: 40px;
        }
        .detail-install {
            width: 120px;
            height: 40px;
            background: $primaryColor;
            border: none;
            font-size: 14px;
            color: $white;
            line-height: 40px;
            text-align: center;
            &.opicity-hidden {
                opacity: 0;
                user-select: none;
            }
            &:active {
                transform: scale(.97)
            }
        }
    }
</style>
