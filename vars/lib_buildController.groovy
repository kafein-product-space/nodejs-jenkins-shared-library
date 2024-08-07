def call(Map config) {
    def builder = "npm"

    if ( config.b_config.project.builderVersion != "nodejs" ) {
        builder = "${tool config.b_config.project.builderVersion}/bin/npm"
    }

    config.b_config.project.each { it ->
        def path = "."

        if (it.containsKey("path")) {
            path = it.path
        }

        sh """
        cd ${path} && \
        ${builder} install && \
        ${builder} run ${config.b_config.project.buildCommand ? config.b_config.project.buildCommand : 'build'}
        """
    }
}
