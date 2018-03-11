package io.gravis.tool.gradle.extensions

import groovy.transform.CompileStatic

@CompileStatic
class PsqlConfiguration {

    String version = 'latest'
    String username = 'testUser'
    String password = 'testPassword'
    Integer port = 5432
    String dbname = 'testdb'
    // Delay to wait after container started in seconds
    Integer delay = 3

}
