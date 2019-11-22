<template>
    <li :class="[{ 'active': currentItem === card.code, 'disable': !card.availableFlag }, 'select-card']" @click="clickItem" ref="card" v-bk-tooltips="toolTip">
        <section class="card-info">
            <img :src="card.logoUrl" class="info-pic">
            <p class="info-main">
                <span class="main-name">{{card.name}}<logo class="bk-icon" name="LDImage" size="13" v-if="card.certificationFlag" /></span>
                <span class="main-summary" :title="card.summary">{{card.summary}}</span>
                <span class="main-repo" :title="`${card.imageRepoUrl}/${card.imageRepoName}:${card.imageTag}`">
                    <logo class="bk-icon" name="imagedocker" size="10" />
                    {{card.imageRepoUrl}}/{{card.imageRepoName}}:{{card.imageTag}}
                </span>
                <ul class="main-label" v-if="card.labelNames">
                    <li v-for="label in card.labelNames.split(',')" :key="label">{{ label }}</li>
                </ul>
            </p>
            <template v-if="card.availableFlag">
                <template v-if="type === 'store'">
                    <bk-button size="small" class="info-button" @click="choose" v-if="card.installed">{{code === card.code ? '已选' : '选择'}}</bk-button>
                    <bk-button size="small" class="info-button" @click="installImage" v-else-if="card.flag" :loading="isInstalling">安装</bk-button>
                    <bk-button size="small" class="info-button" v-else :disabled="true" title="暂无权限安装该镜像">安装</bk-button>
                </template>
                <bk-button size="small" class="info-button" @click="choose" v-else>{{code === card.code ? '已选' : '选择'}}</bk-button>
            </template>
        </section>
        <p class="card-link">
            <span class="link-pub">由{{card.publisher}}提供</span>
            <a class="link-more" :href="card.docsLink" target="_blank">了解更多</a>
        </p>
    </li>
</template>

<script>
    import logo from '@/components/Logo'

    export default {
        components: {
            logo
        },

        props: {
            code: {
                type: String,
                required: true
            },

            card: {
                type: Object,
                required: true
            },

            currentItem: {
                type: String,
                required: true
            },

            type: {
                type: String
            }
        },

        data () {
            return {
                isInstalling: false,
                toolTip: {
                    content: !this.card.availableFlag ? `在当前构建资源类型下不可用${this.card.agentTypeScope.length ? '，仅支持' : ''}${this.card.agentTypeScope.map((item) => {
                        let res = ''
                        switch (item) {
                            case 'DOCKER':
                                res = 'Devnet 物理机'
                                break
                            case 'IDC':
                                res = 'IDC CVM'
                                break
                            case 'PUBLIC_DEVCLOUD':
                                res = 'DevCloud'
                                break
                        }
                        return res
                    }).join('，')}` : '',
                    appendTo: () => this.$refs.card
                }
            }
        },

        methods: {
            clickItem () {
                this.$emit('update:currentItem', this.card.code)
            },

            choose (event) {
                event.preventDefault()
                if (this.code === this.card.code) return
                this.$emit('choose', this.card)
            },

            installImage () {
                const postData = {
                    imageCode: this.card.code,
                    projectCodeList: [this.$route.params.projectId]
                }
                this.isInstalling = true
                this.$store.dispatch('pipelines/requestInstallImage', postData).then((res) => {
                    this.card.installed = true
                    this.$showTips({ theme: 'success', message: '安装成功' })
                }).catch((err) => {
                    this.$showTips({ theme: 'error', message: err.message || err })
                }).finally(() => (this.isInstalling = false))
            }
        }
    }
</script>

<style lang="scss" scoped>
    @import '../../scss/conf';

    .select-card {
        padding: 20px 15px 18px;
        position: relative;
        /deep/ .tippy-popper {
            display: none;
            transition: display 200ms;
        }
        &:hover {
            background: #fafbfd;
            .card-link, /deep/ .tippy-popper {
                display: block;
            }
        }
        &.active {
            background: #e9f4ff;
            .card-link {
                display: block;
            }
        }
        &.disable {
            .info-main, .card-link, .info-main .main-name {
                color: #c3cdd7;
            }
        }
    }

    .card-link {
        display: none;
        position: absolute;
        width: 100%;
        left: 0;
        bottom: 9px;
        padding-left: 80px;
        font-size: 12px;
        color: #979ba5;
        line-height: 16px;
        .link-more {
            position: absolute;
            right: 15px;
            color: $primaryColor;
            cursor: pointer;
        }
    }

    .card-info {
        display: flex;
        align-items: flex-start;
        margin-bottom: 10px;
        .info-pic {
            height: 50px;
            min-width: 50px;
            display: block;
        }
        .info-main {
            flex: 1;
            padding: 0 15px;
            color: $fontWeightColor;
            font-size: 12px;
            color: #63656e;
            line-height: 16px;
            .main-name {
                display: flex;
                align-items: center;
                font-size: 14px;
                color: #313238;
                line-height: 19px;
                font-weight: bold;
                margin-bottom: 2px;
                .bk-icon {
                    margin-left: 4px;
                }
            }
            .main-summary {
                overflow: hidden;
                text-overflow: ellipsis;
                white-space: nowrap;
                max-width: 350px;
                display: block;
            }
            .main-repo {
                display: block;
                overflow: hidden;
                text-overflow: ellipsis;
                white-space: nowrap;
                max-width: 350px;
            }
            .main-label {
                &::after {
                    content: '';
                    display: table;
                    clear: both;
                }
                li {
                    float: left;
                    margin: 4px 4px 0 0;
                    padding: 0 6px;
                    box-sizing: border-box;
                    height: 20px;
                    line-height: 20px;
                    color: #81939f;
                    background: #f1f4f6;
                }
            }
        }
        .info-button:not(.is-disabled):hover {
            background: $primaryColor;
            color: #fff;
        }
    }
</style>
