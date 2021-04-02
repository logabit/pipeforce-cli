#!groovy
properties([
        parameters([
                string(name: 'GIT_BRANCH', defaultValue: 'master', description: 'The branch to do the modules build.',)
        ])
])
podTemplate(
        containers: [
                containerTemplate(name: 'pipeforce-buildbox', image: 'pipeforce-build:latest', ttyEnabled: true, command: 'cat')
        ],
        volumes: [
                hostPathVolume(hostPath: '/var/run/docker.sock', mountPath: '/var/run/docker.sock'),
                persistentVolumeClaim(claimName: 'pvc-build-m2-repo', mountPath: '/root/.m2'),
        ],
        workspaceVolume: persistentVolumeClaimWorkspaceVolume(claimName: 'pvc-build-jenkins', readOnly: false),
) {

    node(POD_LABEL) {

        container('pipeforce-buildbox') {

            stage('Checkout') {

                // Log important information
                sh('echo Running container: $POD_CONTAINER, GIT_BRANCH: $GIT_BRANCH')
                sh('python3 --version')
                sh('mvn -version')
                sh('java -version')


                // Checkout all required repos from GitHub
                def repos = [
                        'pipeforce-build',
                        'pipeforce-cli',
                        'pipeforce-defaults',
                        'pipeforce-sdk-java',
                        'pipeforce-service-drive',
                        'pipeforce-service-hub',
                        'pipeforce-service-iam',
                        'pipeforce-service-onlyoffice',
                        'pipeforce-service-portal',
                        'pipeforce-service-postgres',
                        'pipeforce-service-redis',
                        'pipeforce-service-reporting',
                        'pipeforce-service-workflow',
                        'pipeforce-tools',
                ]

                for (String repo : repos) {
                    // TODO Optimize here to do next step only in case sources have changed
                    dir(repo) {
                        git branch: '$GIT_BRANCH', url: 'https://github.com/logabit/' + repo + '.git', credentialsId: 'github'
                    }
                }
            }

            stage('Build') {

                // Log important information
                sh('ls /app')
                sh(' mvn help:effective-settings ')
//                sh('ls /')
//                sh('docker images') // Add list of existing Docker images into logs
//                sh('ls /home/jenkins/agent/workspace/pipeforce-cli_master')

                // Start PIPEFORCE build
                dir('pipeforce-build') {
//                    sh('ls /home')
//                    sh('ls /home/root')
                    sh('python3 pi-build.py containerize pipeforce-service-hub ' +
                            '-p build_home=/home/jenkins/agent/workspace/pipeforce-cli_master')
                    sh('ls /home')
                }

                // TODO Refer from inside
            }
        }
    }
}
