/*
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

     https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
import jenkins.model.Jenkins;

class JobHelper {

    /**
     * Determines if a job with the given name exists and is runnable
     * @param jobName
     * @return
     */
    public static boolean jobIsRunnable(String jobName) {
        return Jenkins.getInstance().getAllItems()
                .findAll { job ->
            job.fullName == jobName && !job.isDisabled()
        }.size() > 0;
    }

    /**
    * Determines if a job is currently running
    * @param jobName
    * @return
    */
    public static boolean jobIsRunning(String jobName) {
        return Jenkins.getInstance().getAllItems()
            .findAll { job -> 
                job.fullName == jobName && (job.isBuilding() || job.isInQueue())
            }.size() > 0;
    }
}
