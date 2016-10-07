#PromptCloud-data-api-java

This is PromptCloud's (http://promptcloud.com) data API in java. It can be used to fetch the client specific data from PromptCloud data api.

NOTE: 
* API v1 query requires a valid userid and password.  
* API v2 query requires a valid userid and client authentication key.
* PromptCloud provides userid and password/authentication key to the client.
* If option --perform_initial_setup is provided along with other options, then initial setup will be performed(create conf file, download dir).
* If we do not pass any of --timestamp, --days, --hours and --minutes, then past 2 days data will be downloaded(default setting).

For queries related to this gem please contact the folks at promptcloud or open a github issue.

## API Help Links

API v1 - https://api.promptcloud.com/data/info?type=help

API v2 - https://api.promptcloud.com/v2/data/info?type=help

## Installation
* Download latest jar form https://github.com/promptcloud/promptcloud-data-api-java/releases
* And install java (version >= "1.7.0")

## Usage

### Access using Command line:

To get help -

    java -jar get_promptcloud_data-<version_of_jar_file> -h 

usage: Main
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
 -k,--client_auth_key <arg>          to pass the client authentication key
                                     if the api version is v2
 -l,--loop                           to download new data files and keep
                                     looking for new one. i.e it doesn't
                                     exit, if no new feed is found it will
                                     sleep. minimun sleep time is 10 secs
                                     and max sleep time is 300 secs
 -n,--ignore_ssl_certificate <arg>   to ignore invalid ssl certificate, by
                                     default ssl certificate is ignored,
                                     if you want enable it then pass -n
                                     false
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
 -v,--api_version <arg>              to pass the api version, Default is
                                     v2
    --version                        to show the version

User Help:
To perform initial setup,please pass --perform_initial_setup and/or --user <client_id>
To download data,please pass --user <client_id> and --pass <password>
Please note that client_id will be saved in config.yml but pass will not be.


## To download data -

	if api_version is "v2"(default) :-

    		java -jar get_promptcloud_data-<version_of_jar_file> --user <username> --client_auth_key <client_auth_key> [--timestamp <timestamp>] [--apiparam <api_param_comma_separated>]

		To ignore invalid ssl certificate - 

		Note :- This field is set to true by default, if you don't want to ignore ssl certificate please pass false.
    
    		java -jar get_promptcloud_data-<version_of_jar_file> --user <username> --client_auth_key <client_auth_key> --ignore_ssl_certificate [--timestamp <timestamp>] [--apiparam <api_param_comma_separated>] 
	
	if api_version is "v1" :-

    		java -jar get_promptcloud_data-<version_of_jar_file> --user <username> --pass <password> --api_version v1 [--timestamp <timestamp>] [--apiparam <api_param_comma_separated>]

		To ignore invalid ssl certificate - 

    		java -jar get_promptcloud_data-<api_version> --user <username> --pass <password> --api_version v1 --ignore_ssl_certificate [--timestamp <timestamp>] [--apiparam <api_param_comma_separated>] 



* Above command will put the downloaded files in ~/promptcloud/downloads
* Log file can be viewed at ~/promptcloud/log/*log
* Api config file at ~/promptcloud/configs/config.yml
* To override the downloaded file use option --download_dir <apidir full path>
* To override config dir use option --apiconf <apiconf full path>

In command line tool, if option --perform_initial_setup is provided along with other options, then initial setup will be performed (create conf file, download dir).

## Contributing
In order to contribute,

1. Fork it
2. Create your feature branch (`git checkout -b my-new-feature`)
3. Commit your changes (`git commit -am 'Add some feature'`)
4. Push to the branch (`git push origin my-new-feature`)
5. Create new Pull Request


## Note
Solution of "SSLHandshakeException" due to invalid SSL certificate check.  
 
    Best and safe solution: 
    Follow this post http://www.java-samples.com/showtutorial.php?tutorialid=210
    One issue might be raised while doing this -
    Final result:
    Certificate was added to keystore.
    keytool error: java.io.FileNotFoundException: C:\Program files\...jre\lib\cacerts(Access is Denied)
    Solution: Run all keytool commands as administrator(windiws) and as root/sudo(linux). More help http://stackoverflow.com/questions/10321211/java-keytool-error-after-importing-certificate-keytool-error-java-io-filenot

Or

    Pass --ignore_ssl_certificate option to ignore invalid ssl certificate.
