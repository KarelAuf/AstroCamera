package cz.muni.fi.astrocamera.entity;

import java.math.BigDecimal;

/**
 * This entity holds Meteorological data which are used for updating information
 * in FITS files
 *
 * @author Karel Auf
 */
public class MeteoData {

    private BigDecimal temperatureInside; //temperature in globe variable
    private BigDecimal temperatureOutside; //temperature outside
    private BigDecimal pressure; //pressure variable
    private BigDecimal wind; //windspeed variable
    private BigDecimal pyrgeometer; //pyrgeometer variable
    private BigDecimal humidity; //humidity variable
    private BigDecimal time; //time variable

    public MeteoData() {
    }

    public BigDecimal getTemperatureOutside() {
        return temperatureOutside;
    }

    public void setTemperatureOutside(String te) {
        this.temperatureOutside = new BigDecimal(te).setScale(1, BigDecimal.ROUND_HALF_UP).stripTrailingZeros();
    }

    public BigDecimal getTemperatureInside() {
        return temperatureInside;
    }

    public void setTemperatureInside(String te) {
        this.temperatureInside = new BigDecimal(te).setScale(1, BigDecimal.ROUND_HALF_UP).stripTrailingZeros();
    }

    public BigDecimal getPressure() {
        return pressure;
    }

    public void setPressure(String pr) {
        this.pressure = new BigDecimal(pr).setScale(1, BigDecimal.ROUND_HALF_UP).stripTrailingZeros();
    }

    public BigDecimal getWind() {
        return wind;
    }

    public void setWind(String w) {
        this.wind = new BigDecimal(w).setScale(1, BigDecimal.ROUND_HALF_UP).stripTrailingZeros();
    }

    public BigDecimal getPyrgeometer() {
        return pyrgeometer;
    }

    public void setPyrgeometer(String py) {
        this.pyrgeometer = new BigDecimal(py).setScale(1, BigDecimal.ROUND_HALF_UP).stripTrailingZeros();
    }

    public BigDecimal getHumidity() {
        return humidity;
    }

    public void setHumidity(String h) {
        this.humidity = new BigDecimal(h).setScale(1, BigDecimal.ROUND_HALF_UP).stripTrailingZeros();
    }

    public BigDecimal getTime() {
        return time;
    }

    public void setTime(BigDecimal time) {
        this.time = time;
    }

}
