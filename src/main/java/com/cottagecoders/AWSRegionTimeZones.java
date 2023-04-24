package com.cottagecoders;

import java.util.HashMap;
import java.util.Map;

public class AWSRegionTimeZones {

  public static Map<String, String> regionTz = new HashMap<>();

  public AWSRegionTimeZones() {

    regionTz.put("ap-east-1", "IST");
    regionTz.put("ap-northeast-1", "IST");
    regionTz.put("ap-northeast-2", "IST");
    regionTz.put("ap-northeast-3", "IST");
    regionTz.put("ap-southeast-1", "IST");
    regionTz.put("ap-southeast-2", "IST");
    regionTz.put("ap-south-1", "IST");

    regionTz.put("cn-northwest-1", "IST");

    regionTz.put("eu-central-1", "EMEA");
    regionTz.put("eu-north-1", "EMEA");
    regionTz.put("eu-west-1", "EMEA");
    regionTz.put("eu-west-2", "EMEA");
    regionTz.put("eu-west-3", "EMEA");
    regionTz.put("eu-south-1", "EMEA");

    regionTz.put("sa-east-1", "US-EAST");

    regionTz.put("ca-central-1", "US-EAST");

    regionTz.put("us-east-1", "US-EAST");
    regionTz.put("us-east-2", "US-EAST");

    regionTz.put("us-west-1", "US-WEST");
    regionTz.put("us-west-2", "US-WEST");
  }

  String fetchRegionsTZ(String region) {
    return regionTz.get(region);
  }
}
