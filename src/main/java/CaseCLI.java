import adapter.Connector;
import java.util.Scanner;

public class CaseCLI {

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);
        Connector connector = new Connector();
        boolean run = true;
        int choice;

        while (run) {

            System.out.println("\nOptions:");
            System.out.println("1. List authentication methods");
            System.out.println("2. Initiate Mobil BankID login");
            System.out.println("3. Exit");

            choice = scanner.nextInt();
            System.out.println();

            switch (choice) {
                case 1:
                    connector.getAvailableLoginMethods();
                    break;
                case 2:
                    System.out.println("Please provide a customer number YYYYMMDDXXXX");
                    String customerNr = scanner.next();
                    connector.initiateMobileBankIdLogin(customerNr);
                    break;
                case 3:
                    System.out.println("Exiting application");
                    run = false;
                    break;
                default:
                    System.out.println("Invalid option");
                    break;
            }
        }

        scanner.close();
    }
}