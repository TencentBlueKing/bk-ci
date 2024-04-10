<template>
    <section>
        <div>
            <atom-checkbox ref="turbo" :disabled="turboDisabled" :handle-change="handleSwitch" :name="name" :text="text" :value="turboValue" :desc="desc"></atom-checkbox>
            <a href="javascript: void(0);" class="check-inline-link" target="_blank" v-if="taskId" @click.stop="goTurboLink">{{ taskName }}</a>
        </div>
        <bk-alert type="error" class="turbo-tip" v-if="taskId && turboValue">
            <div slot="title">当前使用的是旧版 distcc，建议迁移到新版。<bk-link :href="migrationUrl" target="_blank" theme="primary">了解更多</bk-link></div>
        </bk-alert>
        <div class="build-quote" v-if="taskId && turboValue">
            <div class="quote-ident">
                <i class="devops-icon icon-info-circle quote-ident-icon"></i>
            </div>
            <div class="quote-content">
                <p class="quote-text quote-title">
                    警告：关联的编译加速任务为{{ projLang() }}语言，使用了{{ quoteTitle() }}加速方案，需修改编译脚本。修改如下：</p>
                <template v-if="task.toolType === '1'">
                    <p class="quote-text">1、若你的makefile是由cmake自动生成的，需先修改cmake并加上编译加速任务ID参数。cmake改为bk-cmake -p ${turbo.task.id} {{ extraParam() }}</p>
                    <p class="quote-text">2、使用bk-make来替代make，并加上编译加速任务ID和流水线当前构建ID参数。make改为bk-make -p ${turbo.task.id} -b ${pipeline.build.id} {{ extraParam() }}</p>
                    <p class="quote-text">
                        <a :href="docsURL" target="_blank" class="bk-text-primary">其它情况请查看更多修改示例&gt;&gt;</a>
                    </p>
                </template>
                <template v-if="task.toolType === '2'">
                    <p class="quote-text">1、打开distcc开关，在项目的 BLADE_ROOT 文件里面添加：</p>
                    <p class="quote-text">distcc_config(</p>
                    <p class="quote-text">&nbsp;&nbsp;&nbsp;&nbsp;enabled=True</p>
                    <p class="quote-text">)</p>
                    <p class="quote-text">2、修改blade命令，例如：</p>
                    <p class="quote-text">cd /path/to/your_project</p>
                    <p class="quote-text">blade build</p>
                    <br />
                    <p class="quote-text">改为使用bk-blade：</p>
                    <p class="quote-text">cd /path/to/your_project</p>
                    <p class="quote-text">bk-blade -p ${turbo.task.id} -b ${pipeline.build.id} -a "build"</p>
                    <br />
                    <p class="quote-text">-a 用于传递原始blade命令后面带的参数</p>
                    <p class="quote-text">
                        <a :href="docsURL" target="_blank" class="bk-text-primary">其它情况请查看更多修改示例&gt;&gt;</a>
                    </p>
                </template>
                <template v-if="task.toolType === '3'">
                    <p class="quote-text">1、复制和整理模板文件</p>
                    <p class="quote-text">将配置文件bk_bazelrc和文件夹bkdistcctoolchain（含里面的文件）拷贝到项目根目录下。<a :href="linkUrl" target="_blank" class="bk-text-primary">点击下载模板文件>></a></p>
                    <p class="quote-text">如果项目有自己的toolchain配置，需要将其内容合并到上述文件里。</p>
                    <p class="quote-text">2、修改bazel命令，例如：</p>
                    <p class="quote-text">cd /path/to/your_project</p>
                    <p class="quote-text">bazel build //main:hello-world</p>
                    <br />
                    <p class="quote-text">改为使用bk-bazel：</p>
                    <p class="quote-text">cd /path/to/your_project</p>
                    <p class="quote-text">bk-bazel -p ${turbo.task.id} -b ${pipeline.build.id} -a "build //main:hello-world"</p>
                    <br />
                    <p class="quote-text">-a 用于传递原始bazel命令后面带的参数</p>
                    <p class="quote-text">
                        <a :href="docsURL" target="_blank" class="bk-text-primary">其它情况请查看更多修改示例&gt;&gt;</a>
                    </p>
                </template>
                <div class="quote-item">
                    <!-- <p class="quote-desc" v-if="quoteDesc.length" v-for="(desc, index) in quoteDesc" :key="index">{{ desc }}</p> -->
                    <p class="quote-desc">如遇到问题请联系<a href="wxwork://message/?username=DevOps" class="bk-text-primary">蓝盾助手DevOps</a></p>
                </div>
            </div>
        </div>
    </section>
