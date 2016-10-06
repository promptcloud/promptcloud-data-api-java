// net classes
import java.net.URI;
import java.net.URL;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
// io classes
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
// security classes
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
// text classes
import java.text.SimpleDateFormat;
// util classes
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TimeZone;
import java.util.zip.GZIPInputStream;
// javax classes
import javax.net.ssl.SSLHandshakeException;
// cli classes
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
// http classes
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
// json classes
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.json.simple.JSONArray;
// yml classes
import org.yaml.snakeyaml.Yaml;



class GetPromptCloudData {
	public static void main(String args[]) {
		try {
			HashMap options = (new PromptCloudApiArgParser(args)).parse();
			if(options.get("version")!=null) {
				System.out.println("Version: 0.1");
				System.exit(1);
			}
			
			PromptCloudDataAPI promptCloudDataAPI = new PromptCloudDataAPI(options);
			PromptCloudDataAPITimer timer = new PromptCloudDataAPITimer();

			if( options.get("display_info") != null ) { 
				promptCloudDataAPI.display_info(options);
			}
			else if( options.get("perform_initial_setup") != null ) { 
				System.out.println("Initial setup is started.");
				boolean status = promptCloudDataAPI.perform_initial_setup(options);
				if(status) {
					System.out.println("Initial setup is done.");
				}
				else {
					System.out.println("Initial setup is failed.");
				}
			}
			else { 
				if( options.get("loop") != null ) {
					while(true) {
						boolean new_feed_exists = promptCloudDataAPI.download_files(options);
						if( new_feed_exists == false ) {
							timer.do_wait();
						}
					}
				}
				else {
					promptCloudDataAPI.download_files(options);
				}
			}
		}catch(UserAndPassException e) {
			System.out.println(e.getMessage());
		}catch(Exception e) {	
			System.out.println(e.getMessage());
			e.printStackTrace(System.out);
		}	
	}
}

class PromptCloudDataAPI {
	static String sysPathSep = "/";
	static String userhome = System.getProperty("user.home");
	static String promptcloudhome = userhome+sysPathSep+"promptcloud";
	static String client_id = null;
	static String client_auth_key = null;
	static String password = null;
	long api_downtime = 0;

	PromptCloudDataAPI(HashMap options) {	
		boolean status = perform_initial_setup(options);
		if(!status) {
			System.out.println("Initial setup is failed.");
			System.exit(1);
		}
	}

	void display_info(HashMap options) {
		try {
			String apiconf = promptcloudhome+sysPathSep+"configs"+sysPathSep+"config.yml";
			if( options.get("apiconf") != null ) {
				apiconf = (String)options.get("apiconf");
			}
			File apiconf_file = new File(apiconf);
			if( apiconf_file.exists() && !apiconf_file.isDirectory() ) {
				Yaml yaml = new Yaml();
				HashMap conf_hash = (HashMap)yaml.load(new FileReader(apiconf));
				Iterator it = conf_hash.entrySet().iterator();
				while (it.hasNext()) {
					Map.Entry pairs = (Map.Entry)it.next();
					System.out.println(pairs.getKey() + ": " + pairs.getValue());
				}
			}
			else {
				System.out.println("Config file "+apiconf+" doesn't exist");
			}
		}catch(Exception e) {
			e.printStackTrace(System.out);
		}
	}

