def call(Map config) {
    def builder = "npm"
    def projectConfig = config.b_config.project
    def projectPath = projectConfig.path ? projectConfig.path : '.' // Set path under the project to use monorepo

    // Ensure the npm auth token is set in the environment variable
    if (!env.NPM_AUTH_KEY) {
        error("Environment variable NPM_AUTH_KEY is not set.")
    }

    // Optionally, create or modify the .npmrc file to include the auth token
//    writeFile file: "${projectPath}/.npmrc", text: """
//    @kafein:registry=https://gitlab.netfein.com/api/v4/projects/12/packages/npm/
    //gitlab.netfein.com/api/v4/projects/12/packages/npm/:_authToken=${env.NPM_AUTH_KEY}
//    """

    if (projectConfig.builderVersion != "nodejs") {
        builder = "${tool projectConfig.builderVersion}/bin/npm"
    }

    sh """
    cd ${projectPath} && \
    ${builder} install && \
    ${builder} run ${projectConfig.buildCommand ? projectConfig.buildCommand : 'build'}
    """
}
