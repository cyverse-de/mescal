def repo = "mescal"
def dockerUser = "discoenv"

node {
    stage "Build"
    checkout scm

    dockerRepo = "test-${repo}-${env.BRANCH_NAME}"

    sh "docker build --rm -t ${dockerRepo} ."

    stage "Test"
	sh "docker run --rm ${dockerRepo}"

    stage "Deploy"
    withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'jenkins-clojars-credentials', usernameVariable: 'LEIN_USERNAME', passwordVariable: 'LEIN_PASSWORD']]) {
        sh "docker run -e LEIN_USERNAME -e LEIN_PASSWORD --rm ${dockerRepo} lein deploy"
    }

    stage "Clean"
    sh "docker rmi ${dockerRepo}"
}
