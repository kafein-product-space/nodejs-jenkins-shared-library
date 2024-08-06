def call(Map config) {
    config.b_config.projects.each { it ->
        def builder = "npm"

        if (project.builderVersion != "nodejs") {
            builder = "${tool project.builderVersion}/bin/npm"
        }

        if (project.containsKey('path')) {
            // Run the command if the key exists
            sh """
            cd ${it.path} && \
            ${builder} install && \
            ${builder} run ${project.buildCommand ? project.buildCommand : 'build'}
            """
        } else {
            sh """
            ${builder} install && \
            ${builder} run ${project.buildCommand ? project.buildCommand : 'build'}
            """
        }
    }
}