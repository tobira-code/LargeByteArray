package com.tobira.code.liblargebytearray

import java.io.File
import java.io.RandomAccessFile
import java.lang.Exception

/**
 * LargeByteArrayの実装クラス
 *
 * ```
 * 内部構造を図示する
 * 　説明：
 *    length : fileのバイト長
 *    file : LargeByteArrayの実体であるRandomAccessFile
 *    file range : 0からe=length-1の範囲
 *    cache : fileの一部をmemoryにcopyしたもの、fileへのアクセスを減らすために利用する
 *    cache range : cacheが対応しているfile rangeを表す
 *
 *                  |<------------- length ----------->|
 * file             --------------|--------|------------
 * file range       0             n        m           e
 * cache                          |--------|
 * cache range                    n ------ m
 * ```
 */
class LargeByteArrayImpl private constructor(
    private val randomAccessFile: RandomAccessFile,
    private val property: Property,
    private val cache: ByteArray,
    private var cacheRange: LongRange,
    private val fileLength: Long
) : LargeByteArray {

    override val readOnly: Boolean = true
    override val size get() = fileLength
    override val hasError get() = _hasError

    /**
     * ユーザが設定可能なパラメータ
     */
    data class Property(
        /**
         * [cache]のバイトサイズ
         */
        val cacheSize: Long
    )

    private var offsetByteOfCache: Long = 0
    private var _hasError = false
    private val _fileRange = LongRange(start = 0, fileLength - 1)

    override fun get(index: Long): Byte {
        return when (index) {
            in cacheRange -> {
                // hit
                cache[index.toInt() - offsetByteOfCache.toInt()]
            }
            in _fileRange -> {
                // miss
                updateCache(index)
                cache[index.toInt() - offsetByteOfCache.toInt()]
            }
            else -> {
                debugPrint(tag = "get(index)", message = "index=$index")
                _hasError = true
                0
            }
        }
    }

    override fun get(range: LongRange): ByteArray {
        val rangeWidth: Long = range.last - range.first + 1
        return when (range) {
            in cacheRange -> {
                // hit
                val fromIndex: Long = range.first - offsetByteOfCache
                val toIndex: Long = fromIndex + rangeWidth
                cache.copyOfRange(fromIndex = fromIndex.toInt(), toIndex = toIndex.toInt())
            }
            in _fileRange -> {
                // miss
                try {
                    randomAccessFile.seek(range.first)
                    ByteArray(rangeWidth.toInt()).also {
                        randomAccessFile.readFully(it)
                        updateCache(index = range.last + 1)
                    }
                } catch (e: Exception) {
                    debugPrint(tag = "get(range)", message = "range=$range")
                    _hasError = true
                    byteArrayOf()
                }
            }
            else -> {
                debugPrint(tag = "get(range)", message = "range=$range")
                _hasError = true
                byteArrayOf()
            }
        }
    }

    /**
     * [RandomAccessFile]をcloseする
     */
    override fun close() {
        randomAccessFile.close()
    }

    private fun updateCache(index: Long) {
        val cacheSize = minOf(size - index, property.cacheSize)
        if (cacheSize <= 0) return
        try {
            randomAccessFile.seek(index)
            randomAccessFile.readFully(cache, 0, cacheSize.toInt())
        } catch (e: Exception) {
            debugPrint(tag = "updateCache", message = "index=$index")
            _hasError = true
        }
        offsetByteOfCache = index
        cacheRange = index until index + cacheSize
    }

    private fun debugPrint(tag: String, message: String) {
        if (DEBUG_ENABLED) {
            print("[D] $tag : $message ")
            print("randomAccessFile=$randomAccessFile ")
            print("size=$size ")
            print("_fileRange=$_fileRange ")
            print("cache=$cache ")
            print("cacheRange=$cacheRange ")
            print("property=$property ")
            println()
        }
    }

    companion object {
        /**
         *  DebugPrintを有効にする
         *
         * @param [enable] : true:DebugPrintを有効にする、false:無効にする(初期値)
         */
        fun enableDebugPrint(enable: Boolean) {
            DEBUG_ENABLED = enable
        }

        /**
         * FileNameを指定して[LargeByteArray]をつくる
         *
         * @param [fileName] : ファイル名
         * @param [property] [LargeByteArray]のプロパティを指定する
         * @return [LargeByteArray]のインスタンス、失敗した場合はnullを返す
         */
        fun of(fileName: String, property: Property = defaultProperty): LargeByteArray? =
            create(fileName = fileName)

        /**
         * FileNameを指定して[LargeByteArray]をつくる
         *
         * @param [fileName] ファイル名
         * @param [property] [LargeByteArray]のプロパティを指定する
         * @return [LargeByteArray]のインスタンス、失敗した場合はnullを返す
         */
        fun create(fileName: String, property: Property = defaultProperty): LargeByteArray? {
            return create(file = File(fileName), property = property)
        }

        /**
         * [File]を指定して[LargeByteArray]をつくる
         *
         * @param [fileName] ファイル名
         * @param [property] [LargeByteArray]のプロパティを指定する
         * @return [LargeByteArray]のインスタンス、失敗した場合はnullを返す
         */
        fun create(file: File, property: Property = defaultProperty): LargeByteArray? {
            return try {
                create(
                    randomAccessFile = RandomAccessFile(file, MODE_READ_ONLY),
                    property = property
                )
            } catch (e: Exception) {
                debugPrint(tag = "create", message = "e=$e")
                null
            }
        }

        /**
         * [RandomAccessFile]を指定して[LargeByteArray]をつくる
         *
         * @param [fileName] ファイル名
         * @param [property] [LargeByteArray]のプロパティを指定する
         * @return [LargeByteArray]のインスタンス、失敗した場合はnullを返す
         */
        fun create(
            randomAccessFile: RandomAccessFile, property: Property = defaultProperty
        ): LargeByteArray? {
            return try {
                val fileLength = randomAccessFile.length()
                val actualCacheSize = minOf(fileLength, property.cacheSize)
                val cache = ByteArray(actualCacheSize.toInt()).also {
                    randomAccessFile.readFully(it)
                }
                LargeByteArrayImpl(
                    randomAccessFile = randomAccessFile,
                    property = property,
                    cache = cache,
                    cacheRange = LongRange(start = 0, endInclusive = actualCacheSize - 1),
                    fileLength = fileLength
                )
            } catch (e: Exception) {
                debugPrint(tag = "create", message = "e=$e")
                null
            }
        }

        private var DEBUG_ENABLED = false
        private const val MODE_READ_ONLY = "r"
        private const val CACHE_SIZE: Long = 1024 * 1024
        private val defaultProperty = Property(cacheSize = CACHE_SIZE)

        private operator fun ClosedRange<Long>.contains(range: LongRange): Boolean {
            return (this.start <= range.first) && (range.last <= this.endInclusive)
        }

        private fun debugPrint(tag: String, message: String) {
            if (DEBUG_ENABLED) {
                print("[D] $tag : $message ")
                println()
            }
        }
    }
}
