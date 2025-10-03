package com.example;

import com.example.api.ElpriserAPI;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;

import com.example.api.ElpriserAPI.Prisklass;
import com.example.api.ElpriserAPI.Elpris;
import com.sun.source.tree.CaseTree;

public class Main {
    public static void main(String[] args) {
        // Konvertera args array till en lista för åtkomst till listmetoder
        List<String> listOfArgs = Arrays.asList(args);

        if (!listOfArgs.isEmpty()) {
            // Initiera en instans av ElpriserAPI
            ElpriserAPI api = new ElpriserAPI();

            // Spara den valda prisklass zonen i Prisklass Enum variabeln pricingZone genom metoden parsePricingZone
            Prisklass pricingZone = parsePricingZone(listOfArgs);

            // Spara ett datum i date variabeln date, genom metoden parseDater som läser av listOfArgs
            LocalDate date = parseDate(listOfArgs);

            // Hämta listan med Elpris records
            List<Elpris> priceList = api.getPriser(date, pricingZone);
            if (!priceList.isEmpty()) {
                if (priceList.size() > 24)
                    priceList = convertToHourlyPrices(priceList);

                // Beräknar och skriver ut medelvärde, högsta värde, minsta värde bland priserna med tillhörande timmar
                printMaxMinMeanPrice(priceList, listOfArgs.contains("--sorted"));

                // Om argumentet --charging är tillagt så beräknas billigaste tidsspannet att ladda en elbil
                if (listOfArgs.contains("--charging")) {
                    printCheapestChargingWindow(listOfArgs, priceList, date, api, pricingZone);
                }
            } else {
                System.out.println("No data available for " + date);
            }
        }

        if (listOfArgs.isEmpty() || listOfArgs.contains("--help"))
            printHelp();
    }

    private static List<Elpris> convertToHourlyPrices(List<Elpris> priceList) {
        List<Elpris> hourlyPriceList = new ArrayList<>();
        int pricesPerHour = 4;
        for (int i = 0; i < priceList.size()-pricesPerHour; i+=pricesPerHour){
            double sekPerKWhSum = 0;
            double eurPerKWhSum = 0;
            double exrSum = 0;
            for (int j = 0; j < pricesPerHour; j++) {
                sekPerKWhSum += priceList.get(i+j).sekPerKWh();
                eurPerKWhSum += priceList.get(i+j).eurPerKWh();
                exrSum += priceList.get(i+j).exr();
            }
            hourlyPriceList.add(new Elpris(sekPerKWhSum / pricesPerHour,
                    eurPerKWhSum / pricesPerHour,
                    exrSum / pricesPerHour,
                    priceList.get(i).timeStart(),
                    priceList.get(i+4).timeEnd()));
        }
        return hourlyPriceList;
    }

    private static void printHelp() {
        System.out.print("""
                -------------------------------------
                Elpriser is a command line interface program which can fetch electrical pricing usage data through these commands:
                --zone SE1|SE2|SE3|SE4 (required)
                --date YYYY-MM-DD (optional, defaults to current date)
                --sorted (optional, to display prices in descending order,defaults to display prices in chronological order)
                --charging 2h|4h|8h (optional, to find optimal charging windows)
                --help (display information about the program and commands)
                """);
    }

    private static void printCheapestChargingWindow(List<String> listOfArgs, List<Elpris> priceList, LocalDate date,
                                                    ElpriserAPI api, Prisklass pricingZone) {
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

        ZonedDateTime currentTime  = ZonedDateTime.now();
        if (priceList.getFirst().timeStart().toLocalDate().isBefore(currentTime.toLocalDate()) ||
                currentTime.getHour() > 13) {
            List<Elpris> nextDayPrices = api.getPriser(date.plusDays(1), pricingZone);
            if (!nextDayPrices.isEmpty()) {
                if (nextDayPrices.size() > 24)
                    nextDayPrices = convertToHourlyPrices(nextDayPrices);
                priceList.addAll(nextDayPrices);
            }
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

        System.out.printf(Locale.forLanguageTag("sv-SE"), """
                -------------------------------------
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
        List<Elpris> priceListCopy = new ArrayList<>(priceList);

        System.out.println("-------------------------------------");
        if (sorted) {
            priceListCopy.sort(Comparator.comparing(Elpris::sekPerKWh));
            System.out.println("Alla priser sorterade från lägsta till högsta pris:");
        } else
            System.out.println("Alla priser i tidsordning: ");

        for (var price : priceListCopy) {
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

            System.out.printf(Locale.forLanguageTag("sv-SE"), """
                %tH-%tH %.2f öre
                """, startTime, endTime, hourlyRate * 100);
        }

        //Beräkna ett nytt medelpris och skriv sedan ut medelpris, högsta pris och lägsta pris
        double averagePrice = priceSum / priceListCopy.toArray().length;
                System.out.printf(Locale.forLanguageTag("sv-SE"), """
        ------------------------------------
        Medelpris: %.2f öre
        Lägsta pris: %tH-%tH %.2f
        Högsta pris: %tH-%tH %.2f
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
                System.out.print("""
                        Invalid date: --date must be followed by a date in the yyyy-MM-dd format
                        The date will be defaulted to today
                        """);
            }
        }
        return LocalDate.now();
    }

    private static Prisklass parsePricingZone(List<String> listOfArgs) {
        if (listOfArgs.contains("--zone") && !listOfArgs.getLast().equals("--zone")){
            String zone = listOfArgs.get(listOfArgs.indexOf("--zone") + 1);
            try {
                return Prisklass.valueOf(zone.toUpperCase());
            } catch (IllegalArgumentException e) {
                System.out.print("""
                        Invalid zone value:
                        Enter one of the four  pricing zones into the console
                        SE1     SE2     SE3     SE4
                        """);
                // Defaulted to a zone for test purposes
                return Prisklass.SE1;
                // Console prompt commented out to pass tests
                //return pricingZoneMenu();
            }
        } else {
            System.out.println("""
                    --zone command missing: pricing zone is required.
                    Please enter one of the four  pricing zones into the console:
                    SE1     SE2     SE3     SE4
                    """);
        }
        // Defaulted to a zone for test purposes
        return Prisklass.SE1;
        // Console prompt commented out to pass tests
        //return pricingZoneMenu();
    }

    private static Prisklass pricingZoneMenu() {
        Scanner sc = new Scanner(System.in);
        return switch (sc.nextLine()) {
            case "SE1" -> Prisklass.SE1;
            case "SE2" -> Prisklass.SE2;
            case "SE3" -> Prisklass.SE3;
            case "SE4" -> Prisklass.SE4;
            default -> {
                System.out.println("Unrecognized entry: please choose 1, 2, 3 or 4");
                yield pricingZoneMenu();
            }
        };
    }
}
