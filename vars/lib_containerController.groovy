def call(Map config) {
    // Check object existence
    if (!config.b_config.containerConfig) {
        currentBuild.result = "ABORTED"
        error("You have to set 'containerConfig' in your yaml file.")
    }

    // Locals
    def containerImages = []
    def tasks = [:]  // For parallel execution
    def container_repository = "${config.container_artifact_repo_address}"

    if (config.container_repo != "") {
        container_repository = "${config.container_artifact_repo_address}/${config.container_repo}"
    }

    buildDescription("Container ID: ${config.b_config.imageTag}")

    config.b_config.containerConfig.each { it ->
        def repoName = it.name.replace("_", "-").toLowerCase()
        def dockerFilePath = it.dockerFilePath.replace("_", "-")

        if (it.containsKey('copyToContext')) {
            it.copyToContext.each { ti ->
                def from = ti.from.replace("{commit-id}", config.b_config.imageTag)
                def to = ti.to.replace("{context-path}", it.contextPath)

                sh """
                cp -a ${from} ${to}
                """
            }
        }

        def imageTag = "${container_repository}/${repoName}:${config.b_config.imageTag}"
        containerImages.add("${imageTag} ${dockerFilePath}")

        // Define the build task for the current image
        tasks["${repoName}_build"] = {
            timeout(time: 25, unit: "MINUTES") {
                stage("Building ${repoName}") {
                    script {
                        try {
                            sh """
                            docker build --rm  \
                                -t ${imageTag} \
                                -t ${container_repository}/${repoName}:${config.b_config.imageLatestTag} \
                                -f ${dockerFilePath} \
                                ${it.contextPath}
                            """
                        } catch (Exception e) {
                            state = sh(
                                script: """
                                docker image inspect ${imageTag} 2> /dev/null && echo success || echo failed
                                """,
                                returnStdout: true
                            ).trim()

                            if (state == "failed") {
                                currentBuild.result = "ABORTED"
                                error("Error occurred when building container image. Image Name: ${it.name}")
                            }
                        }
                    }
                }
            }
        }

        // Define the push task for the current image
        tasks["${repoName}_push"] = {
            stage("Pushing ${repoName}") {
                withCredentials([[$class: "UsernamePasswordMultiBinding", credentialsId: it.credentialsId ? it.credentialsId : "user-nexus", usernameVariable: "USERNAME", passwordVariable: "PASSWORD"]]) {
                    sh """
                    docker login --username $USERNAME --password $PASSWORD ${container_repository}
                    docker push ${imageTag} && \
                    docker push ${container_repository}/${repoName}:${config.b_config.imageLatestTag}
                    """
                }
            }
        }

        // Define the scan task for the current image
        tasks["${repoName}_scan"] = {
            stage("Scanning ${repoName} with Trivy") {
                script {
                    trivyScan(imageTag)  // Calling the Trivy scan function from lib_containerscan.groovy
                }
            }
        }
    }

    // Run build, push, and scan tasks in parallel
    parallel tasks

    // Define the remove images task
    def removeImages = {
        stage("Removing Docker Images") {
            script {
                containerImages.each { image ->
                    sh """
                    docker rmi ${image} || true
                    """
                }
            }
        }
    }

    // Run remove images task after parallel tasks
    removeImages()

    // Assign the container images to the config object for further use
    config.containerImages = containerImages
}
