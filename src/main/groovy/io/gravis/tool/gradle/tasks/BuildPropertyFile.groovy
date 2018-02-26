package io.gravis.tool.gradle.tasks

import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import io.gravis.tool.gradle.util.GitTools

import java.text.SimpleDateFormat

@CompileStatic
class BuildPropertyFile extends DefaultTask  {

    final private static String FILE_PATH = 'resources/main/build.properties'
    final private static String DATE_FORMAT = 'yyyy-MM-dd\'T\'HH:mm:ss.SSSZ'

    @Input
    @Optional
    Boolean makeFile = true

    @OutputFile
    File propertyFile

    BuildPropertyFile() {

        propertyFile = project.file("${project.buildDir}/${FILE_PATH}")
        // This file is always rebuilt
        outputs.upToDateWhen { false }
    }

    @TaskAction
    void run() {

        if (!makeFile) {
            logger.info('Build property file task disabled')
            return
        }

        Date now = new Date()
        Properties buildProperties = new Properties()
        SimpleDateFormat formatter = new SimpleDateFormat(DATE_FORMAT)
        buildProperties.setProperty('gradle', project.gradle.gradleVersion)
        buildProperties.setProperty('name', project.name)
        buildProperties.setProperty('group', project.group.toString())
        buildProperties.setProperty('version', project.version.toString())
        buildProperties.setProperty('date', formatter.format(now))
        buildProperties.setProperty('timestamp', now.time.toString())
        try {
            buildProperties.setProperty('commit', GitTools.commitHash())
            buildProperties.setProperty('branch', GitTools.branchName())
        } catch (IllegalArgumentException e) {
            if (e.message.contains('setGitDir or setWorkTree'))
                logger.warn('No git repository appears to be initialized, build properties will not contain git attributes')
            else
                throw e
        }
        buildProperties.store(new FileOutputStream(propertyFile), null)
    }

}
