<template>
    <section class="wrapper-container-success">
        <h5 class="success-title"><i class="bk-icon icon-check-circle-shape"></i>恭喜，任务注册成功</h5>
        <template v-if="isInstallToMachine && isToolMake">
            <p class="success-desc">编译加速任务的配置信息已经全部配置完成。还需修改目标构建机上编译命令，方能顺利使用编译加速服务。</p>
            <div class="success-info">
                <i class="bk-icon icon-exclamation-circle-shape"></i>
                <p class="success-info-title">提示：修改makefile文件和编译命令</p>
                <p class="success-info-title-level-2">1、执行./install.sh将软件包安装到目标构建机上</p>
                <p class="success-info-title-level-2">2、修改编译命令（请将示例中$PROJECTID替换为本任务ID: <span class="warning">{{ getRegister.taskId }}</span>）</p>
                <p class="success-info-title-level-3">2.1、如果你的makefile是由cmake自动生成的，请先修改cmake命令</p>
                <p class="success-info-desc">cmake 改为bk-cmake -p $PROJECTID {{ extraParam() }}</p>
                <p class="success-info-desc">接着使用bk-make来替代make，加上编译加速任务ID作为参数</p>
                <p class="success-info-desc">例1、make 改为bk-make -p $PROJECTID {{ extraParam() }}</p>
                <p class="success-info-desc">例2、make all 改为 bk-make -p $PROJECTID -a "all" {{ extraParam() }}</p>
                <p class="success-info-title-level-3">2.2、如果你的makefile为手动编排，请先修改makefile文件的编译器配置，使用以下几个环境变量</p>
                <p class="success-info-desc"><span class="success-info-inline">BK_CC</span>C文件编译器设置</p>
                <p class="success-info-desc"><span class="success-info-inline">BK_CXX</span>CPP文件编译器设置</p>
                <p class="success-info-desc"><span class="success-info-inline">BK_JOBS</span>make job数</p>
                <p class="success-info-desc">例1、在头部增加一行 CC=$(BK_CC) CXX=$(BK_CXX)</p>
                <p class="success-info-desc">例2、替换显示指定的MAKE命令。如MAKE:= make -j480 改为 MAKE:= make -j$(BK_JOBS) CC=$(BK_CC) CXX=$(BK_CXX)</p>
                <p class="success-info-desc">接着使用bk-make来替代make，加上编译加速任务ID作为参数</p>
                <p class="success-info-desc">例1、make 改为bk-make -p $PROJECTID {{ extraParam() }}</p>
                <p class="success-info-desc">例2、make all 改为 bk-make -p $PROJECTID -a "all" {{ extraParam() }}</p>
                <p class="success-info-desc">
                    <a :href="`${DOCS_URL_PREFIX}/x/sobm`"
                        class="text-link" target="_blank">查看更多修改案例</a></p>
            </div>
        </template>
        <template v-else-if="isInstallToMachine && isToolBlade">
            <p class="success-desc">编译加速任务的配置信息已经全部配置完成。还需修改目标构建机上编译命令，方能顺利使用编译加速服务。</p>
            <div class="success-info">
                <i class="bk-icon icon-exclamation-circle-shape"></i>
                <p class="success-info-title">提示：修改makefile文件和编译命令</p>
                <p class="success-info-title-level-2">1、执行./install.sh将软件包安装到目标构建机上</p>
                <p class="success-info-title-level-2">2、打开distcc开关。在项目的 BLADE_ROOT 文件里面添加：</p>
                <p class="success-info-desc">distcc_config(</p>
                <p class="success-info-desc">&nbsp;&nbsp;&nbsp;&nbsp;enabled=True</p>
                <p class="success-info-desc">)</p>
                <p class="success-info-title-level-2">3、修改blade命令（请将示例中${turbo.task.id}替换为本任务ID: <span class="warning">{{ getRegister.taskId }}</span>）</p>
                <p class="success-info-desc">例如：</p>
                <p class="success-info-desc">cd /path/to/your_project</p>
                <p class="success-info-desc">blade build</p>
                <br />
                <p class="success-info-desc">改为使用bk-blade：</p>
                <p class="success-info-desc">cd /path/to/your_project</p>
                <p class="success-info-desc">bk-blade -p ${turbo.task.id} -a "build"</p>
                <br />
                <p class="success-info-desc">-a 用于传递原始blade命令后面带的参数</p>
                <p class="success-info-desc">
                    <a :href="`${DOCS_URL_PREFIX}/x/sobm`"
                        class="text-link" target="_blank">查看更多修改案例</a></p>
            </div>
        </template>
        <template v-else-if="isInstallToMachine && isToolBazel">
            <p class="success-desc">编译加速任务的配置信息已经全部配置完成。还需修改目标构建机上编译命令，方能顺利使用编译加速服务。</p>
            <div class="success-info">
                <i class="bk-icon icon-exclamation-circle-shape"></i>
                <p class="success-info-title">提示：修改makefile文件和编译命令</p>
                <p class="success-info-title-level-2">1、执行./install.sh将软件包安装到目标构建机上</p>
                <p class="success-info-title-level-2">2、复制和整理模板文件</p>
                <p class="success-info-desc">将 /etc/bk_distcc/bazel下的配置文件bk_bazelrc和文件夹bkdistcctoolchain（含里面的文件）拷贝到项目根目录下。</p>
                <p class="success-info-desc">如果项目有自己的toolchain配置，需要将其内容合并到上述文件里。</p>
                <p class="success-info-title-level-2">3、更新bazel命令(请将示例中${turbo.task.id}替换为本任务ID:<span class="warning">{{ getRegister.taskId }}</span>）</p>
                <p class="success-info-desc">例如：</p>
                <p class="success-info-desc">cd /path/to/your_project</p>
                <p class="success-info-desc">bazel build //main:hello-world</p>
                <br />
                <p class="success-info-desc">改为使用bk-bazel：</p>
                <p class="success-info-desc">cd /path/to/your_project</p>
                <p class="success-info-desc">bk-bazel -p ${turbo.task.id} -a "build //main:hello-world"</p>
                <br />
                <p class="success-info-desc">-a 用于传递原始bazel命令后面带的参数</p>
                <p class="success-info-desc">
                    <a :href="`${DOCS_URL_PREFIX}/x/sobm`"
                        class="text-link" target="_blank">查看更多修改案例</a></p>
            </div>
        </template>
        <template v-else-if="isToolMake">
            <p class="success-desc">编译加速任务的配置信息已经全部配置完成。还需修改流水线中脚本内容，方能顺利使用编译加速服务。</p>
            <div class="success-info">
                <i class="bk-icon icon-exclamation-circle-shape"></i>
                <p class="success-info-title">提示：修改流水线脚本内容</p>
                <p class="success-info-title-level-2">1、若你的makefile是由cmake自动生成的，需先修改cmake并加上编译加速任务ID参数</p>
                <p class="success-info-desc">cmake改为bk-cmake -p ${turbo.task.id} {{ extraParam() }}</p>
                <p class="success-info-title-level-2">2、使用bk-make来替代make，并加上编译加速任务ID和流水线当前构建ID参数</p>
                <p class="success-info-desc">make改为bk-make -p ${turbo.task.id} -b ${pipeline.build.id} {{ extraParam() }}</p>
                <p class="success-info-desc">
                    <a :href="`${DOCS_URL_PREFIX}/x/tYbm`"
                        class="text-link" target="_blank">查看更多修改案例</a></p>
            </div>
        </template>
        <template v-else-if="isToolBlade">
            <p class="success-desc">编译加速任务的配置信息已经全部配置完成。还需修改流水线中脚本内容，方能顺利使用编译加速服务。</p>
            <div class="success-info">
                <i class="bk-icon icon-exclamation-circle-shape"></i>
                <p class="success-info-title">提示：修改编译命令</p>
                <p class="success-info-title-level-2">1、打开distcc开关。在项目的 BLADE_ROOT 文件里面添加：</p>
                <p class="success-info-desc">distcc_config(</p>
                <p class="success-info-desc">&nbsp;&nbsp;&nbsp;&nbsp;enabled=True</p>
                <p class="success-info-desc">)</p>
                <p class="success-info-title-level-2">2、修改blade命令</p>
                <p class="success-info-desc">例如：</p>
                <p class="success-info-desc">cd /path/to/your_project</p>
                <p class="success-info-desc">blade build</p>
                <br />
                <p class="success-info-desc">改为使用bk-blade：</p>
                <p class="success-info-desc">cd /path/to/your_project</p>
                <p class="success-info-desc">bk-blade -p ${turbo.task.id} -b ${pipeline.build.id} -a "build"</p>
                <br />
                <p class="success-info-desc">-a 用于传递原始blade命令后面带的参数</p>
                <p class="success-info-desc">
                    <a :href="`${DOCS_URL_PREFIX}/x/tYbm`"
                        class="text-link" target="_blank">查看更多修改案例</a></p>
            </div>
        </template>
        <template v-else-if="isToolBazel">
            <p class="success-desc">编译加速任务的配置信息已经全部配置完成。还需修改流水线中脚本内容，方能顺利使用编译加速服务。</p>
            <div class="success-info">
                <i class="bk-icon icon-exclamation-circle-shape"></i>
                <p class="success-info-title">提示：修改编译命令</p>
                <p class="success-info-title-level-2">1、复制和整理模板文件</p>
                <p class="success-info-desc">将配置文件bk_bazelrc和文件夹bkdistcctoolchain（含里面的文件）拷贝到项目根目录下。<a href="http://devgw.devops.oa.com/turbo-client/bazel.zip" class="text-link">点击下载模板文件>></a></p>
                <p class="success-info-desc">如果项目有自己的toolchain配置，需要将其内容合并到上述文件里。</p>
                <p class="success-info-title-level-2">2、修改bazel命令</p>
                <p class="success-info-desc">例如：</p>
                <p class="success-info-desc">cd /path/to/your_project</p>
                <p class="success-info-desc">bazel build //main:hello-world</p>
                <br />
                <p class="success-info-desc">改为使用bk-bazel：</p>
                <p class="success-info-desc">cd /path/to/your_project</p>
                <p class="success-info-desc">bk-bazel -p ${turbo.task.id} -b ${pipeline.build.id} -a "build //main:hello-world"</p>
                <br />
                <p class="success-info-desc">-a 用于传递原始bazel命令后面带的参数</p>
                <p class="success-info-desc">
                    <a :href="`${DOCS_URL_PREFIX}/x/tYbm`"
                        class="text-link" target="_blank">查看更多修改案例</a></p>
            </div>
        </template>
        <div class="success-group" v-if="isInstallToMachine">
            <bk-button theme="primary" @click="registCancel">确定</bk-button>
        </div>
        <div class="success-group" v-else>
            <bk-button theme="primary" @click="jumpPipeline">前往流水线修改脚本</bk-button>
            <bk-button theme="default" @click="registCancel">知道了</bk-button>
        </div>
    </section>
</template>

<script>
    import { mapGetters } from 'vuex'
    
    export default {
        name: 'registSuccess',
        data () {
            return {
                buildText: '',
                DOCS_URL_PREFIX: DOCS_URL_PREFIX
            }
        },
        computed: {
            ...mapGetters('turbo', [
                'getRegister'
            ]),
            projectId () {
                return this.$route.params.projectId
            }
        },
        created () {
            const { banDistcc, ccacheEnabled } = this.getRegister
            const installToMachine = '3'
            const toolMake = '1'
            const toolBlade = '2'
            const toolBazel = '3'
            this.isInstallToMachine = this.getRegister.machineType === installToMachine
            this.isToolMake = this.getRegister.toolType === toolMake
            this.isToolBlade = this.getRegister.toolType === toolBlade
            this.isToolBazel = this.getRegister.toolType === toolBazel
            if (banDistcc === 'false' && ccacheEnabled === 'true') {
                this.buildText = 'distcc+ccache'
            } else if (banDistcc === 'true' && ccacheEnabled === 'true') {
                this.buildText = 'ccache'
            } else {
                this.buildText = 'distcc'
            }
        },
        methods: {
            registCancel () {
                this.$router.push({
                    name: 'acceleration',
                    params: this.$router.params
                })
            },
            jumpPipeline () {
                window.open(`/console/pipeline/${this.projectId}/${this.getRegister.bsPipelineId}/edit#${this.getRegister.bsElementId}`, '_blank')
            },
            extraParam () {
                const { gccVersion } = this.getRegister
                if (gccVersion && gccVersion.indexOf('clang') !== -1) {
                    return '-C'
                }
                return ''
            }
        }
    }
</script>

<style scoped lang="scss">
    @import '../../assets/scss/conf.scss';

    .wrapper-container-success {
        .success-title {
            display: flex;
            align-items: center;
            margin-bottom: 10px;
            font-size: 16px;
            color: #333c48;
            >.bk-icon {
                margin-right: 8px;
                font-size: 32px;
                color: #00c873;
            }
        }
        .success-desc {
            margin: 0 0 8px 0;
            font-size: 14px;
        }
        .success-info {
            position: relative;
            margin-bottom: 20px;
            padding: 18px 20px  18px 44px;
            width: 870px;
            min-height: 80px;
            border: 1px solid #ffB848;
            border-radius: 2px;
            font-size: 14px;
            color: $fontColor;
            background: #fff4e2;
            .bk-icon {
                position: absolute;
                top: 19px;
                left: 16px;
                font-size: 18px;
                color: #ff9600;
            }
            .success-info-title {
                font-weight: bold;
                margin: 0 0 6px 0;
                color: #333c48;
            }
            .success-info-title-level-2 {
                margin: 10px 0 6px 0;
                color: #3d4044;
            }
            .success-info-title-level-3 {
                margin: 10px 0 6px 20px;
                color: #3d4044;
            }
            .success-info-desc {
                margin: 0;
                padding-left: 20px;
            }
        }
        .success-info-inline {
            display: inline-block;
            min-width: 100px;
        }
        .success-group {
            button + button {
                margin-left: 8px;
            }
        }
    }
    
</style>
