package com.tobira.code.liblargebytearray

/**
 * [Int]の範囲を超えたサイズを扱うことができるバイト配列
 */
interface LargeByteArray : AutoCloseable {
    /**
     * 読み取り専用かどうかをあらわす
     *
     * ```
     * true:読み取り専用
     * false:書き込み可能
     * ```
     */
    val readOnly: Boolean

    /**
     * [LargeByteArray]のバイト数
     */
    val size: Long

    /**
     * エラーが発生したかどうかをあらわす
     *
     * ```
     * true:エラーが発生した
     * false:エラーが発生していない
     * エラーが発生した場合は読みだしたデータが正しくない可能性があるため、破棄すること
     * エラーが発生した後にhasError=trueにすることはできないためLargeByteArrayをつくりなおすこと
     * ```
     */
    val hasError: Boolean

    /**
     * [index]で指定した位置にある[Byte]を返す
     *
     * 範囲外にアクセスすると[hasError]=trueとなる
     *
     * @param index : 指定可能な範囲は 0 から size-1
     */
    operator fun get(index: Long): Byte

    /**
     * [range]で指定した範囲にある[ByteArray]をコピーして返す
     *
     * 範囲外にアクセスすると[hasError]=trueとなる
     *
     * @param range : 指定可能な範囲は 0 から size-1
     */
    operator fun get(range: LongRange): ByteArray
}
