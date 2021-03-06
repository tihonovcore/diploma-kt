/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.tihonovcore.diploma.kt.legacy.paths

interface DatasetSample<T> {
    val leafPaths: List<List<T>>
    val rootPath: List<T>
    val typesForLeafPaths: List<Map<Int, Int>>
    val typesForRootPath: Map<Int, Int>
    val leftBrothers: List<T>
    val indexAmongBrothers: Int
    val target: T?
}

data class StringDatasetSample(
    override val leafPaths: List<List<String>>,
    override val rootPath: List<String>,
    override val typesForLeafPaths: List<Map<Int, Int>> = emptyList(),
    override val typesForRootPath: Map<Int, Int> = emptyMap(),
    override val leftBrothers: List<String>,
    override val indexAmongBrothers: Int,
    override val target: String? = null
) : DatasetSample<String>

data class IntegerDatasetSample(
    override val leafPaths: List<List<Int>>,
    override val rootPath: List<Int>,
    override val typesForLeafPaths: List<Map<Int, Int>> = emptyList(),
    override val typesForRootPath: Map<Int, Int> = emptyMap(),
    override val leftBrothers: List<Int>,
    override val indexAmongBrothers: Int,
    override val target: Int? = null
) : DatasetSample<Int>
