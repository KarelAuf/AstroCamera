package cz.muni.fi.astrocamera.coordinates;

import cz.muni.fi.astrocamera.entity.AstronomicalObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import org.apache.log4j.Logger;

/**
 *
 * @author Karel Auf
 */
public class AstroObjectServiceImpl implements AstroObjectService {

    private final static Logger logger = Logger.getLogger(AstroObjectServiceImpl.class);

    public AstroObjectServiceImpl() {
    }

    /**
     * Gets Right Ascension (RA) and Declination (Dec) for single Astronomical
     * Object
     *
     * @param astroObject AstronomicalObject with filled name
     * @return AstronomicalObject with filled name, RA and DEC or only name in
     * case of any error
     */
    @Override
    public AstronomicalObject getObjectOnline(AstronomicalObject astroObject) {
        logger.debug("getObjectOnline()");
        try {
            validate(astroObject);
            searchSimbadDatabase(astroObject);
            if (astroObject.getRa() == null) {
                searchAavsoDatabase(astroObject);
            }
        } catch (IllegalArgumentException ex) {
            logger.error("Invalid AstronomicalObject.", ex);
        }
        return astroObject;

    }

    /**
     * Gets Right Ascension (RA) and Declination (Dec) for given name
     *
     * @param file file with coordinates in format name#RA#Dec#\n
     * @param astroObject AstronomicalObject with filled name
     * @return AstronomicalObject with filled name and coordinates if found
     * otherwise only with filled name
     */
    @Override
    public AstronomicalObject loadObjectFromFile(File file, AstronomicalObject astroObject) {
        logger.debug("loadObjectFromFile()");
        try {
            validate(astroObject);
            String name = astroObject.getName();
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.contains(name)) {
                        String[] lineParts = line.split("#");
                        astroObject.setRa(lineParts[1]);
                        astroObject.setDec(lineParts[2]);
                        break;
                    }
                }
            }

        } catch (FileNotFoundException ex) {
            logger.error("File not Found.", ex);
        } catch (IOException ex) {
            logger.error("Error while reading file.", ex);
        } catch (IllegalArgumentException ex) {
            logger.error("Invalid AstronomicalObject.", ex);
        }
        return astroObject;
    }

    /**
     * Validates given AstronomicalObject if it is different from null and has
     * filled name
     *
     * @param astroObject
     */
    private static void validate(AstronomicalObject astroObject) {
        logger.debug("validate()");
        if (astroObject == null) {
            throw new IllegalArgumentException("astrobject pointer is null");
        }
        if (astroObject.getName() == null) {
            throw new IllegalArgumentException("astrobject name is null");
        }
    }

    /**
     * Searches online database for coordinates for given object name
     *
     * @param astroObject AstronomicalObject with name of queried object
     * @return AstronomicalObject containing coordinates or empty string in case of error
     */
    private AstronomicalObject searchSimbadDatabase(AstronomicalObject astroObject) {
        logger.debug("searchSimbadDatabase()");
        URL oracle;
        String inputLine1;
        String resultLine = "";

        String name = astroObject.getName();
        name = name.replace("+", "%2B"); //replace + signs for special names
        name = name.replace(" ", "+"); //replace spaces with + signs in multi word         
        try {
            oracle = new URL("http://simbad.u-strasbg.fr/simbad/sim-script?submit=submit+script&"
                    + "script=format+object+form1+%22%25IDLIST%281%29+%3A+%25COO%28RA%29%3B%25COO%28D%29%22%0D%0A"
                    + "query+id+"
                    + name //query for object
                    + "%0D%0A"
                    + "format+display");

            URLConnection yc = oracle.openConnection();
            try (BufferedReader in = new BufferedReader(new InputStreamReader(yc.getInputStream()))) {
                Boolean check = false;
                int i = 0;
                //retrieve the line with coordinates
                while ((inputLine1 = in.readLine()) != null) {
                    if (inputLine1.contains("data")) {
                        check = true;
                    }
                    if (check) {
                        i++;
                    }
                    if (i == 3) {
                        resultLine = inputLine1;
                    }
                }
            }

        } catch (MalformedURLException ex) {
            logger.error("Bad URL.", ex);
        } catch (IOException ex) {
            logger.error("Error when reading response from server.", ex);
        }
        
        logger.info("Line with coordinates: " + resultLine);
        parseInfoSimbad(astroObject, resultLine);
        return astroObject;
    }

    /**
     * Parses RA and DEC from given String line into given AstronomicalObject
     * astroObject
     *
     * @param astroObject object whose coordinates will be filled
     * @param line String which will be parsed
     * @return astroObject with filled coordinates or with null in case the
     * String doesn't contain coordinates
     */
    private AstronomicalObject parseInfoSimbad(AstronomicalObject astroObject, String line) {
        logger.debug("parseInfoSimbad()");
        if (line.length() < 3) {
            //String doesn't contain coordinates
            astroObject.setRa(null);
            astroObject.setDec(null);
        } else {
            String[] parts = line.split(":");
            String[] coo = parts[1].split(";");
            String ra = coo[0].substring(2);
            ra = getRA(ra);
            astroObject.setRa(ra);
            //if declination is positive remove + sign
            if (coo[1].contains("+")) {
                coo[1] = coo[1].substring(1);
            }
            String dec = coo[1];
            dec = getDec(dec);
            astroObject.setDec(dec);
        }
        return astroObject;
    }

    public AstronomicalObject searchAavsoDatabase(AstronomicalObject astroObject) {
        logger.debug("searchAavsoDatabase()");
        URL oracle;
        String inputLine;
        String raLine = "";
        String decLine = "";
        String name = astroObject.getName();
        name = name.replace("+", "%2B"); //replace + signs for special names
        name = name.replace(" ", "+"); //replace spaces with + signs in multi word         
        try {
            oracle = new URL("https://www.aavso.org/apps/vsp/api/chart/?format=xml&fov=60&maglimit=14.5&star="
                    + name);//object's name

            URLConnection yc = oracle.openConnection();
            try (BufferedReader in = new BufferedReader(new InputStreamReader(yc.getInputStream()))) {
                int i = 0;
                //retrieve the line with coordinates - only 1 line returned, cycle just in case of change of output format
                while ((inputLine = in.readLine()) != null) {
                    int decIndex = inputLine.indexOf("<dec>");
                    int raIndex = inputLine.indexOf("<ra>");                    
                    if (decIndex>0) {
                        decLine = inputLine.substring(decIndex, decIndex+20);
                    }
                    if (raIndex>0) {
                        raLine = inputLine.substring(raIndex, raIndex+20);
                    }                    
                }
            }
        } catch (MalformedURLException ex) {
            logger.error("Bad URL.", ex);
        } catch (IOException ex) {
            logger.error("Error when reading response from server.", ex);
        }
        if (decLine.length() > 1 && raLine.length() > 1) {
            //parse info here
            String[] decParts = decLine.substring(5).split("<");
            String[] raParts = raLine.substring(4).split("<");
            decParts[0] = decParts[0].replace(":", " ");
            raParts[0] = raParts[0].replace(":", " ");
            astroObject.setDec(getDec(decParts[0]));
            astroObject.setRa(getRA(raParts[0]));
        }else{
            astroObject.setRa(null);
            astroObject.setDec(null);
        }

        return astroObject;
    }

    /**
     * converts ra in case of unusual format from Simbad DB
     *
     * @param ra
     * @return
     */
    private String getRA(String ra) {
        ra = ra.replace(" ", "");

        switch (ra.length()) {
            case 1:
            case 2:
            case 3:
                throw new IllegalArgumentException("Invalid coordiantes");
            case 4:
                if (ra.contains(".")) {
                    throw new IllegalArgumentException("Invalid coordiantes");
                } else {
                    ra = ra + "00.00";
                }
                break;
            case 5:
                if (ra.contains(".")) {
                    if (ra.indexOf(".") != 4) {
                        throw new IllegalArgumentException("Invalid coordiantes");
                    }
                    String[] parts = ra.split("\\.");
                    ra = parts[0] + "00.00";
                } else {
                    throw new IllegalArgumentException("Invalid coordiantes");
                }
                break;
            case 6:
                if (ra.contains(".")) {
                    if (ra.indexOf(".") != 4) {
                        throw new IllegalArgumentException("Invalid coordiantes");
                    }
                    String[] parts = ra.split("\\.");
                    int i = Integer.parseInt(parts[1]);
                    i = i * 6;
                    if (i > 9) {
                        ra = parts[0] + i + ".00";
                    } else {
                        ra = parts[0] + "0" + i + ".00";
                    }

                } else {
                    ra = ra + ".00";
                }
                break;
            case 7:
                if (ra.contains(".") && ra.indexOf(".") == 6) {
                    ra = ra + "00";
                } else {
                    throw new IllegalArgumentException("Invalid coordiantes");
                }
                break;
            case 8:
                if (ra.contains(".") && ra.indexOf(".") == 6) {
                    ra = ra + "0";
                } else {
                    throw new IllegalArgumentException("Invalid coordiantes");
                }
                break;
            case 9:

            default:
                if (ra.contains(".")) {
                    ra = ra.substring(0, 9);
                } else {
                    throw new IllegalArgumentException("Invalid coordiantes");
                }
                break;
        }

        ra = ra.substring(0, 2) + " " + ra.substring(2, 4) + " " + ra.substring(4, 9);

        return ra;
    }

    /**
     * converts dec in case of unusual format from Simbad DB
     *
     * @param dec
     * @return
     */
    private String getDec(String dec) {
        boolean negative = false;
        if (dec.contains("-")) {
            negative = true;
            dec = dec.substring(1);
        }
        dec = dec.replace(" ", "");
        switch (dec.length()) {
            case 1:
            case 2:
            case 3:
                throw new IllegalArgumentException("Invalid coordiantes");
            case 4:
                if (dec.contains(".")) {
                    throw new IllegalArgumentException("Invalid coordiantes");
                } else {
                    dec = dec + "00.00";
                }
                break;
            case 5:
                if (dec.contains(".")) {
                    if (dec.indexOf(".") != 4) {
                        throw new IllegalArgumentException("Invalid coordiantes");
                    }
                    String[] parts = dec.split("\\.");
                    dec = parts[0] + "00.00";
                } else {
                    throw new IllegalArgumentException("Invalid coordiantes");
                }
                break;
            case 6:
                if (dec.contains(".")) {
                    if (dec.indexOf(".") != 4) {
                        throw new IllegalArgumentException("Invalid coordiantes");
                    }
                    String[] parts = dec.split("\\.");
                    int i = Integer.parseInt(parts[1]);
                    i = i * 6;
                    if (i > 9) {
                        dec = parts[0] + i + ".00";
                    } else {
                        dec = parts[0] + "0" + i + ".00";
                    }

                } else {
                    dec = dec + ".00";
                }
                break;
            case 7:
                if (dec.contains(".") && dec.indexOf(".") == 6) {
                    dec = dec + "00";
                } else {
                    throw new IllegalArgumentException("Invalid coordiantes");
                }
                break;
            case 8:
                if (dec.contains(".") && dec.indexOf(".") == 6) {
                    dec = dec + "0";
                } else {
                    throw new IllegalArgumentException("Invalid coordiantes");
                }
                break;
            case 9:

            default:
                if (dec.contains(".")) {
                    dec = dec.substring(0, 9);
                } else {
                    throw new IllegalArgumentException("Invalid coordiantes");
                }
                break;
        }

        dec = dec.substring(0, 2) + " " + dec.substring(2, 4) + " " + dec.substring(4, 9);
        if (negative) {
            dec = "-" + dec;
        }
        return dec;
    }

}