</template>

<script>
    import atomFieldMixin from '../atomFieldMixin'
    import AtomCheckbox from '../AtomCheckbox'
    export default {
        name: 'check-inline',
        components: {
            AtomCheckbox
        },
        mixins: [atomFieldMixin],
        props: {
            turboValue: {
                type: Boolean,
                default: false
            },
            turboDisabled: {
                type: Boolean,
                default: false
            },
            taskName: {
                type: String,
                default: ''
            },
            task: {
                type: Object,
                default: {}
            },
            taskId: {
                type: String,
                default: ''
            },
            projectId: {
                type: String,
                default: ''
            },
            text: {
                type: String,
                default: ''
            },
            desc: {
                type: String,
                default: ''
            },
            title: {
                type: String,
                default: ''
            },
            inlineLabel: {
                type: String,
                default: ''
            },
            isInline: {
                type: Boolean,
                default: true
            },
            quoteText: {
                type: Array,
                default: []
            },
            quoteDesc: {
                type: Array,
                default: []
            }
        },
        
        computed: {
            docsURL () {
                return `${IWIKI_DOCS_URL}/x/tYbm`
            },
            linkUrl () {
                return '/turbo-client/bazel.zip'
            },
            migrationUrl () {
                return `${IWIKI_DOCS_URL}/x/dj67Lw`
            }
        },
        methods: {
            handleSwitch (name, value) {
                !this.turboDisabled && this.$emit('handleChange', name, value)
            },
            goTurboLink () {
                window.open(`${WEB_URL_PREFIX}/turbo/${this.$route.params.projectId}/history/?pipelineId=${this.$route.params.pipelineId}&planId=${this.taskId}`, '_blank')
            },
            projLang () {
                if (this.task.projLang === '1') {
                    return 'C/C++'
                } else if (this.task.projLang === '2') {
                    return 'Objective C/C++'
                } else {
                    return 'C#'
                }
            },
            quoteTitle () {
                const { banDistcc, ccacheEnabled } = this.task
                if (banDistcc === 'false' && ccacheEnabled === 'true') {
                    return 'distcc+ccache'
                } else if (banDistcc === 'false' && ccacheEnabled === 'false') {
                    return 'distcc'
                } else {
                    return 'ccache'
                }
            },
            extraParam () {
                const { gccVersion } = this.task
                if (gccVersion.indexOf('clang') !== -1) {
                    return '-C'
                } else {
                    return ''
                }
            }
        }
    }
</script>

<style lang="scss">
    @import '../../../scss/conf.scss';

    .atom-checkbox {
        display: flex;
        font-weight: bold;
        padding: 5px 20px 0 0;
    }

    .check-inline-link {
        margin-left: 28px;
        &:link, &:visited {
            color: #3c96ff;
        }
    }
    .turbo-tip {
        margin-bottom: 5px;
        .bk-link .bk-link-text {
            font-size: 12px;
        }
    }
    .build-quote {
        position: relative;
        padding-left: 45px;
        border: 1px solid #c3cdd7;
        border-radius: 2px;
        .quote-ident {
            position: absolute;
            top: 0;
            left: 0;
            bottom: 0;
            display: flex;
            align-items: center;
            width: 45px;
            font-size: 12px;
            text-align: center;
            background: #ffb400;
            .quote-ident-icon {
                margin: 0 auto;
                color: #fff;
            }
        }
        .quote-content {
            width: 100%;
            padding: 15px;
            font-size: 12px;
            background: #fff;
            .quote-item {
                margin-top: 15px;
            }
            .quote-text {
                margin: 0;
            }
            .quote-title {
                font-weight: bold;
                margin: 0 0 6px 0;
            }
            .quote-desc {
                margin: 0;
                color: #999;
            }
        }
    }
</style>
