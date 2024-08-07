def call(Map config) {
    def builder = "npm"
    def projectConfig = config.b_config.project
    def projectPath = projectConfig.path ? projectConfig.path : '.'

    if ( projectConfig.builderVersion != "nodejs" ) {
        builder = "${tool projectConfig.builderVersion}/bin/npm"
    }

    sh """
    cd ${projectPath} && \
    ${builder} install && \
    ${builder} run ${projectConfig.buildCommand ? projectConfig.buildCommand : 'build'}
    """
}
