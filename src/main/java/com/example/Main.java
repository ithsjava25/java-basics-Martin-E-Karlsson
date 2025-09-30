package com.example;

import com.example.api.ElpriserAPI;

import java.time.LocalDate;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        for( var arg : args )
            System.out.println(arg);
        ElpriserAPI elpriserAPI = new ElpriserAPI();
        ElpriserAPI.Prisklass prisklass = ElpriserAPI.Prisklass.valueOf(args[1]);
        LocalDate datum = LocalDate.parse(args[3]);
        List<ElpriserAPI.Elpris> dagensPriser = elpriserAPI.getPriser(datum, prisklass);
//        IO.println(dagensPriser.toString());
        double prisSumma = 0.0;
        int antalTimIntervall = dagensPriser.toArray().length;
        for (var elpris : dagensPriser) {
            double timpris = elpris.sekPerKWh();
            prisSumma += timpris;
            System.out.printf("""
                    ---------------------------------------------
                    Tidsperiod: %d-%d   Pris: %f kr
                    """, elpris.timeStart().getHour(), elpris.timeEnd().getHour(), timpris);
        }
        double medelPris = prisSumma / antalTimIntervall;
        System.out.println("Medelpris: " + medelPris + " kr");

    }

}
