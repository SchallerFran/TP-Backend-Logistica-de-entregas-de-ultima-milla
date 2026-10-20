package utnfc.isi.back.logistica.despacho.service.planificacion;

import utnfc.isi.back.logistica.commons.Coordenada;

import java.util.*;

/**
 * Distancias y tiempos de viaje entre todos los pares de un conjunto de puntos.
 * Los puntos se identifican por su coordenada redondeada a 4 decimales (la misma clave que la caché).
 * La matriz NO es simétrica: ida y vuelta por calle pueden diferir (sentidos de circulación).
 */
public final class MatrizDistancias {

    private final Map<Coordenada, Integer> indice;
    private final int[][] metros;
    private final int[][] segundos;
    private final FuenteDistancia[][] fuente;

    public MatrizDistancias(List<Coordenada> puntosRedondeados, int[][] metros, int[][] segundos,
                            FuenteDistancia[][] fuente) {
        this.indice = new HashMap<>();
        for (int i = 0; i < puntosRedondeados.size(); i++) {
            this.indice.put(puntosRedondeados.get(i), i);
        }
        this.metros = metros;
        this.segundos = segundos;
        this.fuente = fuente;
    }

    private int idx(Coordenada c) {
        Integer i = indice.get(c.redondeada());
        if (i == null) {
            throw new IllegalArgumentException("Punto fuera de la matriz: " + c);
        }
        return i;
    }

    public double km(Coordenada origen, Coordenada destino) {
        return metros[idx(origen)][idx(destino)] / 1000.0;
    }

    public int segundos(Coordenada origen, Coordenada destino) {
        return segundos[idx(origen)][idx(destino)];
    }

    public FuenteDistancia fuente(Coordenada origen, Coordenada destino) {
        return fuente[idx(origen)][idx(destino)];
    }

    public boolean esEstimada(Coordenada origen, Coordenada destino) {
        return fuente(origen, destino) == FuenteDistancia.ESTIMADA;
    }

    /** Cantidad de pares (i≠j) por fuente, para informar al despachante qué tan confiable es el cálculo. */
    public Map<FuenteDistancia, Integer> resumenFuentes() {
        Map<FuenteDistancia, Integer> resumen = new EnumMap<>(FuenteDistancia.class);
        for (int i = 0; i < fuente.length; i++) {
            for (int j = 0; j < fuente.length; j++) {
                if (i != j) {
                    resumen.merge(fuente[i][j], 1, Integer::sum);
                }
            }
        }
        return resumen;
    }
}
