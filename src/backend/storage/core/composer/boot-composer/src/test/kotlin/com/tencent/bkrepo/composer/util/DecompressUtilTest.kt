/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bkrepo.composer.util

import com.tencent.bkrepo.composer.pojo.ComposerMetadata
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class DecompressUtilTest {

    @Test
    fun jsonTest() {
        val json =
            """
{
    "name": "monolog/monolog",
    "description": "Sends your logs to files, sockets, inboxes, databases and various web services",
    "keywords": ["log", "logging", "psr-3"],
    "homepage": "http://github.com/Seldaek/monolog",
    "type": "library",
    "license": "MIT",
    "authors": [
        {
            "name": "Jordi Boggiano",
            "email": "j.boggiano@seld.be",
            "homepage": "http://seld.be"
        }
    ],
    "version": "2.1.0",
    "require": {
        "php": "^7.2",
        "psr/log": "^1.0.1"
    },
    "require-dev": {
        "aws/aws-sdk-php": "^2.4.9 || ^3.0",
        "doctrine/couchdb": "~1.0@dev",
        "elasticsearch/elasticsearch": "^6.0",
        "graylog2/gelf-php": "^1.4.2",
        "jakub-onderka/php-parallel-lint": "^0.9",
        "php-amqplib/php-amqplib": "~2.4",
        "php-console/php-console": "^3.1.3",
        "phpspec/prophecy": "^1.6.1",
        "phpunit/phpunit": "^8.3",
        "predis/predis": "^1.1",
        "rollbar/rollbar": "^1.3",
        "ruflin/elastica": ">=0.90 <3.0",
        "swiftmailer/swiftmailer": "^5.3|^6.0"
    },
    "suggest": {
        "graylog2/gelf-php": "Allow sending log messages to a GrayLog2 server",
        "doctrine/couchdb": "Allow sending log messages to a CouchDB server",
        "ruflin/elastica": "Allow sending log messages to an Elastic Search server",
        "elasticsearch/elasticsearch": "Allow sending log messages to an Elasticsearch server via official client",
        "php-amqplib/php-amqplib": "Allow sending log messages to an AMQP server using php-amqplib",
        "ext-amqp": "Allow sending log messages to an AMQP server (1.0+ required)",
        "ext-mongodb": "Allow sending log messages to a MongoDB server (via driver)",
        "mongodb/mongodb": "Allow sending log messages to a MongoDB server (via library)",
        "aws/aws-sdk-php": "Allow sending log messages to AWS services like DynamoDB",
        "rollbar/rollbar": "Allow sending log messages to Rollbar",
        "php-console/php-console": "Allow sending log messages to Google Chrome",
        "ext-mbstring": "Allow to work properly with unicode symbols"
    },
    "autoload": {
        "psr-4": {"Monolog\\": "src/Monolog"}
    },
    "autoload-dev": {
        "psr-4": {"Monolog\\": "tests/Monolog"}
    },
    "provide": {
        "psr/log-implementation": "1.0.0"
    },
    "extra": {
        "branch-alias": {
            "dev-master": "2.x-dev"
        }
    },
    "scripts": {
        "test": [
            "parallel-lint . --exclude vendor",
            "phpunit"
        ]
    },
    "config": {
        "sort-packages": true
    }
}
            """.trimIndent()

        val json01 =
            """
{
    "name": "weaving/blog",
    "description": "It is a test project!",
    "type": "library",
    "version": "1.0",
    "license": "MIT",
    "authors": [
        {
            "name": "weaving",
            "email": "onnt1997@outlook.com"
        }
    ],
    "minimum-stability": "dev"
}            
            """.trimIndent()

        println(json)
        println(json01)
        val composerMetadata = JsonUtil.mapper.readValue<ComposerMetadata>(json, ComposerMetadata::class.java)
        val composerMetadata01 = JsonUtil.mapper.readValue<ComposerMetadata>(json01, ComposerMetadata::class.java)
        Assertions.assertNotNull(composerMetadata)
        Assertions.assertNotNull(composerMetadata01)
    }
}
