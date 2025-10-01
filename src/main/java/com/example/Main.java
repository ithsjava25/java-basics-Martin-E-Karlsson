package com.example;

import com.example.api.ElpriserAPI;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.example.api.ElpriserAPI.Prisklass;

public class Main {
    public static void main(String[] args) {
        ElpriserAPI api = new ElpriserAPI();
        List<String> listOfArgs = Arrays.asList(args);
        Prisklass pricingZone = parsePricingZone(listOfArgs);//Prisklass.SE1;
        LocalDate date = parseDate(listOfArgs);



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

    private static LocalDate parseDate(List<String> listOfArgs) {
        if (listOfArgs.contains("--date")) {
            String dateString = listOfArgs.get(listOfArgs.indexOf("--date") + 1);
            try {
                return LocalDate.parse(dateString);
            } catch (DateTimeParseException e) {
                System.out.println("--date must be followed by a date in the yyyy-MM-dd format");
            }
        }
        return LocalDate.now();
    }

    private static Prisklass parsePricingZone(List<String> listOfArgs) {
        if (listOfArgs.contains("--zone")) {
            String zone = listOfArgs.get(listOfArgs.indexOf("--zone") + 1);
            try {
                return Prisklass.valueOf(zone.toUpperCase());
            } catch (IllegalArgumentException e){
                System.out.println("Unexpected zone value: --zone must be followed by one of " +
                        "the specific zone values: SE1, SE2, SE3, SE4");
            }
        } else {
            throw new IllegalArgumentException("--zone command missing: a --zone command must be provided");
        }
        return null;
    }
}
