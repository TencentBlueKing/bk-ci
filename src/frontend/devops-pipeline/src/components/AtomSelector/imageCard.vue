<template>
    <li
        :class="[{ 'active': currentItem === card.code, 'disable': !card.availableFlag }, 'select-card']"
        @click="clickItem"
        ref="card"
        v-bk-tooltips="toolTip"
    >
        <section class="card-info">
            <img
                :src="card.logoUrl"
                class="info-pic"
            >
            <p class="info-main">
                <span class="main-name">
                    <span
                        :class="{ 'not-recommend': card.recommendFlag === false }"
                        :title="card.recommendFlag === false ? $t('editPage.notRecomendImage') : ''"
                    >{{ card.name }}</span>
                    <span
                        :title="$t('editPage.officialCertification')"
                        class="icon-title"
                    >
                        <logo
                            class="devops-icon"
                            name="LDImage"
                            size="13"
                            v-if="card.certificationFlag"
                        />
                    </span>
                </span>
                <span
                    class="main-summary"
                    :title="card.summary"
                >{{ card.summary }}</span>
                <span
                    class="main-repo"
                    :title="`${card.imageRepoUrl}${card.imageRepoUrl ? '/' : ''}${card.imageRepoName}:${card.imageTag}`"
                >
                    <logo
                        class="devops-icon"
                        name="imagedocker"
                        size="10"
                    />
                    {{ card.imageRepoUrl }}{{ card.imageRepoUrl ? '/' : '' }}{{ card.imageRepoName }}:{{ card.imageTag }}
                </span>
                <ul
                    class="main-label"
                    v-if="card.labelNames"
                >
                    <li
                        v-for="label in card.labelNames.split(',')"
                        :key="label"
                    >
                        {{ label }}
                    </li>
                </ul>
            </p>
            <template v-if="card.availableFlag">
                <template v-if="type === 'store'">
                    <bk-button
                        size="small"
                        class="info-button"
                        @click="choose"
                        :disabled="code === card.code"
                        v-if="card.installedFlag"
                    >
                        {{ code === card.code ? this.$t('editPage.selected') : this.$t('editPage.select') }}
                    </bk-button>
                    <bk-button
                        size="small"
                        class="info-button"
                        @click="installImage"
                        v-else-if="card.flag"
                        :loading="isInstalling"
                    >
                        {{ $t('editPage.install') }}
                    </bk-button>
                    <bk-button
                        size="small"
                        class="info-button"
                        v-else
                        :disabled="true"
                        :title="$t('editPage.noInstallRight')"
                    >
                        {{ $t('editPage.install') }}
                    </bk-button>
                </template>
                <bk-button
                    size="small"
                    class="info-button"
                    :disabled="code === card.code"
                    @click="choose"
                    v-else
                >
                    {{ code === card.code ? this.$t('editPage.selected') : this.$t('editPage.select') }}
                </bk-button>
            </template>
        </section>
        <p class="card-link">
            <span class="link-pub">{{ $t('editPage.provideInfo', [card.publisher, card.modifier, convertTime(card.updateTime)]) }}</span>
            <a
                class="link-more"
                @click="goToStore"
            >{{ $t('editPage.knowMore') }}</a>
        </p>
    </li>
</template>

<script>
    import logo from '@/components/Logo'
    import { convertTime } from '@/utils/util.js'

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
                    content: !this.card.availableFlag
                        ? `${this.$t('editPage.notWorkAtCur')}${this.card.agentTypeScope.length ? `，${this.$t('editPage.onlySupport')}` : ''}${this.card.agentTypeScope.map((item) => {
                            let res = ''
                            switch (item) {
                                case 'DOCKER':
                                    res = this.$t('editPage.devnet')
                                    break
                                case 'IDC':
                                    res = 'IDC CVM'
                                    break
                                case 'PUBLIC_DEVCLOUD':
                                    res = 'DevCloud'
                                    break
                            }
                            return res
                        }).join('，')}`
                        : '',
                    appendTo: () => this.$refs.card
                }
            }
        },

        methods: {
            goToStore () {
                window.open(`${WEB_URL_PREFIX}/store/atomStore/detail/image/${this.card.code}`, '_blank')
            },

            convertTime (val) {
                return convertTime(val)
            },

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
                    this.card.installedFlag = true
                    this.$showTips({ theme: 'success', message: this.$t('editPage.installSuc') })
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
        ::v-deep .tippy-popper {
            display: none;
            transition: display 200ms;
        }
        &:hover {
            background: #fafbfd;
            .card-link, &.disable ::v-deep .tippy-popper {
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
            .info-main, .card-link, .info-main .main-name,.card-info .info-main .main-label li {
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
            width: 68px;
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
                .not-recommend {
                    text-decoration: line-through;
                }
                .devops-icon {
                    margin-left: 4px;
                }
                .icon-title {
                    display: flex;
                    align-items: center;
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
