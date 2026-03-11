<template>
    <section>
        <dashboard-header></dashboard-header>
        <div id="modal-root"></div>
        <section class="hero">
            <div class="hero--content">
                <div class="container" style="display: flex">
                    <div class="hero--text-block" style="padding: 0 100px 80px 0; z-index: 100">
                        <h1 class="hero--title">用 Stream 来
                            <ul class="anima-title">
                                <li class="anima-item"> 编译</li>
                                <li class="anima-item"> 测试</li>
                                <li class="anima-item"> 部署</li>
                            </ul>
                            <span style="margin-left: 135px;">你的工程</span>
                        </h1>
                        <div>Stream 服务可以在主流的操作系统上持续快速地编译、测试、部署你的工程。<br>
                            让你聚焦于编码本身，而非权限申请、构建机准备、发布沟通等琐事。
                        </div>
                        <div class="hero-cta">
                            <a href="/dashboard" class="button big black" data-proofer-ignore="">
                                在 Stream 上运行一个 "Hello World"
                            </a>
                            <a :href="LINK_CONFIG.STREAM" target="_blank" class="button big " data-proofer-ignore="">
                                了解更多特性
                            </a>
                        </div>
                    </div>

                    <div class="gif-item leftAnima">
                        <img class="leftImg" ref="leftImg" :src="require('./../images/home/ui.gif')" />
                    </div>
                    <div class="gif-item rightAnima">
                        <img class="rightImg" ref="rightImg" :src="require('./../images/home/code.gif')" />
                    </div>
                </div>

            </div>
        </section>

        <section id="your-site-your-workflow" class="section section-roots">
            <div class="container">
                <div class="feature-block ltr" style="display: flex;justify-content: space-between;">
                    <div class="feature-block--copy">
                        <h2 class="section--title" style="">
                            语法对齐GitHub Actions<br>
                            将语法优雅进行到底！
                        </h2>
                        <div class="section--sub">
                            <p>
                                最直接与开发者打交道的就是 YAML 语法，我们在调研了 Azure Pipelines / Gitlab CI / Travis CI / Circle CI 等行业主流 CI YAML语法后最终决定：Stream 语法将100%兼容 GitHub Actions ！
                            </p>
                            <div class="hero-cta" style="margin-top: 20px;">
                                <a :href="LINK_CONFIG.YAML_SCHEMA" target="_blank" class="button big black" data-proofer-ignore="">
                                    点击查阅完整 YAML 语法
                                </a>
                            </div>
                        </div>
                    </div>
                    <div class="feature-block--illu illu-shadow--right">
                        <img loading="lazy" src="./../images/home/yaml.png" style="width:410px;height:554px;">
                    </div>
                </div>
            </div>
        </section>

        <section id="empower-your-editors" class="section section-sync">
            <div class="container">
                <div class="feature-block rtl">
                    <div class="feature-block--illu illu-shadow--left">
                        <img loading="lazy" src="./../images/home/home.png" data-sizes="(max-width: 76px) 100%, 800px" class=" lazyloaded" sizes="(max-width: 76px) 100%, 800px">
                    </div>
                    <div class="feature-block--copy">
                        <h2 class="section--title" style="">代码与UI，二者皆可得</h2>
                        <div class="section--sub">
                            <p>
                                在 Stream 里，你可以用 YAML 代码的方式描述出你的 CI 流水线，这样可以将流水线的配置变更也纳入日常的Git工作流中。
                            </p>
                            <p>
                                同时，因为与蓝盾复用了相同的 CI 引擎，所以依然可以保持与蓝盾图形化 UI 相同的体验，在出问题时可快速定位。
                            </p>
                        </div>
                    </div>
                </div>
            </div>
        </section>

        <section id="own-your-content" class="section section-automated">
            <div class="container">
                <div class="feature-block ltr">
                    <div class="feature-block--copy">
                        <h2 class="section--title" style="">丰富的腾讯 CI 插件生态</h2>
                        <div class="section--sub">
                            <p>蓝盾研发商店自发布以来，共上架<strong>1250</strong>个流水线插件</p>
                            <p>在 Stream 中，你可以通过<em>uses</em>关键字方便引入这些插件，让插件生态得以最大化利用。<a href="https://devops.woa.com/console/store/market/list?features=&sortType=RECENT_EXECUTE_NUM&pipeType=atom" target="_blank">
                                点击浏览所有腾讯CI插件
                            </a></p>
                        </div>
                            
                    </div>
                    <div class="feature-block--illu illu-shadow--right">
                        <img loading="lazy" src="./../images/home/plugins.png" data-sizes="(max-width: 76px) 100%, 800px" class=" lazyloaded" sizes="(max-width: 76px) 100%, 800px">
                    </div>
                </div>
                
            </div>
        </section>
        <!--
        <section id="trusted-by" class="section section-logos">
            <div class="container">
                <span class="logos--title">他们都在用</span>
                <div class="logos--container">
                    <img src="./../images/home/bk-ci.png" class="logos--logo" width="auto" height="60" style="margin-top:0px;" alt="蓝盾">
                    <img src="./../images/home/tgit-with.png" class="logos--logo" width="auto" height="60" style="margin-top:0px;" alt="工蜂">
                    <img src="./../images/home/tencent_ci.png" class="logos--logo" width="auto" height="50" style="margin-top:0px;" alt="腾讯CI">
                    <img src="./../images/home/EP.png" class="logos--logo" width="auto" height="60" style="margin-top:-1px;" alt="EP度量">
                </div>
            </div>
        </section> -->

        <section class="section section-cta center">
            <div class="container">
                <h2 class="section--title">快速高质量地交付代码</h2>
                <p class="section--sub is-center">用最好的 CI 工具，交付更好的代码</p>
                <br><br>
                <a href="/dashboard" class="button black">为你的工程开启 CI</a>
            </div>
        </section>

        <footer class="footer center">
            <div class="lower-footer">
                <div class="container">
                    <div>Copyright © 2012-2021 Tencent BlueKing. All Rights Reserved</div>
                </div>
            </div>
        </footer>
    </section>
