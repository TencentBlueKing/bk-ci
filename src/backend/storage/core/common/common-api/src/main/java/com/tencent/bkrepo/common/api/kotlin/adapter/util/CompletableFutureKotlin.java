package com.tencent.bkrepo.common.api.kotlin.adapter.util;

import java.util.concurrent.CompletableFuture;

/**
 * 适配java CompletableFuture
 * 将可变参数转化为数组，避免在kotlin中使用*
 */
public class CompletableFutureKotlin<T> extends CompletableFuture<T> {
    public static CompletableFuture<Void> allOf(CompletableFuture<?>[] cfs) {
        return CompletableFuture.allOf(cfs);
    }
}
