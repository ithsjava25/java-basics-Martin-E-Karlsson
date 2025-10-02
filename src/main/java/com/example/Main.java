package com.example;

import com.example.api.ElpriserAPI;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import com.example.api.ElpriserAPI.Prisklass;
import com.example.api.ElpriserAPI.Elpris;

public class Main {
    public static void main(String[] args) {
        // Initiera en instans av ElpriserAPI
        ElpriserAPI api = new ElpriserAPI();
        // Konvertera args arrayen till en lista för åtkomst till listmetoder
        List<String> listOfArgs = Arrays.asList(args);
        // Spara den valda prisklass zonen i Prisklass Enum variabeln pricingZone genom metoden parsePricingZone
        Prisklass pricingZone = parsePricingZone(listOfArgs);
        // Spara ett datum i date variabeln date, genom metoden parseDater som läser av listOfArgs
        LocalDate date = parseDate(listOfArgs);
        // Hämta listan med Elpris records
        List<Elpris> priceList = api.getPriser(date, pricingZone);

        // Utskrift av args för testning
        for (var arg : args)
            System.out.println(arg);
        
        double priceSum = 0.0;
        double minPrice = Double.MAX_VALUE;
        int[] minHour = new int[2];
        double maxPrice = Double.MIN_VALUE;
        int[] maxHour = new int[2];
        
        for (var price : priceList) {
            // Create a variable for each of the used Elpris record variables for easier reading
            double hourlyRate = price.sekPerKWh();
            int startHour = price.timeStart().getHour();
            int endHour = price.timeEnd().getHour();

            // Store the current price as the lowest if it's lower than minPrice
            if (minPrice > hourlyRate){
                minPrice = hourlyRate;
                minHour[0] = startHour;
                minHour[1] = endHour;
            }

            // Store the current price as the highest if it's lower than minPrice
            if (maxPrice < hourlyRate){
                maxPrice = hourlyRate;
                maxHour[0] = startHour;
                maxHour[1] = endHour;
            }

            priceSum += hourlyRate;
            System.out.printf("""
                    ---------------------------------------------
                    Tidsperiod: %d-%d   Pris: %f kr
                    """, startHour, endHour, hourlyRate);
        }
        double averagePrice = priceSum / priceList.toArray().length;;
        System.out.printf("""
        Medelpris: %.2f öre
        Lägsta pris: 0%d-0%d | %.2f
        Högsta pris: 0%d-0%d | %.2f""", averagePrice * 100,
                minHour[0], minHour[1], minPrice * 100,
                maxHour[0], maxHour[1], maxPrice * 100);

        if (listOfArgs.contains("--charging")) {
            int chargingIndex = listOfArgs.indexOf("--charging");
            String chargingWindowArg = listOfArgs.get(listOfArgs.indexOf("--charging") + 1);
            int windowSize = 0;

            switch (chargingWindowArg) {
                case "2h":
                    windowSize = 2;
                    break;
                case "4h":
                    windowSize = 4;
                    break;
                case "8h":
                    windowSize = 8;
                    break;
                default:
                    System.out.println("Unrecognized charging window value: " +
                            "--charging needs to be followed by either 2h, 4h or 8h");
            }

            double windowPriceSum = 0;
            for (int i = 0; i < windowSize; i++)
                windowPriceSum += priceList.get(i).sekPerKWh();

            double minPriceSum = windowPriceSum;
            List<Elpris> lowestWindowList = priceList.subList(0, windowSize);


            for (int i = windowSize; i<priceList.toArray().length; i++) {
                windowPriceSum += priceList.get(i).sekPerKWh() - priceList.get(i-windowSize).sekPerKWh();
                if (windowPriceSum < minPriceSum) {
                    minPriceSum = windowPriceSum;
                    lowestWindowList = priceList.subList(i-windowSize, i);
                }
            }

            System.out.printf("""
                    Påbörja laddning: %s kl 0%d:00
                    Avsluta laddning: %s kl 0%d:00
                    Medelpris för fönster: %.2f öre
                    """, date, lowestWindowList.getFirst().timeEnd().getHour(),
                    date, lowestWindowList.getLast().timeEnd().getHour(),
                    minPriceSum / windowSize * 100);

        }
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
        if (!listOfArgs.contains("--zone"))
            throw new IllegalArgumentException("--zone command missing: a --zone command must be provided");

        String zone = listOfArgs.get(listOfArgs.indexOf("--zone") + 1);
        try {
            return Prisklass.valueOf(zone.toUpperCase());
        } catch (IllegalArgumentException e){
            throw new IllegalArgumentException("Unexpected zone value: --zone must be followed by one of " +
                    "the specific zone values: SE1, SE2, SE3, SE4");
        }
    }
}
