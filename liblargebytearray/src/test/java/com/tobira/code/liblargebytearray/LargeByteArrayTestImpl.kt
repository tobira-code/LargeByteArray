package com.tobira.code.liblargebytearray

import io.kotlintest.matchers.shouldBe
import org.junit.Ignore
import org.junit.Test
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.CRC32
import kotlin.random.Random

class LargeByteArrayTestImpl {
    private fun createAndDoActionAndDeleteRandomAccessFile(
        content: ByteArray,
        cacheSize: Int,
        // contentをrepeatCount回数繰り返してRandomAccessFileを作成する
        // RandomAccessFileのサイズは (content.size * repeatCount) バイトになる
        // 4GiB Bytesを超えるFileを作成する際に単一のByteArrayを確保するとメモリ不足になるためrepeatCountが必要になった
        repeatCount: Int = 1,
        action: (LargeByteArray) -> Unit,
    ) {
        val fileName = RANDOM_ACCESS_FILE_PREFIX
        FileOutputStream(fileName).use { outputStream ->
            repeat(times = repeatCount) { outputStream.write(content) }
        }
        LargeByteArrayImpl.create(
            fileName = fileName,
            property = LargeByteArrayImpl.Property(cacheSize = cacheSize.toLong())
        )?.use { largeByteArray ->
            action(largeByteArray)
        } ?: run {
            assert(value = false)
        }
        Files.delete(Path.of(fileName))
    }

    private fun generateRandomByteArray(size: Int): ByteArray {
        return ByteArray(size).also { byteArray ->
            Random(seed = RANDOM_SEED).also { random ->
                random.nextBytes(byteArray)
            }
        }
    }

    @Test
    fun test01_get_index() {
        createAndDoActionAndDeleteRandomAccessFile(
            content = byteArrayOf(1, 2, 3),
            cacheSize = 2
        ) { largeByteArray ->
            largeByteArray.size shouldBe 3L
            largeByteArray[0] shouldBe 1.toByte()
            largeByteArray[1] shouldBe 2.toByte()
            largeByteArray[2] shouldBe 3.toByte()
            largeByteArray[0] shouldBe 1.toByte()
            largeByteArray.hasError shouldBe false
            largeByteArray[3] shouldBe 0.toByte()
            largeByteArray.hasError shouldBe true
        }
    }

    @Test
    fun test02_get_index() {
        createAndDoActionAndDeleteRandomAccessFile(
            content = byteArrayOf(1, 2),
            cacheSize = 2
        ) { largeByteArray ->
            largeByteArray.size shouldBe 2L
            largeByteArray[0] shouldBe 1.toByte()
            largeByteArray[1] shouldBe 2.toByte()
            largeByteArray.hasError shouldBe false
            largeByteArray[2] shouldBe 0.toByte()
            largeByteArray.hasError shouldBe true
        }
    }

    @Test
    fun test03_get_index() {
        createAndDoActionAndDeleteRandomAccessFile(
            content = byteArrayOf(1, 2),
            cacheSize = 3
        ) { largeByteArray ->
            largeByteArray.size shouldBe 2L
            largeByteArray[0] shouldBe 1.toByte()
            largeByteArray[1] shouldBe 2.toByte()
            largeByteArray.hasError shouldBe false
            largeByteArray[2] shouldBe 0.toByte()
            largeByteArray.hasError shouldBe true
        }
    }

    @Test
    fun test04_get_range() {
        createAndDoActionAndDeleteRandomAccessFile(
            content = byteArrayOf(1, 2, 3, 4, 5),
            cacheSize = 2
        ) { largeByteArray ->
            largeByteArray.size shouldBe 5L
            largeByteArray[0..1L] shouldBe byteArrayOf(1, 2)
            largeByteArray[2..4L] shouldBe byteArrayOf(3, 4, 5)
            largeByteArray[0..1L] shouldBe byteArrayOf(1, 2)
            largeByteArray.hasError shouldBe false
            largeByteArray[5..5L] shouldBe byteArrayOf()
            largeByteArray.hasError shouldBe true
        }
    }

    @Ignore(value = "take a long time")
    @Test
    fun test05_get_index_random() {
        val repeatCount = 1024 * 4 + 16 // repeatCount * content.size = 4GiB Bytes + alpha
        val content = generateRandomByteArray(size = 1024 * 1024)
        createAndDoActionAndDeleteRandomAccessFile(
            content = content,
            cacheSize = 1024 * 1024 * 128,
            repeatCount = repeatCount
        ) { largeByteArray ->
            largeByteArray.size shouldBe (content.size.toLong() * repeatCount.toLong())
            ByteArray(content.size).also { readByteArray ->
                var idx = 0L
                val crcOfLargeByteArray = CRC32()
                repeat(times = repeatCount) {
                    repeat(times = content.size) { index ->
                        readByteArray[index] = largeByteArray[idx]
                        idx++
                    }
                    crcOfLargeByteArray.update(readByteArray)
                }
                crcOfLargeByteArray.value shouldBe getCrcValue(content, repeatCount)
            }
            largeByteArray.hasError shouldBe false
        }
    }

    @Ignore(value = "take a long time")
    @Test
    fun test06_get_range_random() {
        val repeatCount = 1024 + 16 // repeatCount * content.size = 4GiB Bytes + alpha
        val rangeWidth = 4
        val content = generateRandomByteArray(size = rangeWidth * 1024 * 1024)
        createAndDoActionAndDeleteRandomAccessFile(
            content = content,
            cacheSize = 1024 * 1024 * 128,
            repeatCount = repeatCount
        ) { largeByteArray ->
            largeByteArray.size shouldBe (content.size.toLong() * repeatCount.toLong())
            ByteArray(content.size).also { readByteArray ->
                var idx = 0L
                val crcOfLargeByteArray = CRC32()
                repeat(times = repeatCount) {
                    repeat(times = content.size / rangeWidth) { index ->
                        largeByteArray[idx until idx + rangeWidth].copyInto(
                            destination = readByteArray,
                            destinationOffset = index * rangeWidth,
                            startIndex = 0, endIndex = rangeWidth
                        )
                        idx += rangeWidth
                    }
                    crcOfLargeByteArray.update(readByteArray)
                }
                crcOfLargeByteArray.value shouldBe getCrcValue(content, repeatCount)
            }
            largeByteArray.hasError shouldBe false
        }
    }

    companion object {
        private const val RANDOM_SEED = 0
        private const val RANDOM_ACCESS_FILE_PREFIX = "LargeByteArrayTestImpl"

        private fun getCrcValue(byteArray: ByteArray, repeatCount: Int = 1): Long {
            return CRC32().let { crc ->
                repeat(times = repeatCount) { crc.update(byteArray) }
                crc.value
            }
        }

        private fun equalsError(expected: Any?, actual: Any?) =
            AssertionError(equalsErrorMessage(expected, actual))

        private fun equalsErrorMessage(expected: Any?, actual: Any?) =
            "expected: $expected but was: $actual"

        private infix fun ByteArray.shouldBe(other: ByteArray) {
            val expected = other.toList()
            val actual = this.toList()
            if (actual != expected)
                throw equalsError(expected, actual)
        }
    }
}
