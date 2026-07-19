/* This file is part of jmltoolkit project - https://github.com/jmltoolkit
 * jmltk is licensed under the Lesser GNU Public License Version 2 and Apache License
 * SPDX-License-Identifier: LGPL-3.0-or-later Apache-2.0
 */
package io.github.jmltoolkit.jmlstub

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.Parameter
import com.github.javaparser.ast.type.ClassOrInterfaceType
import javassist.ClassPool
import javassist.CtClass
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.inputStream
import java.lang.reflect.Modifier as JavaModifier

class ClassStubGenerator(private val path: Path) {
    companion object {
        @JvmStatic
        private val classPool: ClassPool by lazy {
            ClassPool.getDefault()
        }
    }

    fun generate(): CompilationUnit {
        require(Files.exists(path)) { "Class file does not exist: $path" }
        require(path.extension == ".class") { "Path must point to a .class file: $path" }

        path.inputStream().use {
            val ctClass = classPool.makeClass(it)
            return createCompilationUnit(ctClass)
        }
    }

    private fun createCompilationUnit(ctClass: CtClass): CompilationUnit {
        return buildSourceFromCtClass(ctClass)
    }

    private fun buildSourceFromCtClass(ctClass: CtClass): CompilationUnit {
        val cu = CompilationUnit()

        val packageName = ctClass.packageName
        if (!packageName.isNullOrBlank()) {
            cu.setPackageDeclaration(packageName)
        }

        ctClass.simpleName

        val type = when {
            ctClass.isInterface -> cu.addInterface(ctClass.simpleName)
            ctClass.isAnnotation -> cu.addAnnotationDeclaration(ctClass.simpleName)
            ctClass.isEnum -> cu.addEnum(ctClass.simpleName)
            else -> cu.addClass(ctClass.simpleName)
        }

        val modifiers = getModifier(ctClass.modifiers)
        type.modifiers.addAll(modifiers.map { Modifier(it) })


        if (type is ClassOrInterfaceDeclaration) {
            if (!type.isInterface) {
                val superClass = ctClass.superclass
                if (superClass != null && superClass.name != "java.lang.Object") {
                    cu.addImport(superClass.name)
                    type.extendedTypes().add(ClassOrInterfaceType(null, superClass.simpleName))
                }

                ctClass.interfaces.forEach {
                    type.implementedTypes().add(ClassOrInterfaceType(null, it.name))
                }
            } else {
                ctClass.interfaces.forEach {
                    type.extendedTypes().add(ClassOrInterfaceType(null, it.name))
                }
            }
        }

        ctClass.declaredFields.forEach { field ->
            val fieldModifiers = getModifier(field.modifiers)
            type.addField(field.type.name, field.name, *fieldModifiers.toTypedArray())
        }

        ctClass.constructors.forEach { constructor ->
            val mods = getModifier(constructor.modifiers)
            val ctor = type.addConstructor(*mods.toTypedArray())
            ctor.setBody(null)
            ctor.parameters().addAll(getParameters(constructor))
        }

        ctClass.declaredMethods
            .filterNot { isClinitOrInit(it.name) }
            .forEach { method ->
                val methodModifiers = getModifier(method.modifiers)
                val met = type.addMethod(method.name, *methodModifiers.toTypedArray())
                met.parameters().addAll(getParameters(method))

                method.exceptionTypes.forEach { exc ->
                    met.addThrownException(ClassOrInterfaceType(null, exc.name))
                }

                met.setBody(null)
            }
        return cu
    }

    private fun getModifier(modifiers: Int): List<Modifier.DefaultKeyword> {
        val mods = mutableListOf<Modifier.DefaultKeyword>()
        if (JavaModifier.isPublic(modifiers)) mods.add(Modifier.DefaultKeyword.PUBLIC)
        if (JavaModifier.isProtected(modifiers)) mods.add(Modifier.DefaultKeyword.PROTECTED)
        if (JavaModifier.isPrivate(modifiers)) mods.add(Modifier.DefaultKeyword.PRIVATE)
        if (JavaModifier.isStatic(modifiers)) mods.add(Modifier.DefaultKeyword.STATIC)
        if (JavaModifier.isAbstract(modifiers)) mods.add(Modifier.DefaultKeyword.ABSTRACT)
        if (JavaModifier.isFinal(modifiers)) mods.add(Modifier.DefaultKeyword.FINAL)
        if (JavaModifier.isSynchronized(modifiers)) mods.add(Modifier.DefaultKeyword.SYNCHRONIZED)
        if (JavaModifier.isNative(modifiers)) mods.add(Modifier.DefaultKeyword.NATIVE)
        if (JavaModifier.isStrict(modifiers)) mods.add(Modifier.DefaultKeyword.STRICTFP)
        if (JavaModifier.isTransient(modifiers)) mods.add(Modifier.DefaultKeyword.TRANSIENT)
        if (JavaModifier.isVolatile(modifiers)) mods.add(Modifier.DefaultKeyword.VOLATILE)
        return mods
    }

    private fun getParameters(behavior: javassist.CtBehavior): List<Parameter> {
        val types = behavior.parameterTypes.map { ClassOrInterfaceType(null, it.name) }
        val methodInfo = behavior.getMethodInfo()
        val table = methodInfo.getCodeAttribute().getAttribute(javassist.bytecode.LocalVariableAttribute.tag)
        val numberOfLocalVariables = table.length()
        val names = (0..numberOfLocalVariables).map {
            methodInfo.getConstPool().getUtf8Info(
                0// TODO table.constPool
            )
        }

        return types.zip(names).map { (t, n) -> Parameter(t, n) }
    }

    private fun isClinitOrInit(name: String): Boolean {
        return name == "<clinit>" || name == "<init>"
    }
}
