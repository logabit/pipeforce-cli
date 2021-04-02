#!groovy
properties([
        parameters([
                string(name: 'DEPLOY_ENV', defaultValue: 'TESTING', description: 'The target environment', )
        ])
])
podTemplate(
        containers: [
                containerTemplate(name: 'jdk-mvn-py', image: 'openkbs/jre-mvn-py3:latest', ttyEnabled: true, command: 'cat')
        ],
        volumes: [
                hostPathVolume(hostPath: '/var/run/docker.sock', mountPath: '/var/run/docker.sock'),
                persistentVolumeClaim(claimName: 'pvc-build-m2-repo', mountPath: '/home/developer/.m2')
        ],
        workspaceVolume: persistentVolumeClaimWorkspaceVolume(claimName: 'pvc-build-jenkins', readOnly: false),
) {

    node(POD_LABEL) {

        container('jdk-mvn-py') {

            stage('Checkout') {

                sh('echo Running container: $POD_CONTAINER')
                sh('python3 --version')
                sh('mvn -version')
                sh('java -version')

                sh('ls')
                sh('ls pipeforce-build')

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

            stage('Build') {

                dir('pipeforce-build') {

                    sh('pip install -r pipeforce-build/requirements.txt')

                    sh('python3 pi-build.py build pipeforce-cli ' +
                            '-p build_home=/home/jenkins/agent/workspace/pipeforce-cli_master')
                }
            }
        }
    }
}
