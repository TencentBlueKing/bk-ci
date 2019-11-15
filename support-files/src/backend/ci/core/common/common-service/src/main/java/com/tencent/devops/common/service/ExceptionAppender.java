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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.common.service;

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.io.Serializable;

@Plugin(name = ExceptionAppender.PLUGIN_NAME, category = "Core", elementType = Appender.ELEMENT_TYPE, printObject = true)
public class ExceptionAppender extends AbstractAppender {

    static final String PLUGIN_NAME = "Exception";

    private ExceptionAppender(String name, Filter filter, Layout<? extends Serializable> layout) {
        super(name, filter, layout);
    }

    @PluginFactory
    public static ExceptionAppender createAppender(@PluginAttribute("name") String name,
                                                   @PluginElement("Filter") final Filter filter,
                                                   @PluginElement("Layout") Layout<? extends Serializable> layout) {
        if (name == null) {
            LOGGER.error("ExceptionAppender No name provided.");
            return null;
        }
        if (layout == null) {
            layout = PatternLayout.createDefaultLayout();
        }
        return new ExceptionAppender(name, filter, layout);
    }

    @Override
    public void append(LogEvent event) {
        if (event.getThrown() != null) {
            // to do something
        }
    }

    @Override
    public void stop() {
        super.stop();
    }
}
