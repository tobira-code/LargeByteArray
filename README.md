# LargeByteArray
LargeByteArray treats Large File like ByteArray

# Sample

```kotlin
package com.tobira.code.liblargebytearray

import java.io.RandomAccessFile
import java.nio.file.Files
import java.nio.file.Path

fun main() {
    fun generateFile(fileName: String, fileSize: Long) {
        RandomAccessFile(fileName, "rw").use { file ->
            file.setLength(fileSize)
            file.seek(0)
            file.write(1)
            file.seek(fileSize - 1)
            file.write(2)
        }
    }

    "over4GiBFile.bin".also { fileName ->
        // 2^32 = 4,294,967,296
        val fileSize = 4294967296L + 1L
        generateFile(fileName = fileName, fileSize = fileSize)
        LargeByteArrayImpl.of(fileName = fileName)?.use { byteArray ->
            fun println(index: Long) {
                println("byteArray[$index]=${byteArray[index]} hasError=${byteArray.hasError}")
            }
            println(0L)
            println(1L)
            println(4294967295L)
            println(4294967296L)
            println(4294967297L)
            /**
             * byteArray[0]=1 hasError=false
             * byteArray[1]=0 hasError=false
             * byteArray[4294967295]=0 hasError=false
             * byteArray[4294967296]=2 hasError=false
             * byteArray[4294967297]=0 hasError=true
             */
        } ?: println("error")
        Files.delete(Path.of(fileName))
    }
}
```
