# openjdk-jenkins-helper

## Files
### API Functions (NodeHelper.groovy)
The NodeHelper API contains helper functions to query basic machine stats in real time, add new machines, update/overwrite labels.
* Get CPU count ```String getCpuCount(String computerName)```
* Get Installed memory ```Tuple  getMemory(String computerName)```
* Get OS information ```Tuple  getOsInfo(String computerName)```
* Get Kernel information ```Tuple  getOsKernelInfo(String computerName)```
* Get Endian ```String getEndian(String computerName)```
* Get Location ```String getLocation(String computerName)```
* Get Description ```String getDescription(String computerName)```
* Get Home directory ```String getHomeDirectoryPath(String computerName)```
* Get free space in the home directory ```String getSpaceLeftInGb(String computerName)```
* Get Total installed disk space ```String getTotalSpace(String computerName)```
* Get Labels ```String getLabels(String computerName)``` 
* Add new Node ```String addNewNode(String newNodeName,String newNodeDescription,String newNodeRemoteFS,int newNodeNumExecutors,Mode newNodeMode,String newNodeLabelString,def launcher)```
* Add Label ```String addLabel(String computerName, String label)```
* Append Label ```String appendlabel(String computerName, String label)```

### Space Monitoring (WorkspaceInfo.groovy)
Iterates over online nodes on Jenkins and prints the contents of the workspace directory along with the space they occupy
* The computers it iterates over can be limited by input parameter, ```projectLabel```
* As of now, it only works for linux, aix, and mac

### Create New Node (CreateNewNode.groovy)
Used to create new nodes with any basic labels

* This job expects 3 parameters
    * ```String machineNames```
        * Comma seperated host names of the machine(s)
    * ```String machineIPs```
        * Comma seperated IP address of the machine(s)
    * ```String labelStrings```
        * Labels you would like to be added to the machine.
        * Each label must be separated by spaces and labels for different machines must be separated by `,`
        * If identical labels need to be applied to all the machines, only one set of labels need to be supplied

## How-to

### Setup
[Here](https://jenkins.io/doc/book/pipeline/shared-libraries/) are the instructions on how to add the library to Jenkins

### Usuage
To use the API in a Jenkins job you'll need to add ```@Library('NodeHelper') _``` to import the API.

## TODOs
* A job that pulls machine list from a inventory file and validates the names with those already on Jenkins. It also adds any labels mentioned in the inventory file to the machine on Jenkins, [issue](https://github.com/AdoptOpenJDK/openjdk-jenkins-helper/issues/10)
* CPU Information for a ZOS, [issue](https://github.com/AdoptOpenJDK/openjdk-jenkins-helper/issues/9)
* Logic to get Hypervisor information, [issue](https://github.com/AdoptOpenJDK/openjdk-jenkins-helper/issues/4)
* Move strings out to a config file, [issue](https://github.com/AdoptOpenJDK/openjdk-jenkins-helper/issues/2)