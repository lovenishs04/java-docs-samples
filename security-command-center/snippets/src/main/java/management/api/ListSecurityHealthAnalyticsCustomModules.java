/*
 * Copyright 2024 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package management.api;

// [START securitycenter_list_security_health_analytics_custom_module]
import com.google.cloud.securitycentermanagement.v1.ListSecurityHealthAnalyticsCustomModulesRequest;
import com.google.cloud.securitycentermanagement.v1.SecurityCenterManagementClient;
import com.google.cloud.securitycentermanagement.v1.SecurityCenterManagementClient.ListSecurityHealthAnalyticsCustomModulesPagedResponse;
import com.google.cloud.securitycentermanagement.v1.SecurityHealthAnalyticsCustomModule;
import java.io.IOException;

public class ListSecurityHealthAnalyticsCustomModules {

  public static void main(String[] args) throws IOException {
    // https://cloud.google.com/security-command-center/docs/reference/security-center-management/rest/v1/organizations.locations.securityHealthAnalyticsCustomModules/list
    // parent: Use any one of the following options:
    // - organizations/{organization_id}/locations/{location_id}
    // - folders/{folder_id}/locations/{location_id}
    // - projects/{project_id}/locations/{location_id}
    String parent = String.format("projects/%s/locations/%s", "project_id", "global");

    listSecurityHealthAnalyticsCustomModules(parent);
  }

  public static ListSecurityHealthAnalyticsCustomModulesPagedResponse
      listSecurityHealthAnalyticsCustomModules(String parent) throws IOException {
    // Initialize client that will be used to send requests. This client only needs
    // to be created
    // once, and can be reused for multiple requests.
    try (SecurityCenterManagementClient client = SecurityCenterManagementClient.create()) {

      // create the request
      ListSecurityHealthAnalyticsCustomModulesRequest request =
          ListSecurityHealthAnalyticsCustomModulesRequest.newBuilder().setParent(parent).build();

      // calls the API
      ListSecurityHealthAnalyticsCustomModulesPagedResponse response =
          client.listSecurityHealthAnalyticsCustomModules(request);

      // List all security health analytics custom modules present in the resource
      for (SecurityHealthAnalyticsCustomModule module : response.iterateAll()) {
        System.out.println("Custom module name : " + module.getDisplayName());
      }
      return response;
    }
  }
}
// [END securitycenter_list_security_health_analytics_custom_module]