</template>

<script>
    import uiGif from '@/images/home/ui.gif'
    import uiJpg from '@/images/home/ui.jpg'
    import codeGif from '@/images/home/code.gif'
    import codeJpg from '@/images/home/code.jpg'
    import LINK_CONFIG from '@/conf/link-config.js'
    import DashboardHeader from '@/components/dashboard-header.vue'

    export default {
        components: {
            DashboardHeader
        },
        data () {
            return {
                LINK_CONFIG
            }
        },
        mounted () {
            this.initAnimaShow()
            this.initGifAnimaShow()
            this.$once('hook:beforeDestroy', () => {
                clearTimeout(this.timer)
                clearTimeout(this.gifTimer)
            })
        },
        methods: {
            initAnimaShow () {
                clearTimeout(this.timer)
                this.timer = null
                let time = 0
                const animaItems = document.querySelectorAll('.anima-item')
                for (let i = 0; i < animaItems.length; i++) {
                    this.timer = setTimeout(() => {
                        animaItems[i].className += ' moving'
                        animaItems[i].style.top = 0
                        if (!i) return
                        animaItems[i - 1].className = 'anima-item'
                        animaItems[i - 1].style.top = '-200px'
                    }, time)
                    time += 3000
                }

                this.timer = setTimeout(() => {
                    for (let i = 0; i < animaItems.length; i++) {
                        animaItems[i].className = 'anima-item'
                        animaItems[i].style.top = '-200px'
                    }
                    this.initAnimaShow()
                }, 9000)
            },

            initGifAnimaShow () {
                this.$refs.leftImg && (this.$refs.leftImg.src = uiGif)
                this.$refs.rightImg && (this.$refs.rightImg.src = codeJpg)
                const gifItems = document.querySelectorAll('.gif-item')
                
                setTimeout(() => {
                    this.$refs.leftImg && (this.$refs.leftImg.src = uiJpg)
                    this.$refs.rightImg && (this.$refs.rightImg.src = codeGif)
                    gifItems[1].style.zIndex = '999'
                    setTimeout(() => {
                        gifItems[1].style.zIndex = '0'
                        this.$refs.rightImg && (this.$refs.rightImg.src = codeJpg)
                    }, 11300)
                }, 4800)
                this.gifTimer = setTimeout(() => {
                    this.initGifAnimaShow()
                }, 18000)
            }
        }
    }
