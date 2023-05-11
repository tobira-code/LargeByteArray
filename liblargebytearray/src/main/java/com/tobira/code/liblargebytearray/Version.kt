package com.tobira.code.liblargebytearray

/**
 * バージョン
 *
 * インタフェースと実装それぞれにバージョンを設ける
 *
 * ```
 * 例. 0.1.0
 * major:互換性のない変更時にインクリメントする、例. 既存インタフェースの変更
 * minor:互換性のある機能追加時にインクリメントする、例. 新規インタフェースの追加
 * patch:不具合修正時に数値をインクリメントする
 * 上記ルールは 1.x.y 以降に適用する、0.x.yは常に0.1.0とする
 * 初回の機能実装完了時に0.1.0を1.0.0にする
 * ```
 */
object Version {
    const val INTERFACE_VERSION = "0.1.0"
    const val IMPLEMENTATION_VERSION = "0.1.0"
}
