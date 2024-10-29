<template>
    <section class="detail-title">
        <img
            class="detail-pic atom-logo"
            :src="detail.logoUrl"
        >
        <hgroup class="store-item-detail detail-info-group">
            <h3
                :class="[{ 'not-recommend': detail.recommendFlag === false }, 'title-with-img']"
                :title="detail.recommendFlag === false ? $t('store.该镜像不推荐使用') : ''"
            >
                {{ detail.name }}
            </h3>
            <div class="detail-info-row">
                <h5 class="detail-info">
                    <span> {{ $t('store.发布者：') }} </span><span>{{ detail.publisher || '-' }}</span>
                </h5>
                <h5 class="detail-info">
                    <span> {{ $t('store.版本：') }} </span><span>{{ detail.version || '-' }}</span>
                </h5>
                <h5
                    class="detail-info detail-score"
                    :title="$t('store.rateTips', [(detail.score || 0), (detail.totalNum || 0)])"
                >
                    <span> {{ $t('store.评分：') }} </span>
                    <p class="score-group">
                        <comment-rate
                            :rate="5"
                            :width="14"
                            :height="14"
                            :style="{ width: starWidth }"
                            class="score-real"
                        ></comment-rate>
                        <comment-rate
                            :rate="0"
                            :width="14"
                            :height="14"
                        ></comment-rate>
                    </p>
                    <span class="rate-num">{{ detail.totalNum || 0 }}</span>
                </h5>
            </div>
            <div class="detail-info-row">
                <h5 class="detail-info">
                    <span> {{ $t('store.镜像源：') }} </span><span>{{ detail.imageSourceType | imageTypeFilter }}</span>
                </h5>
                <h5 class="detail-info">
                    <span> {{ $t('store.分类：') }} </span><span>{{ detail.classifyName || '-' }}</span>
                </h5>
                <h5 class="detail-info">
                    <span> {{ $t('store.热度：') }} </span><span>{{ detail.downloads || 0 }}</span>
                </h5>
            </div>
            <h5
                class="detail-info detail-label"
                :title="`${detail.imageRepoUrl}${detail.imageRepoUrl ? '/' : ''}${detail.imageRepoName}:${detail.imageTag}`"
            >
                <span> {{ $t('store.镜像地址：') }} </span>
                <span class="detail-image-address">
                    <e>{{ detail.imageRepoUrl }}{{ detail.imageRepoUrl ? '/' : '' }}{{ detail.imageRepoName }}:{{ detail.imageTag }}</e>
                    <i
                        class="bk-icon icon-clipboard"
                        :title="$t('store.复制')"
                        @click="copyImagePath(`${detail.imageRepoUrl}${detail.imageRepoUrl ? '/' : ''}${detail.imageRepoName}:${detail.imageTag}`)"
                    />
                </span>
            </h5>
            <h5 class="detail-info detail-label">
                <span> {{ $t('store.功能标签：') }} </span>
                <p>
                    <bk-tag
                        v-for="(label, index) in detail.labelList"
                        :key="index"
                    >
                        {{ label.labelName }}
                    </bk-tag>
                    <span v-if="!detail.labelList || detail.labelList.length <= 0 ">--</span>
                </p>
            </h5>
            <h5
                class="detail-info detail-label"
                :title="detail.summary"
            >
                <span> {{ $t('store.简介：') }} </span><span>{{ detail.summary || '-' }}</span>
            </h5>
        </hgroup>
        <template v-if="detail.needInstallToProject === 'NEED_INSTALL_TO_PROJECT_TRUE'">
            <bk-popover
                placement="top"
                v-if="buttonInfo.disable"
            >
                <button
                    class="bk-button bk-primary"
                    type="button"
                    disabled
                >
                    {{ $t('store.安装') }}
                </button>
                <template slot="content">
                    <p>{{ buttonInfo.des }}</p>
                </template>
            </bk-popover>
            <button
                class="detail-install"
                @click="goToInstall"
                v-else
            >
                {{ $t('store.安装') }}
            </button>
        </template>
    </section>
</template>

<script>
    import commentRate from '../comment-rate'

    export default {
        components: {
            commentRate
        },

        filters: {
            imageTypeFilter (val) {
                const local = window.devops || {}
                let res = ''
                switch (val) {
                    case 'THIRD':
                        res = local.$t('store.第三方源')
                        break
                    case 'BKDEVOPS':
                        res = local.$t('store.蓝盾源')
                        break
                }
                return res
            }
        },

        props: {
            detail: Object
        },

        data () {
            return {
                user: JSON.parse(localStorage.getItem('_cache_userInfo')).username,
                isLoading: false
            }
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
                info.disable = this.detail.publicFlag || !this.detail.flag
                if (this.detail.publicFlag) info.des = `${this.$t('store.通用镜像，所有项目默认可用，无需安装')}`
                if (!this.detail.flag) info.des = `${this.$t('store.你没有该镜像的安装权限，请联系镜像发布者')}`
                return info
            }
        },

        methods: {
            copyImagePath (val) {
                const input = document.createElement('input')
                document.body.appendChild(input)
                input.setAttribute('value', val)
                input.select()
                if (document.execCommand('copy')) {
                    document.execCommand('copy')
                    this.$bkMessage({ theme: 'success', message: this.$t('store.复制成功') })
                }
                document.body.removeChild(input)
            },

            goToInstall () {
                this.$router.push({
                    name: 'install',
                    query: {
                        code: this.detail.imageCode,
                        type: 'image',
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
