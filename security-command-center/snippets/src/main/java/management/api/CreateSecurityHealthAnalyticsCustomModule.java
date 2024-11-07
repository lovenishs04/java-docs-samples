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

// [START securitycenter_management_api_create_security_health_analytics_custom_module]
import com.google.cloud.securitycentermanagement.v1.CreateSecurityHealthAnalyticsCustomModuleRequest;
import com.google.cloud.securitycentermanagement.v1.CustomConfig;
import com.google.cloud.securitycentermanagement.v1.CustomConfig.ResourceSelector;
import com.google.cloud.securitycentermanagement.v1.CustomConfig.Severity;
import com.google.cloud.securitycentermanagement.v1.SecurityCenterManagementClient;
import com.google.cloud.securitycentermanagement.v1.SecurityHealthAnalyticsCustomModule;
import com.google.cloud.securitycentermanagement.v1.SecurityHealthAnalyticsCustomModule.EnablementState;
import com.google.type.Expr;
import java.io.IOException;

public class CreateSecurityHealthAnalyticsCustomModule {

  public static void main(String[] args) throws IOException {
    // https://cloud.google.com/security-command-center/docs/reference/security-center-management/rest/v1/organizations.locations.securityHealthAnalyticsCustomModules/create
    // parent: Use any one of the following options:
    // - organizations/{organization_id}/locations/{location_id}
    // - folders/{folder_id}/locations/{location_id}
    // - projects/{project_id}/locations/{location_id}
    String parent = String.format("projects/%s/locations/%s", "project_id", "global");

    // display name of the module, update with your name
    String customModuleDisplayName = "custom_module_display_name";

    createSecurityHealthAnalyticsCustomModule(parent, customModuleDisplayName);
  }

  // Creates a custom module under the security health analytics service.
  public static SecurityHealthAnalyticsCustomModule createSecurityHealthAnalyticsCustomModule(
      String parent, String customModuleDisplayName) throws IOException {

    // Initialize client that will be used to send requests. This client only needs
    // to be created
    // once, and can be reused for multiple requests.
    try (SecurityCenterManagementClient client = SecurityCenterManagementClient.create()) {

      String name =
          String.format("%s/securityHealthAnalyticsCustomModules/%s", parent, "custom_module");

      // define the CEL expression here, change it according to the your requirements
      Expr expr =
          Expr.newBuilder()
              .setExpression(
                  "has(resource.rotationPeriod) && (resource.rotationPeriod > "
                      + "duration('2592000s'))")
              .build();

      // define the resource selector
      ResourceSelector resourceSelector =
          ResourceSelector.newBuilder()
              .addResourceTypes("cloudkms.googleapis.com/CryptoKey")
              .build();

      // define the custom module configuration, update the severity, description,
      // recommendation below
      CustomConfig customConfig =
          CustomConfig.newBuilder()
              .setPredicate(expr)
              .setResourceSelector(resourceSelector)
              .setSeverity(Severity.MEDIUM)
              .setDescription("add your description here")
              .setRecommendation("add your recommendation here")
              .build();

      // define the security health analytics custom module configuration, update the
      // EnablementState below
      SecurityHealthAnalyticsCustomModule securityHealthAnalyticsCustomModule =
          SecurityHealthAnalyticsCustomModule.newBuilder()
              .setName(name)
              .setDisplayName(customModuleDisplayName)
              .setEnablementState(EnablementState.ENABLED)
              .setCustomConfig(customConfig)
              .build();

      // create the request
      CreateSecurityHealthAnalyticsCustomModuleRequest request =
          CreateSecurityHealthAnalyticsCustomModuleRequest.newBuilder()
              .setParent(parent)
              .setSecurityHealthAnalyticsCustomModule(securityHealthAnalyticsCustomModule)
              .build();

      // calls the API
      SecurityHealthAnalyticsCustomModule response =
          client.createSecurityHealthAnalyticsCustomModule(request);

      return response;
    }
  }
}
// [END securitycenter_management_api_create_security_health_analytics_custom_module]
