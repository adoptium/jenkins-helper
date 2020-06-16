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
import groovy.json.JsonSlurper;

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
    * Determines if a job is currently running or queued up
    * @param jobName
    * @return
    */
    public static boolean jobIsRunning(String jobName) {
        return Jenkins.get().getAllItems()
            .findAll { job -> 
                job.fullName == jobName && (job.isBuilding() || job.isInQueue())
            }.size() > 0;
    }

    /**
    * Gets the full folder of a job
    * @param jobName
    * @return
    */
    public static String getJobFolder(String jobName) {
        try {
          def foundJob
          Jenkins.get().getAllItems().each { job -> 
              if (job.fullName == jobName) {
                  foundJob = job.getFullDisplayName()
              }
          }
          return foundJob
        } catch (Exception e) {
          throw new RuntimeException("${e.getMessage()}")
        }
    }
     
    /**
    * Queries an api and returns the results as a JSON object
    * @param query
    * @param attempts
    * @param context
    * @return
    */
    private static queryApi(String query, Integer attempts, def context) {
        def jsonResponse = null
        def parser = new JsonSlurper()

        for (int count = 1; count <= attempts; count++) {
            try {
                def get = new URL(query).openConnection()
                get.setRequestProperty("User-Agent", "adopt-jenkins-helper")
                jsonResponse = parser.parseText(get.getInputStream().getText())

                // Successful response
                if (jsonResponse != null) {
                    context.println "[SUCCESS] We have a response!\n${jsonResponse}"
                    break
                } else {
                    context.println "[RETRYWARNING] API Request was successful but jsonResponse is null. Retrying in 30 seconds..."
                    context.sleep 30000
                }
            } catch (Exception e) {
                if (count == attempts) {
                    context.println "[ERROR] Query ${count} failed\nException: ${e}"
                    break 
                }
                
                context.println "[RETRYWARNING] Query ${count} failed\nException: ${e}\nRetrying in 30 seconds..."
                context.sleep 30000
            }
        }

        if (jsonResponse != null) {
            return jsonResponse
        } else {
            throw new RuntimeException("[ERROR] Failed to query or parse the ${query} endpoint")
        }
    }

    /**
    * Queries the Adopt API for all releases
    * @param context
    * @return
    */
    public static getAvailableReleases(def context) {
        return queryApi("https://api.adoptopenjdk.net/v3/info/available_releases", 5, context)
    }

}
