#!groovy
properties([
        parameters([
                string(name: 'GIT_BRANCH', defaultValue: 'master', description: 'The branch of modules to checkout.'),
                string(name: 'namespace', defaultValue: 'dev', description: 'The K8s namespace to install PIPEFORCE into.'),
                string(name: 'skip_phase', defaultValue: '', description: 'Comma separated list of phases to skip [render,build,test,containerize,deploy].'),
        ])
])
podTemplate(
        serviceAccount: 'pipeforce-apps-manager-build-sa',
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

                sh('docker images')
                sh('kubectl get pods -n build')
                sh('kubectl get pods -n default')
//                // Log important information
//                sh('echo Running container: $POD_CONTAINER, GIT_BRANCH: $GIT_BRANCH')
//                sh('python3 --version')
//                sh('mvn -version')
//                sh('java -version')
//
//                // Checkout all required repos from GitHub
//                def repos = [
//                        'pipeforce-build',
//                        'pipeforce-cli',
//                        'pipeforce-defaults',
//                        'pipeforce-sdk-java',
//                        'pipeforce-service-drive',
//                        'pipeforce-service-hub',
//                        'pipeforce-service-iam',
//                        'pipeforce-service-onlyoffice',
//                        'pipeforce-service-portal',
//                        'pipeforce-service-postgres',
//                        'pipeforce-service-redis',
//                        'pipeforce-service-reporting',
//                        'pipeforce-service-workflow',
//                        'pipeforce-tools',
//                ]
//
//                for (String repo : repos) {
//                    // TODO Optimize here to do next step only in case sources have changed
//                    dir(repo) {
//                        git branch: '$GIT_BRANCH', url: 'https://github.com/logabit/' + repo + '.git', credentialsId: 'github'
//                    }
//                }
            }

//            stage('Build') {
//
//                sh('ls /app')
//
//                dir('pipeforce-build') {
//                    sh('python3 pi-build.py build,containerize pipeforce-service-hub -p ' +
//                            'build_home=/home/jenkins/agent/workspace/pipeforce-cli_master,' +
//                            'skip_phase=$skip_phase')
//                }
//            }
//
//            stage('Deploy') {
//
//                dir('pipeforce-build') {
//                    sh('python3 pi-build.py deploy $namespace:pipeforce-service-hub -p ' +
//                            'build_home=/home/jenkins/agent/workspace/pipeforce-cli_master,' +
//                            'skip_phase=$skip_phase')
//                }
//            }
        }
    }
}
