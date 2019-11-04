package com.tencent.devops.common.cos.util;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.internal.Util;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;

import java.io.IOException;
import java.io.InputStream;

public class InputStreamRequestBody extends RequestBody {
    private final InputStream inputStream;
    private final MediaType mediaType;

    /**
     * Creates the @link {@link RequestBody} from an @link {@link InputStream}
     *
     * @param mediaType the media type
     * @param inputStream the input stream
     * @return the request body
     */
    public static RequestBody create(final MediaType mediaType, final InputStream inputStream) {
        return new InputStreamRequestBody(inputStream, mediaType);
    }

    private InputStreamRequestBody(final InputStream inputStream, final MediaType mediaType) {
        this.inputStream = inputStream;
        this.mediaType = mediaType;
    }

    @Override
    public MediaType contentType() {
        return mediaType;
    }

    @Override
    public void writeTo(BufferedSink sink) throws IOException {
        Source source = null;
        try {
            source = Okio.source(inputStream);
            sink.writeAll(source);
        } finally {
            Util.closeQuietly(source);
        }
    }
}
