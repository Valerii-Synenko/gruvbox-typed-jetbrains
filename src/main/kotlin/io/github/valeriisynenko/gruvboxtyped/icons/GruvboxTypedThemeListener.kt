package io.github.valeriisynenko.gruvboxtyped.icons

import com.intellij.ide.projectView.ProjectView
import com.intellij.ide.ui.LafManager
import com.intellij.ide.ui.LafManagerListener
import com.intellij.openapi.project.ProjectManager

/** File icons depend on the active UI theme, so the project trees are refreshed when it changes. */
class GruvboxTypedThemeListener : LafManagerListener {

    override fun lookAndFeelChanged(source: LafManager) {
        for (project in ProjectManager.getInstance().openProjects) {
            if (!project.isDisposed) ProjectView.getInstance(project).refresh()
        }
    }
}
