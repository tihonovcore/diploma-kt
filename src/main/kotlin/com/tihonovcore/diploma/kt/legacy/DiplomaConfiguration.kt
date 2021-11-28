/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.tihonovcore.diploma.kt.legacy

import java.io.File

// TODO: read from json
object DiplomaConfiguration {
    init {
        println("### DiplomaConfiguration: " + File("").absolutePath)
    }

    private const val root = "/home/tihonovcore/diploma/kotlin"
    private const val diploma = "$root/compiler/cli/src/org/jetbrains/kotlin/diploma"
    private const val out = "$diploma/out"

    const val sourceCodeDirectory = "$root/compiler/testData/codegen/box"
    const val stringDatasetDirectory = "$out/string"
    const val integerDatasetDirectory = "$out/integer"
    const val typesDatasetDirectory = "$out/types"

    const val cachedTypeIds = "$out/cachedTypeIds.json"
    const val request = "$out/request.txt"
    const val answer = "$out/answer.txt"
    const val paths = "$out/paths.json"
    const val types = "$out/types.json"
    const val compareTypes = "$out/compareTypes.txt"

    const val requestJson = "$diploma/actions/request.json"

    const val ast = "$diploma/cache/cacheExample.json"
    const val attempts = "$diploma/cache/attempts.json"

    const val kind2psiTemplate = "$diploma/psi/Kind2PsiTemplate.kt"
}