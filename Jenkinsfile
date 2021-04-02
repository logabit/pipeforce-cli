#!groovy
podTemplate(
        containers: [
                containerTemplate(name: 'jdk-mvn-py', image: 'openkbs/jre-mvn-py3:latest', ttyEnabled: true, command: 'cat')
        ]
) {

    node(POD_LABEL) {

        container('jdk-mvn-py') {

            stage('Checkout') {

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

                sh('pip install -r pipeforce-build/requirements.txt')
            }

            stage('Build') {
                sh('ls')
                sh('cd pipeforce-build')
                sh('ls')
                sh('python3 pi-build.py build pipeforce-cli')
            }
        }
    }
}
