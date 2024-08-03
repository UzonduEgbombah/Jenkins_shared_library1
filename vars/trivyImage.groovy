def call() {
    sh 'trivy image uzondu/youtube:latest > trivyimage.txt'
}