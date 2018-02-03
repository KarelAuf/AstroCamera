package cz.muni.fi.astrocamera.coordinates;

import cz.muni.fi.astrocamera.entity.Telescope;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ResourceBundle;
import org.apache.log4j.Logger;

/**
 *
 * @author Karel Auf
 */
public class TelescopeControlImpl implements TelescopeControl {

    private final static Logger logger = Logger.getLogger(TelescopeControlImpl.class);
    private Date date;
    private SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

    public TelescopeControlImpl() {
    }

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
    @Override
    public void sendData(Telescope telescope, String password, String ra, String dec, javax.swing.JTextArea textArea) {
        logger.debug("sendData()");
        //validate telescope
        try {
            validate(telescope);
        } catch (IllegalArgumentException ex) {
            logger.error("Invalid Telescope object.", ex);
            date = new Date();
            textArea.insert(sdf.format(date) + " " + ResourceBundle.getBundle("cz/muni/fi/astrocamera/ui/Bundle").getString("TelescopeControlImpl.error.port") + ex.toString() + "\n", 0);
            return;
        }
        //validate coordinates
        try {
            if (ra.length() < 9 || dec.length() < 9) {
                throw new IllegalArgumentException();
            }
            //convert ra and dec to appropriate format
            ra = ra.replace(" ", "");
            dec = dec.replace(" ", "");

            ra = ra.substring(0, 9);
            if (dec.charAt(0) == '-') {
                dec = dec.substring(0, 10);
            } else {
                dec = dec.substring(0, 9);
            }
            //check whether they can be converted into numbers
            BigDecimal raD = new BigDecimal(ra);
            BigDecimal decD = new BigDecimal(dec);
        } catch (Exception ex) {
            logger.error("Invalid coordinates", ex);
            date = new Date();
            textArea.insert(sdf.format(date) + " " + ResourceBundle.getBundle("cz/muni/fi/astrocamera/ui/Bundle").getString("TelescopeControlImpl.error.coordinates"), 0);
            return;
        }
        int position = 0;//insignificant variable for setting east or west position
        logger.debug("Connecting to " + telescope.getAddress() + ":" + telescope.getPort() + "\n");
        try (Socket socket = new Socket(telescope.getAddress(), telescope.getPort()); PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            //InputStream inFromServer = socket.getInputStream();
            //DataInputStream in = new DataInputStream(inFromServer);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            out.println("GLLG " + password); //login
            //if (in.read() == 49) { //49 is ASCII decimal value for 1
            if (in.readLine().contains("1")) { //49 is ASCII decimal value for 1
                //success
                //in.read();  //remove CRLF from InputStream
                //in.read();
                out.println("TSRA " + ra + " " + dec + " " + position);

                //if (in.read() == 49) { //read response from server
                if (in.readLine().contains("1")) {
                    //success 
                    date = new Date();
                    textArea.insert(sdf.format(date) + " " + ResourceBundle.getBundle("cz/muni/fi/astrocamera/ui/Bundle").getString("TelescopeControlImpl.success.send"), 0);
                } else {
                    //error sending coordinates
                    logger.debug("Invalid coordinates send to server.");
                    date = new Date();
                    textArea.insert(sdf.format(date) + " " + ResourceBundle.getBundle("cz/muni/fi/astrocamera/ui/Bundle").getString("TelescopeControlImpl.error.send"), 0);
                }
            } else {
                //failure
                date = new Date();
                textArea.insert(sdf.format(date) + " " + ResourceBundle.getBundle("cz/muni/fi/astrocamera/ui/Bundle").getString("TelescopeControlImpl.error.password"), 0);
            }
        } catch (IOException ex) {
            logger.error("Error while communicating with server", ex);
            date = new Date();
            textArea.insert(sdf.format(date) + " " + ResourceBundle.getBundle("cz/muni/fi/astrocamera/ui/Bundle").getString("TelescopeControlImpl.error.communication") + ex.toString() + "\n", 0);
        }

    }

    /**
     * Retrieve current coordinates from Telescope
     *
     * @param telescope object holding information about ip adress and port for
     * communication
     * @param textArea area visible to user, used for status updates
     * @return String containing coordinates in format #RA#Dec#\n or empty
     * String
     */
    @Override
    public String retrieveData(Telescope telescope, javax.swing.JTextArea textArea) {
        logger.debug("retrieveData()");
        try (Socket socket = new Socket(telescope.getAddress(), telescope.getPort()); PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            try {
                validate(telescope);
            } catch (IllegalArgumentException ex) {
                logger.error("Invalid Telescope object.", ex);
                date = new Date();
                textArea.append(sdf.format(date) + " " + ResourceBundle.getBundle("cz/muni/fi/astrocamera/ui/Bundle").getString("TelescopeControlImpl.error.port") + ex.toString() + "\n");
                return "";
            }
            InputStream inFromServer = socket.getInputStream();
            DataInputStream in = new DataInputStream(inFromServer);

            out.println("TRRD");
            byte[] array = new byte[100];
            int i = 0;
            byte symbol;
            //read response
            while ((symbol = in.readByte()) != 13) {
                array[i] = symbol;
                i++;
            }
            String response = new String(array, "UTF-8");

            String[] responseParts = response.split(" ");
            response = "#" + responseParts[0] + "#" + responseParts[1] + "#\n";
            if (response.length() > 9) {
                date = new Date();
                textArea.append(sdf.format(date) + " " + ResourceBundle.getBundle("cz/muni/fi/astrocamera/ui/Bundle").getString("TelescopeControlImpl.success.retrieve"));
            } else {
                date = new Date();
                textArea.append(sdf.format(date) + " " + ResourceBundle.getBundle("cz/muni/fi/astrocamera/ui/Bundle").getString("TelescopeControlImpl.error.retrieve"));
            }
            return response;
        } catch (IOException ex) {
            date = new Date();
            textArea.append(sdf.format(date) + " " + ResourceBundle.getBundle("cz/muni/fi/astrocamera/ui/Bundle").getString("TelescopeControlImpl.error.retrieve") + ex.toString() + "\n");
            logger.error("Error while communicating with server.", ex);
        } catch (IllegalArgumentException ex) {
            logger.error("Invalid Telescope object.", ex);
        }
        return "";

    }

    /**
     * Validates telescope parameters
     *
     * @param telescope telescope object for evaluation
     */
    private static void validate(Telescope telescope) {
        logger.debug("validate()");
        if (telescope == null) {
            throw new IllegalArgumentException("Telescope pointer is null");
        } else {
            if (telescope.getAddress() == null || telescope.getAddress().length() < 7 || telescope.getAddress().length() > 15) {
                throw new IllegalArgumentException("Invalid telescope IP adress");
            }
            if (telescope.getPort() < 2000 || telescope.getPort() > 2004) {
                throw new IllegalArgumentException("Invalid telescope port number");
            }
        }

    }

}
