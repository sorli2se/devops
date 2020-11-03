def ci_job_name = "ci-job-cts-webserver"
def cd_job_name = "cd-job-cts-webserver"

freeStyleJob(ci_job_name) {

    logRotator(-1, 30)

    properties{
        copyArtifactPermissionProperty {
            projectNames(cd_job_name)
        }
    }

    parameters {
        runParam('SERVICE', "cts-webserver", "", 'SUCCESSFUL')
        runParam('K8S_CONFIG', "kubernetes-config", "", 'SUCCESSFUL')
    }

    steps {
        copyArtifacts('${SERVICE_JOBNAME}') {
            includePatterns('metadata.txt')
            targetDirectory('artifacts/${SERVICE_JOBNAME}')
            buildSelector {
                buildNumber('${SERVICE_NUMBER}')
            }
        }

        copyArtifacts('${K8S_CONFIG_JOBNAME}') {
            includePatterns('metadata.txt')
            targetDirectory('artifacts/${K8S_CONFIG_JOBNAME}')
            buildSelector {
                buildNumber('${K8S_CONFIG_NUMBER}')
            }
        }

        shell('''
export graph_file=${SERVICE_JOBNAME}_${BUILD_NUMBER}.yaml
echo $graph_file
rm -f $graph_file

# Generate new graph for service
cat artifacts/$SERVICE_JOBNAME/metadata.txt >> $graph_file
cat artifacts/$K8S_CONFIG_JOBNAME/metadata.txt >> $graph_file
''')
    }

    publishers {
        archiveArtifacts{
            pattern('${SERVICE_JOBNAME}_${BUILD_NUMBER}.yaml')
            onlyIfSuccessful()
            fingerprint()
        }
        postBuildTask {
            trigger(cd_job_name) {
                block {
                    buildStepFailure('FAILURE')
                    failure('FAILURE')
                    unstable('UNSTABLE')
                }
                parameters {
                    predefinedProp('CI_JOB', '$JOB_NAME')
                }
            }
        }
    }
}
