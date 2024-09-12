def trivyScan(Map config, String imageName, String outputDir = "${env.WORKSPACE}/trivy-reports", String templateDir = '/home/jenkins/.templates') {
    script {
        try {
            echo "Running Trivy scan for image: ${imageName}"

            // Pull Trivy image
            sh "docker pull aquasec/trivy:latest"

            // Ensure the output directory exists in the Jenkins workspace
            sh "mkdir -p ${outputDir}"

            // Generate HTML report using the custom template and store it in the Jenkins workspace
            sh """
            docker run --rm --user 1000:1000 -v /var/run/docker.sock:/var/run/docker.sock \
                -v ${templateDir}/.cache:/root/.cache/ \
                -v ${templateDir}/html.tpl:/html.tpl \
                -v ${outputDir}:${outputDir} \
                aquasec/trivy:latest image \
                --no-progress --exit-code 1 --format template --scanners vuln \
                --template /html.tpl \
                --output ${outputDir}/trivy-report-${config.b_config.project.name}.html \
                ${imageName}
            """
            sh "chown -R 1000:1000 ${outputDir}"

            echo "Trivy scan completed for image: ${imageName}. HTML report saved in ${outputDir}."

            // Archive the report as a Jenkins artifact
            archiveArtifacts artifacts: "${outputDir}/trivy-report-${config.b_config.project.name}.html", allowEmptyArchive: false

        } catch (Exception e) {
            error "Trivy scan failed: ${e.message}"
        }
    }
}
