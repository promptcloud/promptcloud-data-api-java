promptcloud-data-api-java
=========================
Latest version: 0.1
Dependency: 
	java, version >= "1.7.0"

Command to run: 
	java -jar get_promptcloud_data-<version> <arguments>

Usage: Main
 -a,--apiparam <arg>                 some valid data api param comma
                                     separated examle:
                                     days=1,filter=sitewiselatest
 -b,--bcp                            to use api.bcp.promptcloud.com
                                     instead of api.promptcloud.com
 -c,--apiconf <arg>                  APICONFIG FILE PATH, to override the
                                     config file location, APICONFIG file
                                     stores information like client_id,
                                     downloadir, previous timestamp file
 -d,--download_dir <arg>             DOWNLOADDIR, to override the download
                                     dir obtained from apiconf file
    --display_info                   display config info
 -h,--help                           show help.
 -i,--perform_initial_setup          perform initial setup
 -l,--loop                           to download new data files and keep
                                     looking for new one. i.e it doesn't
                                     exit, if no new feed is found it will
                                     sleep. minimun sleep time is 10 secs
                                     and max sleep time is 300 secs
 -n,--ignore_ssl_certificate         to ignore invalid ssl certificate
    --noloop                         to download new data files and and
                                     exit, this is the default behaviour
 -p,--pass <arg>                     PASSWORD, data api password
    --promptcloudhome <arg>          PROMPTCLOUDHOME, to override the
                                     promptcloudhome dir: ~/promptcloud
    --queried_timestamp_file <arg>    QUERIED TIME STAMP FILE PATH, to
                                     override default queried timestamp
                                     file, file that stores last queried
                                     timestamp
 -t,--timestamp <arg>                Local TIMESTAMP(nano time(integer)
                                     i.e. 1417069800000000000 or
                                     String(yyyy-MM-dd-hh:mm:ss.SSS-a)
                                     i.e. 2014-11-29-2:04:00.000-AM), to
                                     get files newer than or equal to
                                     given timestamp
 -u,--user <arg>                     USER, data api user id

User Help:
To perform initial setup,please pass --perform_initial_setup and/or --user <client_id>
To download data,please pass --user <client_id> and --pass <password>
Please note that client_id will be saved in config.yml but pass will not be.


FAQ:
1. Solution of "SSLHandshakeException" due to SSLHandshakeException due to invalid SSL certificate check.
Ans:  
 Best and safe solution: 
 Follow this post http://www.java-samples.com/showtutorial.php?tutorialid=210
 One issue might be raised while doing this -
 Final result:
 Certificate was added to keystore.
 keytool error: java.io.FileNotFoundException: C:\Program files\...jre\lib\cacerts(Access is Denied)
 Solution: Run all keytool commands as administrator(windiws) and as root/sudo(linux). More help http://stackoverflow.com/questions/10321211/java-keytool-error-after-importing-certificate-keytool-error-java-io-filenot

 OR

 Pass --ignore_ssl_certificate option to ignore invalid ssl certificate.
