<template>
    <div
        :class="atomCls"
        ref="atomCard"
        v-bk-tooltips="atomOsTooltips">
        <div class="atom-logo">
            <honer-img v-if="atom.logoUrl" :detail="atom" />
            <logo v-else class="devops-icon" :name="getIconByCode(atom.atomCode)" size="50" />
        </div>
        <div class="atom-info-content">
            <p class="atom-name">
                <span :class="[atomNameCls, 'mr16', 'text-overflow']" :title="atomNameTitle" v-bk-overflow-tips="{ extCls: 'tippy-padding', zIndex: 10000 }">{{ atom.name }}</span>
                <honer-tag :detail="atom" :max-num="1" />
                <img
                    v-for="indexInfo in atom.indexInfos"
                    v-bk-tooltips="{
                        allowHTML: true,
                        zIndex: 10000,
                        content: indexInfo.hover
                    }"
                    :key="indexInfo.indexCode"
                    :src="indexInfo.iconUrl"
                    :style="{
                        color: indexInfo.iconColor,
                        height: '16px',
                        width: '16px',
                        marginRight: '8px',
                        cursor: 'pointer'
                    }"
                >
            </p>
            <p class="desc">{{atom.summary || $t('editPage.noDesc')}}</p>
            <section class="atom-rate">
                <p class="score-group">
                    <rate
                        :max-stars="1"
                        :rate="1"
                        :width="14"
                        :height="14"
                        :style="{
                            width: atom.score >= 5 ? '14px' : '7px'
                        }"
                        class="score-real"
                    />
                    <rate
                        :max-stars="1"
                        :rate="0"
                        :width="14"
                        :height="14"
                    />
                </p>
                <span class="ml6">{{ atom.score }}</span>
                <img v-if="atom.hotFlag" class="hot-icon" src="../../images/hot-red.png">
                <img v-else class="hot-icon" src="../../images/hot.png">
                <span class="ml3">{{ getShowNum(atom.recentExecuteNum) }}</span>
            </section>
        </div>
        <div class="atom-operate">
            <bk-button
                class="select-atom-btn"
                :class="{ 'disabled': atom.disabled }"
                @click="handleUpdateAtomType(atom.atomCode)"
                :disabled="atom.disabled || atom.atomCode === atomCode"
                v-if="atom.installed || atom.defaultFlag"
            >
                {{atom.atomCode === atomCode ? $t('editPage.selected') : $t('editPage.select')}}
            </bk-button>
            <bk-button
                v-else
                class="select-atom-btn"
                size="small"
                @click="handleInstallStoreAtom(atom.atomCode)"
                :disabled="!atom.installFlag"
                :title="atom.installFlag ? '' : $t('editPage.noPermToInstall')"
                :loading="isInstalling"
            >{{ $t('editPage.install') }}
            </bk-button>
            <p class="atom-from">{{`${atom.publisher} ${$t('editPage.provided')}`}}</p>
            <a v-if="atom.docsLink" target="_blank" class="atom-link" :href="atom.docsLink">{{ $t('newlist.knowMore') }}</a>
        </div>
    </div>
</template>

