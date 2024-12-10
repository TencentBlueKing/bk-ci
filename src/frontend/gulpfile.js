const { src, dest, parallel, series, task } = require('gulp')
const fetch = require('node-fetch')
const chalk = require('chalk')
const fs = require('fs')
const path = require('path')
const htmlmin = require('gulp-html-minifier-terser')
const svgSprite = require('gulp-svg-sprite')
const inject = require('gulp-inject')
const rename = require('gulp-rename')
const hash = require('gulp-hash')
const { globSync } = require('glob')
const replace = require('gulp-replace')
const Ora = require('ora')
const yargs = require('yargs')
const del = require('del')

const argv = yargs.alias({
    dist: 'd',
    env: 'e',
    lsVersion: 'l',
    type: 't',
    scope: 's',
    effect: 'effect'
}).default({
    dist: 'frontend',
    env: 'master',
    lsVersion: 'v2',
    type: 'tencent',
    effect: true
}).describe({
    dist: 'build output dist directory',
    env: 'environment [dev, test, master, external]',
    lsVersion: 'localStorage version',
    head: 'head file path',
    base: 'base file path',
    effect: 'only buuild effected service'
}).argv
const { dist, env, lsVersion, scope, head = 'HEAD', base = 'master', effect } = argv
console.log(env, head, base)

const svgSpriteConfig = {
    mode: {
        symbol: true
    }
}

const envPrefix = ['dev', 'test'].indexOf(env) > -1 ? `${env}.` : ''
const BUNDLE_NAME = 'assets_bundle.json'
const FINAL_ASSETS_JSON_FILENAME = `${dist}/assetsBundles.js`
const ASSETS_JSON_URL = `https://${envPrefix}devnet.devops.woa.com/${BUNDLE_NAME}`
const gateWayTagMap = {
    dev: 'dev-rbac',
    test: 'test-rbac',
    stream: '',
    'stream-gray': ''
}

async function getAssetsJSON (jsonUrl) {
    try {
        const res = await fetch(jsonUrl, {
            headers: {
                'X-GATEWAY-TAG': gateWayTagMap[env] ?? env
            }
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
        return `-p ${scopeArray.map(item => `devops-${item}`).join(' ')}`
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
    return await execAsync()
})

task('generate-assets-json', () => {
    const entryDir = path.join(__dirname, dist, "entry's")
    const assetsBundlesName = path.join(__dirname, dist, BUNDLE_NAME)
    const prevAssets = JSON.parse(fs.readFileSync(assetsBundlesName, 'utf-8'))
    // 读取path.join(__dirname, dist, 'entry's', '*.json')所有Json合并成一个
    const finalAssets = globSync(path.join(entryDir, '*.json')).reduce((acc, file) => {
        const content = JSON.parse(fs.readFileSync(file, 'utf-8'))
        acc = {
            ...acc,
            ...content
        }
        return acc
    }, prevAssets)

    console.log('final assets json!')
    console.table(finalAssets)
    const fileContent = `window.SERVICE_ASSETS = ${JSON.stringify(finalAssets)}`
    
    fs.writeFileSync(FINAL_ASSETS_JSON_FILENAME, fileContent)
    fs.writeFileSync(path.join(__dirname, dist, BUNDLE_NAME), JSON.stringify(finalAssets))
    if (fs.existsSync(entryDir)) {
        fs.rmSync(entryDir, {
            recursive: true,
            force: true
        })
    }
    return src(FINAL_ASSETS_JSON_FILENAME).pipe(hash()).pipe(dest(dist))
})

task('inject-asset', parallel(['console', 'pipeline'].map(prefix => {
    const dir = path.join(dist, prefix)
    const spriteNameGlob = `${prefix === 'console' ? 'devops' : 'pipeline'}_sprite-*.js`
    const fileName = `frontend#${prefix}#index.html`
    return () => src(path.join(dir, fileName), { allowEmpty: true })
        .pipe(inject(src([
            ...(prefix === 'console' ? [`${dist}/assetsBundles-*.js`] : []),
            `${dist}/svg-sprites/${spriteNameGlob}`
        ], {
            read: false
        }), {
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

async function execAsync () {
    const spinner = new Ora('building bk-ci frontend project').start()
    
    return new Promise((resolve, reject) => {
        const scopeStr = getScopeStr(scope)
        const cmd = effect ? 'affected -t public:master' : `run-many -t public:master ${scopeStr}`
        console.log('gulp cmd: ', cmd, cmd.split(' '))
        const { spawn } = require('node:child_process')
        const spawnCmd = spawn('pnpm', [
            'exec',
            'nx',
            '--parallel=22',
            ...cmd.split(' ')
        ], {
            stdio: 'inherit',
            env: {
                ...process.env,
                dist,
                lsVersion
            }
        })
        
        spawnCmd.on('close', async (code) => {
            console.log(`child process exited with code ${code}`)
            if (code) {
                spinner.fail('Failed to build bk-ci frontend project')
                reject(Error('Failed to build bk-ci frontend project'))
                process.exit(1)
            }
            spinner.succeed('Finished building bk-ci frontend project')
            const assetJson = await getAssetsJSON(ASSETS_JSON_URL)
            fs.writeFileSync(path.join(__dirname, dist, BUNDLE_NAME), JSON.stringify(assetJson))
            resolve()
        })
    })
}
  
exports.default = series('clean', parallel('devops', 'pipeline', 'copy', 'build'), 'generate-assets-json', 'inject-asset')
