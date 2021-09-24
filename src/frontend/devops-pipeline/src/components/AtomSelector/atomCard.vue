<template>
    <section @click="handleUpdateAtomType(atom)">
        <div class="atom-logo">
            <img v-if="atom.logoUrl" :src="atom.logoUrl" alt="">
            <logo v-else class="devops-icon" :name="getIconByCode(atom.atomCode)" size="50" />
        </div>
        <div class="atom-info-content">
            <span v-if="isProjectAtom && isRecommend">
                <!-- 移除按钮 -->
                <span
                    v-if="!('uninstallFlag' in atom) && !atom.defaultFlag & atomCode !== atom.atomCode"
                    class="remove-atom"
                    v-bk-tooltips="{ content: `${$t('editPage.removeAtom')}`, zIndex: 99999, delay: 200 }"
                    @click.stop="handleUnInstallAtom(atom)">
                    <logo class="remove-icon" name="minus" size="14" />
                </span>
                <!-- 正在适用插件,无法移除 -->
                <span
                    v-if="('uninstallFlag' in atom && !atom.defaultFlag) || atomCode === atom.atomCode"
                    :class="{ 'un-remove': !atom.uninstallFlag }"
                    v-bk-tooltips="{ content: `${$t('editPage.unRemoveAtom')}`, zIndex: 99999, delay: 200 }"
                    @click.stop>
                    <logo class="remove-icon" name="minus" size="14" />
                </span>
            </span>
            <span v-else-if="!isProjectAtom && isRecommend">
                <!-- 安装按钮 -->
                <span
                    v-if="!atom.installed && !atom.defaultFlag"
                    :class="{ 'install-atom': true, 'install-disabled': !atom.installFlag }"
                    v-bk-tooltips="{ content: !atom.installFlag ? `${$t('editPage.noInstallPerm')}` : `${$t('editPage.installAtom')}`, zIndex: 99999, delay: 200 }"
                    @click.stop="handleInstallAtom(atom)">
                    <i class="bk-icon left-icon icon-devops-icon icon-plus install-icon" style="position:relative; font-size: 24px;" />
                </span>
                <!-- 移除按钮 -->
                <span
                    v-if="atom.installed && !atom.defaultFlag"
                    class="remove-atom"
                    v-bk-tooltips="{ content: `${$t('editPage.removeAtom')}`, zIndex: 99999, delay: 200 }"
                    @click.stop="handleUnInstallAtom(atom)">
                    <logo class="remove-icon" name="minus" size="14" />
                </span>
            </span>
            <p class="atom-name">
                <a class="atom-link">
                    {{ atom.name }}
                    <span @click.stop="handleGoDocs(atom.docsLink)">
                        <logo v-if="atom.docsLink" class="jump-icon" name="tiaozhuan" size="14" style="fill:#3c96ff; position:relative; top:2px;" />
                    </span>
                    <span class="fire-num">
                        <logo v-if="atom.recentExecuteNum >= 50000" class="fire-icon" name="fire-red" size="14" style="position:relative; top:2px;" />
                        <logo v-else class="fire-red-icon" name="fire" size="14" style="position:relative; top:2px;" />
                        {{ getHeatNum(atom.recentExecuteNum) }}
                    </span>
                    <bk-rate class="atom-rate" width="10" :rate.sync="atom.score" :edit="false" />
                </a>
            </p>
            <template>
                <p v-if="atom.summary" :class="{ 'desc': true, 'desc-height': !atom.labelList }">{{ atom.summary }}</p>
                <div v-else style="padding-bottom: 30px;"></div>
            </template>
            <template>
                <span v-if="atom.labelList" class="atom-label">
                    <span
                        v-for="(label, labelIndex) in atom.labelList"
                        :key="labelIndex">
                        {{ label.labelName }}
                    </span>
                </span>
                <div v-else style="padding-bottom: 10px;"></div>
            </template>
            <span
                v-if="!isRecommend && (atom.os && atom.os.length > 0)"
                class="allow-os-list"
                v-bk-tooltips="{ content: osTips(atom.os), zIndex: 99999 }">
                <template>
                    <i v-for="(os, osIndex) in atom.os" :key="osIndex" style="margin-right: 3px;" :class="`os-tag devops-icon icon-${os.toLowerCase()}`" />
                </template>
            </span>
            <p class="atom-update-time" v-if="atom.publisher">{{ atom.publisher }} 更新于 {{ formatDiff(atom.updateTime) }}</p>
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
                'getAtomModal',
                'getStoreRecommendAtomMap'
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
                'unInstallAtom',
                'updateStoreAtoms'
            ]),
            formatDiff,
            /**
             * 适用系统tips
             */
            osTips (os) {
                return `${this.$t('适用于')}${os.join(' / ')}`
            },

            /**
             * 默认logo
             */
            getIconByCode (atomCode) {
                const svg = document.getElementById(atomCode)
                return svg ? atomCode : 'placeholder'
            },

            /**
             * 选择插件
             *
             * @param atomCode 插件名 Code
             */
            handleUpdateAtomType (atom) {
                const { atomCode, installed } = atom
                if (!this.isRecommend || !installed) return
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
                    const atoms = this.getStoreRecommendAtomMap
                    atoms[atomCode].installed = false
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
                    const atoms = this.getStoreRecommendAtomMap
                    this.$set(atoms[atomCode], 'installed', true)
                    this.updateStoreAtoms({
                        atoms: atoms,
                        recommend: true
                    })
                    this.$bkMessage({
                        message: this.$t('editPage.installAtomSuc'),
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
