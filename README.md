# Image2Tiles
This program downloads images from PlanetLab website ([planet.com](https://www.planet.com/)) based on the given filters and break those images to tiles so that the image can be overlaid in maps like Google maps. 

The filters used are date and area only. The images will be downloaded only if the published date of the image is greater than or equal to the given date. The area of which the image is to be downloaded is given by the longitude and latitude of upper left corner and the lower right corner. The are covered will be a rectangle.

The program was built and tested on Windows 8.1. For other operating softwares, some modifications might be needed.

#Requirements
The program uses third party library and applications so some of the libraries and applications needs to be downloaded and installed before using this program. Following are some of the libraries and applications used:
* [javax.json-1.0.4](http://central.maven.org/maven2/org/glassfish/javax.json/1.0.4/javax.json-1.0.4.jar)
* 
