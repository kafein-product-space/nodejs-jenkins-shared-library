def call(Map config) {

    pipeline {
        agent {
            label "auto-devops"
        }

        parameters {
            string(name: 'IMAGE', defaultValue: '', description: '')
            string(name: 'BRANCH', description: 'Branch to build', defaultValue: '')
        }

        stages {
            stage("Configure Init") {
                steps {
                    script {
                        lib_helper.configureInit(
                            config
                        )
                    }
                }
            }

            stage("Checkout Project Code") {
                steps {
                    checkout scm: [
                        $class: "GitSCM",
                        branches: [[name: "refs/heads/${config.target_branch}"]],
                        submoduleCfg: [],
                        userRemoteConfigs: [
                            config.scm_global_config
                        ]
                    ]
                }
            }

            stage("Read Project Config") {
                steps {
                    script {
                        // Create config file variable
                        config.config_file = ".jenkins/buildspec.yaml"
                        config.b_config = readYaml file: config.config_file
                        config.job_base = sh(
                            script: "python3 -c 'print(\"${JENKINS_HOME}/jobs/%s\" % \"/jobs/\".join(\"${JOB_NAME}\".split(\"/\")))'",
                            returnStdout: true
                        ).trim()
                        commitID = sh(
                            script: """
                            git log --pretty=format:"%h" | head -1
                            """,
                            returnStdout: true
                        ).trim()
                        config.b_config.imageTag = commitID
                        config.b_config.imageLatestTag = "latest"

                        if ( config.scope == "branch" ) {
                            config.b_config.imageTag = "${config.target_branch}-${commitID}"
                            config.b_config.imageLatestTag = "${config.target_branch}-latest"
                        }

                        config.commitID = commitID
                    }
                }
            }

            stage("Configure for Branch Deployment") {
                when {
                    expression {
                        return config.b_config.controllers.deployController
                    }
                }
                steps {
                    withCredentials([sshUserPrivateKey(credentialsId: config.scm_credentials_id, keyFileVariable: 'keyfile')]) {
                        script {
                            lib_helper.configureBranchDeployment(
                                config,
                                keyfile
                            )
                        }
                    }
                }
            }

            stage("Deploy New Code") {
                when {
                    expression {
                        return config.b_config.controllers.deployController
                    }
                }
                steps {
                    withCredentials([sshUserPrivateKey(credentialsId: config.scm_credentials_id, keyFileVariable: 'keyfile')]) {
                        script {
                            lib_deployController(
                                config,
                                keyfile
                            )
                        }
                    }
                }
            }

        }

        post {
            always {
                script {
                    lib_cleanupController(config)
                    lib_postBuildController(config)
                }
            }
            success {
                buildDescription("Container ID: ${env.CONTAINER_IMAGE_ID}")

                script {
                    lib_helper.triggerJob(
                        config
                    )
                }
                script {
                    def buildTime = lib_teamsnotifications.getBuildTime()
                    lib_teamsnotifications('Success', "The deployment has completed successfully in ${buildTime}. Current version is: ${env.CONTAINER_IMAGE_ID}", 'teams-webhook-url')
                }
                script {
                    def publisher = LastChanges.getLastChangesPublisher("PREVIOUS_REVISION", "SIDE", "LINE", true, true, "", "", "", "", "")
                    publisher.publishLastChanges()
                    def htmlDiff = publisher.getHtmlDiff()
                    writeFile file: 'build-diff.html', text: htmlDiff

                    lib_helper.triggerJob(config)
                }
            }
            failure {
                script {
                    def buildTime = lib_teamsnotifications.getBuildTime()
                    lib_teamsnotifications('Failure', "The build has failed after ${buildTime}. Please check the logs for details.", 'teams-webhook-url')
                }
            }
        }
    }
}
