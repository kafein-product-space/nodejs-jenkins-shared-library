def call(Map config) {
    def builder = "npm"

    if (config.b_config.project.builderVersion != "nodejs") {
        builder = "${tool config.b_config.project.builderVersion}/bin/npm"
    }

    def path = "."
    if (config.b_config.project.containsKey("path")) {
        path = config.b_config.project.containsKey("path")
    }

    sh """
    cd ${path} && \
    ${builder} install && \
    ${builder} run ${config.b_config.project.buildCommand ? config.b_config.project.buildCommand : 'build'}
    """
}

