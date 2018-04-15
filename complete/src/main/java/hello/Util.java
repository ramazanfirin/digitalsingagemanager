package hello;

public class Util {

	public static Boolean needNotify(long lastNotifyDate){
		if(lastNotifyDate ==0)
			return true;
		else{
			long currentTime= System.currentTimeMillis();
			if(System.currentTimeMillis()-lastNotifyDate>10000){
				lastNotifyDate = currentTime;
				return true;
			}else{
				System.out.println("no need notify");
				return false;
			}
		}
	}
	
	public static Boolean needResetPlaylist(Boolean notifyDone,long lastNotifyDate){
		if(lastNotifyDate ==0)
			return false;
		else if(!notifyDone)
			return false;
		else{
			if(System.currentTimeMillis()-lastNotifyDate>5000){
				return true;
			}else{
				System.out.println("no need reset");
				return false;
			}
		}
	}
}
