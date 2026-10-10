package com.example.data.upload

import okhttp3.MediaType
import okhttp3.RequestBody
import okio.Buffer
import okio.BufferedSink
import okio.ForwardingSink
import okio.Sink
import okio.buffer
import java.io.IOException

/**
 * A custom OkHttp RequestBody that wraps an existing RequestBody to track
 * and emit upload progress (bytes written / total bytes).
 */
class ProgressRequestBody(
  private val delegate: RequestBody,
  private val onProgress: (bytesWritten: Long, totalBytes: Long, percent: Int) -> Unit
) : RequestBody() {

  override fun contentType(): MediaType? = delegate.contentType()

  override fun contentLength(): Long = delegate.contentLength()

  @Throws(IOException::class)
  override fun writeTo(sink: BufferedSink) {
    val totalBytes = contentLength()
    val countingSink = CountingSink(sink, totalBytes, onProgress)
    val bufferedSink = countingSink.buffer()

    delegate.writeTo(bufferedSink)
    bufferedSink.flush()
  }

  private class CountingSink(
    delegate: Sink,
    private val totalBytes: Long,
    private val onProgress: (bytesWritten: Long, totalBytes: Long, percent: Int) -> Unit
  ) : ForwardingSink(delegate) {

    private var bytesWritten = 0L

    @Throws(IOException::class)
    override fun write(source: Buffer, byteCount: Long) {
      super.write(source, byteCount)
      bytesWritten += byteCount
      val percent = if (totalBytes > 0) {
        ((bytesWritten * 100) / totalBytes).toInt().coerceIn(0, 100)
      } else {
        0
      }
      onProgress(bytesWritten, totalBytes, percent)
    }
  }
}
