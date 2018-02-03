package cz.muni.fi.astrocamera.entity;

import java.math.BigDecimal;

/**
 *
 * This entity holds telescope data which are used for updating information in
 * FITS files
 *
 * @author Karel Auf
 */
public class TeleData {

    private BigDecimal raNum;   //right ascension in degree format
    private BigDecimal decNum;  //declination in degree format
    private String raString;    //right ascension in hours minutes seconds format
    private String decString;   //declination in degrees minutes seconds format
    private String longitude;   //longitude of telescope
    private String latitude;    //latitude of telescope
    private String elevation;   //elevation above sea level of telescope
    private String observer;

    public TeleData() {
    }

    public BigDecimal getRaNum() {
        return raNum;
    }

    public void setRaNum(String raNum) {
        this.raNum = new BigDecimal(raNum);
    }

    public BigDecimal getDecNum() {
        return decNum;
    }

    public void setDecNum(String decNum) {
        this.decNum = new BigDecimal(decNum);
    }

    public String getRaString() {
        return raString;
    }

    public void setRaString(String raString) {
        this.raString = raString;
    }

    public String getDecString() {
        return decString;
    }

    public void setDecString(String decString) {
        this.decString = decString;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getElevation() {
        return elevation;
    }

    public void setElevation(String elevation) {
        this.elevation = elevation;
    }

    public String getObserver() {
        return observer;
    }

    public void setObserver(String observer) {
        this.observer = observer;
    }
    
    

}
