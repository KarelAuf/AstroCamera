/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.muni.fi.astrocamera.fits;

import cz.muni.fi.astrocamera.entity.MeteoData;
import cz.muni.fi.astrocamera.entity.TeleData;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import static java.lang.Thread.sleep;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ResourceBundle;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;
import nom.tam.util.BufferedFile;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.log4j.Logger;
import static cz.muni.fi.astrocamera.fits.HJD.computeHJD;

/**
 *
 * @author Karel Auf
 */
public class FitsFileUpdateImpl implements FitsFileUpdate {

    private final static Logger logger = Logger.getLogger(FitsFileUpdateImpl.class);
    private String fileName;
    private MeteoData meteodata;
    private TeleData teledata;
    private String objectName = "";
    private final javax.swing.JTextArea textArea;
    private Thread t;
    private Date date;
    private final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
    private final javax.swing.JCheckBox altFileName;
    private String imageType = "";
    private String filter = "";
    private String expTime = "";
    private String jd = "";
    private String ra = "";
    private String dec = "";

    public FitsFileUpdateImpl(String fileName, MeteoData meteodata, TeleData teledata, String objectName, javax.swing.JTextArea textArea, javax.swing.JCheckBox altFileName) {
        this.fileName = fileName;
        this.meteodata = meteodata;
        this.teledata = teledata;
        this.objectName = objectName;
        this.textArea = textArea;
        this.altFileName = altFileName;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public MeteoData getMeteodata() {
        return meteodata;
    }

    public void setMeteodata(MeteoData meteodata) {
        this.meteodata = meteodata;
    }

    public TeleData getTeledata() {
        return teledata;
    }

    public void setTeledata(TeleData teledata) {
        this.teledata = teledata;
    }

    public String getObjectName() {
        return objectName;
    }

    public void setObjectName(String objectName) {
        this.objectName = objectName;
    }

    public void update(String fileName, MeteoData meteodata, TeleData teledata, String objectName, javax.swing.JTextArea textArea) {
        logger.debug("update()");
        Fits f;
        try {
            sleep(2000); //to ensure that file is fully created before updating it
            f = new Fits(fileName);
            f.read();
            Header hdr = f.getHDU(0).getHeader();

            //adds/updates header's value
            
            if (objectName.length() > 0 || hdr.containsKey("OBJECT") == false) hdr.addValue("OBJECT", objectName, "object name");
            this.objectName = hdr.getStringValue("OBJECT");
            hdr.addValue("RA", getRa(teledata.getRaNum()), "right ascension in [hours:minutes:seconds]");
            hdr.addValue("DEC", getDec(teledata.getDecNum()), "declination in [degrees:minutes:seconds]");

            hdr.addValue("RA1", teledata.getRaNum().setScale(6, BigDecimal.ROUND_HALF_UP).toPlainString(), "right ascension");
            hdr.addValue("DEC1", teledata.getDecNum().setScale(6, BigDecimal.ROUND_HALF_UP).toPlainString(), "declination");
            if (teledata.getObserver() != null) {
                hdr.addValue("OBSERVER", teledata.getObserver(), "observer");
            }
            if (teledata.getLongitude() != null) {
                String[] longParts = teledata.getLongitude().split("/");
                hdr.addValue("LONG-OBS", longParts[0], longParts[1]);
            }
            if (teledata.getLatitude() != null) {
                String[] latParts = teledata.getLatitude().split("/");
                hdr.addValue("LAT--OBS", latParts[0], latParts[1]);
            }
            if (teledata.getElevation() != null) {
                String[] elevParts = teledata.getElevation().split("/");
                hdr.addValue("ELEV-OBS", elevParts[0], elevParts[1]);
                hdr.addValue("PRES_SEA", getSeaLevelPressure(meteodata.getPressure(), new BigDecimal(elevParts[0])), "pressure at sea level in [mbar]");
            }
            
            hdr.addValue("PRES_OBS", meteodata.getPressure().toPlainString(), "pressure at observation in [mbar]");
            hdr.addValue("TEMP_IN", meteodata.getTemperatureInside().toPlainString(), "temperature in dome in [deg C]");
            hdr.addValue("TEMP_OUT", meteodata.getTemperatureOutside().toPlainString(), "temperature outside in [deg C]");
            hdr.addValue("HUMIDITY", meteodata.getHumidity().toPlainString(), "humidity in [%]");
            hdr.addValue("PYRGEOM", meteodata.getPyrgeometer().toPlainString(), "pyrgeometer in [Wm-2]");
            hdr.addValue("WIND_SP", meteodata.getWind().toPlainString(), "wind speed in [ms-1]");

            //gets header values
            getHeaderData(hdr);
            hdr.addValue("HJD", getHJD(jd, ra, dec), "Heliocentric Julian Date in the middle of exposure");

            //save changes to file
            File backupFile = new File(fileName + ".orig");//backup file of original FITS
            File file = new File(fileName);
            BufferedFile bf = null;
            try {
                Files.copy(file.toPath(), backupFile.toPath());
                bf = new BufferedFile(file, "rw");
                f.write(bf);
                Files.delete(backupFile.toPath());
                f.close();
            } catch (IOException ex) {
                date = new Date();
                textArea.insert(sdf.format(date) + " " + ResourceBundle.getBundle("cz/muni/fi/astrocamera/ui/Bundle").getString("FitsFileUpdateImpl.error.file") + fileName + ": " + ex.getMessage() + "\n", 0);
                logger.error("IO Error with file " + fileName + ": " + ex.getMessage());
            } finally {
                if (bf != null) {
                    bf.close();
                }
                f.close();
            }
            
            //alternative name for files
            if (altFileName.isSelected()) { 
                //tady zmena nazvu
                //vyresit problem s detekci vzniku ve folderwatch -2x podtrzitko v nazvu souboru pred porad cislem -- VYRESENO
                //vyresit problem s nenastavenym/nulovym filtrem a expTime
                Path source = file.toPath();    
                //imageType is BIAS
                if (imageType.compareToIgnoreCase("bias") == 0) {
                    File dir = file.getParentFile();
                    FileFilter fileFilter = new WildcardFileFilter("bias__*.fits");
                    File[] files = dir.listFiles(fileFilter);
                    long numberB = 0; 
                    if (files.length==0) {
                        numberB = 1;
                    } else {
                        for (File file1 : files) {
                            String name = file1.toString();
                            int i = name.indexOf("__") + 2;
                            int j = name.indexOf(".fits");
                            if (numberB <= Long.parseLong(name.substring(i, j))) numberB = Long.parseLong(name.substring(i, j)) + 1;
                        } 
                    } 
                    try {
                        Files.move(source, source.resolveSibling("bias__" + numberB +".fits"));
                    } catch (IOException ex) {
                        logger.error("IO Error with bias alternative file name for file" + fileName + ": " + ex.getMessage());
                    }
                } else {
                    //imageType is FLAT
                    if (imageType.compareToIgnoreCase("flat") == 0) {
                        File dir = file.getParentFile();
                        FileFilter fileFilter = new WildcardFileFilter("flat*__*.fits");
                        File[] files = dir.listFiles(fileFilter);
                        long numberF = 0; 
                        if (files.length==0) {
                            numberF = 1;
                        } else {
                            for (File file1 : files) {
                                String name = file1.toString();
                                int i = name.indexOf("__") + 2;
                                int j = name.indexOf(".fits");
                                if (numberF <= Long.parseLong(name.substring(i, j))) numberF = Long.parseLong(name.substring(i, j)) + 1;
                            } 
                        }
                        
                        try {
                            Files.move(source, source.resolveSibling("flat" + filter +  expTime + "__" + numberF + ".fits"));
                        } catch (IOException ex) {
                            logger.error("IO Error with flat alternative file name for file" + fileName + ": " + ex.getMessage());
                        }
                    } else {
                        //imageType is DARK
                        if (imageType.compareToIgnoreCase("dark") == 0) {
                            File dir = file.getParentFile();
                            FileFilter fileFilter = new WildcardFileFilter("dark*__*.fits");
                            File[] files = dir.listFiles(fileFilter);
                            long numberD = 0; 
                            if (files.length==0) {
                                numberD = 1;
                            } else {
                                for (File file1 : files) {
                                    String name = file1.toString();
                                    int i = name.indexOf("__") + 2;
                                    int j = name.indexOf(".fits");
                                    if (numberD <= Long.parseLong(name.substring(i, j))) numberD = Long.parseLong(name.substring(i, j)) + 1;
                                } 
                            }
                            try {
                                Files.move(source, source.resolveSibling("dark"  + expTime + "__" + numberD + ".fits"));
                            } catch (IOException ex) {
                                logger.error("IO Error with dark alternative file name for file" + fileName + ": " + ex.getMessage());
                            }
                        } else {
                            //other imageType, file name is name of the object                            
                            if (/*imageType.compareToIgnoreCase("") == 0 &&*/ this.objectName.replaceAll("\\s+", "").compareToIgnoreCase("") != 0) {
                                
                                File dir = file.getParentFile();
                                FileFilter fileFilter = new WildcardFileFilter(this.objectName + "*__*.fits");
                                File[] files = dir.listFiles(fileFilter);
                                long numberO = 0; 
                                if (files.length==0) {
                                    numberO = 1;
                                } else {
                                    for (File file1 : files) {
                                        String name = file1.toString();
                                        int i = name.indexOf("__") + 2;
                                        int j = name.indexOf(".fits");
                                        if (numberO <= Long.parseLong(name.substring(i, j))) numberO = Long.parseLong(name.substring(i, j)) + 1;
                                    } 
                                }
                                try {
                                    Files.move(source, source.resolveSibling(this.objectName + "__" + numberO + ".fits"));
                                } catch (IOException ex) {
                                    logger.error("IO Error with alternative file name for file" + fileName + ": " + ex.getMessage());
                                }
                            }
                        }

                    }
                }

            } else {
                //name doesn't change - no valid condition meet
            }

        } catch (FitsException ex) {
            //not valid path
            textArea.insert(ex.getMessage() + "\n", 0);
            logger.error("FITS error: ", ex);
        } catch (IOException ex) {
            logger.error("IO error while modifying FITS files: ", ex);
        } catch (InterruptedException ex) {
            logger.error("Sleep error: ", ex);
        }

    }
    
    private void getHeaderData(Header hdr){
        if (hdr.containsKey("IMAGETYP")) {
                imageType = hdr.getStringValue("IMAGETYP").replaceAll("\\s+", "").replace("\\", "").replace("/", "");
                try { 
                    filter = "_" + hdr.getStringValue("FILTER").replaceAll("\\s+", "").replace("\\", "").replace("/", "");
                    //textArea.insert("filter loaded \n", 0);
                } catch (Exception ex){
                    //textArea.insert("filter error " + ex.getMessage() + "\n", 0);
                    filter = "";
                }
                try {
                   expTime = "_" + hdr.getBigDecimalValue("EXPTIME").setScale(1, BigDecimal.ROUND_HALF_UP); 
                   //textArea.insert("exptime loaded \n", 0);
                } catch (Exception ex) {
                    //textArea.insert("exptime error " + ex.getMessage() + "\n", 0);
                    expTime = "";
                }

            } else {
                imageType = "";
            }
            try { 
                    jd = Double.toString(hdr.getDoubleValue("JD")).replaceAll("\\s+", "").replace("\\", "").replace("/", "");                      
                } catch (Exception ex){                        
                    jd = "";
                }
            try { 
                    ra = teledata.getRaNum().setScale(6, BigDecimal.ROUND_HALF_UP).toPlainString();                    
                } catch (Exception ex){                       
                    ra = "";
                }
            try { 
                    dec = teledata.getDecNum().setScale(6, BigDecimal.ROUND_HALF_UP).toPlainString();                    
                } catch (Exception ex){                    
                    dec = "";
                }
    }

    private String getRa(BigDecimal ra) {
        //DecimalFormat df = new DecimalFormat("00.########");
        DecimalFormat df = new DecimalFormat("00.##");
        BigDecimal help = ra.divide(new BigDecimal("15"), RoundingMode.HALF_UP);
        BigDecimal fraction = help.remainder(BigDecimal.ONE);
        help = help.subtract(fraction).stripTrailingZeros();
        String hours = df.format(help);

        help = fraction.multiply(new BigDecimal("60"));
        fraction = help.remainder(BigDecimal.ONE);
        help = help.subtract(fraction).stripTrailingZeros();
        String minutes = df.format(help);

        help = fraction.multiply(new BigDecimal("60")).stripTrailingZeros();
        help = help.setScale(2, BigDecimal.ROUND_HALF_UP);
        String seconds = df.format(help);
        return (hours + ":" + minutes + ":" + seconds).replace(',', '.');
    }

    private String getDec(BigDecimal dec) {
        //DecimalFormat df = new DecimalFormat("00.########");
        DecimalFormat df = new DecimalFormat("00.##");
        BigDecimal help = dec;
        BigDecimal fraction = help.remainder(BigDecimal.ONE);
        help = help.subtract(fraction).stripTrailingZeros();
        String hours = df.format(help);

        help = fraction.multiply(new BigDecimal("60")).abs();
        fraction = help.remainder(BigDecimal.ONE);
        help = help.subtract(fraction).stripTrailingZeros();
        String minutes = df.format(help);

        help = fraction.multiply(new BigDecimal("60")).stripTrailingZeros();
        //
        help = help.setScale(2, BigDecimal.ROUND_HALF_UP);
        String seconds = df.format(help);
        return (hours + ":" + minutes + ":" + seconds).replace(',', '.');
    }
    
    private double getHJD(String jd, String ra, String dec){
        double hjd = 0;        
        if (jd.length()==0 || ra.length()==0 || dec.length()==0){
            return hjd;
        }
        double julianDate = Double.parseDouble(jd);
        double rightAscension = Double.parseDouble(ra);
        double declination = Double.parseDouble(dec);
        try{
            hjd = computeHJD(julianDate, rightAscension, declination);
        } catch(IllegalArgumentException ex ){
            textArea.insert("HJD compute error \n", 0);
        }
        //jd = jd + 1/2 EXP - spravne jednotky!!!
        //hjd = jd - r/c * [sin(dec)*sin(decSun) + cos(dec)*cos(decSun)*cos(ra - raSun) ]
        //r = distance Sun-Observer
        double adj;
        try{
            adj = (Double.parseDouble(expTime.substring(1))/2) * 0.00001;
        }catch (Exception ex){
            adj = 0;
        }
        
        hjd = hjd + adj;
        
        return hjd;
    }

    private String getSeaLevelPressure(BigDecimal press, BigDecimal elev) {
        BigDecimal seaPress = press.add(elev.divide(new BigDecimal("8.3"), 1, BigDecimal.ROUND_HALF_UP));        //.setScale(1, BigDecimal.ROUND_HALF_UP).stripTrailingZeros()
        return seaPress.toPlainString();
    }

    @Override
    public void run() {
        update(fileName, meteodata, teledata, objectName, textArea);
    }

    public void start() {
        if (t == null) {
            t = new Thread(this);
            t.start();
        }
    }

}
