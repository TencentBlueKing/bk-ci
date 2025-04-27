<template>
    <div class="release-status-main">
        <section v-if="isInstanceReleasing">
            <bk-loading
                class="loading-icon"
                is-loading
                mode="spin"
                theme="primary"
                :size="16"
            />
           
            <p class="release-status-title"> 10 个实例正在发布中…</p>
            <p class="sub-message">你可以在当前页等待或关闭弹窗继续其他操作后续可在按钮处查看发布结果</p>
        </section>
        <section v-else-if="isPublishSuccess">
            <Logo
                size="64"
                name="success"
                class="release-status-icon"
            />
            <p class="release-status-title"> 100 个流水线实例发布成功</p>
            <p class="sub-message">接下来你可以在实例列表中查看已发布的实例</p>
            <bk-button
                @click="handleClick"
                class="release-status-btn"
            >
                返回列表
            </bk-button>
        </section>
        <section v-else-if="isPublishFailure">
            <Logo
                size="64"
                name="failure"
                class="release-status-icon"
            />
            <p class="release-status-title"> 100 个流水线实例发布失败</p>
            <p class="sub-message">接下来你可以重试或关闭弹窗</p>
            <div class="release-status-btn">
                <bk-button
                    theme="primary"
                    @click="handleClick"
                >
                    重试
                </bk-button>
                <bk-button
                    @click="handleClick"
                >
                    修改配置
                </bk-button>
                <bk-button
                    @click="handleClick"
                >
                    关闭
                </bk-button>
            </div>
        </section>
        <section v-else>
            <Logo
                size="64"
                name="required"
                class="release-status-icon"
            />
            <p class="release-status-title">合并请求创建完成，请到代码库处理...</p>
            <p class="sub-message pending">版本尚未发布成功，下一步 <span>请到代码库处理合并请求</span> </p>
            <p class="pac-mode-message">PAC模式下，YAML 文件合入默认分支，才视为发布正式版本</p>
            <div class="release-status-btn">
                <bk-button
                    theme="primary"
                    @click="handleClick"
                >
                    处理合并请求
                </bk-button>
                <bk-button
                    @click="handleClick"
                >
                    返回列表
                </bk-button>
            </div>
        </section>
    </div>
</template>
<script setup>
    import { computed } from 'vue'
    import UseInstance from '@/hook/useInstance'
    const { proxy } = UseInstance()
    const isInstanceReleasing = computed(() => proxy?.$store?.state?.templates?.isInstanceReleasing)
</script>
<style lang="scss">
.release-status-main {
    display: flex;
    align-items: center;
    justify-content: space-evenly;
    width: 100%;
    height: 100%;
    text-align: center;
    .loading-icon {
        display: flex;
        height: 150px;
    }
    .release-status-icon {
        margin-top: 122px;
        margin-bottom: 40px;
    }

    .release-status-title {
        font-size: 24px;
        color: #313238;
        line-height: 32px;
        margin-bottom: 16px;
    }

    .sub-message {
        margin: auto;
        width: 280px;
        font-size: 14px;
        color: #4D4F56;
        text-align: center;
    }

    .pending {
        width: 552px;
        margin-bottom: 16px;
        text-align: left;
        span {
            cursor: pointer;
            font-weight: 700;
        }
    }

    .pac-mode-message {
        margin: auto;
        text-align: left;
        padding-left: 16px;
        width: 552px;
        height: 46px;
        line-height: 46px;
        background: #F5F7FA;
        border-radius: 2px;
        font-size: 14px;
        color: #4D4F56;
    }

    .release-status-content {
        margin: auto;
        margin-top: 22px;
        width: 294px;
        height: 294px;
        border: 1px solid #DCDEE5;
        border-radius: 50%;
        box-shadow: inset 0 1px 13px 0 #0000001a;
    }

    .release-status-btn {
        margin-top: 28px;
    }

}

</style>