</script>

<style lang="postcss" scoped>
    @import '@/css/home';
    .anima-title {
        display: inline-block;
        width: 120px;
        height: 160px;
        line-height: 160px;
        position: absolute;
        top: 10px;
        overflow: hidden;
        p {
            color: #000;
        }
    }
    .anima-item {
        position: absolute;
        font-weight: 700;
        top: -200px;
        width: 120px;
        text-align: center;
    }
    
    .gif-item {
        .leftImg {
            border-radius: 15px;
            box-shadow: -15px 0 20px -6px #ccc,
                        0 -5px 20px -4px #ccc,
                        0 5px 20px -5px #ccc,
                        0px 0 0px 0px #ccc;
        }
        .rightImg {
            border-radius: 15px;
            box-shadow: 0px 0 0px 0px #ccc,
                        0 -5px 20px -4px #ccc,
                        0 5px 12px -5px #ccc,
                        0px 0 0px 0px #ccc;
        }
    }

    .moving {
        -webkit-animation: moving 3s forwards;
        animation: moving 3s forwards;
    }

    @keyframes moving {
        15% {
            transform: translateY(0px);
            opacity: 0.8;
        }
        30% {
            transform: translateY(0px);
        }
        50% {
            transform: translateY(0px);
            opacity: 1;
        }
        80% {
            transform: translateY(50px);
            opacity: 0;
        }
        100% {
            transform: translateY(50px);
            opacity: 0;
        }
        from {
            transform: translateY(-40px);
            opacity: 0;
        }
        to {
            transform: translateY(40px);
        }
    }
    .leftAnima {
        animation: leftAnima 18s infinite;
        position: absolute;
        width: 600px;
        right: 220px;
        top: -130px;
        z-index: 50;
    }
    .rightAnima {
        animation: rightAnima 18s infinite;
        position: absolute;
        width: 350px;
        right: -75px;
        top: -200px;
    }
    
    @keyframes leftAnima {
        1% {
            transform: translateX(0);
        }
        9% {
            transform: translateX(0);
        }
        26% {
            transform: translateX(0);
        }
        28% {
            transform: translateX(-50px);
        }
        34% {
            transform: translateX(50px);
        }
        88% {
            transform: translateX(50px);
        }
        92% {
            transform: translateX(-70px);
        }
        94% {
            transform: translateX(0);
        }
    }

    @keyframes rightAnima {
        1% {
            transform: translateX(0);
        }
        26% {
            transform: translateX(0);
        }
        28% {
            transform: translateX(50px);
        }
        34% {
            transform: translateX(-75px);
        }
        88% {
            transform: translateX(-75px);
        }
        92% {
            transform: translateX(75px);
        }
        94% {
            transform: translateX(0px);
        }
    }

    @media screen and (max-width: 1680px) {
        .leftAnima {
            width: 500px;
            right: 180px;
            top: -100px;
            z-index: 50;
        }
        .rightAnima {
            width: 300px;
            right: -75px;
            top: -170px;
        }
    }

    @media screen and (max-width: 1400px) {
        .leftAnima {
            width: 400px;
            right: 130px;
            top: -50px;
            z-index: 50;
        }
        .rightAnima {
            width: 250px;
            right: -75px;
            top: -120px;
        }
    }

    @media ( max-height : 820px ) {
        .leftAnima {
            width: 500px;
            right: 180px;
            top: -80px;
            z-index: 50;
        }
        .rightAnima {
            width: 300px;
            right: -75px;
            top: -150px;
        }
    }

    @media ( max-height : 820px ) and (max-width: 1680px) {
        .leftAnima {
            width: 450px;
            right: 180px;
            top: -50px;
            z-index: 50;
        }
        .rightAnima {
            width: 280px;
            right: -75px;
            top: -120px;
        }
    }

    @media ( max-height : 820px ) and (max-width: 1400px) {
        .leftAnima {
            width: 400px;
            right: 130px;
            top: -50px;
            z-index: 50;
        }
        .rightAnima {
            width: 250px;
            right: -75px;
            top: -120px;
        }
    }
</style>
