package hello;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamCompositeDriver;
import com.github.sarxos.webcam.WebcamResolution;
import com.github.sarxos.webcam.ds.buildin.WebcamDefaultDriver;
import com.github.sarxos.webcam.ds.ipcam.IpCamDeviceRegistry;
import com.github.sarxos.webcam.ds.ipcam.IpCamDriver;
import com.github.sarxos.webcam.ds.ipcam.IpCamMode;
import com.github.sarxos.webcam.ds.v4l4j.V4l4jDriver;
import com.innovatrics.iface.Face;
import com.innovatrics.iface.FaceHandler;
import com.innovatrics.iface.IFace;
import com.innovatrics.iface.IFaceException;
import com.innovatrics.iface.enums.AgeGenderSpeedAccuracyMode;
import com.innovatrics.iface.enums.FaceAttributeId;
import com.innovatrics.iface.enums.FacedetSpeedAccuracyMode;
import com.innovatrics.iface.enums.Parameter;

@Service
public class CaptureService {
	private final Logger log = LoggerFactory.getLogger(CaptureService.class);
	
	@Autowired
	NotifyService notifyService;
	
    public int minEyeDistance = 30;
    public int maxEyeDistance = 3000;
    Webcam webcam;
	
	IFace iface= null;
	FaceHandler faceHandler = null;
	
	public static class MyCompositeDriver extends WebcamCompositeDriver {

		public MyCompositeDriver() {
			add(new WebcamDefaultDriver());
			add(new IpCamDriver());
		}
	}

	// register custom composite driver
//	static {
//		Webcam.setDriver(new MyCompositeDriver());
//	}
	
//	static {
//        Webcam.setDriver(new V4l4jDriver()); // this is important
//    }
	
	public CaptureService() throws IFaceException, IOException {
		super();
//				Dimension captureResolution = WebcamResolution.VGA.getSize(); // 640 x 480
//		Dimension displayResolution = WebcamResolution.QVGA.getSize(); // 340 x 240
//		
//		IpCamDeviceRegistry.register("Lignano", "http://192.168.1.132:8080/photo.jpg", IpCamMode.PULL);
		//Webcam webcam = Webcam.getDefault();
		
		webcam = Webcam.getWebcams().get(0);
		webcam.setViewSize(WebcamResolution.VGA.getSize());
		webcam.open();
		
		startIface();
	}
	
	public void startIface() throws IOException {
		iface = IFace.getInstance();
		
//		ClassLoader classLoader = getClass().getClassLoader();
//		Path path = Paths.get(classLoader.getResource("iengine.lic").getPath());
//		iface.initWithLicence(Files.readAllBytes(path));
		
		ClassPathResource cpr = new ClassPathResource("iengine.lic");
		byte[] bdata = FileCopyUtils.copyToByteArray(cpr.getInputStream());
		iface.initWithLicence(bdata);
		
		faceHandler = new FaceHandler();
		faceHandler.setParam(Parameter.FACEDET_SPEED_ACCURACY_MODE, FacedetSpeedAccuracyMode.FAST.toString());
		faceHandler.setParam(Parameter.AGEGENDER_SPEED_ACCURACY_MODE, AgeGenderSpeedAccuracyMode.FAST.toString());
		//         

	}
	
	
	@Scheduled(fixedRate = 1000)
	public void captureFromCamera() throws IOException{
		log.info("capture started");
		if(!Util.needNotify(notifyService.getLastNotifyDate())){
			log.info("no need capture");
			return;
		}
		
		
		
		
		long start =System.currentTimeMillis();
		BufferedImage image  = webcam.getImage();
		//ImageIO.write(image, "PNG", new File("/home/ramazan/webcamtest/phone"+System.currentTimeMillis()+".png"));
		image = resize(image,960,1280);
//		log.info("capture compledted");
		Face[] faces = faceHandler.detectFaces(convertToByteArray(image), minEyeDistance, maxEyeDistance, 3);
		if(faces.length==0){
			log.info("No Face Detected");
			return;
		}
			
		
		for (int i = 0; i < faces.length; i++) {
			Face face = faces[i];
			Float age = face.getAttribute(FaceAttributeId.AGE);
	        Float gender = face.getAttribute(FaceAttributeId.GENDER);
	        long end = System.currentTimeMillis();
	        log.info("age:"+age+",gender:"+gender+",duration="+(end-start));
	        notifyService.sendNotify(age, gender);
	        break;
		}
		log.info("capture finished");
		
	}

	
	private byte[] convertToByteArray(BufferedImage originalImage) throws IOException{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ImageIO.write( originalImage, "png", baos );
		baos.flush();
		byte[] imageInByte = baos.toByteArray();
		baos.close();
		return imageInByte;
	}
	
	private BufferedImage resize(BufferedImage img, int height, int width) {
        Image tmp = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = resized.createGraphics();
        g2d.drawImage(tmp, 0, 0, null);
        g2d.dispose();
        return resized;
	}
	
}
