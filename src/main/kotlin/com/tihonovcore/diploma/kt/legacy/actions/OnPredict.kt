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

fun onPredict(project: Project, requestValue: JsonElement): Unit = with(DiplomaConfiguration) {
    try {
        val kindAndTypes = requestValue.asJsonArray.map { it.asJsonObject }.map { it["kind"].asString to it["type"].asInt }

        val answerFile = File(answer)
        when (val result = workWithPrediction(kindAndTypes, project)) {
            is Paths -> {
                answerFile.writeText("PATH")
                File(paths).writeText(result.integerDatasetJson)
                File(types).writeText(result.typesInfoJson)
            }
            is Success -> {
                answerFile.writeText("SUCC")
                File(compareTypes).writeText(result.typeComparison.joinToString("\n"))
            }
            is Fail -> {
                answerFile.writeText("FAIL")
                File(compareTypes).writeText(result.typeComparison.joinToString("\n"))
            }
        }
    } catch (e: Throwable) {
        File(answer).writeText("ERROR: ${e.stackTraceToString()} \n ${e.cause} \n ${e.message}")
    }

    /* @outdated: used to prevent caching result by gradle */
    throw Exception("OK")
}
