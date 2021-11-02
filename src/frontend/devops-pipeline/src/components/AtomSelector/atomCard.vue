<template>
    <section @click="handleSelectAtom(atom)">
        <div class="atom-logo">
            <img v-if="atom.logoUrl" :src="atom.logoUrl" alt="">
            <logo v-else class="devops-icon" :name="getIconByCode(atom.atomCode)" size="50" />
        </div>
        <div :class="{ 'atom-info-content': true, 'atom-info-content-disabled': !atom.installFlag && !atom.defaultFlag && !isProjectAtom }">
            <!-- 项目插件Tab按钮 -->
            <div v-if="isProjectAtom && isRecommend">
                <!-- 移除按钮 -->
                <span
                    v-if="!('uninstallFlag' in atom) && !atom.defaultFlag & atomCode !== atom.atomCode"
                    class="remove-atom"
                    v-bk-tooltips="{ content: `${$t('editPage.removeAtom')}`, zIndex: 99999, delay: 200 }"
                    @click.stop="handleUnInstallAtom(atom)">
                    <logo class="remove-icon" name="minus" size="14" />
                </span>
                <!-- 正在使用插件,无法移除  uninstallFlag--是否能卸载标识 -->
                <span
                    v-if="('uninstallFlag' in atom && !atom.defaultFlag) || (atomCode === atom.atomCode && !atom.defaultFlag)"
                    :class="{ 'un-remove': !atom.uninstallFlag }"
                    v-bk-tooltips="unRemoveAtomTipsConfig"
                    @click.stop>
                    <logo class="remove-icon" name="minus" size="14" />
                </span>
                <div v-if="('uninstallFlag' in atom && !atom.defaultFlag) || (atomCode === atom.atomCode && !atom.defaultFlag)" id="unRemoveAtomTips" class="un-remove-atom-tips">
                    <span class="row">{{ $t('editPage.unRemoveAtom') }}，</span>
                    <span class="row" style="color: #3A84FF; cursor: pointer;" @click="handleGoPipelineAtomManage(atom.name)">
                        {{ $t('editPage.viewPipeline') }}
                    </span>
                </div>
            </div>

            <!-- 研发商店Tab按钮 -->
            <span v-else-if="!isProjectAtom && isRecommend">
                <!-- 安装按钮 -->
                <span
                    v-if="!atom.installed && !atom.defaultFlag"
                    :class="{ 'install-atom': true, 'install-disabled': !atom.installFlag }"
                    v-bk-tooltips="{ content: !atom.installFlag ? `${$t('editPage.noInstallPerm')}` : `${$t('editPage.installAtom')}`, zIndex: 99999, delay: 200 }"
                    @click.stop="handleInstallAtom(atom)">
                    <i class="bk-icon left-icon icon-devops-icon icon-plus install-icon" style="position:relative; font-size: 24px;" />
                </span>
            </span>

            <p class="atom-name">
                <span class="atom-link">
                    {{ atom.name }}
                    <span style="cursor: pointer;" @click.stop="handleGoDocs(atom.docsLink)">
                        <logo v-if="atom.docsLink" class="jump-icon" name="tiaozhuan" size="14" style="fill:#3c96ff; position:relative; top:2px;" />
                    </span>
                    <span class="fire-num">
                        <logo v-if="atom.recentExecuteNum >= 10000" class="fire-icon" name="fire-red" size="14" style="position:relative; top:2px;" />
                        <logo v-else class="fire-red-icon" name="fire" size="14" style="position:relative; top:2px;" />
                        {{ getHeatNum(atom.recentExecuteNum) || '-' }}
                    </span>
                    <bk-rate class="atom-rate" :width="10" :rate="atom.score" :edit="false" />
                </span>
            </p>
            <template>
                <p v-if="atom.summary" :class="{ 'desc': true, 'desc-height': !atom.labelList }">{{ atom.summary }}</p>
                <div v-else style="padding-bottom: 20px;"></div>
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
            <div
                v-if="!isRecommend && (atom.os && atom.os.length > 0)"
                class="allow-os-list"
                v-bk-tooltips="{ content: osTips(atom.os), zIndex: 99999 }">
                <template>
                    <i v-for="(os, osIndex) in atom.os" :key="osIndex" style="margin-right: 3px;" :class="`os-tag devops-icon icon-${os.toLowerCase()}`" />
                </template>
            </div>
            <div
                v-if="!isRecommend && (atom.os && atom.os.length === 0)"
                class="allow-os-list"
                v-bk-tooltips="{ content: $t('editPage.suitable') + $t('editPage.noCompilerEnvironment'), zIndex: 99999 }">
                <template>
                    <i class="os-tag devops-icon icon-none stage-type-icon"></i>
                </template>
            </div>

            <p class="atom-update-time" v-if="atom.publisher">{{ atom.publisher }} {{ $t('editPage.update') }} {{ formatDiff(atom.updateTime) }}</p>
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
        
        created () {
            this.unRemoveAtomTipsConfig = {
                allowHtml: true,
                trigger: 'mouseenter',
                content: '#unRemoveAtomTips',
                zIndex: 99999,
                delay: 200,
                onShow: this.mouseEnterAtomUnRemove,
                onClose: this.mouseLeaveAtomUnRemove
            }
        },
        methods: {
            ...mapActions('atom', [
                'updateAtomType',
                'fetchAtomModal',
                'installAtom',
                'unInstallAtom',
                'updateStoreAtoms',
                'updateProjectAtoms',
                'setAtomCode'
            ]),
            formatDiff,
            /**
             * 适用系统tips
             */
            osTips (os) {
                return `${this.$t('editPage.suitable')}${os.join(' / ')}`
            },

            handleGoPipelineAtomManage (name) {
                window.open(`${WEB_URL_PREFIX}/pipeline/${this.projectCode}/list/atomManage/${name}`, '_blank')
            },

            /**
             * 默认logo
             */
            getIconByCode (atomCode) {
                const svg = document.getElementById(atomCode)
                return svg ? atomCode : 'placeholder'
            },

            /**
             * 鼠标移入无法移除插件按钮
             */
            mouseEnterAtomUnRemove () {
                const curRecommendItemDom = document.querySelectorAll('.recommend-atom-item')[this.atomIndex]
                curRecommendItemDom.setAttribute('class', 'recommend-atom-item enter-atom')
            },
            /**
             * 鼠标移出无法移除插件按钮
             */
            mouseLeaveAtomUnRemove () {
                const curRecommendItemDom = document.querySelectorAll('.recommend-atom-item')[this.atomIndex]
                curRecommendItemDom.setAttribute('class', 'recommend-atom-item')
            },

            /**
             * 选择插件
             *
             * @param atomCode 插件名 Code
             */
            handleSelectAtom (atom) {
                const { atomCode, installFlag } = atom
                let { installed } = atom
                // 未安装且有权限的适用插件，点击行自动安装并选中
                if (installFlag && this.isRecommend) {
                    installed = true
                    const atoms = {}
                    atoms[atomCode] = atom
                    this.updateProjectAtoms({
                        atoms: atoms,
                        recommend: true
                    })
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
                    }).catch((err) => {
                        this.$bkMessage({
                            message: err.message || err,
                            theme: 'error',
                            extCls: 'install-tips'
                        })
                    })
                }
                
                if (!this.isRecommend || !installed) return
                const { elementIndex, container, updateAtomType, getAtomModal, fetchAtomModal, getDefaultVersion } = this
                this.setAtomCode(atomCode)
                const version = getDefaultVersion(atomCode)
                const atomModal = getAtomModal({
                    atomCode,
                    version
                })

                const fn = atomModal ? updateAtomType : fetchAtomModal

                // 这里延迟调用接口，是因为安装完立马去获取插件详细信息，接口返回的是空的数据，导致页面选择插件后不显示数据，报undefined错误
                setTimeout(() => {
                    fn({
                        projectCode: this.projectCode,
                        container,
                        version,
                        atomCode,
                        atomIndex: elementIndex
                    })
                }, 200)
                this.$emit('close')
            },

            /**
             * 跳转至插件文档
             */
            handleGoDocs (link) {
                window.open(link)
            },

            /**
             * 移除插件
             */
            handleUnInstallAtom (atom) {
                if ('uninstallFlag' in atom) return
                const { atomCode, name } = atom
                this.unInstallAtom({
                    projectCode: this.projectCode,
                    atomCode,
                    reasonList: this.defaultReasons
                }).then(() => {
                    atom.installed = false
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

            /**
             * 安装插件
             */
            handleInstallAtom (atom) {
                const { installFlag, atomCode } = atom
                if (!installFlag) return
                const param = {
                    projectCode: [this.$route.params.projectId],
                    atomCode
                }
                this.installAtom(param).then(() => {
                    const atoms = this.getStoreRecommendAtomMap
                    atom.installed = true
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

            /**
             * 插件热度数据转换为以（k）为单位
             */
            getHeatNum (num) {
                // 超1000转为以（k）为单位
                return num >= 1000 ? (num / 1000).toFixed(1) + 'k' : num
            }
        }
    }
</script>
