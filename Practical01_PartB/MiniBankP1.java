import java.util.Scanner;

public class MiniBankP1 {
    record BankInfo(String name, String branch) {}
    enum MenuOption { OPEN_ACCOUNT, DEPOSIT, WITHDRAW, TRANSFER, EXIT }

    public static void main(String[] args) {
        BankInfo bank = new BankInfo("MiniBank", "Main Branch");
        System.out.println("===== " + bank.name() + " | " + bank.branch() + " =====");
        try (Scanner sc = new Scanner(System.in)) {
            boolean running = true;
            while (running) {
                System.out.println("\n1. Open Account\n2. Deposit\n3. Withdraw\n4. Transfer\n5. Exit");
                System.out.print("Choose: ");
                String input = sc.nextLine().trim();
                int choice;
                try { choice = Integer.parseInt(input); }
                catch (NumberFormatException e) { System.out.println("Enter a number."); continue; }
                MenuOption option = switch (choice) {
                    case 1 -> MenuOption.OPEN_ACCOUNT;
                    case 2 -> MenuOption.DEPOSIT;
                    case 3 -> MenuOption.WITHDRAW;
                    case 4 -> MenuOption.TRANSFER;
                    case 5 -> MenuOption.EXIT;
                    default -> null;
                };
                if (option == null) { System.out.println("Invalid choice."); continue; }
                switch (option) {
                    case OPEN_ACCOUNT -> System.out.println("Open Account — to be implemented in a later lab");
                    case DEPOSIT -> System.out.println("Deposit — to be implemented in a later lab");
                    case WITHDRAW -> System.out.println("Withdraw — to be implemented in a later lab");
                    case TRANSFER -> System.out.println("Transfer — to be implemented in a later lab");
                    case EXIT -> { System.out.println("Thank you for using MiniBank."); running = false; }
                }
            }
        }
    }
}
