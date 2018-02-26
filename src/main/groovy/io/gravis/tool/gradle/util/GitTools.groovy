package io.gravis.tool.gradle.util

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.apache.maven.artifact.versioning.ComparableVersion
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.gradle.api.GradleException
import java.util.regex.Matcher

@CompileStatic
@Slf4j
class GitTools {

    private static Repository REPO
    private static String BRANCH_DEVELOP = 'develop'
    private static String BRANCH_MASTER = 'master'
    private static String RELEASE_PREFIX = 'refs/tags/release/'
    private static String MAJOR_MINOR = /\d+\.\d+/
    private static String SEMANTIC_CAPTURE = /(\d+\.\d+)\.?(\d+)?/

    private static initialize() {
        if (!REPO) {
            FileRepositoryBuilder builder = new FileRepositoryBuilder()
            builder.setMustExist(true)
            builder.findGitDir()
            REPO = builder.build()
        }
    }

    static String branchBasedVersion(String majorMinor) {

        if (!(majorMinor ==~ MAJOR_MINOR)) {
            throw new GradleException("Version ${majorMinor} supplied is an invalid format: must by X.Y")
        }

        try {
            String branch = branchName()
            switch (branch) {
                case BRANCH_DEVELOP:
                    return "${majorMinor}-SNAPSHOT"
                case BRANCH_MASTER:
                    String patch = getPatchFromTag(majorMinor)
                    return "${majorMinor}.${patch}"
                default:
                    return "${majorMinor}-${branch}-SNAPSHOT"
            }

        } catch (IllegalArgumentException e) {
            if (!e.message.contains('setGitDir or setWorkTree'))
                throw e
            log.warn("No git repository appears to be initialized returning ${majorMinor}.0")
            return "${majorMinor}.0"
        }
    }

    static String getPatchFromTag(String targetVersion) {
        initialize()
        Git git = new Git(REPO)
        List<Ref> tags = git.tagList().call()
        Ref lastReleaseTag = tags.reverse().find { Ref tag ->
            tag.getName().startsWith(RELEASE_PREFIX)
        }

        if (!lastReleaseTag) {
            return "0"
        }

        String taggedVersion = lastReleaseTag.name.replace(RELEASE_PREFIX, '')

        Matcher matcher = (taggedVersion =~ SEMANTIC_CAPTURE)
        if (!matcher.matches()) {
            throw new GroovyRuntimeException("Invalid release version tag: '${lastReleaseTag.getName()}")
        }

        String patch = "0"
        if (matcher.groupCount() > 1) {
            patch = (Integer.parseInt(matcher.group(2))+1).toString()
        }

        ComparableVersion cmpTargetVersion = new ComparableVersion(targetVersion)
        ComparableVersion cmpTaggedVersion = new ComparableVersion(matcher.group(1))

        if (cmpTaggedVersion > cmpTargetVersion) {
            throw new GroovyRuntimeException("Target version is ${targetVersion} but last release version is ${taggedVersion}")
        }

        return patch
    }

    static String commitHash() {
        initialize()
        Git git = new Git(REPO)
        return git.log().call()[0].name
    }

    static String branchName() {
        initialize()
        String name = REPO.getBranch()
        return name.length() < 20 ? name : name.substring(0,19)
    }
}
