/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.tihonovcore.diploma.kt.legacy.psi

internal sealed class SparseDataStrategy {
    abstract fun process(data: () -> String): String
}

internal object NoProcess : SparseDataStrategy() {
    override fun process(data: () -> String): String = data()
}

internal object Embeds : SparseDataStrategy() {
    override fun process(data: () -> String): String = TODO("implement embedding")
}

internal class MarkWith(val marker: String) : SparseDataStrategy() {
    override fun process(data: () -> String): String = marker + data()
}

internal object Drop : SparseDataStrategy() {
    override fun process(data: () -> String): String = ""
}
