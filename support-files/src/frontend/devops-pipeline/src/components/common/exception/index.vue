<template>
	<div class="bk-exception bk-exception-center" v-show="show">
	    <img :src="image">
	    <h2 class="exception-text">{{message}}</h2>
	</div>
</template>

<script>
    /**
    *  app-exception
    *  @desc 异常页面
    *  @param type {String} - 异常类型，有：404（找不到）、403（权限不足）、500（服务器问题）、building（建设中）
    *  @param delay {Number} - 延时显示
    *  @param text {String} - 显示的文案，默认：有：404（页面找不到了！）、403（Sorry，您的权限不足）、500（）、building(功能正在建设中···)
    *  @example1 <app-exception type="404"></app-exception>
    */
    import img403 from '../../../images/403.png'
    import img404 from '../../../images/404.png'
    import img500 from '../../../images/500.png'
    import imgBuilding from '../../../images/building.png'

    export default {
        name: 'app-exception',
        props: {
            type: {
                type: String,
                default: '404'
            },
            delay: {
                type: Number,
                default: 0
            },
            text: {
                type: String,
                default: ''
            }
        },
        data () {
            let message = ''
            let image = ''

            switch (this.type) {
                case '403':
                    image = img403
                    message = 'Sorry，您的权限不足！'
                    break

                case '404':
                    image = img404
                    message = '页面找不到了！'
                    break

                case '500':
                    image = img500
                    message = '服务器维护中，请稍后重试!'
                    break

                case 'building':
                    image = imgBuilding
                    message = '功能正在建设中···'
                    break
            }

            if (this.text) {
                message = this.text
            }
            
            return {
                show: false,
                message: message,
                image: image
            }
        },
        created () {
            setTimeout(() => {
                this.show = true
            }, this.delay)
        }
    }
</script>