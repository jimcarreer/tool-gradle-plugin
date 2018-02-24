package io.gravis.tool.gradle.tasks

import com.bmuschko.gradle.docker.tasks.container.DockerRemoveContainer
import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

@CompileStatic
class StopPsql extends DefaultTask {

    private String imageName = "test-psql-${project.name}"
    private File imageIdFile = new File("${project.buildDir}/.${imageName}.dockerid")

    @TaskAction
    void run() {

        if (!imageIdFile.exists()) {
            logger.quiet("Image id file for ${imageName} does not exist continuing ... ")
            return
        }

        String id = imageIdFile.readLines()[0]
        DockerRemoveContainer remove = project.tasks.create('removePsqlDockerContainer', DockerRemoveContainer)
        remove.force = true
        remove.removeVolumes = true
        remove.containerId = id
        remove.onError = { Exception error ->
            // Ignore no such container errors
            if (error.message.contains('No such container'))
                logger.quiet("No container with Id ${id} continuing ...")
            else
                throw error
        }
        remove.execute()
        imageIdFile.delete()
    }

}