	boolean perform_initial_setup(HashMap options) {
		boolean perform_initial_setup_status = false;
		try {
			if( options.get("promptcloudhome") != null ) {
				promptcloudhome = (String)options.get("promptcloudhome");
			}
			File promptcloudhome_dir = new File(promptcloudhome);
			if( ! promptcloudhome_dir.isDirectory() ) {
				promptcloudhome_dir.mkdirs();
			}
			String download_dir = promptcloudhome+sysPathSep+"downloads";
			if( options.get("download_dir") != null ) {
				download_dir = (String)options.get("download_dir");
			}
			if( options.get("apiconf") == null ) {
				options.put("apiconf",promptcloudhome+sysPathSep+"configs"+sysPathSep+"config.yml");
			}
			String configs_file_path = (String)options.get("apiconf");
			File configs_file = new File(configs_file_path);
			File configs_dir = configs_file.getParentFile();
			if(!configs_dir.isDirectory()) {
				configs_dir.mkdirs();
			}
			if( !configs_file.exists() ) {
				String client_id = (String)options.get("user");
				if( client_id==null || client_id.equals("")) {
					System.out.println("Could not find config file: "+configs_file_path);
					System.out.print("Please enter your user id(for example if you use url https://api.promptcloud.com/data/info?id=demo then your user id is demo)\n:");
					DataInputStream dataInputStream = new DataInputStream(System.in);
					client_id = dataInputStream.readLine().trim();
				}
				FileWriter configsFileWriter = new FileWriter(configs_file_path);
				configsFileWriter.write("---\n");
				configsFileWriter.write("client_id: "+client_id+"\n");
				configsFileWriter.write("download_dir: "+download_dir+"\n");
				configsFileWriter.close();
			}

			if( options.get("log_dir") == null ) {
				options.put("log_dir",promptcloudhome+sysPathSep+"log");
			}
			File log_dir = new File((String)options.get("log_dir"));
			if( ! log_dir.isDirectory() ) {
				log_dir.mkdirs();
			}

			if( options.get("md5_dir") == null ) {
				options.put("md5_dir",promptcloudhome+sysPathSep+"md5sums");
			}
			File md5_dir = new File((String)options.get("md5_dir"));
			if( !md5_dir.isDirectory() ) {
				md5_dir.mkdirs();
			}

			if( options.get("queried_timestamp_file") == null ) {
				options.put("queried_timestamp_file",promptcloudhome+sysPathSep+"last_queried_ts");
			}

			Yaml yaml = new Yaml();
			String apiconf_file_path = (String)options.get("apiconf");
			HashMap conf_hash = (HashMap)yaml.load(new FileReader(apiconf_file_path));
			client_id = (String)conf_hash.get("client_id");
			if(options.get("api_version") == "v1"){
				password = (String)conf_hash.get("password");
			}
			else{
				if(options.get("api_version") == null){
					client_auth_key = (String)conf_hash.get("client_auth_key");
					options.put("api_version", "v2");
				}
			}
			if(client_id == null){
				client_id = (String)options.get("user");
			}
			if(client_auth_key == null){
				client_auth_key = (String)options.get("client_auth_key");
			}
			if(password == null){
				password = (String)options.get("pass");
			}
			
			//System.out.println("client_id: "+client_id);
			if( client_id == null ) {
				System.out.println("Could not find client id from config file and neither has been passed from command line: "+apiconf_file_path);
				System.exit(1);
			}
			if(options.get("api_version") == "v2" && client_auth_key == null){
				System.out.println("Could not find client auth key from config file and neither has been passed from command line, Please either pass the client auth key from command prompt (use -h for help) or store the same under client_auth_key: in config.yml file, as for api version 2 this field is mandatory: "+apiconf_file_path);
				System.exit(1);
			}
			if(options.get("api_version") == "v1" && password == null){
				System.out.println("Could not find password from config file and neither has been passed from command line, Please either pass the password from command prompt (use -h for help) or store the same under password: in config.yml file, as for api version 1 this field is mandatory:: "+apiconf_file_path);
				System.exit(1);
			}
			options.put("user",client_id);
			options.put("client_auth_key",client_auth_key);
			options.put("password",password);
			String download_dir_from_conf_hash = (String)(String)conf_hash.get("download_dir");
			if( download_dir_from_conf_hash != null) {
				download_dir = download_dir_from_conf_hash;
			}
			options.put("download_dir",download_dir);
			//System.out.println("download_dir: "+download_dir);
			File dwnld_dir = new File(download_dir);
			if(!dwnld_dir.isDirectory()) {
				dwnld_dir.mkdirs();
			}
			perform_initial_setup_status = true;
		}catch(Exception e) {
			e.printStackTrace(System.out);
		}
		return perform_initial_setup_status;
	}