<script>
    import { jobConst } from '@/utils/pipelineConst'
    import { mapGetters, mapActions } from 'vuex'
    import logo from '@/components/Logo'
    import HonerImg from './honer-img.vue'
    import HonerTag from './honer-tag.vue'
    import Rate from './rate.vue'

    export default {
        components: {
            logo,
            HonerImg,
            HonerTag,
            Rate
        },

        props: {
            atom: {
                type: Object,
                default: {}
            },

            container: {
                type: Object,
                default: () => ({})
            },

            elementIndex: {
                type: Number,
                default: 0
            },

            atomCode: {
                type: String
            },

            activeAtomCode: {
                type: String
            }
        },

        data () {
            return {
                isInstalling: false
            }
        },

        computed: {
            ...mapGetters('atom', [
                'getDefaultVersion',
                'getAtomModal'
            ]),

            atomCls () {
                return [
                    'atom-item-main atom-item',
                    {
                        disabled: this.atom.disabled,
                        active: this.atom.atomCode === this.activeAtomCode
                    }
                ]
            },
            atomNameCls () {
                return { 'not-recommend': this.atom.recommendFlag === false }
            },
            atomNameTitle () {
                return this.atom.recommendFlag === false ? this.$t('editPage.notRecomendPlugin') : ''
            },
            atomOsTooltips () {
                const { atom } = this
                const os = atom.os || []
                let context
                if (os.length && !os.includes('NONE')) {
                    const osListStr = os.map(val => jobConst[val]).join('、')
                    context = `${osListStr}${this.$t('editPage.envUseTips')}`
                } else {
                    context = this.$t('editPage.noEnvUseTips')
                }
                return {
                    delay: 300,
                    disabled: !atom.disabled,
                    content: context,
                    zIndex: 10001
                }
            }
        },

        mounted () {
            this.scrollIntoView()
        },

        methods: {
            ...mapActions('atom', [
                'updateAtomType',
                'fetchAtomModal',
                'installAtom'
            ]),

            scrollIntoView () {
                if (this.atomCode === this.atom.atomCode) this.$refs.atomCard.scrollIntoView(false)
            },

            getIconByCode (atomCode) {
                const svg = document.getElementById(atomCode)
                return svg ? atomCode : 'placeholder'
            },

            handleUpdateAtomType (atomCode) {
                const { elementIndex, container, updateAtomType, getAtomModal, fetchAtomModal, getDefaultVersion } = this
                const version = getDefaultVersion(atomCode)
                const atomModal = getAtomModal({
                    atomCode,
                    version
                })

                const fn = atomModal ? updateAtomType : fetchAtomModal
                fn({
                    projectCode: this.$route.params.projectId,
                    container,
                    version,
                    atomCode,
                    atomIndex: elementIndex
                })
                this.$emit('close')
            },

            handleInstallStoreAtom (atomCode) {
                this.isInstalling = true
                const param = {
                    projectCode: [this.$route.params.projectId],
                    atomCode
                }
                this.installAtom(param).then(() => {
                    this.$bkMessage({ message: this.$t('editPage.installSuc'), theme: 'success', extCls: 'install-tips' })
                    this.atom.installed = !this.atom.installed
                    this.$emit('installAtomSuccess', this.atom)
                }).catch((err) => {
                    this.$bkMessage({ message: err.message || err, theme: 'error' })
                }).finally(() => {
                    this.isInstalling = false
                })
            },

            getShowNum (num) {
                if (+num > 10000) {
                    return Math.floor(+num / 10000) + 'W+'
                } else {
                    return num
                }
            }
        }
    }
</script>

<style lang="scss">
    .atom-item-main:hover {
        .atom-from {
            opacity: 1;
        }
    }
    .atom-from {
        font-size: 12px;
        opacity: 0;
        position: absolute;
        color: #C4C6CC;
        bottom: 0;
        right: 95px;
        width: 160px;
        text-align: right;
    }
    .install-tips {
        z-index: 10001;
    }
    .atom-logo {
        width: 68px !important;
        height: 68px !important;
    }
    .desc {
        font-weight: normal;
    }
    .atom-rate {
        display: flex;
        align-items: center;
        font-size: 12px;
        font-weight: normal;
        color: #979BA5;
    }
    .hot-icon {
        height: 18px;
        width: 18px;
        margin-left: 25px;
    }
    .score-group {
        position: relative;
        .score-real {
            position: absolute;
            overflow: hidden;
            left: 0;
            top: 0;
            height: 16px;
            display: flex;
        }
    }
    .ml6 {
        margin-left: 6px;
    }
    .ml3 {
        margin-left: 3px;
    }
    .mr16 {
        margin-right: 16px;
    }
</style>
