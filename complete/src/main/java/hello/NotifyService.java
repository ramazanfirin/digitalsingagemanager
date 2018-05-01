package hello;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.tomcat.util.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import hello.enums.AgeEnum;
import hello.enums.GenderEnum;

@Service
public class NotifyService {

	private final Logger log = LoggerFactory.getLogger(NotifyService.class);
	//final String BASE_URL = "http://192.168.1.35:8000/api";
	final String BASE_URL = "http://192.168.43.143:8000/api";
	long lastNotifyDate;
	Boolean notifyDone=false;
	
	
	@Async
	public void sendNotify(Float age,Float genderValue) throws IOException{
		GenderEnum gender = getGenderEnum(genderValue);
		AgeEnum ageEnum = getAgeGroup(age);
		String result = ageEnum.toString()+"_"+gender.toString();
//		System.out.println(result);
		
		Map<String,String> playLists = getPlayLists(result);
		if(playLists.get(result)!=null)
			startPlayList(playLists.get(result));
		
		
	}
	
	private AgeEnum getAgeGroup(Float age){
		if(age<15)
			return AgeEnum.CHILD;
		else if(age>15 && age<35)
			return AgeEnum.YOUNG;
		else if(age>35 && age<55)
			return AgeEnum.MIDDLE_AGE;
		else if(age>55)
			return AgeEnum.OLDER;
		else
			return AgeEnum.MIDDLE_AGE;
	}
	
	private GenderEnum getGenderEnum(Float genderValue){
		if(genderValue<0)
			return GenderEnum.MALE;
		else
			return GenderEnum.FEMALE;
		
	}
	
	private  String sendGET(String url) throws IOException {
		
		
		String name = "pi";
		String password = "pi";
		String authString = name + ":" + password;		
		byte[] authEncBytes = Base64.encodeBase64(authString.getBytes());
		String authStringEnc = new String(authEncBytes);

		
		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
		con.setRequestMethod("GET");
		//con.setRequestProperty("User-Agent", USER_AGENT);
		con.setRequestProperty("Authorization", "Basic " + authStringEnc);
		int responseCode = con.getResponseCode();
//		System.out.println("GET Response Code :: " + responseCode);
		if (responseCode == HttpURLConnection.HTTP_OK) { // success
			BufferedReader in = new BufferedReader(new InputStreamReader(
					con.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();

			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();

			// print result
//			System.out.println(response.toString());
			return response.toString();
		} else {
//			System.out.println("GET request not worked");
			return null;
		}

	}
	
	private  String sendPost(String url,String action) throws IOException {
		String name = "pi";
		String password = "pi";
		String authString = name + ":" + password;		
		byte[] authEncBytes = Base64.encodeBase64(authString.getBytes());
		String authStringEnc = new String(authEncBytes);
		
		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
		con.setRequestMethod("POST");
		con.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        con.setDoOutput(true);
        con.setDoInput(true);
		con.setRequestProperty("Authorization", "Basic " + authStringEnc);
		
		OutputStream os = con.getOutputStream();
        //os.write("{\"play\": true,\"stop\":true}".getBytes("UTF-8"));
        if("STOP".equals(action))
        	os.write("{\"stop\":true}".getBytes("UTF-8"));
        if("START".equals(action))
        	os.write("{\"play\":true}".getBytes("UTF-8"));
        os.close();
		
		int responseCode = con.getResponseCode();
//		System.out.println("GET Response Code :: " + responseCode);
		if (responseCode == HttpURLConnection.HTTP_OK) { // success
			BufferedReader in = new BufferedReader(new InputStreamReader(
					con.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();

			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();

			// print result
//			System.out.println(response.toString());
			return response.toString();
		} else {
//			System.out.println("GET request not worked");
			return null;
		}
		
	}
	
	private Map<String,String> getPlayLists(String val) throws IOException{
		Map<String,String> playListMap = new HashMap<String,String>();
		String result = sendGET(BASE_URL+"/playlists/"+"FACE_RECOGNITION");
		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode node = objectMapper.readValue(result, JsonNode.class);
		JsonNode data = (JsonNode)node.get("data");
		ArrayNode array = (ArrayNode)data.get("assets");
		for (int i = 0; i < array.size(); i++) {
			JsonNode asset = array.get(i);
			String fileName = asset.get("filename").asText();
			if(fileName.contains(val)){
				playListMap.put(val, fileName);
			}
			
			//playListMap.put(as.get("name").asText(), as.get("name").asText());
		}
	
		return playListMap;
	}
	
	
	private void startPlayList(String file) throws IOException{
		
//			String response = sendPost(BASE_URL+"/play/playlists/"+file,"STOP");
//			String response2 = sendPost(BASE_URL+"/play/playlists/"+file,"START");
//		play/files/play?file=asd
		String response2 = sendPost(BASE_URL+"/play/files/play?file="+file,"sdasdasdasdas");
//		System.out.println("playlist started");
		log.info(file +" playing");
		lastNotifyDate = System.currentTimeMillis();
		notifyDone = true;
	}
	
	@Scheduled(fixedRate = 5000)
	public void resetPlaylist() throws IOException{
		if(Util.needResetPlaylist(notifyDone,lastNotifyDate)){
			String response  = sendPost(BASE_URL+"/play/playlists/"+"Reklam","STOP");
			String response2 = sendPost(BASE_URL+"/play/playlists/"+"Reklam","START");
			notifyDone = false;
//			System.out.println("playlist reset");
			log.info("playlist reset");
		}
	}
	
	
	public static void main(String[] args) throws IOException {
		NotifyService notifyService = new NotifyService();
		Map<String,String> playListMap  = notifyService.getPlayLists("dsd");		
//		System.out.println("bitti");
	}

	public long getLastNotifyDate() {
		return lastNotifyDate;
	}

	public void setLastNotifyDate(long lastNotifyDate) {
		this.lastNotifyDate = lastNotifyDate;
	}

	public Boolean getNotifyDone() {
		return notifyDone;
	}

	public void setNotifyDone(Boolean notifyDone) {
		this.notifyDone = notifyDone;
	}
	
}
