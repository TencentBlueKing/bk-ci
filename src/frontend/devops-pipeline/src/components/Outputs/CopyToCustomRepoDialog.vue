<template>
    <bk-dialog
        v-model="isCopyDialogShow"
        ext-cls="copy-to-custom-dialog auto-height-dialog"
        :title="$t('details.copyTo')"
        :close-icon="false"
        :quick-close="false"
        header-position="left"
        :loading="isCopying"
        width="600"
        @confirm="copyToCustom"
        @cancel="hide"
    >
        <p class="copy-to-custom-repo-dialog-header" v-html="title"></p>
        <bk-input
            class="copy-to-custom-repo-search"
            right-icon="bk-icon icon-search"
            :placeholder="$t('details.serachCustomRepo')"
            v-model="keyPath"
            @enter="search"
            @right-icon-click="search"
        />
        <section class="copy-custom-repo-tree">
            <bk-big-tree
                ref="customRepoTree"
                selectable
                :expand-on-click="false"
                :options="repoOptions"
                :default-expanded-nodes="[customRootFolder.fullPath]"
                @select-change="handleSelect"
                :data="customFolders"
                :lazy-method="getFolders"
            />
        </section>
    </bk-dialog>
</template>
<script>
    import { mapActions } from 'vuex'
    import Logo from '@/components/Logo'

    export default {
        components: {
            // eslint-disable-next-line vue/no-unused-components
            Logo
        },
        props: {
            artifact: {
                type: Object,
                required: true
            }
        },
        data () {
            return {
                isCopying: false,
                isCopyDialogShow: false,
                keyPath: '',
                activeFolder: null,
                customRootFolder: {
                    fullPath: '/',
                    name: this.$t('details.customRepo'),
                    level: 0,
                    children: []
                }
            }
        },
        computed: {
            title () {
                return this.$t('details.copyArtifactTo', [this.artifact?.name ?? '--'])
            },
            customFolders () {
                return [this.customRootFolder]
            },
            repoOptions () {
                return {
                    idKey: 'fullPath'
                }
            }
        },

        methods: {
            ...mapActions('common', ['requestCustomFolder', 'requestCopyArtifactories']),
            show () {
                this.isCopyDialogShow = true
            },
            hide () {
                this.isCopyDialogShow = false
                this.$emit('close')
            },
            async getFolders (folder) {
                const res = await this.requestCustomFolder({
                    projectId: this.$route.params.projectId,
                    params: {
                        path: folder.id
                    }
                })
                return {
                    data:
          res.children?.map((item) => ({
            ...item,
            level: folder.level + 1
          })) ?? []
                }
            },
            async search (keyword) {
                const res = await this.requestCustomRepo({
                    projectId: this.$route.params.projectId,
                    params: {
                        name: `*${keyword}*`
                    }
                })
      this.$refs?.customRepoTree?.setData?.([
        {
          ...this.customRootFolder,
          children: res.children
        }
      ])
            },
            handleSelect (folder) {
                this.activeFolder = folder.id
            },
            async copyToCustom () {
                let message, theme
                try {
                    console.log(this.artifact)
                    const res = await this.requestCopyArtifactories({
                        projectId: this.$route.params.projectId,
                        srcArtifactoryType: this.artifact.artifactoryType,
                        srcFileFullPaths: [
                            this.artifact.fullPath
                        ],
                        dstArtifactoryType: 'CUSTOM_DIR',
                        dstDirFullPath: this.activeFolder

                    })
                    if (res) {
                        message = this.$createElement('span', {}, [
                            this.$t('details.copyToCustomSuc', [this.artifact.name, this.activeFolder]),
                            this.$createElement(
                                'bk-link',
                                {
                                    domProps: {
                                        target: '_blank'
                                    },
                                    props: {
                                        theme: 'primary',
                                        href: `${WEB_URL_PREFIX}/repo/${
                                            this.$route.params.projectId
                                        }/generic?repoName=custom&path=${encodeURIComponent(
                                            `${this.activeFolder}/default`
                                        )}`
                                    }
                                },
                                [
                                    this.$createElement(
                                        'span',
                                        {
                                            class: 'go-dist-link'
                                        },
                                        [
                                            this.$createElement(Logo, {
                                                props: {
                                                    name: 'tiaozhuan',
                                                    size: 14
                                                }
                                            }),
                                            this.$t('details.goDistFolder')
                                        ]
                                    )
                                ]
                            )
                        ])
                        theme = 'success'
                    }
                } catch (err) {
                    message = err.message ? err.message : err
                    theme = 'error'
                } finally {
                    console.log(message)
                    this.$bkMessage({
                        message,
                        theme,
                        delay: 5000
                    })
                }
            }
        }
    }
</script>

<style lang="scss">
.copy-to-custom-dialog {
  .copy-to-custom-repo-dialog-header {
    font-size: 12px;
  }
  .bk-dialog-body {
    display: flex;
    flex-direction: column;
    overflow: hidden;
  }
  .copy-to-custom-repo-search {
    margin: 16px 0;
  }
  .copy-custom-repo-tree {
    flex: 1;
    overflow: auto;
  }
}
.go-dist-link {
  margin-left: 10px;
  display: flex;
  align-items: center;
  font-size: 12px;
}
</style>
