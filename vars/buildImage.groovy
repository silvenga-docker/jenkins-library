def call(body) {
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    def versionString = config.version
    def imageName = config.name
    def workingPath = config.path ?: "."

    def registryUrl = config.registryUrl ?: "silvenga.azurecr.io"
    def registryCredential = config.registryCredential ?: "jenkins-silvenga.azurecr.io"

    def version = VersionNumber(versionNumberString: versionString)

    if (!versionString || !imageName) {
        currentBuild.rawBuild.result = Result.ABORTED
        throw new hudson.AbortException('The properties name or version were not set.')
    }

    node {
        stage("Checkout") {
            checkout scm
        }
        
        def image

        dir(workingPath) {
            stage("Build") {
                ansiColor('xterm') {
                    echo "Building ${imageName} with version ${version}."
                    image = docker.build("${registryUrl}/${imageName}:${version}", "--pull .")
                }
            }

            stage("Publish") {
                ansiColor('xterm') {
                    docker.withRegistry("https://${registryUrl}", registryCredential) {
                        image.push()
                        image.push("latest")
                    }
                }
            }
        }
    }
}