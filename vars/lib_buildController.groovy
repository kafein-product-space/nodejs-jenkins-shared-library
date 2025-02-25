def call(Map config) {
    def builder = "npm"
    def projectConfig = config.b_config.project
    def projectPath = projectConfig.path ? projectConfig.path : '.' // Set path under the project to use monorepo

    // Ensure the npm auth token is set in the environment variable
    if (!env.NPM_AUTH_KEY) {
        error("Environment variable NPM_AUTH_KEY is not set.")
    }

    // Remove existing .npmrc file if it exists
    def npmrcFilePath = "${projectPath}/.npmrc"
    if (fileExists(npmrcFilePath)) {
        sh "rm -f ${npmrcFilePath}"
    }

    // Create a new .npmrc file
    writeFile file: npmrcFilePath, text: """
    @shared:registry=https://gitlab.netfein.com/api/v4/packages/npm/
    @integro:registry=https://gitlab.netfein.com/api/v4/packages/npm/
    //gitlab.netfein.com/api/v4/projects/12/packages/npm/:_authToken=${env.NPM_AUTH_KEY}
    """

    if (projectConfig.builderVersion != "nodejs") {
        builder = "${tool projectConfig.builderVersion}/bin/npm"
    }

    sh """
    cd ${projectPath} && \
    ${builder} config list && \
    ${builder} cache clean --force && \
    ${builder} install && \
    ${builder} run ${projectConfig.buildCommand ? projectConfig.buildCommand : 'build'}
    """
}
