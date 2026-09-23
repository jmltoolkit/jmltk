package io.github.jmltoolkit.buildhelpers.kdoc

import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile

class SpecialDocsProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        val target = environment.options["specialInterface"] ?: "com.example.MySpecialInterface"
        val resourceFolder = environment.options["output"] ?: "build/resources/META-INF/docs"
        return SpecialDocsProcessor(environment.codeGenerator, environment.logger, target)
    }
}

class SpecialDocsProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val targetInterface: String
) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        resolver.getAllFiles().flatMap { file ->
            file.declarations
                .filterIsInstance<KSClassDeclaration>()
                .filter { implementsTarget(it) }
                .mapNotNull { classDecl ->
                    val fileName = classDecl.qualifiedName?.asString()
                    logger.info("Found documentation $fileName")
                    val str = classDecl.docString?.trim()
                    if (fileName == null || str == null) null
                    else fileName to (str to classDecl.containingFile)
                }
        }.toMap()
            .forEach { n, (s,f) -> store(n, s, f) }

        return emptyList()
    }

    private fun store(fileName: String, str: String, f: KSFile?) {
        try {
            codeGenerator.createNewFile(
                dependencies = Dependencies(aggregating = true, f!!),
                packageName = "META-INF/docs/",
                fileName = fileName,
                extensionName = "md"
            ).writer().use {
                it.write(str)
            }
        }catch (e: FileAlreadyExistsException) {
        }
    }

    private fun implementsTarget(classDecl: KSClassDeclaration): Boolean {
        return classDecl.getAllSuperTypes().any { superType ->
            superType.declaration.qualifiedName?.asString() == targetInterface
        }
    }
}