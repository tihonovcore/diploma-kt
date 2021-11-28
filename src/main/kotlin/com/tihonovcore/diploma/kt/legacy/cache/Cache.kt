/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.tihonovcore.diploma.kt.legacy.cache

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.tree.IElementType
import org.jetbrains.annotations.NotNull
import com.tihonovcore.diploma.kt.legacy.DiplomaConfiguration
import com.tihonovcore.diploma.kt.legacy.psi.Kind2Psi
import com.tihonovcore.diploma.kt.legacy.json
import com.tihonovcore.diploma.kt.legacy.kind
import org.jetbrains.kotlin.lexer.*
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.allChildren
import org.jetbrains.kotlin.psi.psiUtil.children
import java.io.File

private val ktTokensInstance = object : KtTokens {}

fun attempts(): Int {
    return JsonParser.parseString(File(DiplomaConfiguration.attempts).readText()).asJsonObject["attempts"].asInt
}

fun attempts(new: Int) {
    File(DiplomaConfiguration.attempts).writeText("{ \"attempts\": $new }")
}

fun save(
    file: KtFile,
    except: PsiElement? = null,
    notFinished: List<PsiElement> = emptyList(),
    predictedTypes: Map<PsiElement, Int> = emptyMap()
) {
    val json = file.encode(except, notFinished, predictedTypes).json()
    File(DiplomaConfiguration.ast).writeText(json)
}

data class Loaded(
    val file: KtFile,
    val notFinished: MutableList<PsiElement>,
    val predictedTypes: MutableMap<PsiElement, Int>
)

/**
 * @return decoded file and list of not finished elements
 */
fun load(project: Project): Loaded {
    val json = File(DiplomaConfiguration.ast).readText()

    val tree = Gson().fromJson(json, JsonTree::class.java)
    val factory = KtPsiFactory(project)
    val kind2Psi = Kind2Psi(project)

    val notFinished = mutableListOf<PsiElement>()
    val predictedTypes = mutableMapOf<PsiElement, Int>()
    val file = tree.decode(kind2Psi, factory, notFinished, predictedTypes) as KtFile
    return Loaded(file, notFinished, predictedTypes)
}

private data class JsonTree(
    val kind: String,
    val text: String = "",
    var finished: Boolean = true,
    val predictedType: Int? = null,
    val children: MutableList<JsonTree> = mutableListOf()
)

private fun PsiElement.encode(
    except: PsiElement? = null,
    notFinished: List<PsiElement>,
    predictedTypes: Map<PsiElement, Int>
): JsonTree {
    val tree = JsonTree(kind = kind(), predictedType = predictedTypes[this])
    if (notFinished.any { it === this }) {
        tree.finished = false
    }

    var skipInnerNodes = false
    for (child in node.children()) {
        if (child.psi === except) {
            skipInnerNodes = true
        }

        if (child.psi is LeafPsiElement) {
            val kind = KtTokens::class.java.fields.find { it.get(ktTokensInstance) === child.elementType }!!.name
            val text = child.text

            tree.children += JsonTree(kind, text)
        } else if (!skipInnerNodes) {
            tree.children += child.psi.encode(except, notFinished, predictedTypes)
        }
    }

    return tree
}

private fun JsonTree.decode(
    kind2Psi: Kind2Psi,
    factory: KtPsiFactory,
    notFinished: MutableList<PsiElement>,
    predictedTypes: MutableMap<PsiElement, Int>
): PsiElement {
    val tokenTypeOrNull = KtTokens::class.java.fields.find { it.name == kind }
    val element = if (tokenTypeOrNull != null) {
        LeafPsiElement(tokenTypeOrNull.get(ktTokensInstance) as IElementType, text)
    } else {
        kind2Psi.decode(kind).apply {
            val f = allChildren.first
            val l = allChildren.last
            if (f != null && l != null) deleteChildRange(f, l)
        }
    }

    children.forEach { child ->
        val decodedChild = child.decode(kind2Psi, factory, notFinished, predictedTypes)
        element.node.addChild(decodedChild.node)
    }

    if (!finished) {
        notFinished += element
    }

    if (predictedType != null) {
        predictedTypes[element] = predictedType
    }

    return element
}
