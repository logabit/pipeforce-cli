#!groovy
properties([
        parameters([
                string(name: 'GIT_BRANCH', defaultValue: 'master', description: 'The branch to do the modules build.', )
        ])
])
podTemplate(
        containers: [
                containerTemplate(name: 'pipeforce-buildbox', image: 'pipeforce-build:latest', ttyEnabled: true, command: 'cat')
        ],
        volumes: [
                hostPathVolume(hostPath: '/var/run/docker.sock', mountPath: '/var/run/docker.sock'),
                persistentVolumeClaim(claimName: 'pvc-build-m2-repo', mountPath: '/home/developer/.m2'),
                persistentVolumeClaim(claimName: 'pvc-build-python3-repo', mountPath: '/home/developer/.m2')
        ],
        workspaceVolume: persistentVolumeClaimWorkspaceVolume(claimName: 'pvc-build-jenkins', readOnly: false),
) {

    node(POD_LABEL) {

        container('pipeforce-buildbox') {

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

                sh('ls /app')
                sh('docker images')
//                sh('pip3 install -r pipeforce-build/requirements.txt')

//                dir('pipeforce-build') {
//                    sh('python3 pi-build.py build pipeforce-cli ' +
//                            '-p build_home=/home/jenkins/agent/workspace/pipeforce-cli_master')
//                }
            }
        }
    }
}
