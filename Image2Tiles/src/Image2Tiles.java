
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.FileInputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javax.imageio.ImageIO;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.stream.JsonParser;

import org.apache.commons.io.FileUtils;

import javax.json.stream.*;
import java.io.FileOutputStream;
import java.io.IOException;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.awt.image.ImageProducer;
import java.awt.image.RGBImageFilter;
import java.io.File;
import java.util.LinkedList;
import java.util.Queue;
import java.util.regex.Pattern;

public class Image2Tiles {


    static class MyAuthenticator extends Authenticator {
    	private String userName;
    	private String password;
    	
    	public MyAuthenticator(String givenUserName, String givenPassword){
    		this.userName = givenUserName;
    		this.password = givenPassword;
    		
    	}
   
        public PasswordAuthentication getPasswordAuthentication() {
            return (new PasswordAuthentication(this.userName, this.password.toCharArray()));
        }
    }// End of class MyAuthenticator 
    
    
    
    
    
    static final String baseURL = "https://api.planet.com/v0/scenes/";
    static final String pathOfShell = "C:/Program Files/QGIS Essen/OSGeo4W.bat";
    static final String pathToStoreUpdatedTime = "C:/Users/bregmi1/Lab";
    static final String pathToStoreImages = "C:/Users/bregmi1/Lab/Project1";
    static final String downloadFromDate = "2016/05/12";
    static final String planetLabUsername = "bregmi1@uno.edu";
    static final String planetLabPassword = "planetlab";
    
    static final String[] sceneType = {"rapideye", "landsat", "ortho"};
    static LocalDateTime lastUpdatedTime;
    static Queue<String> downloadedRapidEyeImages = new LinkedList<String>();
    static Queue<String> downloadedOrthoImages = new LinkedList<String>();
    static Queue<String> downloadedLandsatImages = new LinkedList<String>();
    
    
    public static void main(String[] args){
    	
    	MyAuthenticator auth = new MyAuthenticator(planetLabUsername, planetLabPassword);
        Authenticator.setDefault(auth);

        try{
        	File file = new File("C:/Users/bregmi1/Lab/Mosaic/LastUpdatedTime.ser");
        	if(file.exists() && file.isFile()){
	        	ObjectInputStream input = new ObjectInputStream(new FileInputStream(file));
	        	lastUpdatedTime = (LocalDateTime) input.readObject();
	        	input.close();
        	}
        	else{
        		lastUpdatedTime = LocalDateTime.of(2016, 05, 15, 00, 00,00);
        	}
        	
        }
        catch(Exception e){
        	System.out.println("Error in reading last updated time");
        	e.printStackTrace();
        }
       
        double[] point1 = {-117.48, 32.83}; 
        double[] point2 = {-116.94, 32.57};
        JsonObject myObject = buildJson(point1, point2);
    
        LocalDateTime queryTime = lastUpdatedTime;
        lastUpdatedTime = LocalDateTime.now();
        
        try{
        	File file = new File("C:/Users/bregmi1/Lab/Mosaic/LastUpdatedTime.ser");
        	file.createNewFile();
        	ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream(file));
        	output.writeObject(lastUpdatedTime);
        	output.close();
        }
        catch(Exception e){
        	System.out.println("Error occured while saving updated time");
        	e.printStackTrace();
        }
        
        Queue<String> urls = new LinkedList<String>();
      
        for(int i =0; i<sceneType.length; i++){
        	String extendedURL = baseURL + sceneType[i] + "/";
        	fillQueue(myObject, queryTime, extendedURL, urls);
        	
        }
       
     
        if(urls.size() >0){
        	downloadImage(urls);
    	}
        
