package io.embrace.android.intellij.plugin.actions

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener


class MyProjectManagerListener : ProjectManagerListener {

    override fun projectOpened(project: Project) {
        super.projectOpened(project)
    }

    override fun canCloseProject(project: Project): Boolean {
        return super.canCloseProject(project)
    }

    override fun projectClosed(project: Project) {
        super.projectClosed(project)
    }

    override fun projectClosing(project: Project) {
        super.projectClosing(project)
    }

    override fun projectClosingBeforeSave(project: Project) {
        super.projectClosingBeforeSave(project)
    }
}