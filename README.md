# Hibernate Ec2 Instances

This program scans the available regions and hibernates instances that are specified by regions timezone.

here are the crontab entries - the time are in EDT. 
``` shell
33 12 * * * nohup /home/ubuntu/hibernate IST >>nohup.out.IST 2>&1
3 17 * * * nohup /home/ubuntu/hibernate EMEA >>nohup.out.EMEA 2>&1
3 21 * * * nohup /home/ubuntu/hibernate US-EAST >>nohup.out.US-EAST 2>&1
3 1  * * * nohup /home/ubuntu/hibernate US-WEST >>nohup.out.US-WEST 2>&1
```

The instance must have hibernation enabled.  

The program accepts 4 values for `--region`:
* EMEA
* IST
* US-EAST
* US-WEST