    	if(!downloadedRapidEyeImages.isEmpty()){
    		createAndMergeTileTrees(downloadedRapidEyeImages,"C:/Users/bregmi1/Lab/Images/RapidEye", "C:/Program Files/QGIS Essen/OSGeo4W.bat" );
    	}
    	if(!downloadedOrthoImages.isEmpty()){
    		createAndMergeTileTrees(downloadedOrthoImages,"C:/Users/bregmi1/Lab/Images/RapidEye", "C:/Program Files/QGIS Essen/OSGeo4W.bat");
    	}
    	if(!downloadedLandsatImages.isEmpty()){
    		createAndMergeTileTrees(downloadedLandsatImages,"C:/Users/bregmi1/Lab/Images/RapidEye", "C:/Program Files/QGIS Essen/OSGeo4W.bat");
    	}
    	
    	
    	
    	
    	
    	
    	
//    	File[] downloadedImages = new File("C:/Users/bregmi1/Lab/Images/RapidEye").listFiles();
//    	String pathOfShell = "C:/Program Files/QGIS Essen/OSGeo4W.bat";
//    	String pathOfWorkingDirectory = "C:/Users/bregmi1/Lab/Images/RapidEye";
//    	Queue<String> queue = new LinkedList<String>();
//    	for(File temp: downloadedImages){
//    		String path = temp.getAbsolutePath();
//    		queue.offer(path);
//    	}
//    	createAndMergeTileTrees(queue, pathOfWorkingDirectory, pathOfShell);
    	
   
        System.out.println("Done");
        

    }// End of main
    
    

    public static Queue<String> fillQueue (JsonObject givenObject, LocalDateTime givenDateTime, String givenURL, Queue<String> urls){
    	
    	String jsonQuery = givenObject.toString();
    	String timeQuery = givenDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    	String query = "?intersects=" + jsonQuery + "&published.gte=" + timeQuery;
    	
    	
    	
    	HttpURLConnection con = null;
    	InputStream ins = null;
    	URL url = null;
    	String newURLString = givenURL + query;
    	
    	
    	try{
    		url = new URL(newURLString);
    	}
    	catch(MalformedURLException e){
    		System.out.println("Invalid URL");
    		e.printStackTrace();
    	}
    	
    	
    	try{
    		con = (HttpURLConnection) url.openConnection();
    	}
    	catch(IOException e){
    		System.out.println("Exception in opening connection");
    		e.printStackTrace();
    	}
        
        
        con.setDoOutput(true);
        OutputStream os  = null;
        
        try{
        	os = con.getOutputStream();
        }
        catch(IOException e){
        	System.out.println("Exception in getting output stream from connection");
        	e.printStackTrace();
        }
        
        try{
        	os.write(query.getBytes(Charset.forName("UTF-8")));
        }
        catch(IOException e){
        	System.out.println("Exception on writing to output stream");
        	e.printStackTrace();
        }
        
        try{
        	os.close();
        }
        catch(IOException e){
        	System.out.println("Exception on closing output stream");
        	e.printStackTrace();
        }
        
        try{
        	ins = con.getInputStream();
        }
        catch(IOException e){
        	System.out.println("Exception on initializing input stream");
        	e.printStackTrace();
        }
       
        JsonParser parser = Json.createParser(ins);
        while(parser.hasNext()){
        	JsonParser.Event event = parser.next();
        	if(event == JsonParser.Event.KEY_NAME){
        		String receivedString = parser.getString();
        		switch(receivedString){
	        		case "full":
	        			parser.next();
	        			String imageUrl = parser.getString();
	        			String[] parts = imageUrl.split("/");
	        			if(!(parts[parts.length-1].equals("full"))){
		        			String[] imageType = parts[parts.length-1].split("=");
		        			if(!(imageType[imageType.length-1].equals("analytic"))){
		        				urls.offer(imageUrl);
		        			}
	        			}
	        			
	        			break;    	        		
        		}
        	}
        } 
        
        
        con.disconnect();
        
       try{
        	ins.close();  
        } 
        catch(IOException e){
        	System.out.println("Exception on closing input stream");
        	e.printStackTrace();
        }
        
      	return urls;
      	
    }// End of fillQueue
    
    
    
    
    public static void downloadImage(Queue<String> urls){
    	
    	URL newUrl = null;
        HttpURLConnection newCon = null;
        InputStream newIns = null;
        OutputStream out = null;
       
        while(!urls.isEmpty()){
        	String imageURL = urls.poll();
        	
        	try{
        		newUrl = new URL(imageURL);
        	}
        	catch(MalformedURLException e){
        		System.out.println("Invalid URL");
        		e.printStackTrace();
        	}
        	
        	try{
        		newCon = (HttpURLConnection) newUrl.openConnection();
        	}
        	catch(IOException e){
        		System.out.println("Exception in getting connection");
        		e.printStackTrace();
        	}
        	
        	try{
        		newIns = newCon.getInputStream();
        	}
        	catch(IOException e){
        		System.out.println("Exception in getting input stream");
        		e.printStackTrace();
        	}
        	String[] temp = imageURL.split("/");
        	String outputFilePath = "C:/Users/bregmi1/Lab/Images/";
        	String imageType = temp[5];
        	if(imageType.toLowerCase().equals("rapideye")){
        		outputFilePath += "RapidEye/";
        	}
        	else if(imageType.toLowerCase().equals("ortho")){
        		outputFilePath += "Ortho/";
        	}
        	else if(imageType.toLowerCase().equals("landsat")){
        		outputFilePath += "Landsat/";
        	}
        	String fileName = imageType + "-" + temp[6] + ".tiff";
        	outputFilePath += fileName ;
        	
        	if(imageType.toLowerCase().equals("rapideye")){
        		downloadedRapidEyeImages.offer(outputFilePath);
        	}
        	else if(imageType.toLowerCase().equals("ortho")){
        		downloadedOrthoImages.offer(outputFilePath);
        	}
        	else if(imageType.toLowerCase().equals("landsat")){
        		downloadedLandsatImages.offer(outputFilePath);
        	}
        	
        	
        	
        	try{
        		out = new FileOutputStream(new File(outputFilePath));
        	}
        	catch(IOException e){
        		System.out.print("Exception in initializing File output stream");
        		e.printStackTrace();
        	}
        	int read = 0;
            byte[] bytes = new byte[64* 1024];
           
            try{
	            while((read = newIns.read(bytes)) != -1){
	            	out.write(bytes,0,read);
	            }
            }
            catch(IOException e){
            	System.out.print("Exception in reading and writing to file");
            	e.printStackTrace();
            }
           
        }
        try{
        	out.close();
        }
        catch(IOException e){
        	System.out.println("Exception in closing output stream");
        	e.printStackTrace();
        }
        
        try{        
        	newIns.close();
        }
        catch(IOException e){
        	System.out.println("Exception in closing input stream");
        	e.printStackTrace();
        }
        
        newCon.disconnect();
    	
    }// End of downloadImage
    	
    
    
    public static void createAndMergeTileTrees(Queue<String> downloadedImages, String pathWorkingDirectory, String pathOfShell){
    	while(!downloadedImages.isEmpty()){
    		File rootTiles = new File(pathWorkingDirectory +"/rootTiles");
    		if(!rootTiles.exists()){
    			rootTiles.mkdirs();
    		}
    		String pathOfImage = downloadedImages.poll();
    		//System.out.println("pathOfImage: " + pathOfImage);
    		String directoryName = pathOfImage.split(Pattern.quote("."))[0];
    		//System.out.println("DirectoryName: " + directoryName);
    		createTiles(pathOfShell, pathOfImage, directoryName);
    		String pathToNewFolder = pathOfImage.split(Pattern.quote("."))[0];
    		//System.out.println("PathToNewFolder: " + pathToNewFolder);
    		mergeTileTrees(rootTiles.getAbsolutePath(), pathToNewFolder);
    	}
    }
    
    
    public static void createTiles(String pathOfShell, String pathOfImage, String directoryName){
    	String command = pathOfShell + " gdal2tiles " + pathOfImage + " " + directoryName;
    	System.out.println(command);
    	Runtime runtime = Runtime.getRuntime();
    	try{
    		Process p = runtime.exec(command);
    		p.waitFor();
    		p.destroy();
    	}
    	catch(Exception e){
    		System.out.println("Error occured while creating tiles");
    		e.printStackTrace();
    	}
    	System.out.println("Done creating tiles");	
    }// End of createTiles
    
    public static void mergeTileTrees(String pathToOldFolder, String pathToNewFolder){
		File tiles = new File(pathToOldFolder);
		File[] levels = tiles.listFiles();
		File newTiles = new File(pathToNewFolder);
		File[] newLevels = newTiles.listFiles();
		
		
		if(newLevels != null && newLevels.length >0){
			for(File newlyGeneratedLevels: newLevels){
				
				String newLvl = newlyGeneratedLevels.getName();
				boolean present = false;
				for(File oldLevels: levels){
					String oldLvl = oldLevels.getName();
					
					// if a certain level directory is present in the old tile tree
					if(oldLvl.equals(newLvl)){
						present = true;
						
						// look in the folder i.e look the coordinates
						File[] newCoordinates = newlyGeneratedLevels.listFiles();
						File[] oldCoordinates = oldLevels.listFiles();
						
						if(newCoordinates != null && newCoordinates.length > 0){
							for(File newCoord: newCoordinates){
								String newCoordNum = newCoord.getName();
								boolean coordinatesPresent = false;
								
								for(File oldCoord: oldCoordinates){
									String oldCoordNum = oldCoord.getName();
									
									// if the given coordinates is present
									if(oldCoordNum.equals(newCoordNum)){
										coordinatesPresent = true;
										
										File[] oldRasterImages = oldCoord.listFiles();
										File[] newRasterImages = newCoord.listFiles();
										
										if(newRasterImages != null && newRasterImages.length > 0){
											for(File newRaster: newRasterImages){
												String newRasterNum = newRaster.getName();
												boolean rasterPresent = false;
												for(File oldRaster: oldRasterImages){
													String oldRasterNum = oldRaster.getName();
													// The raster num is present in old tile tree
													if(oldRasterNum.equals(newRasterNum)){
														rasterPresent = true;
														BufferedImage oldImage = null;
														BufferedImage newImage = null;
														try{
															oldImage = ImageIO.read(oldRaster);
															newImage = ImageIO.read(newRaster);
														}
														
														
														catch(Exception e){
															System.out.println("Error in reading the images");
															e.printStackTrace();
														}
														
														
														BufferedImage oldImageTransparent = makeWhiteTransparent(oldImage);
														BufferedImage newImageTransparent = makeWhiteTransparent(newImage);
														int width = Math.max(oldImage.getWidth(), newImage.getWidth());
														int height = Math.max(oldImage.getHeight(), newImage.getHeight());
														BufferedImage combined = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
														Graphics2D graphics = (Graphics2D) combined.getGraphics();
														graphics.drawImage(newImageTransparent,0,0, null);
														graphics.drawImage(oldImageTransparent, 0, 0, null);
														try{
															ImageIO.write(combined, "png", oldRaster);
														}
														catch(Exception e){
															e.printStackTrace();
														}
														//System.out.println("completed rendering");
													}
												}
												
												if(!rasterPresent){
													try{
														//System.out.println("The raster image is not present in old tile tree");
														FileUtils.moveFileToDirectory(newRaster, oldCoord, true);
														//System.out.println("Completed Moving");
													}
													catch(Exception e){
														//System.out.println("Error in moving images");
														e.printStackTrace();
													}
												}
											}
										}
									}
								}
								
								if(! coordinatesPresent){
									try{
										//System.out.println("The coordinate is not present in the old tile tree");
										FileUtils.moveToDirectory(newCoord, oldLevels, true);
										//System.out.println("Done moving coordinate folder");
									}
									catch(Exception e){
										//System.out.println("Error in moving a coord directory");
										e.printStackTrace();
									}
								}
								
							}
						}
					
					}
				}
				
				if(!present){
					try{
						//System.out.println("The level is not present in old tile tree");
						FileUtils.moveToDirectory(newlyGeneratedLevels, tiles, true);
						//System.out.println("Done moving level folders");
					}
					catch(Exception e){
						//System.out.println("Error in moving level folders");
						e.printStackTrace();
					}
					
				}
			}
		}
			
			
		try{
			FileUtils.deleteDirectory(newTiles);
		}
		catch(Exception e){
			e.printStackTrace();
		}
			
		}
	
    public static BufferedImage makeWhiteTransparent(BufferedImage img){
    	BufferedImage dst = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_4BYTE_ABGR);
		dst.getGraphics().drawImage(img, 0, 0, null);
		int markerRGB = Color.WHITE.getRGB();
		int width = dst.getWidth();
		int height = dst.getHeight();
		for(int x = 0; x < width; x++){
			for(int y = 0; y < height; y++){
				int rgb = dst.getRGB(x, y);
				if ( ( rgb | 0xFF000000 ) == markerRGB ) {
					int value = 0x00FFFFFF & rgb;
					dst.setRGB(x, y, value); 
				}
			}
		}
		return dst;
	}
    
    
    
    
    
    /**
     * 
     * @param point1: Upper left corner
     * @param point2: Lower Rigth corner
     * @return json object representing a rectangle representated by point1 and point2
     */
    public static JsonObject buildJson(double[] point1, double[] point2){
    	JsonObject returnValue = null;
    	if(point1.length == 2 && point2.length ==2){
    		JsonObject json = Json.createObjectBuilder()
    				.add("type", "Polygon" )
    				.add("coordinates", Json.createArrayBuilder()
    					.add(Json.createArrayBuilder()
    							.add(Json.createArrayBuilder()
    									.add(point1[0])
    									.add(point1[1]))
    							.add(Json.createArrayBuilder()
    									.add(point2[0])
    									.add(point1[1]))
    							.add(Json.createArrayBuilder()
    									.add(point2[0])
    									.add(point2[1]))
    							.add(Json.createArrayBuilder()
    									.add(point1[0])
    									.add(point2[1]))
    							.add(Json.createArrayBuilder()
    									.add(point1[0])
    									.add(point1[1]))
    							)
    					).build();
    		System.out.println(json);
    		returnValue = json;   		
    	}
    	return returnValue;
    }// End of buildJson
}