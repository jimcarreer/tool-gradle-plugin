package io.gravis.tool.gradle

import com.bmuschko.gradle.docker.tasks.container.DockerCreateContainer
import com.bmuschko.gradle.docker.tasks.container.DockerRemoveContainer
import com.bmuschko.gradle.docker.tasks.container.DockerStartContainer
import com.bmuschko.gradle.docker.tasks.image.DockerPullImage
import groovy.transform.CompileStatic
import io.gravis.tool.gradle.extensions.PsqlConfiguration
import org.gradle.api.Plugin
import org.gradle.api.Project

@CompileStatic
class PsqlHelperPlugin implements Plugin<Project> {

    private static final String STOP_TASK = 'stopPsql'
    private static final String STOP_OLD_TASK = 'destroyOldPsqlDockerContainer'
    private static final String PULL_TASK = 'pullPsqlDockerImage'
    private static final String CREATE_TASK = 'createPsqlDockerContainer'
    private static final String START_TASK = 'startPsql'

    private String containerName
    private PsqlConfiguration config

    @Override
    void apply(Project project) {

        project.getPluginManager().apply('com.bmuschko.docker-remote-api')
        config = project.extensions.create('psqlConfig', PsqlConfiguration)
        containerName = "test-psql-${project.name}"

        addStopTask(project, STOP_OLD_TASK)
        addStopTask(project, STOP_TASK)
        addPullTask(project)
        addCreateTask(project)
        addStartTask(project)
    }

    protected addStopTask(Project project, String name) {

        DockerRemoveContainer stop = project.tasks.create(name, DockerRemoveContainer)
        stop.configure({ DockerRemoveContainer task ->
            task.force = true
            task.removeVolumes = true
            task.containerId = containerName
        })
        stop.onError = { Exception error ->
            if (error.message.contains('No such container'))
                stop.logger.quiet("No container with Id ${containerName} continuing ...")
            else
                throw error
        }
    }

    protected addPullTask(Project project) {

        project.tasks.create(PULL_TASK, DockerPullImage)
            .configure({ DockerPullImage task ->
                task.repository = 'postgres'
                task.tag = config.version
            })
    }

    protected addCreateTask(Project project) {

        project.tasks.create(CREATE_TASK, DockerCreateContainer)
            .dependsOn(PULL_TASK, STOP_OLD_TASK)
            .configure({ DockerCreateContainer task ->
                task.targetImageId { "postgres:${config.version}".toString() }
                task.containerName = containerName
                task.portBindings = ["5432:${config.port}".toString()]
                task.setEnv(
                    "POSTGRES_USER=${config.username}",
                    "POSTGRES_PASSWORD=${config.password}",
                    "POSTGRES_DB=${config.dbname}"
                )
            })
    }

    protected addStartTask(Project project) {

        project.tasks.create(START_TASK, DockerStartContainer)
            .dependsOn(CREATE_TASK)
            .doLast({ Thread.sleep(config.delay*1000) } )
            .configure({ DockerStartContainer task ->
                task.targetContainerId { containerName }
            })
    }
}
