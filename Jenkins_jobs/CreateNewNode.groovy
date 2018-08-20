@Library('NodeHelper') _
import hudson.slaves.CommandLauncher;
import hudson.plugins.sshslaves.SSHLauncher
import hudson.model.Node.Mode;
import hudson.plugins.sshslaves.verifiers.NonVerifyingKeyVerificationStrategy;
import jenkins.model.Jenkins;
import hudson.model.Computer;
import org.jenkinsci.plugins.scriptsecurity.scripts.ScriptApproval;
import hudson.model.Slave;
import hudson.slaves.JNLPLauncher;

node {
    stage('AddNewNode') {
        def nodeHelper = new NodeHelper();

        String[] machines = params.machineNames.split(",")
        String[] machineIPs = params.machineIPs.split(",")
        String[] labels = params.labelStrings.split(",")
        def FILES = []
        git url: "git@github.ibm.com:Xiaoxiao-Wang/infra-can.git", credentialsId: Constants.SSH_CREDENTIAL_ID, branch:"INI"
        FILES = sh (
                    script: "cat ansible/playbooks/Infrastructure/dynamic_inventory.ini",
                    returnStdout: true
                ).trim()
        def FILES_LIST = FILES.split("\\n")
        for (machine in FILES_LIST){ 
            def EXISTED = false;
            def configMap = [:];
            def TruncComment = "";
            if (machine =~ /[^\s]+ \s*(\S+)\s*=\s*(.*)?(?:#|$)\S*/) {
                if (machine.contains("#")) {
                    def location = machine.indexOf("#")
                    TruncComment = machine.substring(location + 1,machine.length())
                    def splitLines = TruncComment.split(',')
                    splitLines.each {
                        if (it != "" && it != null){
                            String[] parts = it.split("=",2);
                            configMap.put(parts[0],parts[1])
                        }
                    }
                // Original: ub14x64cudart1.canlab.ibm.com ansible_host=9.24.248.175 # NVidia Cuda
                // TruncComment ub14x64cudart1.canlab.ibm.com ansible_host=9.24.248.175

                for (aSlave in hudson.model.Hudson.instance.slaves) {
                    if (machine.contains(aSlave.name)){
                    EXISTED = true;

                    if (configMap.get("Labels") != null && configMap.get("Labels") != aSlave.getLabelString()) {
                        aSlave.setLabelString(configMap.get("Labels"))
                    }

                    if (configMap.get("Executor") != null && configMap.get("Executor") != aSlave.getNumExecutors()) {
                        aSlave.setNumExecutors(configMap.get("Executor").toInteger())
                    }

                    if (configMap.get("Description")!= null && configMap.get("Description") != aSlave.getDescriptor()){
                        aSlave.setNodeDescription(configMap.get("Description"))
                    }
                    break;
                }
                }
                def name = "";
                if (!EXISTED){
                    machine.find (/\S+.ibm.com/){
                        def location = it.indexOf(".")
                        it = it.substring(0,location)
                        name = it
                    }
                    machines = machines + "${name}"
                    // if machine in the ini is not on Jenkins, then start the new machine by setting its config according to the ini file
                    def hostLocation = machine.indexOf("=")
                    def machineIP
                    if (machine.contains("#")) {
                        def location = machine.indexOf("#")
                        machineIP = machine.substring(hostLocation + 1,location -1)
                    } else {
                        location = machine.length()
                        machineIP = machine.substring(hostLocation + 1,location)
                    }
                    
                    machineIPs = machineIPs + machineIP
                    if (configMap.get("Labels") != null) {
                        labels = labels + configMap.get("Labels");
                    }
                }
            }
        }

        def launcher
        String remoteFS
        String newMachineLabels
        String os
        String newMachineName
        for (int index = 0; index < machineIPs.length; index++) {

            if (nodeHelper.getComputer(machines[index]) != null) {
                println "Machine: '${machines[index]}' already exists."
            } else {
                if (Integer.parseInt(machineIPs[index].split("\\.")[0]) == 10) {
                    launcher = new CommandLauncher(Constants.SSH_COMMAND + "${machineIPs[index]} " + "\"wget -q --no-check-certificate -O slave.jar ${JENKINS_URL}jnlpJars/slave.jar ; java -jar slave.jar\"");
                    remoteFS = Constants.REMOTE_FS;
                } else if (machines[index].contains("win")) {
                    launcher = new JNLPLauncher("", "", new jenkins.slaves.RemotingWorkDirSettings(false, "", "remoting", false));
                    remoteFS  = Constants.WIN_REMOTE_FS;
                } else {
                    launcher = new SSHLauncher(
                                machines[index],
                                22,
                                params.SSHCredentialId.isEmpty() ? Constants.SSH_CREDENTIAL_ID : params.SSHCredentialId,
                                null, null, null, null, null, null, null,
                                new NonVerifyingKeyVerificationStrategy());
                    if (machines[index].contains("osx")){
                        remoteFS = Constants.OSX_REMOTE_FS;
                    } else {
                        remoteFS = Constants.REMOTE_FS;
                    }
                }

                newMachineLabels = labels[index%labels.length]

                newMachineName = nodeHelper.addNewNode(
                        machines[index],
                        machineIPs[index],
                        remoteFS,
                        1, // Number of executers 
                        Mode.EXCLUSIVE,
                        newMachineLabels.toLowerCase(),
                        launcher
                        );

                // This part is to approve the script used to add a 10. machine
                def scripts  = ScriptApproval.get()
                def scriptSet = scripts.getPendingScripts()
                def iterator = scriptSet.iterator()
                if (launcher.getClass().toString().contains("slaves.CommandLauncher")) {
                    for (ScriptApproval.PendingScript script : scriptSet) {
                        if (script.script.contains(Constants.SSH_COMMAND + machineIPs[index])) {
                            println "Script Approved"
                            scripts.approveScript(script.getHash());
                        }
                    }
                   (Jenkins.getInstance().getComputer(newMachineName)).connect(false);
                }

                println "\nMachine:'${newMachineName}'\nlabels: '${newMachineLabels}'\nremote root directory: '${remoteFS}'\n";
            }
        }

    }
}
