package io.gravis.tool.gradle.tasks

import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import com.bmuschko.gradle.docker.tasks.container.*
import com.bmuschko.gradle.docker.tasks.image.*
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

@CompileStatic
class StartPsql extends DefaultTask {

    @Input
    @Optional
    String version = 'latest'

    @Input
    @Optional
    String username = 'testUser'

    @Input
    @Optional
    String password = 'testPassword'

    @Input
    @Optional
    Integer port = 5432

    @Input
    @Optional
    String dbname = 'testdb'

    // Delay to wait after container started in seconds
    @Input
    @Optional
    Integer delay = 3

    @OutputFile
    File idFile

    private String imageName = "test-psql-${project.name}"
    private String imageIdFile = "${project.buildDir}/.${imageName}.dockerid"

    StartPsql() {

        idFile = project.file(imageIdFile)
        // This file is always rebuilt
        outputs.upToDateWhen { false }
    }

    @TaskAction
    void run() {

        String[] psqlEnv = [
                "POSTGRES_USER=${username}",
                "POSTGRES_PASSWORD=${password}",
                "POSTGRES_DB=${dbname}"
        ]
        List<String> psqlPorts = ["5432:${port}".toString()]
        String imageId = "postgres:${version}"

        DockerPullImage pull = project.tasks.create('pullPsqlDockerImage', DockerPullImage)
        pull.repository = 'postgres'
        pull.tag = version
        pull.execute()

        StopPsql stopOld = project.tasks.create('stopOldPsqlDockerContainer', StopPsql)
        stopOld.execute()

        DockerCreateContainer create = project.tasks.create('createPsqlDockerContainer', DockerCreateContainer)
        create.targetImageId { imageId }
        create.containerName = imageName
        create.portBindings = psqlPorts
        create.env = psqlEnv
        create.execute()
        String containerId = create.containerId

        // Write for later so we can stop the container
        FileOutputStream idOutput = new FileOutputStream(idFile)
        idOutput.write(containerId.bytes)
        idOutput.close()

        project.extensions.extraProperties.set('testPsqlContainerId', containerId)

        DockerStartContainer start = project.tasks.create('startPsqlDockerContainer', DockerStartContainer)
        start.targetContainerId { containerId }
        start.execute()

        Thread.sleep(delay*1000)
    }

}
