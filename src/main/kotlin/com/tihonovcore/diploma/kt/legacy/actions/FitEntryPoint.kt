/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.tihonovcore.diploma.kt.legacy.actions

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.checkers.utils.TypedNode
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diploma.*
import com.tihonovcore.diploma.kt.legacy.cache.attempts
import com.tihonovcore.diploma.kt.legacy.cache.load
import com.tihonovcore.diploma.kt.legacy.cache.save
import com.tihonovcore.diploma.kt.legacy.json
import com.tihonovcore.diploma.kt.legacy.paths.AfterLastException
import com.tihonovcore.diploma.kt.legacy.paths.createSampleForFit
import com.tihonovcore.diploma.kt.legacy.paths.createSampleForPredict
import com.tihonovcore.diploma.kt.legacy.psi.Kind2Psi
import com.tihonovcore.diploma.kt.legacy.toIntegerSample
import com.tihonovcore.diploma.kt.legacy.toVirtualFile
import com.tihonovcore.diploma.kt.legacy.types.ExtractedTypes
import com.tihonovcore.diploma.kt.legacy.types.convertToJson
import com.tihonovcore.diploma.kt.legacy.types.extractTypes
import com.tihonovcore.diploma.kt.legacy.types.extractTypesSkipErrors
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.children
import java.io.File
import java.nio.file.Files

/**
 * @return integer dataset sample and types info at json
 */
fun extractPathsFrom(path: String, project: Project): Pair<String, String> {
    val file = File(path)

    val ktFile = PsiManager.getInstance(project).findFile(file.toVirtualFile()!!) as KtFile
    val typesFromFile = extractTypes(ktFile)
    val (typedNodes, hasCompileErrors) = checkFileSkipErrors(ktFile)

    /* @suggest: set samples count to 1 */
    val (stringSample, from) = createSampleForFit(ktFile, getMapPsiToTypeId(typesFromFile.class2spec, typedNodes), 5..25, 25)
    val integerSample = stringSample.toIntegerSample()

    val notFinished = mutableListOf(from.parent)
    while (notFinished.last() !== ktFile) {
        notFinished += notFinished.last().parent
    }

    save(ktFile, from, notFinished)
    attempts(new = 0)

    Files.createTempFile(
        java.nio.file.Paths.get("/home/tihonovcore/diploma/kotlin/idea/tests/org/jetbrains/kotlin/diploma/out/code"),
        "save" + System.currentTimeMillis().toString(),
        ".txt"
    ).toFile().writeText(ktFile.text)

    return Pair(integerSample.json(), typesFromFile.convertToJson())
}

private const val MAX_ATTEMPTS = 3

fun workWithPrediction(kindsAndTypes: List<Pair<String, Int>>, project: Project): KotlinResponse {
    val attempts = attempts()
    val (file, notFinished, predictedTypes) = load(project)
    //TODO: saving and loading `typedNodes` is good practice, because
    // analysing some AST is impossible (e.g. binary_expression without operation node)
    // In this case `checkFile` says, that there are no one typed node
    val nodeForChildAddition = findNodeForChildAddition(file, notFinished) as KtElement

    val kind2Psi = Kind2Psi(project)
    fun PsiElement.addSubtree(kindsAndTypes: Iterator<Pair<String, Int>>) {
        while (true) {
            if (!kindsAndTypes.hasNext()) {
                assert(this === nodeForChildAddition)
                return
            }

            val kind = kindsAndTypes.next().first

            val decodedChild = try {
                kind2Psi.decode(kind)
            } catch (_: AfterLastException) {
                notFinished.removeIf { it === this }
                return
            }

            val appended = (this as KtElement).append(decodedChild)
            appended.apply { children.forEach { it.delete() } }
            notFinished += appended

            appended.addSubtree(kindsAndTypes)
        }
    }
    nodeForChildAddition.addSubtree(kindsAndTypes.iterator())

    val (typedNodes, _) = checkFileSkipErrors(file)
    val typesFromFile = extractTypesSkipErrors(file)
    fun PsiElement.updateNames(kindsAndTypes: Iterator<Pair<String, Int>>) {
        val type = kindsAndTypes.next().second

        val predictedType = typesFromFile.class2spec.entries.find { it.value.id == type }?.key
        if (this is KtNameReferenceExpression) {
            val oldIdentifier = node.children().find { it.elementType === KtTokens.IDENTIFIER }!!
            val newIdentifier = when (this.parent) {
                is KtCallExpression -> typesFromFile.functionDescriptors.shuffled().firstOrNull()?.name ?: "no_found_call"
                is KtUserType -> typesFromFile.class2spec.values.shuffled().firstOrNull()?.name ?: "no_found_type"
                else -> findVisibleProperties(file, this, typedNodes, predictedType).shuffled().firstOrNull()?.name ?: "no_found_var"
            }

            node.replaceChild(oldIdentifier, LeafPsiElement(KtTokens.IDENTIFIER, newIdentifier.toString()))
        }

        predictedTypes[this] = type

        for (child in children) {
            child.updateNames(kindsAndTypes)
        }

        //Skip AFTER_LAST
        kindsAndTypes.next()
    }

    if (kindsAndTypes.first().first != "AFTER_LAST") {
        // iterator is empty, if only AL predicted
        nodeForChildAddition.children.last().updateNames(kindsAndTypes.iterator())
    }

    Files.createTempFile(
        java.nio.file.Paths.get("/home/tihonovcore/diploma/kotlin/idea/tests/org/jetbrains/kotlin/diploma/out/code"),
        "onPredict" + System.currentTimeMillis().toString(),
        ".txt"
    ).toFile().writeText(file.text)

    val (typedNodesAfterRenaming, hasCompileErrorsAfterRenaming) = checkFileSkipErrors(file)
    val typesFromFileAfterRenaming = extractTypesSkipErrors(file) // probably it gives nothing, we may use old extracted types

    return when {
        !hasCompileErrorsAfterRenaming -> {
            Success(comparePredictedAndRealTypes(file, typedNodesAfterRenaming, typesFromFileAfterRenaming, predictedTypes))
        }
        /* @doubt */
        // 1. `notFinished.any { it === nodeForChildAddition }` is probably stange condition, now we want to finish subtree completely
        // 2. `attempts < MAX_ATTEMPTS` probably doesn't work
        attempts < MAX_ATTEMPTS && notFinished.any { it === nodeForChildAddition } -> {
            attempts(new = attempts + 1)
            save(file, notFinished = notFinished, predictedTypes = predictedTypes)
            extractPaths(file, notFinished, typedNodesAfterRenaming, typesFromFileAfterRenaming)
        }
        else -> Fail(comparePredictedAndRealTypes(file, typedNodesAfterRenaming, typesFromFileAfterRenaming, predictedTypes))
    }
}

