def call(body) {
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    def versionString = config.version
    def imageName = config.name

    def registryUrl = config.registryUrl ?: "registry.silvenga.com"
    def registryCredential = config.registryCredential ?: "registry"

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

        stage("Build") {
            echo "Building ${imageName} with version ${version}."
            image = docker.build("${registryUrl}/${imageName}:${version}")
        }

        stage("Publish") {
            docker.withRegistry("https://${registryUrl}", registryCredential) {
                image.push()
                image.push("latest")
            }
        }
    }
}