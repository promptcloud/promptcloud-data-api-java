#PromptCloud-data-api-java

This is PromptCloud's (http://promptcloud.com) data API in java. It can be used to fetch the client specific data from PromptCloud data api.

NOTE: API query  requires a valid userid and password for api_version "v1" and requires userid and client authentication key for api_version "v2".

For queries related to this gem please contact the folks at promptcloud or open a github issue.

## Installation
* Download latest jar form https://github.com/promptcloud/promptcloud-data-api-java/releases
* And install java (version >= "1.7.0")

## Usage

Access using Command line:

To get help -

    java -jar get_promptcloud_data-<api_version> -h 

To download data -

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
