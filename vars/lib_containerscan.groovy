// vars/lib_containerscan.groovy

def trivyScan(String imageName, String outputDir = 'trivy-reports') {
    script {
        try {
            echo "Running Trivy scan for image: ${imageName}"

            // Pull Trivy and scan the image
            sh "docker pull aquasec/trivy:latest"
            
            // Ensure the output directory exists
            sh "mkdir -p ${outputDir}"

            // Generate HTML report
            sh """
            docker run --rm -v /var/run/docker.sock:/var/run/docker.sock \
                -v $(pwd)/${outputDir}:/root/.cache/ aquasec/trivy:latest image \
                --no-progress --exit-code 1 --format html \
                --output ${outputDir}/trivy-report-${imageName.replaceAll('/', '_')}.html \
                ${imageName}
            """
            
            echo "Trivy scan completed for image: ${imageName}. Reports available in ${outputDir}."

        } catch (Exception e) {
            error "Trivy scan failed: ${e.message}"
        }
    }
}
