@Library('NodeHelper') _
import jenkins.model.Jenkins;
import hudson.model.Computer;

clones = [:]
List<String> newMachines = new ArrayList<String>();

/* Iterates over all the online nodes in Jenkins and
 * prints contents of workspace folder along with
 * the space they occupy. With exception of tmp directory
 */
node {
    stage('Print_Space_Monitoring_Data') {
        NodeHelper nodeHelper = new NodeHelper();

        String projectLabel = params.projectLabel;

        def currentInstance = Jenkins.getInstance();
        Computer[] computers;

        if (currentInstance != null) {
            computers = currentInstance.getComputers();
        }

        for (Computer computer : computers) {
            String machineName = computer.getName();

            if (computer.isOnline() && computer.getName() != "") {
                String kernelName = nodeHelper.getOsKernelInfo(computer.getName()).get(0).toLowerCase()

                if (nodeHelper.getLabels(computer.getName()).contains(projectLabel)) {
                
                    String workspaceDirectory = nodeHelper.getHomeDirectoryPath(machineName);

                    switch (kernelName) {
                        case 'linux':
                            clones[machineName] = {
                                node (machineName) {
                                    workspaceDirectory += "/workspace"

                                    /* #!/bin/sh -e\n is there to keep the output to console as clean as
                                     * possible
                                     */
                                    String workspaceStats = sh (
                                            script: '#!/bin/sh -e\n' + 'du -sh ' + workspaceDirectory,
                                            returnStdout: true).trim()

                                    String subdirectories = sh (
                                            script: '#!/bin/sh -e\n du -sh ' + workspaceDirectory + '/* | sort -h',
                                            returnStdout: true).trim()

                                    println beautify(machineName,workspaceDirectory, workspaceStats,subdirectories)

                                    cleanWs()
                                }
                            }
                            break;
                        case 'aix':
                            clones[machineName] = {
                                node (machineName) {
                                    workspaceDirectory += "/workspace"
                                    
                                    String workspaceStats = sh (
                                            script: '#!/bin/sh -e\n' + 'du -sg ' + workspaceDirectory,
                                            returnStdout: true).trim()

                                    String subdirectories = sh (
                                            script: '#!/bin/sh -e\n du -sg ' + workspaceDirectory + '/* | sort -n',
                                            returnStdout: true).trim()

                                    println beautify(machineName,workspaceDirectory, workspaceStats,subdirectories)

                                    cleanWs()
                                }
                            }
                            break;
                        case 'mac':
                            clones[machineName] = {
                                node (machineName) {
                                    workspaceDirectory += "/workspace"

                                    String workspaceStats = sh (
                                            script: '#!/bin/sh -e\n' + 'du -sh ' + workspaceDirectory,
                                            returnStdout: true).trim()

                                    String subdirectories = sh (
                                            script: '#!/bin/sh -e\n du -sh ' + workspaceDirectory + '/* | sort -n',
                                            returnStdout: true).trim()

                                    println beautify(machineName,workspaceDirectory, workspaceStats,subdirectories)

                                    cleanWs()
                                }
                            }
                            break;
                        /* This is commented out because it takes way too long to return 
                         * it isn't a top priority
                         * dir /s /-c
                         */
                        case 'windows':
                        //     clones[machineName] = {
                        //         node(machineName) {
                        //                 cleanWs()
                        //                 NodeHelper nodeHelper = new NodeHelper();
                        //                 workspaceDirectory += "\\workspace";
                        //                 println workspaceDirectory
                        //                 // TODO: get path to directory from the api
                        //                 String workspaceStats = sh (
                        //                         script: '#!/bin/sh -e\n'  'du -sh '  workspaceDirectory,
                        //                         returnStdout: true).trim()

                        //                 String script = 'set x; du -sh '  workspaceDirectory  '\\* | sort -rn';
                        //                 String subdirectories = sh (
                        //                         script: 'script',
                        //                         returnStdout: true).trim()
                                 
                                 
                        //                 println beautify(machineName,workspaceDirectory, workspaceStats,subdirectories);                                 
                                 
                        //             cleanWs()
                        //             }
                        //         }
                        //     }
                        //     break;
                        case 'zos':
                        default:
                            println ("Support for ${kernelName} is yet to be implemented");
                            break;
                    }
                }
            }
        }
        currentInstance = null;
        computers = null;

        cleanWs()
    }
}

@NonCPS
def beautify(machineName, workspaceDirectory, workspaceStats, subdirectories) {
    workspaceStats = workspaceStats.replaceAll("\\s+", "        ");
    subdirectories = subdirectories.replace(workspaceDirectory + "/", "");
    String output = "\n\n=======================================================================\n";
    output += "Disk stats for ${machineName}"
    output += "\nWorkspace:\n${workspaceStats}";
    def subdirectoriesArray = subdirectories.split("\n");
    for (String line : subdirectoriesArray) {
        if (!line.contains("tmp")) {
            line = line.replaceAll("\\s+", "    ")
            output += "\n    ${line}";
        }
    }
    output += "\n=======================================================================\n\n";
}

parallel clones