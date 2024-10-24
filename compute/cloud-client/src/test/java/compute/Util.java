/*
 * Copyright 2022 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package compute;

import com.google.api.gax.rpc.NotFoundException;
import com.google.cloud.compute.v1.AggregatedListInstancesRequest;
import com.google.cloud.compute.v1.Disk;
import com.google.cloud.compute.v1.DisksClient;
import com.google.cloud.compute.v1.Instance;
import com.google.cloud.compute.v1.Instance.Status;
import com.google.cloud.compute.v1.InstanceTemplate;
import com.google.cloud.compute.v1.InstanceTemplatesClient;
import com.google.cloud.compute.v1.InstanceTemplatesClient.ListPagedResponse;
import com.google.cloud.compute.v1.InstancesClient;
import com.google.cloud.compute.v1.InstancesClient.AggregatedListPagedResponse;
import com.google.cloud.compute.v1.InstancesScopedList;
import com.google.cloud.compute.v1.ListInstanceTemplatesRequest;
import com.google.cloud.compute.v1.RegionDisksClient;
import com.google.cloud.compute.v1.Reservation;
import com.google.cloud.compute.v1.ReservationsClient;
import compute.reservation.DeleteReservation;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.stream.IntStream;

public abstract class Util {
  // Cleans existing test resources if any.
  // If the project contains too many instances, use "filter" when listing
  // resources
  // and delete the listed resources based on the timestamp.

  private static final int DELETION_THRESHOLD_TIME_HOURS = 24;
  // comma separate list of zone names
  private static final String TEST_ZONES_NAME = "JAVA_DOCS_COMPUTE_TEST_ZONES";
  private static final String DEFAULT_ZONES = "us-central1-a,us-west1-a,asia-south1-a";

  // Delete templates which starts with the given prefixToDelete and
  // has creation timestamp >24 hours.
  public static void cleanUpExistingInstanceTemplates(String prefixToDelete, String projectId)
      throws IOException, ExecutionException, InterruptedException, TimeoutException {
    for (InstanceTemplate template : listFilteredInstanceTemplates(projectId, prefixToDelete)
        .iterateAll()) {
      if (!template.hasCreationTimestamp()) {
        continue;
      }
      if (template.getName().contains(prefixToDelete)
          && isCreatedBeforeThresholdTime(template.getCreationTimestamp())
          && template.isInitialized()) {
        DeleteInstanceTemplate.deleteInstanceTemplate(projectId, template.getName());
      }
    }
  }

  // Delete instances which starts with the given prefixToDelete and
  // has creation timestamp >24 hours.
  public static void cleanUpExistingInstances(String prefixToDelete, String projectId,
      String instanceZone)
      throws IOException, ExecutionException, InterruptedException, TimeoutException {
    for (Map.Entry<String, InstancesScopedList> instanceGroup : listFilteredInstances(
        projectId, prefixToDelete).iterateAll()) {
      for (Instance instance : instanceGroup.getValue().getInstancesList()) {
        if (!instance.hasCreationTimestamp()) {
          continue;
        }
        if (instance.getName().contains(prefixToDelete)
            && isCreatedBeforeThresholdTime(instance.getCreationTimestamp())
            && instance.getStatus().equalsIgnoreCase(Status.RUNNING.toString())) {
          DeleteInstance.deleteInstance(projectId, instanceZone, instance.getName());
        }
      }
    }
  }

  public static AggregatedListPagedResponse listFilteredInstances(
      String project, String instanceNamePrefix) throws IOException {
    try (InstancesClient instancesClient = InstancesClient.create()) {

      AggregatedListInstancesRequest aggregatedListInstancesRequest = AggregatedListInstancesRequest
          .newBuilder()
          .setProject(project)
          .setFilter(String.format("name:%s", instanceNamePrefix))
          .build();

      return instancesClient
          .aggregatedList(aggregatedListInstancesRequest);
    }
  }

  public static ListPagedResponse listFilteredInstanceTemplates(
      String projectId, String instanceTemplatePrefix) throws IOException {
    try (InstanceTemplatesClient instanceTemplatesClient = InstanceTemplatesClient.create()) {
      ListInstanceTemplatesRequest listInstanceTemplatesRequest =
          ListInstanceTemplatesRequest.newBuilder()
              .setProject(projectId)
              .setFilter(String.format("name:%s", instanceTemplatePrefix))
              .build();

      return instanceTemplatesClient.list(listInstanceTemplatesRequest);
    }
  }

  public static boolean isCreatedBeforeThresholdTime(String timestamp) {
    return OffsetDateTime.parse(timestamp).toInstant()
        .isBefore(Instant.now().minus(DELETION_THRESHOLD_TIME_HOURS, ChronoUnit.HOURS));
  }

  public static String getBase64EncodedKey() {
    String sampleSpace = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    StringBuilder stringBuilder = new StringBuilder();
    SecureRandom random = new SecureRandom();
    IntStream.range(0, 32)
        .forEach(
            x -> stringBuilder.append(sampleSpace.charAt(random.nextInt(sampleSpace.length()))));

    return Base64.getEncoder()
        .encodeToString(stringBuilder.toString().getBytes(StandardCharsets.US_ASCII));
  }

  public static String getInstanceStatus(String project, String zone, String instanceName)
      throws IOException {
    try (InstancesClient instancesClient = InstancesClient.create()) {
      Instance response = instancesClient.get(project, zone, instanceName);
      return response.getStatus();
    }
  }

  public static Instance getInstance(String projectId, String zone, String machineName)
      throws IOException {
    try (InstancesClient instancesClient = InstancesClient.create()) {
      return instancesClient.get(projectId, zone, machineName);
    }
  }

  public static Disk getDisk(String projectId, String zone, String diskName) throws IOException {
    try (DisksClient disksClient = DisksClient.create()) {
      return disksClient.get(projectId, zone, diskName);
    }
  }

  public static Disk getRegionalDisk(String projectId, String region, String diskName)
      throws IOException {
    try (RegionDisksClient regionDisksClient = RegionDisksClient.create()) {
      return regionDisksClient.get(projectId, region, diskName);
    }
  }

  // Returns a random zone.
  public static String getZone() {
    String zones = getEnvVar(TEST_ZONES_NAME, DEFAULT_ZONES);
    String[] parsedZones = zones.split(",");
    if (parsedZones.length == 0) {
      return "unknown";
    }
    return parsedZones[new Random().nextInt(parsedZones.length)].trim();
  }

  public static String getEnvVar(String envVarName, String defaultValue) {
    String val = System.getenv(envVarName);
    if (val == null || val.trim() == "") {
      return defaultValue;
    }
    return val;
  }

  // Delete reservations which starts with the given prefixToDelete and
  // has creation timestamp >24 hours.
  public static void cleanUpExistingReservations(
      String prefixToDelete, String projectId, String zone)
      throws IOException, ExecutionException, InterruptedException, TimeoutException {
    try (ReservationsClient reservationsClient = ReservationsClient.create()) {
      for (Reservation reservation : reservationsClient.list(projectId, zone).iterateAll()) {
        if (!reservationsClient.list(projectId, zone).iterateAll().iterator().hasNext()) {
          break;
        }
        if (reservation.getName().contains(prefixToDelete)
            && isCreatedBeforeThresholdTime(reservation.getCreationTimestamp())) {
          try {
            DeleteReservation.deleteReservation(projectId, zone, reservation.getName());
          } catch (NotFoundException e) {
            System.err.println("Reservation not found, skipping deletion:" + reservation.getName());
          }
        }
      }
    }
  }
}
