podTemplate {
    node(POD_LABEL) {
        stage('Checkout') {
            dir('pipeforce-build') {
                git url: 'https://github.com/logabit/pipeforce-build.git'
            }
            dir('pipeforce-cli') {
                git url: 'https://github.com/logabit/pipeforce-cli.git'
            }
            dir('pipeforce-sdk-java') {
                git url: 'https://github.com/logabit/pipeforce-sdk-java.git'
            }
        }
    }
}
