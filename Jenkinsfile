#!groovy
node {
    stage "Build"
    checkout scm

    dockerRepo = "test-${env.BUILD_TAG}"

    sh "docker build --rm -t ${dockerRepo} ."

    dockerTestRunner = "test-${env.BUILD_TAG}"
    dockerDeployer = "deploy-${env.BUILD_TAG}"
    try {
        stage "Test"
            sh "docker run --name ${dockerTestRunner} --rm ${dockerRepo}"

        stage "Deploy"
        withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'jenkins-clojars-credentials', usernameVariable: 'LEIN_USERNAME', passwordVariable: 'LEIN_PASSWORD']]) {
            sh "docker run --name ${dockerDeployer} -e LEIN_USERNAME -e LEIN_PASSWORD --rm ${dockerRepo} lein deploy"
        }
    } finally {
        sh returnStatus: true, script: "docker kill ${dockerTestRunner}"
        sh returnStatus: true, script: "docker rm ${dockerTestRunner}"
        sh returnStatus: true, script: "docker kill ${dockerDeployer}"
        sh returnStatus: true, script: "docker rm ${dockerDeployer}"
        sh returnStatus: true, script: "docker rmi ${dockerRepo}"
    }
}
