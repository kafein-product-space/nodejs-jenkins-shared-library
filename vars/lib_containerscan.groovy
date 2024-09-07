// vars/lib_containerscan.groovy

def trivyScan(String imageName, String outputDir = 'trivy-reports', String templatePath = '/home/jenkins/.templates/html.tpl', String registryCredentialsId = 'your-credentials-id') {
    script {
        try {
            echo "Running Trivy scan for image: ${imageName}"

            // Authenticate to Docker registry (if needed)
            withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: it.credentialsId ? it.credentialsId : "user-nexus", usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']]) {
                sh """
                docker login --username $USERNAME --password $PASSWORD ${container_repository}
                """
            }

            // Pull Trivy image
            sh "docker pull aquasec/trivy:latest"

            // Ensure the output directory exists
            sh "mkdir -p ${outputDir}"

            // Generate HTML report using the custom template
            sh """
            docker run --rm -v /var/run/docker.sock:/var/run/docker.sock \
                -v \$(pwd)/${outputDir}:/root/.cache/ \
                -v \$(pwd)/${templatePath}:/html.tpl \
                aquasec/trivy:latest image \
                --no-progress --exit-code 1 --format template \
                --template /html.tpl \
                --output ${outputDir}/trivy-report-${imageName.replaceAll('/', '_')}.html \
                ${imageName}
            """
            
            echo "Trivy scan completed for image: ${imageName}. HTML report available in ${outputDir}."

        } catch (Exception e) {
            error "Trivy scan failed: ${e.message}"
        }
    }
}
