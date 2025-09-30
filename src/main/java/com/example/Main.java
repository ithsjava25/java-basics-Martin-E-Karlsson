package com.example;

import com.example.api.ElpriserAPI;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.example.api.ElpriserAPI.Prisklass;

public class Main {
    public static void main(String[] args) {
        ElpriserAPI api = new ElpriserAPI();
        List<String> listOfArgs = Arrays.asList(args);
        Prisklass pricingZone = Prisklass.SE1;
        LocalDate date;

        if (listOfArgs.contains("--zone")) {
            String zone = listOfArgs.get(listOfArgs.indexOf("--zone") + 1);
            try {
                pricingZone = Prisklass.valueOf(zone.toUpperCase());
            } catch (IllegalArgumentException e){
                System.out.println("--zone must be followed by one of the specific zone values:" +
                        " SE1, SE2, SE3, SE4");
            }
        } else {
            throw new IllegalArgumentException("It is required to provide a --zone argument, " +
                    "provide argument --help for more information");
        }
        if (listOfArgs.contains("--date")) {
            date = LocalDate.parse(listOfArgs.get(listOfArgs.indexOf("--date") + 1));
        } else {
            date = LocalDate.now();
        }

        List<ElpriserAPI.Elpris> priceList = api.getPriser(date, pricingZone);




        for (var arg : args)
            System.out.println(arg);

        LocalDate datum = LocalDate.parse(args[3]);
        //        IO.println(dagensPriser.toString());
        double prisSumma = 0.0;
        int antalTimIntervall = priceList.toArray().length;
        for (var elpris : priceList) {
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
