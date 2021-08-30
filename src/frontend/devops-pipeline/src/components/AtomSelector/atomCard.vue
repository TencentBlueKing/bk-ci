<template>
    <section @click="handleUpdateAtomType(atom.atomCode)">
        <div class="atom-logo">
            <img :src="atom.logoUrl" alt="">
        </div>
        <div class="atom-info-content">
            <span v-if="isProjectAtom">
                <span
                    v-if="!('uninstallFlag' in atom) && !atom.defaultFlag & atomCode !== atom.atomCode"
                    class="remove-atom"
                    v-bk-tooltips="{ content: `${$t('editPage.removeAtom')}`, zIndex: 99999, delay: 200 }"
                    @click.stop="handleUnInstallAtom(atom)">
                    <logo class="remove-icon" name="minus" size="14" />
                </span>
                <span
                    v-if="('uninstallFlag' in atom && !atom.defaultFlag) || atomCode === atom.atomCode"
                    :class="{ 'un-remove': !atom.uninstallFlag }"
                    v-bk-tooltips="{ content: `${$t('editPage.unRemoveAtom')}`, zIndex: 99999, delay: 200 }"
                    @click.stop="handleUnInstallAtom(atom)">
                    <logo class="remove-icon" name="minus" size="14" />
                </span>
            </span>
            <span v-else>
                <span
                    v-if="!atom.installed && !atom.defaultFlag"
                    :class="{ 'install-atom': true, 'install-disabled': !atom.installFlag }"
                    v-bk-tooltips="{ content: !atom.installFlag ? `${$t('editPage.noInstallPerm')}` : `${$t('editPage.installAtom')}`, zIndex: 99999, delay: 200 }"
                    @click.stop="handleInstallAtom(atom)">
                    <i class="bk-icon left-icon icon-devops-icon icon-plus install-icon" style="position:relative; font-size: 24px;" />
                </span>
            </span>
            <p class="atom-name">
                <a class="atom-link">
                    <span @click.stop="handleGoDocs(atom.docsLink)">
                        {{ atom.name }}
                        <logo v-if="atom.docsLink" class="jump-icon" name="tiaozhuan" size="14" style="fill:#3c96ff; position:relative; top:2px;" />
                    </span>
                    <span class="fire-num">
                        <logo v-if="atom.recentExecuteNum >= 50000" class="fire-icon" name="fire-red" size="14" style="fill:#3c96ff; position:relative; top:2px;" />
                        <logo v-else class="fire-icon" name="fire" size="14" style="fill:#3c96ff; position:relative; top:2px;" />
                        {{ getHeatNum(atom.recentExecuteNum) }}
                    </span>
                    <bk-rate class="atom-rate" width="10" :rate.sync="atom.score" :edit="false" />
                </a>
            </p>
            <p class="desc">{{ atom.summary }}</p>
            <p class="atom-label">
                <span
                    v-for="(label, labelIndex) in atom.labelList"
                    :key="labelIndex">
                    {{ label.labelName }}
                </span>
            </p>
            <span
                v-if="!isRecommend && (atom.os && atom.os.length > 0)"
                class="allow-os-list"
                v-bk-tooltips="{ content: osTips(atom.os), zIndex: 99999 }">
                <template v-for="(os, osIndex) in atom.os">
                    <i :key="osIndex" style="margin-right: 3px;" :class="`os-tag devops-icon icon-${os.toLowerCase()}`" />
                </template>
            </span>
            <p class="atom-update-time" v-if="atom.modifier">{{ atom.modifier }} 更新于 {{ formatDiff(atom.updateTime) }}</p>
            <span class="atom-active" v-if="atomCode === atom.atomCode">
                <i class="devops-icon icon-check-1" />
            </span>
        </div>
    </section>
</template>

<script>
    import Logo from '@/components/Logo'
    import { formatDiff } from '@/utils/util'
    import { mapGetters, mapActions } from 'vuex'
    
    export default {
        components: {
            Logo
        },
        props: {
            atom: {
                type: Object,
                default: () => {}
            },
            atomIndex: {
                type: Number,
                default: 0
            },
            // 是否为适用插件
            isRecommend: {
                type: Boolean,
                default: true
            },
            container: {
                type: Object,
                default: () => ({})
            },
            elementIndex: Number,
            atomCode: {
                type: String
            },
            isProjectAtom: {
                type: Boolean,
                default: true
            },
            deleteReasons: {
                type: Array,
                default: () => []
            }
        },
        computed: {
            ...mapGetters('atom', [
                'getDefaultVersion',
                'getAtomModal'
            ]),
            projectCode () {
                return this.$route.params.projectId
            },
            defaultReasons () {
                // 移除插件（默认选择：只是试用下）
                return [
                    {
                        reasonId: this.deleteReasons[1].id,
                        note: ''
                    }
                ]
            }
        },
        methods: {
            ...mapActions('atom', [
                'updateAtomType',
                'fetchAtomModal',
                'installAtom',
                'unInstallAtom'
            ]),
            formatDiff,
            /**
             * 适用系统tips
             */
            osTips (os) {
                return `${this.$t('适用于')}${os.join(' / ')}`
            },

            /**
             * 选择插件
             *
             * @param atomCode 插件名 Code
             */
            handleUpdateAtomType (atomCode) {
                if (!this.isProjectAtom || !this.isRecommend) return
                const { elementIndex, container, updateAtomType, getAtomModal, fetchAtomModal, getDefaultVersion } = this
                const version = getDefaultVersion(atomCode)
                const atomModal = getAtomModal({
                    atomCode,
                    version
                })

                const fn = atomModal ? updateAtomType : fetchAtomModal
                fn({
                    projectCode: this.projectCode,
                    container,
                    version,
                    atomCode,
                    atomIndex: elementIndex
                })
                this.$emit('close')
            },
            handleGoDocs (link) {
                window.open(link)
            },
            handleUnInstallAtom (atom) {
                if ('uninstallFlag' in atom) return
                const { atomCode, name } = atom
                this.unInstallAtom({
                    projectCode: this.projectCode,
                    atomCode,
                    reasonList: this.defaultReasons
                }).then(() => {
                    this.$emit('update-atoms', {
                        isRecommend: this.isRecommend,
                        atomCode
                    })
                    this.$bkMessage({
                        theme: 'success',
                        extCls: 'unInstall-tips',
                        message: `${this.$t('atomManage.uninstall')}${name}${this.$t('success')}`
                    })
                }).catch((e) => {
                    this.$bkMessage({
                        theme: 'error',
                        extCls: 'unInstall-tips',
                        message: e.message
                    })
                })
            },
            handleInstallAtom (atom) {
                const { installFlag, atomCode } = atom
                if (!installFlag) return
                const param = {
                    projectCode: [this.$route.params.projectId],
                    atomCode
                }
                this.installAtom(param).then(() => {
                    this.$bkMessage({
                        message: this.$t('editPage.installSuc'),
                        theme: 'success',
                        extCls: 'install-tips'
                    })
                }).catch((err) => {
                    this.$bkMessage({
                        message: err.message || err,
                        theme: 'error',
                        extCls: 'install-tips'
                    })
                })
            },
            getHeatNum (num) {
                // 超1000转为以（k）为单位
                return num >= 1000 ? (num / 1000).toFixed(1) + 'k' : num
            }
        }
    }
</script>
