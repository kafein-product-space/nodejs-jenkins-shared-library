def call(Map config) {
    config.b_config.projects.each { it ->
        def builder = "npm"

        if ( config.b_config.project.builderVersion != "nodejs" ) {
            builder = "${tool config.b_config.project.builderVersion}/bin/npm"
        }

        sh """
        cd ${it.path} && \
        ${builder} install && \
        ${builder} run ${config.b_config.project.buildCommand ? config.b_config.project.buildCommand : 'build'}
        """
    }
}
