package org.jetbrains.research.testspark.helpers.psi.java

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiAnonymousClass
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiModifier
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ClassInheritorsSearch
import com.intellij.psi.util.PsiTypesUtil
import org.jetbrains.kotlin.j2k.ast.declarationIdentifier
import org.jetbrains.kotlin.j2k.isConstructor
import org.jetbrains.research.testspark.core.data.ClassType
import org.jetbrains.research.testspark.core.utils.importPattern
import org.jetbrains.research.testspark.core.utils.packagePattern
import org.jetbrains.research.testspark.helpers.psi.PsiClassWrapper
import org.jetbrains.research.testspark.helpers.psi.PsiMethodWrapper
import org.jetbrains.research.testspark.core.ProjectUnderTestArtifactsCollector

class JavaPsiClassWrapper(private val psiClass: PsiClass) : PsiClassWrapper {
    override val name: String get() = psiClass.name ?: ""

    override val qualifiedName: String get() = psiClass.qualifiedName ?: ""

    override val text: String get() = psiClass.text

    override val methods: List<PsiMethodWrapper> get() = psiClass.methods.map { JavaPsiMethodWrapper(it) }

    override val allMethods: List<PsiMethodWrapper> get() = psiClass.allMethods.map { JavaPsiMethodWrapper(it) }

    override val superClass: PsiClassWrapper? get() = psiClass.superClass?.let { JavaPsiClassWrapper(it) }

    override val virtualFile: VirtualFile get() = psiClass.containingFile.virtualFile

    override val containingFile: PsiFile get() = psiClass.containingFile

    override val fullText: String
        get() {
            var fullText = ""
            val fileText = psiClass.containingFile.text

            // get package
            packagePattern.findAll(fileText).map {
                it.groupValues[0]
            }.forEach {
                fullText += "$it\n\n"
            }

            // get imports
            importPattern.findAll(fileText).map {
                it.groupValues[0]
            }.forEach {
                fullText += "$it\n"
            }

            // Add class code
            fullText += psiClass.text

            return fullText
        }

    override val classType: ClassType
        get() {
            if (psiClass.isInterface) {
                return ClassType.INTERFACE
            }
            if (psiClass.hasModifierProperty(PsiModifier.ABSTRACT)) {
                return ClassType.ABSTRACT_CLASS
            }
            return ClassType.CLASS
        }

    override fun searchSubclasses(project: Project): Collection<PsiClassWrapper> {
        val scope = GlobalSearchScope.projectScope(project)
        val query = ClassInheritorsSearch.search(psiClass, scope, false)
        return query.findAll().map { JavaPsiClassWrapper(it) }
    }

    override fun getInterestingPsiClassesWithQualifiedNames(
        psiMethod: PsiMethodWrapper,
    ): MutableSet<PsiClassWrapper> {
        val interestingMethods = mutableSetOf(psiMethod as JavaPsiMethodWrapper)
        for (currentPsiMethod in allMethods) {
            if ((currentPsiMethod as JavaPsiMethodWrapper).isConstructor) interestingMethods.add(currentPsiMethod)
        }
        val interestingPsiClasses = mutableSetOf(this)
        interestingMethods.forEach { methodIt ->
            methodIt.parameterList.parameters.forEach { paramIt ->
                PsiTypesUtil.getPsiClass(paramIt.type)?.let { typeIt ->
                    JavaPsiClassWrapper(typeIt).let {
                        if (it.qualifiedName != "" && !it.qualifiedName.startsWith("java.")) {
                            interestingPsiClasses.add(it)
                        }
                    }
                }
            }
        }

        return interestingPsiClasses.toMutableSet()
    }

    /**
     * Checks if the constraints on the selected class are satisfied, so that EvoSuite can generate tests for it.
     * Namely, it is not an enum and not an anonymous inner class.
     *
     * @return true if the constraints are satisfied, false otherwise
     */
    fun isTestableClass(): Boolean {
        return !psiClass.isEnum && psiClass !is PsiAnonymousClass
    }

    override fun declaration(): String {
        val wrapper = this
        var fullText = psiClass.text.trim()

        // remove doc comment
        psiClass.docComment?.let {
            ProjectUnderTestArtifactsCollector.log("Removing doc comment to construct declaration...")
            fullText = fullText.removePrefix(it.text)
        }

        val declaration = fullText.split("\n").firstOrNull { it.contains(wrapper.name) }
            ?: fullText.split("\n").firstOrNull()
            ?: throw IllegalStateException("""
Unable to extract declaration of the PSI class ${wrapper.name}.
The full definition:
${psiClass.text}
            """.trimIndent())

        ProjectUnderTestArtifactsCollector.log("Constructed declaration: `$declaration`")
        return declaration
    }

    /*override fun constructorDeclarations(): List<String> {
        val constructors = mutableListOf<String>()

        println("All constructors: ${psiClass.constructors.size}")

        for (constructor in psiClass.constructors) {
            if (constructor.isConstructor && constructor.isPhysical && constructor.isValid) {
                val declaration = constructor.text.split("\n").first().trim().removeSuffix("{")
                constructors.add(declaration)
            }
        }

        return constructors
    }*/
}
