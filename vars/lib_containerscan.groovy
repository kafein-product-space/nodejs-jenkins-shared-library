// vars/lib_containerscan.groovy

def trivyScan(String imageName, String outputDir = 'trivy-reports', String templateDir = '/home/jenkins/.templates') {
    script {
        try {
            echo "Running Trivy scan for image: ${imageName}"

            // Pull Trivy image
            sh "docker pull aquasec/trivy:latest"

            // Ensure the output directory exists
            sh "mkdir -p ${outputDir}"

            // Generate HTML report using the custom template
            sh """
            docker run --rm -v /var/run/docker.sock:/var/run/docker.sock \
                -v ${templateDir}/.cache:/root/.cache/ \
                -v ${templateDir}/html.tpl:/html.tpl \
                aquasec/trivy:latest image \
                --no-progress --exit-code 1 --format template --scanners vuln \
                --template /html.tpl \
                --output ${outputDir}/trivy-report-${config.b_config.project.name}.html \
                ${imageName}
            """
            
            echo "Trivy scan completed for image: ${imageName}. HTML report available in ${outputDir}."

        } catch (Exception e) {
            error "Trivy scan failed: ${e.message}"
        }
    }
}