private fun comparePredictedAndRealTypes(
    element: PsiElement,
    typedNodes: List<TypedNode>,
    typesFromFile: ExtractedTypes,
    predictedTypes: Map<PsiElement, Int>
): List<Boolean> {
    val answerForChildren = element.children.flatMap { comparePredictedAndRealTypes(it, typedNodes, typesFromFile, predictedTypes) }

    if (element !in predictedTypes) {
        return answerForChildren
    }

    val typedNode = typedNodes.find { it.node === element }
    val realType = typedNode?.type

    /* @doubt: what if both of them are nulls? */
    val realTypeId = typesFromFile.class2spec[realType]?.id
    val actualTypeId = predictedTypes[element]

    return listOf(realTypeId == actualTypeId) + answerForChildren
}

private fun findNodeForChildAddition(element: PsiElement, notFinished: List<PsiElement>): PsiElement {
    for (child in element.children) {
        if (notFinished.any { child === it }) {
            return findNodeForChildAddition(child, notFinished)
        }
    }

    return element
}

private fun extractPaths(
    file: KtFile,
    notFinished: List<PsiElement>,
    typedNodes: List<TypedNode>,
    typesFromFile: ExtractedTypes
): Paths {
    val stringSample = createSampleForPredict(
        file,
        findNodeForChildAddition(file, notFinished),
        notFinished.filterIsInstance(KtElement::class.java),
        getMapPsiToTypeId(typesFromFile.class2spec, typedNodes)
    )
    println(stringSample.rootPath)
    val integerSample = stringSample.toIntegerSample()
    return Paths(integerSample.json(), typesFromFile.convertToJson())
}

sealed class KotlinResponse
data class Success(val typeComparison: List<Boolean>) : KotlinResponse()
data class Fail(val typeComparison: List<Boolean>) : KotlinResponse()
class Paths(val integerDatasetJson: String, val typesInfoJson: String) : KotlinResponse()

//ReferenceVariantsProvider.instance.getAvailableReferences(from)!!
private fun findVisibleProperties(
    file: KtFile,
    appended: PsiElement,
    typedNodes: List<TypedNode>,
    predictedType: ClassifierDescriptor?
): List<VariableDescriptor> {
    var current = appended
    while (current !== file) {
        val typedChild = typedNodes.find { it.node === appended }
        if (typedChild == null || typedChild.context.isEmpty()) {
            current = current.parent
            continue
        }

        val allProperties = typedChild.context.map { it as VariableDescriptor }
        val propertiesWithSuitableType = allProperties.filter {
            val descriptor = it.type.constructor.declarationDescriptor
            descriptor != null && descriptor === predictedType
        }
        return propertiesWithSuitableType.ifEmpty { allProperties }
    }

    return emptyList()
}
