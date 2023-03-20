/*
 *
 * Lasius - Open source time tracker for teams
 * Copyright (c) Tegonal Genossenschaft (https://tegonal.com)
 *
 * This file is part of Lasius.
 *
 * Lasius is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * Lasius is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Lasius. If not, see <https://www.gnu.org/licenses/>.
 */

package utils

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.util.zip.{GZIPInputStream, GZIPOutputStream}
import scala.annotation.tailrec

trait GzipCompression {

  protected val minimalBytesToCompress = 100
  private final val BufferSize         = 1024 * 4

  protected def compress(bytes: Array[Byte]): Array[Byte] = {
    if (bytes.length < minimalBytesToCompress) {
      bytes
    } else {
      val bos = new ByteArrayOutputStream(BufferSize)
      val zip = new GZIPOutputStream(bos)
      try zip.write(bytes)
      finally zip.close()
      bos.toByteArray
    }
  }

  protected def decompress(bytes: Array[Byte]): Array[Byte] = {
    if (isGZipped(bytes)) {
      val in     = new GZIPInputStream(new ByteArrayInputStream(bytes))
      val out    = new ByteArrayOutputStream()
      val buffer = new Array[Byte](BufferSize)

      @tailrec def readChunk(): Unit = in.read(buffer) match {
        case -1 => ()
        case n =>
          out.write(buffer, 0, n)
          readChunk()
      }

      try readChunk()
      finally in.close()
      out.toByteArray
    } else {
      bytes
    }
  }

  protected def isGZipped(bytes: Array[Byte]): Boolean = {
    (bytes != null) && (bytes.length >= 2) &&
    (bytes(0) == GZIPInputStream.GZIP_MAGIC.toByte) &&
    (bytes(1) == (GZIPInputStream.GZIP_MAGIC >> 8).toByte)
  }
}
