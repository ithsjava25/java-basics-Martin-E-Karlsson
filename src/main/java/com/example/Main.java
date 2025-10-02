package com.example;

import com.example.api.ElpriserAPI;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import com.example.api.ElpriserAPI.Prisklass;
import com.example.api.ElpriserAPI.Elpris;

public class Main {
    public static void main(String[] args) {
        // Initiera en instans av ElpriserAPI
        ElpriserAPI api = new ElpriserAPI();
        // Konvertera args array till en lista för åtkomst till listmetoder
        List<String> listOfArgs = Arrays.asList(args);
        // Spara den valda prisklass zonen i Prisklass Enum variabeln pricingZone genom metoden parsePricingZone
        Prisklass pricingZone = parsePricingZone(listOfArgs);
        // Spara ett datum i date variabeln date, genom metoden parseDater som läser av listOfArgs
        LocalDate date = parseDate(listOfArgs);
        // Hämta listan med Elpris records
        List<Elpris> priceList = api.getPriser(date, pricingZone);

        // Beräknar och skriver ut medelvärde, högsta värde, minsta värde bland priserna med tillhörande timmar
        printMaxMinMeanPrice(priceList, listOfArgs.contains("--sorted"));

        // Om argumentet --charging är tillagt så beräknas billigaste tidsspannet att ladda en elbil
        if (listOfArgs.contains("--charging")) {
            printLowestChargingWindow(listOfArgs, priceList, date);
        }
    }

    private static void printLowestChargingWindow(List<String> listOfArgs, List<Elpris> priceList, LocalDate date) {
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


        for (int i = windowSize; i< priceList.toArray().length; i++) {
            windowPriceSum += priceList.get(i).sekPerKWh() - priceList.get(i-windowSize).sekPerKWh();
            if (windowPriceSum < minPriceSum) {
                minPriceSum = windowPriceSum;
                lowestWindowList = priceList.subList(i-windowSize, i);
            }
        }

        System.out.printf("""
                -------------------------------
                Påbörja laddning: %s kl %tH:%tM
                Avsluta laddning: %s kl %tH:%tM
                Medelpris för fönster: %.2f öre
                """, date, lowestWindowList.getFirst().timeEnd().toLocalTime(), lowestWindowList.getFirst().timeEnd().toLocalTime(),
                date, lowestWindowList.getLast().timeEnd().toLocalTime(), lowestWindowList.getFirst().timeEnd().toLocalTime(),
                minPriceSum / windowSize * 100);
    }

    private static void printMaxMinMeanPrice(List<Elpris> priceList, boolean sorted) {
        double priceSum = 0.0;
        double minPrice = Double.MAX_VALUE;
        LocalTime[] minTime = new LocalTime[2];
        double maxPrice = Double.MIN_VALUE;
        LocalTime[] maxTime = new LocalTime[2];

        System.out.println("---------------------------");
        if (sorted) {
            priceList.sort(Comparator.comparing(Elpris::sekPerKWh));
            System.out.println("Alla priser sorterade från lägsta till högsta pris:");
        } else
            System.out.println("Alla priser i tidsordning: ");

        for (var price : priceList) {
            // Skapa variabler för timpris, start tid, slut tid i syfte att förbättre läsbarhet
            double hourlyRate = price.sekPerKWh();
            LocalTime startTime = price.timeStart().toLocalTime();
            LocalTime endTime = price.timeEnd().toLocalTime();

            // Lagra det lägsta timpriset med tillhörande start och sluttid om ett nytt lägsta pris hittas
            if (minPrice > hourlyRate){
                minPrice = hourlyRate;
                minTime[0] = startTime;
                minTime[1] = endTime;
            }

            // Lagra det högsta timpriset med tillhörande start och sluttid om ett nytt högsta pris hittas
            if (maxPrice < hourlyRate){
                maxPrice = hourlyRate;
                maxTime[0] = startTime;
                maxTime[1] = endTime;
            }
            // Lägg till det nuvarande priset till den totala prissumman
            priceSum += hourlyRate;

            System.out.printf("""
                %tH-%tH %.2f öre
                """, startTime, endTime, hourlyRate * 100);
        }

        //Beräkna ett nytt medelpris och skriv sedan ut medelpris, högsta pris och lägsta pris
        double averagePrice = priceSum / priceList.toArray().length;
                System.out.printf("""
        -------------------------------
        Medelpris: %.2f öre
        Lägsta pris: %tH-%tH | %.2f
        Högsta pris: %tH-%tH | %.2f
        """, averagePrice * 100,
                        minTime[0], minTime[1], minPrice * 100,
                        maxTime[0], maxTime[1], maxPrice * 100);
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
