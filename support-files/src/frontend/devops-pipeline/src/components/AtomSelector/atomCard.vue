<template>
    <section>
        <div :class="['atom-item-main atom-item', { 'disabled': atom.disabled }, { 'active': atom.atomCode === activeAtomCode }]" ref="atomCard"
            v-bk-tooltips="{ content: atomOsPrompt, delay: 300 }">
            <div class="atom-logo">
                <img v-if="atom.logoUrl" :src="atom.logoUrl" />
                <logo v-else class="bk-icon" :name="getIconByCode(atom.atomCode)" size="50" />
            </div>
            <div class="atom-info-content">
                <p class="atom-name">
                    {{atom.name}}
                    <span class="allow-os-list" @mouseover="showOverallTip = false" @mouseleave="showOverallTip = true">
                        <template v-if="atom.os && atom.os.length > 0">
                            <template v-for="os in atom.os">
                                <bk-popover :content="`${jobConst[os]}${$t('editPage.hasEnv')}`" :key="os">
                                    <i :class="`os-tag bk-icon icon-${os.toLowerCase()}`"></i>
                                </bk-popover>
                            </template>
                        </template>
                        <bk-popover v-else :content="`${$t('editPage.noEnv')}`">
                            <i :class="`os-tag bk-icon icon-none`"></i>
                        </bk-popover>
                    </span>
                </p>
                <p class="desc">{{atom.summary || $t('editPage.noDesc')}}</p>
                <p class="atom-from">{{`${atom.publisher} ${$t('editPage.provided')}`}}</p>
            </div>
            <div class="atom-operate">
                <bk-button class="select-atom-btn"
                    :class="atom.disabled ? &quot;disabled&quot; : &quot;&quot;"
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
                >{{ $t('install') }}
                </bk-button>
                <a v-if="atom.docsLink" target="_blank" class="atom-link" :href="atom.docsLink">{{ $t('newlist.knowMore') }}</a>
            </div>
        </div>
    </section>
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
                showOverallTip: true,
                isInstalling: false
            }
        },

        computed: {
            ...mapGetters('atom', [
                'getDefaultVersion',
                'getAtomModal'
            ]),

            jobConst () {
                return jobConst
            },

            atomOsPrompt () {
                const { atom, jobConst, showOverallTip } = this
                const os = atom.os || []
                let contxt
                if (os.length) {
                    const osListStr = os.map(val => jobConst[val]).join('、')
                    contxt = `${osListStr}${this.$t('editPage.envUseTips')}`
                } else {
                    contxt = this.$t('editPage.noEnvUseTips')
                }
                return atom.disabled && showOverallTip ? contxt : null
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
                    this.$bkMessage({ message: this.$t('editPage.installSuc'), theme: 'success' })
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
