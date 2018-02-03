package cz.muni.fi.astrocamera.entity;

/**
 * This entity holds telescope network information which are used for connecting
 * to telescope
 *
 * @author Karel
 */
public class Telescope {

    private int port;
    private String address;

    public Telescope() {
    }

    public Telescope(int port, String address) {
        this.port = port;
        this.address = address;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

}
