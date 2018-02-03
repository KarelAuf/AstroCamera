package cz.muni.fi.astrocamera.coordinates;

import cz.muni.fi.astrocamera.entity.Telescope;

/**
 *
 * @author Karel Auf
 */
public interface TelescopeControl {

    /**
     * Sends coordinates to Telescope
     *
     * @param telescope object holding information about ip adress and port for
     * communication
     * @param password password for privileged mode on telescope control
     * (password from TCS)
     * @param ra String with coordinates for Right Ascension in format hours
     * minutes seconds
     * @param dec String with coordinates for Declination in format degrees
     * minutes seconds
     * @param textArea area visible to user, used for status updates
     */
    public void sendData(Telescope telescope, String password, String ra, String dec, javax.swing.JTextArea textArea);

    /**
     * Retrieve current coordinates from Telescope
     *
     * @param telescope object holding information about ip adress and port for
     * communication
     * @param textArea area visible to user, used for status updates
     * @return String containing coordinates in format #RA#Dec#\n or empty
     * String
     */
    public String retrieveData(Telescope telescope, javax.swing.JTextArea textArea);
}
