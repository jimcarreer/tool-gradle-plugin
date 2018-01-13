package io.gravis.tool.gradle.tasks

import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import io.gravis.tool.gradle.util.GitTools

import java.text.SimpleDateFormat

@CompileStatic
class BuildPropertyFile extends DefaultTask  {


    final private static String FILE_PATH = 'resources/main/build.properties'
    final private static String DATE_FORMAT = 'yyyy-MM-dd\'T\'HH:mm:ss.SSSZ'

    @OutputFile
    File propertyFile

    BuildPropertyFile() {

        propertyFile = project.file("${project.buildDir}/${FILE_PATH}")
        // This file is always rebuilt
        outputs.upToDateWhen { false }
    }

    @TaskAction
    void run() {

        Date now = new Date()
        Properties buildProperties = new Properties()
        SimpleDateFormat formatter = new SimpleDateFormat(DATE_FORMAT)
        buildProperties.setProperty('gradle', project.gradle.gradleVersion)
        buildProperties.setProperty('name', project.name)
        buildProperties.setProperty('group', project.group.toString())
        buildProperties.setProperty('version', project.version.toString())
        buildProperties.setProperty('date', formatter.format(now))
        buildProperties.setProperty('timestamp', now.time.toString())
        buildProperties.setProperty('commit', GitTools.commitHash())
        buildProperties.setProperty('branch', GitTools.branchName())
        buildProperties.store(new FileOutputStream(propertyFile), null)
    }

}
