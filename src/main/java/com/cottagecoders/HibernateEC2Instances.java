package com.cottagecoders;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.cottagecoders.simpleslack.FetchMembers;
import com.cottagecoders.simpleslack.SendSlackMessage;
import com.cottagecoders.simpleslack.userlist.Member;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import software.amazon.awssdk.services.ec2.model.HibernationOptions;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.InstanceStateChange;
import software.amazon.awssdk.services.ec2.model.Reservation;
import software.amazon.awssdk.services.ec2.model.StopInstancesRequest;
import software.amazon.awssdk.services.ec2.model.StopInstancesResponse;
import software.amazon.awssdk.services.ec2.model.Tag;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;


public class HibernateEC2Instances {
  private static final Logger LOG = LogManager.getLogger(HibernateEC2Instances.class);

  private static final String token = System.getenv("SLACKLIB_TOKEN");

  @Parameter(names = "--dryRun", description = "Dry run - don't make changes")
  private static boolean dryRun = false;

  @Parameter(names = "--region", description = "Region to process - one of IST, EMEA, US-EAST, US-WEST")
  private static String regionToCheck = "";

  @Parameter(names = {"--help"}, description = "This helpful information.")
  private static boolean help = false;


  public static void main(String[] args) {
    HibernateEC2Instances h = new HibernateEC2Instances();
    JCommander jc = JCommander.newBuilder().addObject(h).build();
    jc.parse(args);

    if (help || StringUtils.isEmpty(regionToCheck)) {
      jc.usage();
      return;
    }

    LOG.info("starting region {} {}", regionToCheck, new Date());
    h.run();
  }

  private void run() {
    FetchMembers fetch = new FetchMembers();
    SendSlackMessage ssm = new SendSlackMessage();

    try {
      Map<String, Member> members = fetch.fetchAllMembers();
      Member member = members.get("bobp");
      ssm.sendDM(member.getId(), String.format("Hello %s - checking  %s", member.getProfile().getRealName(), regionToCheck));
    } catch (IOException | InterruptedException ex) {
      LOG.error("exception sending message: {}", ex.getMessage(), ex);
      System.exit(3);
    }

    hibernate();
  }

  private void hibernate() {
    AWSRegionTimeZones regTz = new AWSRegionTimeZones();

    try (Ec2Client client = Ec2Client.builder().credentialsProvider(ProfileCredentialsProvider.create()).region(Region.US_EAST_2).build()) {

      for (software.amazon.awssdk.services.ec2.model.Region region : client.describeRegions().regions()) {
        if (!regTz.fetchRegionsTZ(region.regionName()).equalsIgnoreCase(regionToCheck)) {
          LOG.info("not checking region {}  {}/{}", region.regionName(), regTz.fetchRegionsTZ(region.regionName()), regionToCheck);
          continue;
        }

        LOG.info("checking region {}  {}/{}", region.regionName(), regTz.fetchRegionsTZ(region.regionName()), regionToCheck);
        Region r = Region.of(region.regionName());
        try (Ec2Client ec2 = Ec2Client.builder().credentialsProvider(ProfileCredentialsProvider.create()).region(r).build()) {

          DescribeInstancesResponse response;
          try {
            DescribeInstancesRequest request = DescribeInstancesRequest.builder().build();
            response = ec2.describeInstances(request);

          } catch (Ec2Exception | SdkClientException ex) {
            LOG.warn("SDK Exception {}", ex.getMessage(), ex);
            continue;
          }

          for (Reservation reservation : response.reservations()) {
            for (Instance instance : reservation.instances()) {

              if (instance.state().nameAsString().equalsIgnoreCase("running")) {
                HibernationOptions hibernate = instance.hibernationOptions();

                String name = "";
                String owner = "";
                boolean keep = false;
                if (hibernate.configured()) {
                  for (Tag tag : instance.tags()) {
                    if (tag.key().equalsIgnoreCase("Name")) {
                      name = tag.value();
                    }

                    if (tag.key().trim().equalsIgnoreCase("owner")) {
                      owner = tag.value();
                    }

                    if (tag.key().trim().equalsIgnoreCase("keep")) {
                      keep = true;
                    }
                  }

                  if (dryRun || keep) {
                    continue;
                  }

                  // hibernate here.
                  StopInstancesRequest stopInstance = StopInstancesRequest.builder().hibernate(true).instanceIds(instance.instanceId()).build();
                  StopInstancesResponse hiber = ec2.stopInstances(stopInstance);
                  List<InstanceStateChange> changes = hiber.stoppingInstances();
                  LOG.info("hibernate: {} region: {} state {} ssh key: {} launch time: {}",
                          instance.instanceId(),
                          instance.placement().availabilityZone(),
                          instance.state().nameAsString(),
                          instance.keyName(),
                          instance.launchTime()
                  );


                }
              }
            }
          }
        }
      }
    }
  }
}
