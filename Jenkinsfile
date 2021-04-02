#!groovy
podTemplate(
        containers: [
                containerTemplate(name: 'java', image: 'openkbs/jre-mvn-py3:latest', ttyEnabled: true, command: 'cat')
        ]
) {

    node(POD_LABEL) {

        stage('Checkout2') {

            container('java') {

                sh('echo Running container: $POD_CONTAINER')
                sh('python3 --version')
                sh('mvn -version')
                sh('java -version')

                dir('pipeforce-build') {
                    git url: 'https://github.com/logabit/pipeforce-build.git', credentialsId: 'github'
                }
                dir('pipeforce-cli') {
                    git url: 'https://github.com/logabit/pipeforce-cli.git', credentialsId: 'github'
                }
                dir('pipeforce-sdk-java') {
                    git url: 'https://github.com/logabit/pipeforce-sdk-java.git', credentialsId: 'github'
                }

            }
        }
    }
}
