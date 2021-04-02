#!groovy
properties([
        parameters([
                string(name: 'GIT_BRANCH', defaultValue: 'master', description: 'The branch to do the modules build.', )
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

                sh('echo Running container: $POD_CONTAINER, DEPLOY_ENV: $DEPLOY_ENV')
                sh('python3 --version')
                sh('mvn -version')
                sh('java -version')

                sh('ls')
                sh('ls pipeforce-build')

                dir('pipeforce-build') {
                    git branch: '$GIT_BRANCH', url: 'https://github.com/logabit/pipeforce-build.git', credentialsId: 'github'
                }
                dir('pipeforce-cli') {
                    git branch: '$GIT_BRANCH', url: 'https://github.com/logabit/pipeforce-cli.git', credentialsId: 'github'
                }
                dir('pipeforce-sdk-java') {
                    git branch: '$GIT_BRANCH', url: 'https://github.com/logabit/pipeforce-sdk-java.git', credentialsId: 'github'
                }
            }

            stage('Build') {

                sh('pip install -r pipeforce-build/requirements.txt')

//                dir('pipeforce-build') {
//                    sh('python3 pi-build.py build pipeforce-cli ' +
//                            '-p build_home=/home/jenkins/agent/workspace/pipeforce-cli_master')
//                }
            }
        }
    }
}
