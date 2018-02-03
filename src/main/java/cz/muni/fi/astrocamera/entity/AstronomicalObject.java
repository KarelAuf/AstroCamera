package cz.muni.fi.astrocamera.entity;

/**
 * This entity holds information required for searching for astronomical object
 * 
 * @author Karel Auf
 */
public class AstronomicalObject {
    

    private String name; //name of the object
    private String ra; //right ascension
    private String dec; //declination

    public AstronomicalObject() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRa() {
        return ra;
    }

    public void setRa(String ra) {
        this.ra = ra;
    }

    public String getDec() {
        return dec;
    }

    public void setDec(String dec) {
        this.dec = dec;
    }
    
    
    
    
}
