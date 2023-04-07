const { src, dest, parallel, series, task } = require('gulp')
const fetch = require('node-fetch')
const chalk = require('chalk')
const fs = require('fs')
const path = require('path')
const htmlmin = require('gulp-htmlmin')
const svgSprite = require('gulp-svg-sprite')
const inject = require('gulp-inject')
const rename = require('gulp-rename')
const hash = require('gulp-hash')
const replace = require('gulp-replace')
const Ora = require('ora')
const yargs = require('yargs')
const del = require('del')

const argv = yargs.alias({
    dist: 'd',
    env: 'e',
    lsVersion: 'l',
    type: 't',
    scope: 's'
}).default({
    dist: 'frontend',
    env: 'master',
    lsVersion: 'v2',
    type: 'tencent'
}).describe({
    dist: 'build output dist directory',
    env: 'environment [dev, test, master, external]',
    lsVersion: 'localStorage version',
    type: 'bkdevops version 【ee | tencent】'
}).argv
const { dist, env, lsVersion, type, scope } = argv

const svgSpriteConfig = {
    mode: {
        symbol: true
    }
}
const isGray = env === 'gray'
const envPrefix = isGray || env === 'master' ? '' : `${env}.`
const BUNDLE_NAME = 'assets_bundle.json'
const ASSETS_JSON_URL = `http://${envPrefix}devnet.devops.oa.com/${BUNDLE_NAME}`

async function getAssetsJSON (jsonUrl) {
    try {
        const res = await fetch(jsonUrl, {
            headers: isGray ? { 'X-DEVOPS-PROJECT-ID': 'grayproject' } : {}
        })
        const assets = await res.json()

        console.log(chalk.blue.bold(`Successfully get assets json from ${jsonUrl}!`))
        console.table(assets)
        return assets
    } catch (error) {
        console.log(chalk.yellow.bgRed.bold(`Failed get assets json from ${jsonUrl}!`))
        process.exit(1)
    }
}

function taskGenerator (type) {
    return () => {
        return src(`./svg-sprites/${type}/*.svg`)
            .pipe(svgSprite(svgSpriteConfig))
            .pipe(dest(`./svg-sprites/dist/${type}`))
    }
}

function renameSvg (type) {
    return () => {
        return src(`./svg-sprites/dist/${type}/symbol/svg/sprite.symbol.svg`)
            .pipe(rename(`${type}_sprite.svg`))
            .pipe(dest(`${dist}/svg-sprites/`))
    }
}

function generatorSvgJs (type) {
    return () => {
        const svgCode = fs.readFileSync(`${dist}/svg-sprites/${type}_sprite.svg`, 'utf8')
        return src('./svg-sprites/svgjs-template.js')
            .pipe(replace('__SVG_SPRITES_SYMBOLS__', svgCode))
            .pipe(rename(`${type}_sprite.js`))
            .pipe(hash())
            .pipe(dest(`${dist}/svg-sprites/`))
    }
}

function getScopeStr (scope) {
    try {
        if (!scope) return ''
        let scopeArray
        switch (true) {
            case typeof scope === 'string':
                scopeArray = scope.split(',')
                break
            default:
                scopeArray = scope
        }
        const isMultiple = scopeArray.length > 1
        return `--scope=devops-${isMultiple ? `{${scopeArray.join(',')}}` : scopeArray.join(',')}`
    } catch (e) {
        console.error(e)
        return ''
    }
}

task('clean', () => {
    return del(dist)
})

task('devops', series([taskGenerator('devops'), renameSvg('devops'), generatorSvgJs('devops')]))
task('pipeline', series([taskGenerator('pipeline'), renameSvg('pipeline'), generatorSvgJs('pipeline')]))
task('copy', () => src(['common-lib/**'], { base: '.' }).pipe(dest(`${dist}/`)))

task('build', async () => {
    const assetJson = await getAssetsJSON(ASSETS_JSON_URL)
    fs.writeFileSync(path.join(__dirname, dist, BUNDLE_NAME), JSON.stringify(assetJson))
    const scopeStr = getScopeStr(scope)
    const envConfMap = {
        dist,
        version: type,
        lsVersion
    }
    const envQueryStr = Object.keys(envConfMap).reduce((acc, key) => {
        acc += ` --env ${key}=${envConfMap[key]}`
        return acc
    }, '')
    console.log(envQueryStr)
    await execAsync(`lerna run public:master ${scopeStr}`)
})

task('generate-assets-json', () => {
    const fileContent = `window.SERVICE_ASSETS = ${fs.readFileSync(path.join(__dirname, dist, BUNDLE_NAME), 'utf8')}`
    fs.writeFileSync(`${dist}/assetsBundles.js`, fileContent)
    return src(`${dist}/assetsBundles.js`).pipe(hash()).pipe(dest(`${dist}/`))
})

task('inject-asset', parallel(['console', 'pipeline'].map(prefix => {
    const dir = path.join(dist, prefix)
    const spriteNameGlob = `${prefix === 'console' ? 'devops' : 'pipeline'}_sprite-*.js`
    const fileName = `frontend#${prefix}#index.html`
    return () => src(path.join(dir, fileName))
        .pipe(inject(src([
            ...(prefix === 'console' ? [`${dist}/assetsBundles-*.js`] : []),
            `${dist}/svg-sprites/${spriteNameGlob}`
        ], {
            read: false
        }), {
            allowEmpty: true,
            ignorePath: dist,
            addRootSlash: false,
            addPrefix: '__BK_CI_PUBLIC_PATH__'
        }))
        .pipe(htmlmin({
            collapseWhitespace: true,
            removeComments: true,
            minifyJS: true
        }))
        .pipe(dest(dir))
}
)))

async function execAsync (cmd) {
    const spinner = new Ora('building bk-ci frontend project').start()
    return new Promise((resolve, reject) => {
        require('child_process').exec(cmd, {
            maxBuffer: 5000 * 1024,
            env: {
                ...process.env,
                dist,
                lsVersion
            }
        }, (err, res) => {
            if (err) {
                reject(err)
                process.exit(1)
            }
            spinner.succeed('Finished building bk-ci frontend project')
            resolve()
        })
    })
}
  
exports.default = series('clean', parallel('devops', 'pipeline', 'copy', 'build'), 'generate-assets-json', 'inject-asset')
