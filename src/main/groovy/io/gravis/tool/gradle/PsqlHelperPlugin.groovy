package io.gravis.tool.gradle

import groovy.transform.CompileStatic
import io.gravis.tool.gradle.tasks.StartPsql
import io.gravis.tool.gradle.tasks.StopPsql
import org.gradle.api.Plugin
import org.gradle.api.Project

@CompileStatic
class PsqlHelperPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {

        project.getPluginManager().apply('com.bmuschko.docker-remote-api')
        project.tasks.create('startPsql', StartPsql)
        project.tasks.create('stopPsql', StopPsql)
        project.tasks.getByName('clean').dependsOn(
                project.tasks.create('cleanOldPsqlContainer', StopPsql)
        )
    }
}
