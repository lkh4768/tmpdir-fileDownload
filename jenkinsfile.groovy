node {
	try {
        notifyBuild('STARTED')

		stage "Init"
			STAGE = "Init"
			checkout scm
			DEVELOP_BRANCH = "develop"
			MASTER_BRANCH = "master"
			sh "chmod +x gradlew"
			PACKAGE_NAME = sh script: "./gradlew properties | grep name | awk '{print \$2}' | tr -d '\\n'", returnStdout: true
			PACKAGE_NAME_LOW = sh script: "./gradlew properties | grep name | awk '{print \$2}' | tr -d '\\n' | sed -e 's/./\\L\\0/g'", returnStdout: true
			PACKAGE_VERSION = sh script: "./gradlew properties | grep version | awk '{print \$2}' | tr -d '\\n'", returnStdout: true 
			IMAGE_NAME = "tmpdir-${PACKAGE_NAME_LOW}-${PACKAGE_VERSION}"
			REGISTRY_HOST = env.docker_registry_host
			if(env.BRANCH_NAME == DEVELOP_BRANCH) ENV_PHASE = "stage"
			else ENV_PHASE = "prd"

		if(env.BRANCH_NAME == DEVELOP_BRANCH){
			stage "Unit testing"
				STAGE = "Unit testing"
				sh "./gradlew clean check"
				junit "build/test-results/test/TEST-*.xml"

			stage "Sonarqube"
				STAGE = "Sonarqube"
				withSonarQubeEnv("sonarqube") {
					withCredentials([string(credentialsId: 'sonarqube-token', variable: 'sonar_token')]) {
						sh "./gradlew --info sonarqube -Dsonar.host.url=${env.sonar_url} -Dsonar.login=${sonar_token}"
					}
				}
				sleep 30
				timeout(time: 1, unit: "MINUTES") {
					def qg = waitForQualityGate()
						if (qg.status != "OK") {
							error "Pipeline aborted due to quality gate failure: ${qg.status}"
						}
				}

			stage "Publish image"
				STAGE = "Publish image"
				sh "./gradlew build"
				withDockerRegistry([credentialsId: 'registry', url: "https://${REGISTRY_HOST}"]) {
					def image = docker.build("$REGISTRY_HOST/$IMAGE_NAME", "--build-arg PACKAGE_NAME=${PACKAGE_NAME} --build-arg PACKAGE_VERSION=${PACKAGE_VERSION} .")
					image.push()
				}

			stage "Deploy on stage"
				STAGE = "Deploy on stage"
				withCredentials([usernamePassword(credentialsId: 'registry', passwordVariable: 'docker_registry_pw', usernameVariable: 'docker_registry_user'), usernamePassword(credentialsId: 'ssh/docker', passwordVariable: 'ssh_docker_pw', usernameVariable: 'ssh_docker_user')]) {
					sh "sshpass -p '${ssh_docker_pw}' ssh -T -oStrictHostKeyChecking=no -p ${env.tmpdir_docker_port} ${ssh_docker_user}@${env.tmpdir_docker_host} \"docker rm -f ${IMAGE_NAME}-${ENV_PHASE} 2> /dev/null | echo ok && docker login -u ${REGISTRY_USER} -p ${REGISTRY_PASSWORD} ${REGISTRY_HOST} && docker pull ${REGISTRY_HOST}/${IMAGE_NAME} && docker run -d --network=tmpdir-${ENV_PHASE}-net -e ENV_PHASE='${ENV_PHASE}' -v /app/tmpdir-filedownloader-${ENV_PHASE}/config:/app/config -v /etc/letsencrypt:/app/certs -v /applog/tmpdir-filesdownloader-${ENV_PHASE}:/applog -v /db/tmpdir-${ENV_PHASE}/storage:/storage --name ${IMAGE_NAME}-${ENV_PHASE} ${REGISTRY_HOST}/${IMAGE_NAME}\""
				}
		}

		if(env.BRANCH_NAME == MASTER_BRANCH){
			stage "Deploy on product"
				STAGE = "Deploy on product"
				withCredentials([usernamePassword(credentialsId: 'registry', passwordVariable: 'docker_registry_pw', usernameVariable: 'docker_registry_user'), usernamePassword(credentialsId: 'ssh/docker', passwordVariable: 'ssh_docker_pw', usernameVariable: 'ssh_docker_user')]) {
					sh "sshpass -p '${ssh_docker_pw}' ssh -T -oStrictHostKeyChecking=no -p ${env.tmpdir_docker_port} ${ssh_docker_user}@${env.tmpdir_docker_host} \"docker rm -f ${IMAGE_NAME}-${ENV_PHASE} 2> /dev/null | echo ok && docker login -u ${REGISTRY_USER} -p ${REGISTRY_PASSWORD} ${REGISTRY_HOST} && docker pull ${REGISTRY_HOST}/${IMAGE_NAME} && docker run -d --network=tmpdir-${ENV_PHASE}-net -e ENV_PHASE='${ENV_PHASE}' -v /app/tmpdir-filedownloader-${ENV_PHASE}/config:/app/config -v /etc/letsencrypt:/app/certs -v /applog/tmpdir-filedownloader-${ENV_PHASE}:/applog -v /db/tmpdir-${ENV_PHASE}/storage:/storage --name ${IMAGE_NAME}-${ENV_PHASE} ${REGISTRY_HOST}/${IMAGE_NAME}\""
				}
		}
	} catch (e) {
		currentBuild.result = "FAILED"
		throw e
	} finally {
		notifyBuild(currentBuild.result, STAGE)
	}
}

def notifyBuild(String buildStatus = 'STARTED', String stage = '') {
  buildStatus =  buildStatus ?: 'SUCCESSFUL'

  def colorName = 'RED'
  def colorCode = '#FF0000'
  def subject = "${buildStatus}: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'"
  if(stage != '' && buildStatus == 'FAILED') subject += ", Stage '${stage}'"
  def summary = "${subject} (${env.BUILD_URL})"

  if (buildStatus == 'STARTED') {
    color = 'YELLOW'
    colorCode = '#FFFF00'
  } else if (buildStatus == 'SUCCESSFUL') {
    color = 'GREEN'
    colorCode = '#00FF00'
  } else {
    color = 'RED'
    colorCode = '#FF0000'
  }

  slackSend (color: colorCode, message: summary)
}
