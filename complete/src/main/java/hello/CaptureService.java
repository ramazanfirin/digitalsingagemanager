package hello;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

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
	static {
		Webcam.setDriver(new MyCompositeDriver());
	}
	
	public CaptureService() throws IFaceException, IOException {
		super();
		System.out.println("capture service created");
		Dimension captureResolution = WebcamResolution.VGA.getSize(); // 640 x 480
		Dimension displayResolution = WebcamResolution.QVGA.getSize(); // 340 x 240
		
		IpCamDeviceRegistry.register("Lignano", "http://192.168.1.132:8080/photo.jpg", IpCamMode.PULL);
		//Webcam webcam = Webcam.getDefault();
		
		webcam = Webcam.getWebcams().get(0);
		webcam.setViewSize(WebcamResolution.VGA.getSize());
		webcam.open();
		
		
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
		if(!Util.needNotify(notifyService.getLastNotifyDate())){
			System.out.println("no need notify");
			return;
		}
		
		
		
		
		long start =System.currentTimeMillis();
		BufferedImage image  = webcam.getImage();
		//ImageIO.write(image, "PNG", new File("/home/ramazan/webcamtest/phone"+System.currentTimeMillis()+".png"));
		image = resize(image,960,1280);
		System.out.println("capture edildi");
		Face[] faces = faceHandler.detectFaces(convertToByteArray(image), minEyeDistance, maxEyeDistance, 3);
		if(faces.length==0){
			System.out.println("No Face Detected");
			return;
		}
			
		
		for (int i = 0; i < faces.length; i++) {
			Face face = faces[i];
			Float age = face.getAttribute(FaceAttributeId.AGE);
	        Float gender = face.getAttribute(FaceAttributeId.GENDER);
	        long end = System.currentTimeMillis();
	        System.out.println("age:"+age+",gender:"+gender+",duration="+(end-start));
	        notifyService.sendNotify(age, gender);
		}
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
