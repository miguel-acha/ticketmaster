package edu.upb.tickmaster;

import java.io.Serializable;

/**
 * Clase de configuración simple que implementa Serializable para poder
 * guardarse en disco.
 */
public class Configuracion implements Serializable {

    private static final long serialVersionUID = 1L;

    private String host;
    private int puerto;
    private String usuario;

    public Configuracion(String host, int puerto, String usuario) {
        this.host = host;
        this.puerto = puerto;
        this.usuario = usuario;
    }

    // Getters y Setters
    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPuerto() {
        return puerto;
    }

    public void setPuerto(int puerto) {
        this.puerto = puerto;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    @Override
    public String toString() {
        return "Configuracion{" +
                "host='" + host + '\'' +
                ", puerto=" + puerto +
                ", usuario='" + usuario + '\'' +
                '}';
    }
}
