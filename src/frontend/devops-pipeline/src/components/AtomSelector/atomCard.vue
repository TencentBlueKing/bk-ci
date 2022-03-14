<template>
    
    <div
        :class="atomCls"
        ref="atomCard"
        v-bk-tooltips="atomOsTooltips">
        <div class="atom-logo">
            <img v-if="atom.logoUrl" :src="atom.logoUrl" />
            <logo v-else class="devops-icon" :name="getIconByCode(atom.atomCode)" size="50" />
        </div>
        <div class="atom-info-content">
            <p class="atom-name">
                <span :class="atomNameCls" :title="atomNameTitle">{{ atom.name }}</span>
                <span class="allow-os-list">
                    <template v-if="atom.os && atom.os.length > 0">
                        <template v-for="os in atom.os">
                            <i
                                :key="os"
                                :class="`os-tag devops-icon icon-${os.toLowerCase()}`"
                            ></i>
                        </template>
                    </template>
                    <bk-popover v-else :content="`${$t('editPage.noEnv')}`">
                        <i :class="`os-tag devops-icon icon-none`"></i>
                    </bk-popover>
                </span>
            </p>
            <p class="desc">{{atom.summary || $t('editPage.noDesc')}}</p>
            <p class="atom-from">{{`${atom.publisher} ${$t('editPage.provided')}`}}</p>
        </div>
        <div class="atom-operate">
            <bk-button class="select-atom-btn"
                :class="{ 'disabled': atom.disabled }"
                size="small"
                @click="handleUpdateAtomType(atom.atomCode)"
                :disabled="atom.disabled || atom.atomCode === atomCode"
                v-if="!atom.notShowSelect"
            >{{atom.atomCode === atomCode ? $t('editPage.selected') : $t('editPage.select')}}
            </bk-button>
            <bk-button class="select-atom-btn"
                size="small"
                @click="handleInstallStoreAtom(atom.atomCode)"
                :disabled="!atom.flag"
                :title="atom.tips"
                :loading="isInstalling"
                v-else-if="!atom.hasInstalled"
            >{{ $t('editPage.install') }}
            </bk-button>
            <a v-if="atom.docsLink" target="_blank" class="atom-link" :href="atom.docsLink">{{ $t('newlist.knowMore') }}</a>
        </div>
    </div>
    
</template>

<script>
    import { jobConst } from '@/utils/pipelineConst'
    import { mapGetters, mapActions } from 'vuex'
    import logo from '@/components/Logo'

    export default {
        components: {
            logo
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
                let contxt
                if (os.length) {
                    const osListStr = os.map(val => jobConst[val]).join('、')
                    contxt = `${osListStr}${this.$t('editPage.envUseTips')}`
                } else {
                    contxt = this.$t('editPage.noEnvUseTips')
                }
                
                return {
                    delay: 300,
                    disabled: !atom.disabled,
                    content: contxt,
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
                    this.atom.notShowSelect = !this.atom.isInOs
                    this.atom.hasInstalled = true
                }).catch((err) => {
                    this.$bkMessage({ message: err.message || err, theme: 'error' })
                }).finally(() => {
                    this.isInstalling = false
                })
            }
        }
    }
</script>

<style lang="scss">
    .install-tips {
        z-index: 10001;
    }
</style>
