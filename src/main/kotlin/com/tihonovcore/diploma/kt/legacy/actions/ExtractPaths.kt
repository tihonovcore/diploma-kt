/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.tihonovcore.diploma.kt.legacy.actions

import com.google.gson.JsonElement
import com.intellij.openapi.project.Project
import com.tihonovcore.diploma.kt.legacy.DiplomaConfiguration
import com.tihonovcore.diploma.kt.legacy.stackTraceToString
import java.io.File

fun extractPaths(project: Project, requestValue: JsonElement): Unit = with(DiplomaConfiguration) {
    try {
        File(cachedTypeIds).writeText("{ \"freeid\" : 0, \"ids\": {} }")
        File(types).writeText("{ \"classes\" : [], \"functions\": [] }")

        val file = File(requestValue.asString)
        val (pathsJson, typesJson) = extractPathsFrom(file.path, project)

        File(answer).writeText("PATH")
        File(paths).writeText(pathsJson)
        File(types).writeText(typesJson)
    } catch (e: Throwable) {
        File(answer).writeText("ERROR: ${e.stackTraceToString()} \n ${e.cause} \n ${e.message}")
    }

    /* @outdated: used to prevent caching result by gradle */
    throw Exception("OK")
}
