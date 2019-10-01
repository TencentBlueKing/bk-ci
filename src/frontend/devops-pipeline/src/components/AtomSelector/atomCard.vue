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
                                <bk-popover :content="`${jobConst[os]}编译环境下可用`" :key="os">
                                    <i :class="`os-tag bk-icon icon-${os.toLowerCase()}`"></i>
                                </bk-popover>
                            </template>
                        </template>
                        <bk-popover v-else :content="`无编译环境下可用`">
                            <i :class="`os-tag bk-icon icon-none`"></i>
                        </bk-popover>
                    </span>
                </p>
                <p class="desc">{{atom.summary || '暂无描述'}}</p>
                <p class="atom-from">{{`由${atom.publisher}提供`}}</p>
            </div>
            <div class="atom-operate">
                <bk-button class="select-atom-btn"
                    :class="atom.disabled ? &quot;disabled&quot; : &quot;&quot;"
                    size="small"
                    @click="handleUpdateAtomType(atom.atomCode)"
                    :disabled="atom.disabled || atom.atomCode === atomCode"
                    v-if="!atom.notShowSelect"
                >{{atom.atomCode === atomCode ? '已选' : '选择'}}
                </bk-button>
                <bk-button class="select-atom-btn"
                    size="small"
                    @click="handleInstallStoreAtom(atom.atomCode)"
                    :disabled="!atom.flag"
                    :title="atom.tips"
                    :loading="isInstalling"
                    v-else-if="!atom.hasInstalled"
                >安装
                </bk-button>
                <a v-if="atom.docsLink" target="_blank" class="atom-link" :href="atom.docsLink">了解更多</a>
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
                    contxt = `该插件在${osListStr}编译环境下可用，请切换到对应Job类型后重试`
                } else {
                    contxt = '该插件在无编译环境下可用，请切换到对应Job类型后重试'
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
                    this.$bkMessage({ message: '安装成功', theme: 'success' })
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
