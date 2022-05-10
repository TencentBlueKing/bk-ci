package com.tencent.bkrepo.common.artifact.resolve.file.chunk

import java.io.OutputStream

class ChunkedFileOutputStream(val file: ChunkedArtifactFile) : OutputStream() {
    override fun write(b: ByteArray) {
        file.write(b, 0, b.size)
    }

    override fun write(b: ByteArray, off: Int, len: Int) {
        file.write(b, off, len)
    }

    override fun close() {
        file.close()
    }

    override fun write(b: Int) {
        file.write(b)
    }
}
