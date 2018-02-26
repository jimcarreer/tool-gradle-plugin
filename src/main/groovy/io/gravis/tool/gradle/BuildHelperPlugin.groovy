package io.gravis.tool.gradle

import groovy.transform.CompileStatic
import io.gravis.tool.gradle.util.GitTools
import org.gradle.api.Plugin
import org.gradle.api.Project
import io.gravis.tool.gradle.tasks.BuildPropertyFile

@CompileStatic
class BuildHelperPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {

        project.extensions.add('branchBasedVersion', GitTools.&branchBasedVersion)
        project.tasks.getByName('build').dependsOn(
                project.tasks.create('createPropertyFile', BuildPropertyFile)
        )
    }
}
