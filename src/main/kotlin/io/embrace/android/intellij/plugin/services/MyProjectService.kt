package io.embrace.android.intellij.plugin.services

import com.intellij.openapi.project.Project
import io.embrace.android.intellij.plugin.EmbraceStringResources

class MyProjectService(project: Project) {

    init {
        println(EmbraceStringResources.message("projectService", project.name))

        System.getenv("CI")
            ?: TODO("Don't forget to remove all non-needed sample code files with their corresponding registration entries in `plugin.xml`.")
    }

    /**
     * Chosen by fair dice roll, guaranteed to be random.
     */
    fun getRandomNumber() = 4
}
