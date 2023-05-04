package com.tobira.code.liblargebytearray

/**
 * [Int]の範囲を超えたサイズを扱うことができるバイト配列
 */
interface LargeByteArray : AutoCloseable {
    /**
     * 読み取り専用かどうかをあらわす
     *
     * true:読み取り専用<br>
     * false:書き込み可能<br>
     */
    val readOnly: Boolean

    /**
     * [LargeByteArray]のバイト数
     */
    val length: Long

    /**
     * エラーが発生したかどうかをあらわす
     *
     * true:エラーが発生した<br>
     * false:エラーが発生していない<br>
     * エラーが発生した場合は読みだしたデータが正しくない可能性があるため、破棄すること<br>
     * エラーが発生した後に[hasError]=trueにすることはできないため[LargeByteArray]をつくりなおすこと<br>
     */
    val hasError: Boolean

    /**
     * [index]で指定した位置にある[Byte]を返す
     *
     * 範囲外にアクセスすると[hasError]=trueとなる
     *
     * @param index : 指定可能な範囲は 0 から length-1
     */
    operator fun get(index: Long): Byte

    /**
     * [range]で指定した範囲にある[ByteArray]をコピーして返す
     *
     * 範囲外にアクセスすると[hasError]=trueとなる
     *
     * @param range : 指定可能な範囲は 0 から length-1
     */
    operator fun get(range: LongRange): ByteArray

    companion object {
        /**
         * [LargeByteArray]のバージョン
         */
        const val VERSION = "0.1.0"
    }
}
