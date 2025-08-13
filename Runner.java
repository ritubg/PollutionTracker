import java.util.Scanner;

public class Runner {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int choice;

        while (true) {
            System.out.println("\n===== Data Fetcher Menu =====");
            System.out.println("1. Run WeatherAttributeFetcher");
            System.out.println("2. Run WeatherForecastFetcher");
            System.out.println("3. Run YesterdayDataFetcher");
            System.out.println("4. Run HistoricalDataFetcher");
            System.out.println("5. Run All");
            System.out.println("0. Exit");
            System.out.print("Enter your choice: ");

            // Input validation
            if (!scanner.hasNextInt()) {
                System.out.println("Invalid input. Please enter a number.");
                scanner.next();
                continue;
            }

            choice = scanner.nextInt();

            switch (choice) {
                case 1:
                    System.out.println("\n=== Running WeatherAttributeFetcher ===");
                    WeatherAttributeFetcher.main(new String[]{});
                    break;

                case 2:
                    System.out.println("\n=== Running WeatherForecastFetcher ===");
                    WeatherForecastFetcher.main(new String[]{});
                    break;

                case 3:
                    System.out.println("\n=== Running YesterdayDataFetcher ===");
                    YesterdayDataFetcher.main(new String[]{});
                    break;

                case 4:
                    System.out.println("\n=== Running HistoricalDataFetcher ===");
                    HistoricalDataFetcher.main(new String[]{});
                    break;

                case 5:
                    System.out.println("\n=== Running All Fetchers ===");
                    WeatherAttributeFetcher.main(new String[]{});
                    WeatherForecastFetcher.main(new String[]{});
                    YesterdayDataFetcher.main(new String[]{});
                    HistoricalDataFetcher.main(new String[]{});
                    break;

                case 0:
                    System.out.println("Exiting program...");
                    scanner.close();
                    return;

                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }
    }
}