	boolean download_files(HashMap options) throws Exception {
		if( options.get("api_version") == "v1" && (options.get("user") == null  ||  options.get("pass") == null )) {
			throw new UserAndPassException("You didn't provide username or password. Please pass -h or --help for help.");
		}
		if( options.get("api_version") == "v2" && (options.get("user") == null  ||  options.get("client_auth_key") == null )) {
			throw new UserAndPassException("You didn't provide username or client authentication key. Please pass -h or --help for help.");
		}
		boolean new_feed_exists = false;
		Long ts = System.currentTimeMillis()*1000000;
		String fetch_log_path = options.get("log_dir")+sysPathSep+"fetched_urls-"+ts+".log";
		FileWriter fetchLogFileWriter = new FileWriter(fetch_log_path);

		ArrayList<HashMap> hashMapArr = get_file_urls(options);
		HashMap urls_ts_map = hashMapArr.get(0);
		HashMap url_md5_map = hashMapArr.get(1);
		if(urls_ts_map == null) {
			System.out.println("Could not obtain file urls to download.");
			return new_feed_exists;
		}
		if(urls_ts_map.isEmpty()) {
			System.out.println("No new files to download.");
			return new_feed_exists;
		}

		Iterator it = urls_ts_map.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry pairs = (Map.Entry)it.next();
			//System.out.println(pairs.getKey() + ": " + pairs.getValue()); 
			ArrayList<String> urls = (ArrayList<String>)pairs.getValue();
			int num_urls = urls.size();
			for(int i=0;i<num_urls;i++) {	
				String url = urls.get(i).trim();
				System.out.println("-------------------------------");
				System.out.println("Fetching URL: "+url);
				try {
					String md5sum = ((String)url_md5_map.get(url)).trim();
					String filename = url.substring(url.lastIndexOf('/') + 1); 
					String md5_filename = filename.replaceAll(".gz", ".md5sum");
					String md5_filepath = ((String)options.get("md5_dir")).trim()+sysPathSep+md5_filename;
					File md5_file = new File(md5_filepath);
					if( md5_file.exists() ) {
						String md5_file_content = ((String)FileUtils.readFileToString(md5_file)).trim();
						System.out.println("md5sum--> (from API info, from local .md5sum file: "+md5_filepath+"):");
						System.out.println(md5sum+"\n"+md5_file_content);
						if( md5_file_content.equals(md5sum) ) {
							System.out.println("md5sum matched(downloaded earlier) skipping url: "+ url);
							continue;
						}
					}
					else {
						System.out.println("NOT FOUND local md5sum file: "+md5_filepath);
					}
					new_feed_exists = true ;
					options.put("url_to_download",url);
					boolean downloadFlag = download_file_and_save_in_download_dir(options,md5sum);
					if(downloadFlag) {
						FileUtils.writeStringToFile(md5_file,md5sum);
						fetchLogFileWriter.write("Fetched URL: "+url+"\n");
						System.out.println("Fetched URL: "+url);
					}
					else {
						fetchLogFileWriter.write("Failed to fetch URL: "+url+"\n");
						System.out.println("Failed to fetch URL: "+url);
					}
				}catch(Exception e) {
					fetchLogFileWriter.write("Failed to fetch URL: "+url+"\n");
					System.out.println("Failed to fetch URL: "+url);
				}
			}
		}
		fetchLogFileWriter.close();
		System.out.println("Log file path: "+fetch_log_path);
		System.out.println("Downloaded files are available at: "+options.get("download_dir"));
		return new_feed_exists;
	}

	boolean download_file_and_save_in_download_dir(HashMap options, String md5sum) {
		boolean downloadFlag = false;
		try {
			String strUrl = (String)options.get("url_to_download");
			URI url = new URI(strUrl);
			String user = (String)options.get("user");
			String pass = null;
			String client_auth_key = null;
			if(options.get("api_version") == "v1"){
				pass = (String)options.get("pass");
			}
			else{
				client_auth_key = (String)options.get("client_auth_key");
			}
			String domain = url.getHost();
			HttpHost target = new HttpHost(domain);
			CloseableHttpClient httpclient;
			
			if(options.get("api_version").equals("v1")){
				CredentialsProvider credsProvider = new BasicCredentialsProvider();
				credsProvider.setCredentials(new AuthScope(target.getHostName(), target.getPort()),new UsernamePasswordCredentials(user,pass));
				httpclient = HttpClients.custom().setDefaultCredentialsProvider(credsProvider).build();
			}
			else{
				httpclient = HttpClients.custom().build();
			}
			//CredentialsProvider credsProvider = new BasicCredentialsProvider();
			//credsProvider.setCredentials(new AuthScope(target.getHostName(), target.getPort()),new UsernamePasswordCredentials(user,pass));
			//CloseableHttpClient httpclient = HttpClients.custom().setDefaultCredentialsProvider(credsProvider).build();
						
			try {
				AuthCache authCache = new BasicAuthCache();
				BasicScheme basicAuth = new BasicScheme();
				authCache.put(target, basicAuth);
				HttpClientContext localContext = HttpClientContext.create();
				localContext.setAuthCache(authCache);
				String reqRes = strUrl.split(domain)[1];
				HttpGet httpget = new HttpGet(reqRes);
				System.out.println("Executing request " + httpget.getRequestLine() + " to target " + target);
				CloseableHttpResponse response = httpclient.execute(target, httpget, localContext);
				StatusLine statusLine = response.getStatusLine();
				System.out.println("Status Line: "+statusLine);
				int statusCode = statusLine.getStatusCode();
				System.out.println("Status Code: "+statusCode);
				if(statusCode == 200) {
					String downloadDir = (String)options.get("download_dir");
					String downloadFileName = strUrl.substring(strUrl.lastIndexOf("/")+1);
					String downloadFilePath = downloadDir+sysPathSep+downloadFileName;
					File downloadFile = new File(downloadFilePath);
					InputStream resInputStream = null;
					OutputStream downloadFileOutSteam = new FileOutputStream(downloadFile);
					try {
						HttpEntity responseEntity = response.getEntity();
						resInputStream = responseEntity.getContent();
						int read = 0;
						byte[] bytes = new byte[1024];
						while ((read = resInputStream.read(bytes)) != -1) {
							downloadFileOutSteam.write(bytes, 0, read);
						}                  
					}catch(Exception e) {
						downloadFlag = false;
						e.printStackTrace(System.out);
					}finally {
						resInputStream.close();
						downloadFileOutSteam.close();
						response.close();
					}
					String md5sumNewDownloadFile = null;
					GZIPInputStream gzfis = null;
					if(FilenameUtils.getExtension(downloadFilePath).equals("gz")){        
						gzfis = new GZIPInputStream(new FileInputStream(downloadFile));
						md5sumNewDownloadFile = DigestUtils.md5Hex(gzfis);
						gzfis.close();
					}
					else{
						md5sumNewDownloadFile = DigestUtils.md5Hex(new FileInputStream(downloadFile));
					}
					try {
						if(md5sum.equals(md5sumNewDownloadFile)) {					
							downloadFlag = true;				
							System.out.println("Download successful URL: "+strUrl+" File_Path: "+downloadFilePath+" md5sum: "+md5sumNewDownloadFile);
						}
						else {
							downloadFlag = false;
							downloadFile.delete();						
							System.out.println("Download unsuccessful URL: "+strUrl);
						}
					}catch(Exception e) {
						e.printStackTrace(System.out);
						downloadFlag = false;
					}	
				}
				else {
					System.out.println("API issue, status: "+statusLine+"\nCould not download file: "+strUrl);
					downloadFlag = false;
				}
			}catch(Exception e) {
				e.printStackTrace(System.out);
				downloadFlag = false;
			}finally {
				httpclient.close();
			}
		}catch(Exception e) {
			e.printStackTrace(System.out);
			downloadFlag = false ;
		}finally {
			return downloadFlag;
		}
	}

	ArrayList get_file_urls(HashMap options) {
		HashMap url_ts_map = new HashMap();
		HashMap url_md5_map = new HashMap();
		try {
			String api_query_output = new String();
			String promptcloud_api_query = get_api_url(options); 
			// Create a context that doesn't check certificates.
			SSLContext ssl_ctx = SSLContext.getInstance("TLS");
			TrustManager[ ] trust_mgr = get_trust_mgr();
			ssl_ctx.init(null,                // key manager
					trust_mgr,           // trust manager
					new SecureRandom()); // random number generator
			if(options.get("ignore_ssl_certificate") == null) {
				HttpsURLConnection.setDefaultSSLSocketFactory(ssl_ctx.getSocketFactory());
				//System.out.println("DEBUGING...ignore_ssl_certificate");
			}
			URL api_url = new URL(promptcloud_api_query);
			HttpsURLConnection api_con = (HttpsURLConnection)api_url.openConnection();
			// Guard against "bad hostname" errors during handshake.
			api_con.setHostnameVerifier(new HostnameVerifier() {
				public boolean verify(String host, SSLSession sess) {
					if (host.equals("localhost")) return true;
					else return false;
				}
			});
			String apistatusLine = api_con.getResponseMessage();
			System.out.println("Status Line: "+apistatusLine);
			int apistatusCode = api_con.getResponseCode();
			System.out.println("Status Code: "+apistatusCode);
			if(apistatusCode != 200) {
				if(options.get("bcp") != null) {
					System.out.println("bcp is also down, please mail to apisupport@promptcloud.com");
					disable_bcp(options);
				}
				else {
					if(options.get("loop") != null) {
						System.out.println("Could not fetch from promptcloud api server, will try the api server after the sleep and promptcloud bcp after 30 mins");
					}
					else {
						System.out.println("Main api server seems to be unreachable, please pass --bcp option to use bcp server.");
					}
					handle_api_downtime(options);
				}
			}
			else {
				api_query_output = get_api_res_content(api_con);
				disable_bcp(options); //next fetch will be from promtcloud api
			}

			JSONParser jsonParser = new JSONParser();
			try {
				JSONObject apiResJsonObj = (JSONObject)jsonParser.parse(api_query_output);
				//System.out.println("apiResJsonObj: \n"+apiResJsonObj);
				if(apiResJsonObj != null ) {
					JSONObject rootJsonObj = (JSONObject)apiResJsonObj.get("root");
					//System.out.println("rootJsonObj: \n"+rootJsonObj);	
					if(rootJsonObj != null) {
						Object entryObj = rootJsonObj.get("entry");
						if(entryObj!=null) {
							JSONArray entryJsonObjArray = new JSONArray();
							if( entryObj instanceof JSONObject) {         					
								entryJsonObjArray.add((JSONObject)entryObj);         					
							}
							else if (entryObj instanceof JSONArray) {
								entryJsonObjArray = (JSONArray)entryObj;
							}
							//System.out.println("entryJsonObjArray: \n"+entryJsonObjArray);
							if(entryJsonObjArray != null) {
								int lenEntryJsonObjArray = entryJsonObjArray.size();
								for(int i=0;i<lenEntryJsonObjArray;i++) {
									JSONObject entryJsonObj = (JSONObject)entryJsonObjArray.get(i);
									//System.out.println("entryJsonObj: \n"+entryJsonObj);
									if(entryJsonObj != null) {
										Long updated = Long.parseLong((String)entryJsonObj.get("updated"));
										//System.out.println("updated: "+updated);
										String url = ((String)entryJsonObj.get("url")).trim();
										//System.out.println("url: "+url);
										String md5sum = ((String)entryJsonObj.get("md5sum")).trim();
										//System.out.println("md5sum: "+md5sum);
										if(updated!=null && url!=null && md5sum!=null) {
											url_md5_map.put(url,md5sum);
											if(url_ts_map.get(updated)!=null) {
												((ArrayList)url_ts_map.get(updated)).add(url);
											}
											else {
												ArrayList<String> list = new ArrayList<String>();
												list.add(url);	
												url_ts_map.put(updated,list);
											}
										}
									}
								}
							}
						}
						else {
							System.out.println((String)rootJsonObj.get("status_desc"));
							System.exit(1);
						}
					}
				}
			}catch(ParseException pe) {
				System.out.println("position: " + pe.getPosition());
				System.out.println(pe);
			}
		}catch(SSLHandshakeException sslEx) {
			System.out.println("\nSSLHandshakeException due to invalid SSL certificate check.\nBest and safe solution:\nFollow this post http://www.java-samples.com/showtutorial.php?tutorialid=210\nOne issue might be raised while doing this -\nFinal result:\nCertificate was added to keystore.\nkeytool error: java.io.FileNotFoundException: C:\\Program files\\...jre\\lib\\cacerts(Access is Denied)\nSolution: Run all keytool commands as administrator(windiws) and as root/sudo(linux). More help http://stackoverflow.com/questions/10321211/java-keytool-error-after-importing-certificate-keytool-error-java-io-filenot\n\nOR\n\nPass --ignore_ssl_certificate option to ignore invalid ssl certificate.");
			//sslEx.printStackTrace(System.out);
			System.exit(1);
		}catch(Exception e) {
			e.printStackTrace(System.out);
			System.exit(1);
		}
		finally {
			ArrayList<HashMap> file_url_array = new ArrayList<HashMap>();
			file_url_array.add(url_ts_map);
			file_url_array.add(url_md5_map);
			return file_url_array;
		}
	}

	private TrustManager[ ] get_trust_mgr() {
		TrustManager[ ] certs = new TrustManager[ ] {
			new X509TrustManager() {
				public X509Certificate[ ] getAcceptedIssuers() { return null; }
				public void checkClientTrusted(X509Certificate[ ] certs, String t) { }
				public void checkServerTrusted(X509Certificate[ ] certs, String t) { }
			}
		};
		return certs;
	}

	private String get_api_res_content(HttpsURLConnection api_con) {
		String api_res_content = new String("");
		if(api_con!=null) {
			try {
				BufferedReader br = new BufferedReader(new InputStreamReader(api_con.getInputStream()));
				String input;
				while ((input = br.readLine()) != null) {
					api_res_content += input;
				}
				br.close();
			}catch (IOException e) {
				e.printStackTrace(System.out);
				System.exit(1);
			}finally {
				//System.out.println("****** Content of the URL ********");
				//System.out.println(api_res_content);
				if(api_res_content.equals("")) {
					System.out.println("PromptCloud api response is empty. Please try again.");
					System.exit(1);
				}
			}	
		}
		return api_res_content;
	}

	String get_api_url(HashMap options) {
		String base_url = "https://api.promptcloud.com";
		if(options.get("bcp") != null ) {
			base_url = "https://api.bcp.promptcloud.com";
		}
		String promptcloud_api_query = null;
		if(options.get("api_version").equals("v1")){
			promptcloud_api_query = "" + base_url + "/data/info?id=" + client_id;
		}
		else{
			promptcloud_api_query = "" + base_url + "/v2/data/info?id=" + client_id + "&client_auth_key=" + client_auth_key;
		}
		
		if(options.get("timestamp") != null) {
			promptcloud_api_query = promptcloud_api_query+"&ts="+(String)options.get("timestamp");
			try {
				PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter((String)options.get("queried_timestamp_file"),true)));
				out.println((String)options.get("timestamp"));
				out.close();
			} catch (Exception e) {
				e.printStackTrace(System.out);
			}
		}
		
		if(options.get("category") != null) {
			promptcloud_api_query = promptcloud_api_query+"&cat="+(String)options.get("category");
		}
		
		if(options.get("apiparam") != null) {
			String paramsStr = (String)options.get("apiparam");
			if( paramsStr != null ) {
				String[] params = paramsStr.split(",");
				for(int i=0;i<params.length;i++) {
					promptcloud_api_query = promptcloud_api_query+"&"+params[i];
				}
			}				
		}
		
		promptcloud_api_query = promptcloud_api_query+"&api_res_type=json";
		
		System.out.println("PromptCloud Data API URL: "+promptcloud_api_query);
		return promptcloud_api_query;
	}

	void handle_api_downtime(HashMap options) {
		if( api_downtime != 0 ) {
			long total_downtime = (System.currentTimeMillis()/1000) - api_downtime;
			if( total_downtime > 1800 ) {
				options.put("bcp",true);
			}
		}
		else {
			api_downtime = System.currentTimeMillis()/1000;
		}
	}

	void disable_bcp(HashMap options) {
		if(options.get("bcp") != null ) {
			options.put("bcp",null);
			api_downtime = 0;
		}
	}

}

