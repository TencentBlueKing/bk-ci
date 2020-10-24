/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.  
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *
 */

package com.tencent.bkrepo.docker.manifest

import com.tencent.bkrepo.docker.model.DockerDigest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class ManifestSchema2DeserializerTest {

    @Test
    @DisplayName("测试schema v2协议解析")
    fun manifestSchema2Test() {
        val manifest =
            """{
   "schemaVersion": 2,
   "mediaType": "application/vnd.docker.distribution.manifest.v2+json",
   "config": {
      "mediaType": "application/vnd.docker.container.image.v1+json",
      "size": 7645,
      "digest": "sha256:e2b047b17a138636a6ba9abd71c6e2e99ff38a5110177d2a34557eff61e0b040"
   },
   "layers": [
      {
         "mediaType": "application/vnd.docker.image.rootfs.diff.tar.gzip",
         "size": 27098756,
         "digest": "sha256:afb6ec6fdc1c3ba04f7a56db32c5ff5ff38962dc4cd0ffdef5beaa0ce2eb77e2"
      },
      {
         "mediaType": "application/vnd.docker.image.rootfs.diff.tar.gzip",
         "size": 26201443,
         "digest": "sha256:2e231683bfde7f6cdb860dcaf855c8aaf8a4cdb83ab8dd345ab35e8d5a2e421a"
      },
      {
         "mediaType": "application/vnd.docker.image.rootfs.diff.tar.gzip",
         "size": 202,
         "digest": "sha256:511e2efefada0b20168024e58d73b3c5dcaa71383f77461c4be74d742838f5df"
      },
      {
         "mediaType": "application/vnd.docker.image.rootfs.diff.tar.gzip",
         "size": 635,
         "digest": "sha256:e8fd0ec105c9a0ba769b2357195c0842fddf670c55f9e6a4a5f55d6fa61311d5"
      },
      {
         "mediaType": "application/vnd.docker.image.rootfs.diff.tar.gzip",
         "size": 1256153,
         "digest": "sha256:97357440e4e4b86c59294355fd3747ec274866b3028f9962d16ee131e8bdb0db"
      }
   ]
}"""
        val configFile =
            """{
    "architecture":"amd64",
    "config":{
        "Hostname":"",
        "Domainname":"",
        "User":"",
        "AttachStdin":false,
        "AttachStdout":false,
        "AttachStderr":false,
        "ExposedPorts":{
            "80/tcp":{

            }
        },
        "Tty":false,
        "OpenStdin":false,
        "StdinOnce":false,
        "Env":[
            "PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin",
            "NGINX_VERSION=1.18.0",
            "NJS_VERSION=0.4.0",
            "PKG_RELEASE=1~buster"
        ],
        "Cmd":[
            "nginx",
            "-g",
            "daemon off;"
        ],
        "ArgsEscaped":true,
        "Image":"sha256:741d47c34fe04ee2b25098e547a9337b3793ae3aa26479231f90489a273a3775",
        "Volumes":null,
        "WorkingDir":"",
        "Entrypoint":null,
        "OnBuild":null,
        "Labels":{
            "maintainer":"NGINX Docker Maintainers <docker-maint@nginx.com>"
        },
        "StopSignal":"SIGTERM"
    },
    "container":"05206ac3c29c4c0ac963cb2ce1656b54899c2fc9d5993b0724d6c0885e622780",
    "container_config":{
        "Hostname":"",
        "Domainname":"",
        "User":"",
        "AttachStdin":false,
        "AttachStdout":false,
        "AttachStderr":false,
        "ExposedPorts":{
            "80/tcp":{

            }
        },
        "Tty":false,
        "OpenStdin":false,
        "StdinOnce":false,
        "Env":[
            "PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin",
            "NGINX_VERSION=1.18.0",
            "NJS_VERSION=0.4.0",
            "PKG_RELEASE=1~buster"
        ],
        "Cmd":[
            "/bin/sh",
            "-c",
            "apt-get update     && apt-get install -y iputils-ping telnet procps net-tools curl --no-install-recommends     && rm -rf /var/lib/apt/lists/*     && echo "Asia/Shanghai" > /etc/timezone     && rm -f /etc/localtime     && dpkg-reconfigure -f noninteractive tzdata"
        ],
        "ArgsEscaped":true,
        "Image":"sha256:741d47c34fe04ee2b25098e547a9337b3793ae3aa26479231f90489a273a3775",
        "Volumes":null,
        "WorkingDir":"",
        "Entrypoint":null,
        "OnBuild":null,
        "Labels":{
            "maintainer":"NGINX Docker Maintainers <docker-maint@nginx.com>"
        },
        "StopSignal":"SIGTERM"
    },
    "created":"2020-05-21T14:40:57.129661387Z",
    "docker_version":"18.06.2-ce",
    "history":[
        {
            "created":"2020-05-15T06:28:44.479933669Z",
            "created_by":"/bin/sh -c #(nop) ADD file:7780c81c33e6cc5b6261af4a6c611cce0f39dec3131009bb297e65f12020c150 in / "
        },
        {
            "created":"2020-05-15T06:28:44.969566039Z",
            "created_by":"/bin/sh -c #(nop)  CMD ["bash"]",
            "empty_layer":true
        },
        {
            "created":"2020-05-15T20:15:17.575129526Z",
            "created_by":"/bin/sh -c #(nop)  LABEL maintainer=NGINX Docker Maintainers <docker-maint@nginx.com>",
            "empty_layer":true
        },
        {
            "created":"2020-05-15T20:16:45.463540574Z",
            "created_by":"/bin/sh -c #(nop)  ENV NGINX_VERSION=1.18.0",
            "empty_layer":true
        },
        {
            "created":"2020-05-15T20:16:45.657472556Z",
            "created_by":"/bin/sh -c #(nop)  ENV NJS_VERSION=0.4.0",
            "empty_layer":true
        },
        {
            "created":"2020-05-15T20:16:45.886515372Z",
            "created_by":"/bin/sh -c #(nop)  ENV PKG_RELEASE=1~buster",
            "empty_layer":true
        },
        {
            "created":"2020-05-15T20:17:07.555213971Z",
            "created_by":"/bin/sh -c set -x     && addgroup --system --gid 101 nginx     && adduser --system --disabled-login --ingroup nginx --no-create-home --home /nonexistent --gecos "nginx user" --shell /bin/false --uid 101 nginx     && apt-get update     && apt-get install --no-install-recommends --no-install-suggests -y gnupg1 ca-certificates     &&     NGINX_GPGKEY=573BFD6B3D8FBC641079A6ABABF5BD827BD9BF62;     found='';     for server in         ha.pool.sks-keyservers.net         hkp://keyserver.ubuntu.com:80         hkp://p80.pool.sks-keyservers.net:80         pgp.mit.edu     ; do         echo "Fetching GPG key $NGINX_GPGKEY from $server";         apt-key adv --keyserver "$server" --keyserver-options timeout=10 --recv-keys "$NGINX_GPGKEY" && found=yes && break;     done;     test -z "$found" && echo >&2 "error: failed to fetch GPG key $NGINX_GPGKEY" && exit 1;     apt-get remove --purge --auto-remove -y gnupg1 && rm -rf /var/lib/apt/lists/*     && dpkgArch="${'$'}(dpkg --print-architecture)"     && nginxPackages="         nginx=$NGINX_VERSION-$PKG_RELEASE         nginx-module-xslt=$NGINX_VERSION-$PKG_RELEASE         nginx-module-geoip=$NGINX_VERSION-$PKG_RELEASE         nginx-module-image-filter=$NGINX_VERSION-$PKG_RELEASE         nginx-module-njs=$NGINX_VERSION.$NJS_VERSION-$PKG_RELEASE     "     && case "$dpkgArch" in         amd64|i386)             echo "deb https://nginx.org/packages/debian/ buster nginx" >> /etc/apt/sources.list.d/nginx.list             && apt-get update             ;;         *)             echo "deb-src https://nginx.org/packages/debian/ buster nginx" >> /etc/apt/sources.list.d/nginx.list                         && tempDir="${'$'}(mktemp -d)"             && chmod 777 "$tempDir"                         && savedAptMark="${'$'}(apt-mark showmanual)"                         && apt-get update             && apt-get build-dep -y $nginxPackages             && (                 cd "$tempDir"                 && DEB_BUILD_OPTIONS="nocheck parallel=${'$'}(nproc)"                     apt-get source --compile $nginxPackages             )                         && apt-mark showmanual | xargs apt-mark auto > /dev/null             && { [ -z "$savedAptMark" ] || apt-mark manual $savedAptMark; }                         && ls -lAFh "$tempDir"             && ( cd "$tempDir" && dpkg-scanpackages . > Packages )             && grep '^Package: ' "$tempDir/Packages"             && echo "deb [ trusted=yes ] file://$tempDir ./" > /etc/apt/sources.list.d/temp.list             && apt-get -o Acquire::GzipIndexes=false update             ;;     esac         && apt-get install --no-install-recommends --no-install-suggests -y                         $nginxPackages                         gettext-base                         curl     && apt-get remove --purge --auto-remove -y && rm -rf /var/lib/apt/lists/* /etc/apt/sources.list.d/nginx.list         && if [ -n "$tempDir" ]; then         apt-get purge -y --auto-remove         && rm -rf "$tempDir" /etc/apt/sources.list.d/temp.list;     fi"
        },
        {
            "created":"2020-05-15T20:17:08.407421116Z",
            "created_by":"/bin/sh -c ln -sf /dev/stdout /var/log/nginx/access.log     && ln -sf /dev/stderr /var/log/nginx/error.log"
        },
        {
            "created":"2020-05-15T20:17:09.247741669Z",
            "created_by":"/bin/sh -c sed -i -E 's,listen       80;,listen       80;\n    listen  [::]:80;,'         /etc/nginx/conf.d/default.conf"
        },
        {
            "created":"2020-05-15T20:17:09.467229164Z",
            "created_by":"/bin/sh -c #(nop)  EXPOSE 80",
            "empty_layer":true
        },
        {
            "created":"2020-05-15T20:17:09.664908001Z",
            "created_by":"/bin/sh -c #(nop)  STOPSIGNAL SIGTERM",
            "empty_layer":true
        },
        {
            "created":"2020-05-15T20:17:09.924710991Z",
            "created_by":"/bin/sh -c #(nop)  CMD ["nginx" "-g" "daemon off;"]",
            "empty_layer":true
        },
        {
            "created":"2020-05-21T14:40:57.129661387Z",
            "created_by":"/bin/sh -c apt-get update     && apt-get install -y iputils-ping telnet procps net-tools curl --no-install-recommends     && rm -rf /var/lib/apt/lists/*     && echo "Asia/Shanghai" > /etc/timezone     && rm -f /etc/localtime     && dpkg-reconfigure -f noninteractive tzdata"
        }
    ],
    "os":"linux",
    "rootfs":{
        "type":"layers",
        "diff_ids":[
            "sha256:ffc9b21953f4cd7956cdf532a5db04ff0a2daa7475ad796f1bad58cfbaf77a07",
            "sha256:91776dace4ca09ff28569edf489cfd6a9fcc0c1d31f1090042943df5c5f4abf7",
            "sha256:3e1e3bb78a57c93c44970432ca5fbaab10036e19a78094bd1e7a09a568065566",
            "sha256:3c445cf708a5cd6b5f5d9837c9b9593604914b179799840da626fc1d75f99eca",
            "sha256:9a8d8a3a4baa4acc60af50168b1e2896b57fca113c41503e88f822c2ed19dd86"
        ]
    }
}"""
        val manifestByte = manifest.toByteArray()
        val configByte = configFile.toByteArray()
        val dockerRepo = "nginx"
        val tag = "latest"
        val digest = DockerDigest("sha256:18f43d8df2d31b43bb904aa3c1ae463ca03855f34700ea85bbf17b4b090a9b75")
        val manifestMetaData =
            ManifestSchema2Deserializer.deserialize(manifestByte, configByte, dockerRepo, tag, digest)
        Assertions.assertNotEquals(manifestMetaData.blobsInfo.size, 0)
        Assertions.assertNotEquals(manifestMetaData.tagInfo.totalSize, 0)
        Assertions.assertEquals(manifestMetaData.tagInfo.labels.size(), 0)
    }
}
