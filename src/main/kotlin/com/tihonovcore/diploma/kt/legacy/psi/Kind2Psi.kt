/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.tihonovcore.diploma.kt.legacy.psi

import com.intellij.openapi.project.Project
import com.tihonovcore.diploma.kt.legacy.paths.AFTER_LAST_KIND
import com.tihonovcore.diploma.kt.legacy.paths.AfterLastException
import com.tihonovcore.diploma.kt.legacy.DiplomaConfiguration
import com.tihonovcore.diploma.kt.legacy.kind
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtPsiFactory
import java.io.File

class Kind2Psi(project: Project) {
    private val template = File(DiplomaConfiguration.kind2psiTemplate).readText()
    private val factory = KtPsiFactory(project)
    private val file = factory.createFile(template)

    fun decode(predictedNodeKind: String): KtElement {
        return when (predictedNodeKind) {
            AFTER_LAST_KIND -> throw AfterLastException
            "BOX_TEMPLATE" -> {
                val emptyFile = factory.createPhysicalFile("context", "")
                factory.createAnalyzableFile("from_kind_2_psi", "fun box() {}", emptyFile).also {
                    it.children.first().delete(); it.children.first().delete()
                }
            }
            "FILE" -> {
                val emptyFile = factory.createPhysicalFile("context.kt", "")
                factory.createAnalyzableFile("from_kind_2_psi.kt", "", emptyFile)
            }
            else -> {
                val element = file.findKtElementByKind(predictedNodeKind) ?: throw IllegalArgumentException("<!! expected $predictedNodeKind !!>")
                val copy = element.copy() as KtElement
                copy
            }
        }
    }

    private fun KtElement.findKtElementByKind(expectedKind: String): KtElement? {
        if (kind() == expectedKind) {
            return this
        }

        return children.filterIsInstance(KtElement::class.java).mapNotNull { it.findKtElementByKind(expectedKind) }.firstOrNull()
    }
}