class PromptCloudApiArgParser {
	String[] args = null;
	Options options = new Options();
	PromptCloudApiArgParser(String[] args) {
		this.args = args;
		options.addOption("h", "help", false, "show help.");
		options.addOption("c","apiconf",true, "APICONFIG FILE PATH, to override the config file location, APICONFIG file stores information like client_id, downloadir, previous timestamp file");
		options.addOption("d","download_dir", true, "DOWNLOADDIR, to override the download dir obtained from apiconf file");
		options.addOption(null,"promptcloudhome", true,"PROMPTCLOUDHOME, to override the promptcloudhome dir: ~/promptcloud");
		options.addOption("i","perform_initial_setup", false,"perform initial setup");
		options.addOption(null,"display_info", false,"display config info");
		options.addOption("t","timestamp", true,"Local TIMESTAMP(nano time(integer) i.e. 1417069800000000000 or String(yyyy-MM-dd-hh:mm:ss.SSS-a) i.e. 2014-11-29-2:04:00.000-AM), to get files newer than or equal to given timestamp");
		options.addOption(null,"queried_timestamp_file", true, " QUERIED TIME STAMP FILE PATH, to override default queried timestamp file, file that stores last queried timestamp");
		options.addOption("u","user",true, "USER, data api user id");
		options.addOption("p","pass", true, "PASSWORD, data api password");
		options.addOption("l","loop", false, "to download new data files and keep looking for new one. i.e it doesn't exit, if no new feed is found it will sleep. minimun sleep time is 10 secs and max sleep time is 300 secs");
		options.addOption(null,"noloop", false, "to download new data files and and exit, this is the default behaviour");
		options.addOption("b","bcp", false, "to use api.bcp.promptcloud.com instead of api.promptcloud.com");
		options.addOption("a","apiparam", true, "some valid data api param comma separated examle: days=1,filter=sitewiselatest");
		options.addOption("n","ignore_ssl_certificate", true, "to ignore invalid ssl certificate, by default ssl certificate is ignored, if you want enable it then pass -n false");
		options.addOption(null,"version", false, "to show the version");
		options.addOption("v", "api_version", true, "to pass the api version, Default is v2");
		options.addOption("k", "client_auth_key", true, "to pass the client authentication key if the api version is v2");
	}
	HashMap parse() {
		HashMap optionsHashMap = new HashMap();
		CommandLineParser parser = new BasicParser();
		CommandLine cmd = null;
		try {
			if(args.length == 0) {
				System.out.println("No option is passed.");
				help();
			}
			cmd = parser.parse(options, args);
			if (cmd.hasOption("help")) {
				help();
			}
			if (cmd.hasOption("apiconf")) {
				optionsHashMap.put("apiconf",cmd.getOptionValue("apiconf"));
			}
			if (cmd.hasOption("download_dir")) {
				optionsHashMap.put("download_dir",cmd.getOptionValue("download_dir"));
			}
			if (cmd.hasOption("promptcloudhome")) {
				optionsHashMap.put("promptcloudhome",cmd.getOptionValue("promptcloudhome"));
			}
			if (cmd.hasOption("perform_initial_setup")) {
				optionsHashMap.put("perform_initial_setup",true);
			}
			if (cmd.hasOption("display_info")) {
				optionsHashMap.put("display_info",true);
			}
			if (cmd.hasOption("timestamp")) {
				optionsHashMap.put("timestamp",get_utc_timestamp(cmd.getOptionValue("timestamp"),"yyyy-MM-dd-hh:mm:ss.SSS-a"));
			}
			if (cmd.hasOption("queried_timestamp_file")) {
				optionsHashMap.put("queried_timestamp_file",cmd.getOptionValue("queried_timestamp_file"));
			}
			if (cmd.hasOption("user")) {
				optionsHashMap.put("user",cmd.getOptionValue("user"));
			}
			if (cmd.hasOption("pass")) {
				optionsHashMap.put("pass",cmd.getOptionValue("pass"));
			}
			if (cmd.hasOption("loop")) {
				optionsHashMap.put("loop",true);
			}
			if (cmd.hasOption("noloop")) {
				optionsHashMap.put("noloop",true);
			}
			if (cmd.hasOption("bcp")) {
				optionsHashMap.put("bcp",true);
			}	
			if (cmd.hasOption("apiparam")) {
				optionsHashMap.put("apiparam",cmd.getOptionValue("apiparam"));
			}
			if (cmd.hasOption("ignore_ssl_certificate")) {
				optionsHashMap.put("ignore_ssl_certificate",cmd.getOptionValue("ignore_ssl_certificate"));
			}
			if (cmd.hasOption("version")) {
				optionsHashMap.put("version",true);
			}
			if (cmd.hasOption("api_version")){
				optionsHashMap.put("api_version", cmd.getOptionValue("api_version"));
			}
			if (cmd.hasOption("client_auth_key")){
				optionsHashMap.put("client_auth_key", cmd.getOptionValue("client_auth_key"));
			}			
		}catch( Exception e) {
			System.out.println("Failed to parse comand line properties"+e);
			//e.printStackTrace(System.out);
			help();
		}
		return optionsHashMap;
	}
	void help() {
		HelpFormatter formater = new HelpFormatter();
		formater.printHelp("Main", options);
		System.out.println("\nUser Help:\nTo perform initial setup,please pass --perform_initial_setup and/or --user <client_id>");
		System.out.println("To download data,please pass --user <client_id> and --pass <password>\nPlease note that client_id will be saved in config.yml but pass will not be.");
		System.exit(0);
	}
	String get_utc_timestamp(String timeString,String timeFotmatString) throws Exception {
		long ts = 0;
		String tsStr = null;

		if(timeString.matches("\\d+")) {
			tsStr = timeString;
		}
		else {
			try {
				SimpleDateFormat dateFormat = new SimpleDateFormat(timeFotmatString);
				Date localDate = dateFormat.parse(timeString);
				Calendar utcTime = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
				utcTime.setTime(localDate);
				ts = utcTime.getTimeInMillis()*1000000;
				if(ts < 0) {
					throw new Exception("Unparseable date: "+timeString);
				}
				else {
					tsStr = Long.toString(ts);
				}
			}catch(Exception e) {
				System.out.println(e.getMessage()+"\n Please pass timestamp (nano time(integer) i.e. 1417069800000000000 or String(yyyy-MM-dd-hh:mm:ss.SSS-a) i.e. 2014-11-29-2:04:00.000-AM).");
				System.exit(0);
			}
		}
		return(tsStr);
	}
}

class PromptCloudDataAPITimer {
	int min,max,sleep_interval;
	PromptCloudDataAPITimer() {
		min = 10;	// in seconds
		max = 30;	// in seconds
		sleep_interval = min;	
	}
	PromptCloudDataAPITimer(HashMap args_hash) {
		if( args_hash.get("min") != null ) {
			min = (int)args_hash.get("min");
		}
		if( args_hash.get("max") != null ) {
			max = (int)args_hash.get("max");
		}
		sleep_interval = min;
	}

	void do_wait() {
		try {
			System.out.println("Going to sleep for "+sleep_interval+" seconds");
			Thread.sleep(sleep_interval*1000);
			sleep_interval *= 2; 
			if(sleep_interval > max) {
				sleep_interval = min;
			}
		}catch(Exception e) {
			System.out.println("Exception in do_wait");
			e.printStackTrace(System.out);
		}
	}	
}

class UserAndPassException extends Exception {
	UserAndPassException() {
		super();
	}
	UserAndPassException(String msg) {
		super(msg);
	}
}
