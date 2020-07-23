package com.github.arthas-choi.intellijbitbucket.services

import com.intellij.openapi.project.Project
import com.github.arthas-choi.intellijbitbucket.MyBundle

class MyProjectService(project: Project) {

    init {
        println(MyBundle.message("projectService", project.name))
    }
}
