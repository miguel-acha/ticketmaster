/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package edu.upb.tickmaster;

import edu.upb.tickmaster.httpserver.LoadBalancerServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Main class para el Load Balancer
 * 
 * @author rlaredo
 */
public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        logger.info("  TICKMASTER LOAD BALANCER");

        LoadBalancerServer loadBalancer = new LoadBalancerServer();

        if (loadBalancer.start()) {
            logger.info("Load Balancer corriendo en puerto 1915");
            logger.info("Reenviando peticiones a http://localhost:1914");

            try {
                logger.info("Presiona Ctrl+C para detener...");
                Thread.currentThread().join();
            } catch (InterruptedException e) {
                logger.info("Interrumpido, deteniendo...");
            }

            loadBalancer.stop();
            logger.info("Load Balancer detenido");
        } else {
            logger.error("No se pudo iniciar el Load Balancer");
            System.exit(-1);
        }
    }
}

// mvn exec:java -pl tickmaster-load-balancer
