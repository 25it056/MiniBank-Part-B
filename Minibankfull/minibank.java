import java.io.*;
import java.lang.annotation.*;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;

public class minibank {
    private static final Scanner SC = new Scanner(System.in);
    private static final Path DATA_DIR = Paths.get("data");
    private static final Path ACCOUNT_FILE = DATA_DIR.resolve("accounts.dat");
    private static final Path LOG_DIR = DATA_DIR.resolve("logs");
    private static final Path LOG_FILE = LOG_DIR.resolve("transactions.log");

    record BankInfo(String name, String branch) {}
    enum MenuOption { OPEN_ACCOUNT, DEPOSIT, WITHDRAW, TRANSFER, EXIT }
    enum TransactionType { DEPOSIT, WITHDRAW, TRANSFER }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface Id {}

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface Positive {
        String message() default "must be > 0";
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface MaxLength {
        int value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface Audit {}

    record Command(TransactionType type, String accountNumber, long amount) {}

    static class CommandParser {
        static Command parse(String line) {
            String[] parts = line.trim().split("\\s+");
            if (parts.length != 3) {
                throw new IllegalArgumentException(
                    "Enter command as: DEPOSIT accountNumber amount");
            }
            TransactionType type = TransactionType.valueOf(parts[0].toUpperCase());
            long amount = Long.parseLong(parts[2]);
            return new Command(type, parts[1], amount);
        }
    }

    static class Validator {
        private static final Pattern MOBILE = Pattern.compile("^[6-9][0-9]{9}$");
        private static final Pattern EMAIL =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
        private static final Pattern PAN =
            Pattern.compile("^[A-Z]{5}[0-9]{4}[A-Z]$");
        private static final Pattern IFSC =
            Pattern.compile("^[A-Z]{4}0[A-Z0-9]{6}$");

        static boolean isValidMobile(String s) {
            return s != null && MOBILE.matcher(s).matches();
        }
        static boolean isValidEmail(String s) {
            return s != null && EMAIL.matcher(s).matches();
        }
        static boolean isValidPan(String s) {
            return s != null && PAN.matcher(s).matches();
        }
        static boolean isValidIfsc(String s) {
            return s != null && IFSC.matcher(s).matches();
        }
        static boolean isValidAmount(long amount) {
            return amount > 0;
        }
    }

    static class StatementFormatter {
        static String buildStatement(Account account) {
            StringBuilder sb = new StringBuilder();
            sb.append("----- ACCOUNT STATEMENT -----\n");
            sb.append("Account: ").append(account.getAccountNumber()).append('\n');
            sb.append("Owner: ").append(account.getOwnerName()).append('\n');
            sb.append("Balance: ").append(account.getBalance()).append('\n');
            sb.append("Type: ").append(account.getClass().getSimpleName()).append('\n');
            sb.append("-----------------------------");
            return sb.toString();
        }
    }

    @FunctionalInterface
    interface WithdrawRule {
        boolean canWithdraw(Account account, long amount);
    }

    static class Customer implements Serializable {
        private static final long serialVersionUID = 1L;
        @MaxLength(40)
        private String name;

        Customer(String name) { this.name = name; }
        String getName() { return name; }

        @Override
        public String toString() { return name; }
    }

    static abstract class Account implements Serializable, Comparable<Account> {
        private static final long serialVersionUID = 1L;

        @Id
        @MaxLength(12)
        private String accountNumber;

        @MaxLength(40)
        private String ownerName;

        @Positive
        private long balance;

        private long accountNumberOrder;

        Account(String accountNumber, String ownerName, long balance) {
            this.accountNumber = accountNumber;
            this.ownerName = ownerName;
            this.balance = balance;
            this.accountNumberOrder = extractNumber(accountNumber);
        }

        private static long extractNumber(String s) {
            try { return Long.parseLong(s.replaceAll("\\D", "")); }
            catch (Exception e) { return 0; }
        }

        String getAccountNumber() { return accountNumber; }
        String getOwnerName() { return ownerName; }
        synchronized long getBalance() { return balance; }

        protected synchronized void setBalance(long value) { balance = value; }

        @Audit
        synchronized void deposit(long amount) throws InvalidAmountException {
            if (amount <= 0) throw new InvalidAmountException("Deposit amount must be positive.");
            balance = Math.addExact(balance, amount);
        }

        @Audit
        synchronized void withdraw(long amount)
                throws InsufficientFundsException, InvalidAmountException {
            if (amount <= 0) throw new InvalidAmountException("Withdrawal amount must be positive.");
            if (amount > balance) {
                throw new InsufficientFundsException(amount - balance);
            }
            balance -= amount;
        }

        abstract double interestRate();

        @Override
        public int compareTo(Account other) {
            return this.accountNumber.compareTo(other.accountNumber);
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "{accountNumber='" + accountNumber
                + "', owner='" + ownerName + "', balance=" + getBalance()
                + ", interestRate=" + interestRate() + "%}";
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof Account other)) return false;
            return Objects.equals(accountNumber, other.accountNumber);
        }

        @Override
        public int hashCode() { return Objects.hash(accountNumber); }
    }

    static class SavingsAccount extends Account {
        private static final long serialVersionUID = 1L;
        SavingsAccount(String no, String owner, long balance) {
            super(no, owner, balance);
        }
        @Override double interestRate() { return 4.0; }
    }

    static class CurrentAccount extends Account {
        private static final long serialVersionUID = 1L;
        CurrentAccount(String no, String owner, long balance) {
            super(no, owner, balance);
        }
        @Override double interestRate() { return 0.0; }
    }

    static class FixedDepositAccount extends Account {
        private static final long serialVersionUID = 1L;
        FixedDepositAccount(String no, String owner, long balance) {
            super(no, owner, balance);
        }
        @Override double interestRate() { return 7.0; }
    }

    // Practical 8: checked custom exception hierarchy
    static class BankException extends Exception {
        private static final long serialVersionUID = 1L;
        BankException(String message) { super(message); }
    }

    static class InsufficientFundsException extends BankException {
        private static final long serialVersionUID = 1L;
        private final long shortfall;
        InsufficientFundsException(long shortfall) {
            super("Insufficient funds. Short by " + shortfall + ".");
            this.shortfall = shortfall;
        }
        long getShortfall() { return shortfall; }
    }

    static class AccountNotFoundException extends BankException {
        private static final long serialVersionUID = 1L;
        AccountNotFoundException(String accountNumber) {
            super("Account not found: " + accountNumber);
        }
    }

    static class InvalidAmountException extends BankException {
        private static final long serialVersionUID = 1L;
        InvalidAmountException(String message) { super(message); }
    }

    static class AnnotationValidator {
        static String[] validate(Object obj) {
            ArrayList<String> errors = new ArrayList<>();
            for (Field field : obj.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                try {
                    Object value = field.get(obj);
                    if (field.isAnnotationPresent(Positive.class)
                            && value instanceof Number n && n.longValue() <= 0) {
                        errors.add(field.getName() + " "
                            + field.getAnnotation(Positive.class).message());
                    }
                    if (field.isAnnotationPresent(MaxLength.class)
                            && value instanceof String s
                            && s.length() > field.getAnnotation(MaxLength.class).value()) {
                        errors.add(field.getName() + " exceeds maximum length "
                            + field.getAnnotation(MaxLength.class).value());
                    }
                } catch (IllegalAccessException e) {
                    errors.add("Could not validate " + field.getName());
                }
            }
            return errors.toArray(new String[0]);
        }
    }

    static class TransactionProcessor {
        private final ExecutorService pool = Executors.newFixedThreadPool(4);

        void submit(Runnable task) { pool.execute(task); }

        void stop() {
            pool.shutdown();
            try {
                if (!pool.awaitTermination(30, TimeUnit.SECONDS)) pool.shutdownNow();
            } catch (InterruptedException e) {
                pool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    static class StatePersister {
        static void save(Account[] accounts, Path file) throws IOException {
            Path parent = file.getParent();
            if (parent != null) Files.createDirectories(parent);
            try (ObjectOutputStream out = new ObjectOutputStream(
                    Files.newOutputStream(file))) {
                out.writeObject(accounts);
            }
        }

        static Account[] load(Path file) throws IOException, ClassNotFoundException {
            try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(file))) {
                return (Account[]) in.readObject();
            }
        }
    }

    static class TransactionLog {
        static void append(Path file, String line) throws IOException {
            Path parent = file.getParent();
            if (parent != null) Files.createDirectories(parent);
            Files.write(file, (line + System.lineSeparator()).getBytes(),
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        }
    }

    static class ReportGenerator {
        static String generate(Path folder, Path reportFile) throws IOException {
            Files.createDirectories(folder);
            long deposits = 0;
            long withdrawals = 0;
            StringBuilder report = new StringBuilder();
            report.append("MINIBANK END-OF-DAY REPORT\n");
            report.append("Generated: ").append(LocalDateTime.now()).append('\n');

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(folder, "*.log")) {
                for (Path file : stream) {
                    BasicFileAttributes attrs = Files.readAttributes(
                        file, BasicFileAttributes.class);
                    report.append("\nFile: ").append(file.getFileName())
                        .append(", size: ").append(attrs.size())
                        .append(", modified: ").append(attrs.lastModifiedTime()).append('\n');

                    try (BufferedReader reader = Files.newBufferedReader(file)) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            report.append(line).append('\n');
                            String[] p = line.trim().split("\\s+");
                            if (p.length >= 3) {
                                try {
                                    long amount = Long.parseLong(p[p.length - 1]);
                                    if (p[0].equals("DEPOSIT")) deposits += amount;
                                    if (p[0].equals("WITHDRAW")) withdrawals += amount;
                                } catch (NumberFormatException ignored) { }
                            }
                        }
                    }
                }
            }
            report.append("\nTotal deposits: ").append(deposits);
            report.append("\nTotal withdrawals: ").append(withdrawals);
            report.append("\nNet change: ").append(deposits - withdrawals).append('\n');
            Path parent = reportFile.getParent();
            if (parent != null) Files.createDirectories(parent);
            Files.write(reportFile, report.toString().getBytes());
            return report.toString();
        }
    }

    static class AccountStore {
        private final HashMap<String, Account> accounts = new HashMap<>();
        private final ArrayList<String> history = new ArrayList<>();

        synchronized void add(Account account) {
            accounts.put(account.getAccountNumber(), account);
        }
        synchronized Account get(String number) { return accounts.get(number); }
        synchronized ArrayList<Account> getAll() {
            return new ArrayList<>(accounts.values());
        }
        synchronized ArrayList<Account> sortedByBalance() {
            ArrayList<Account> result = getAll();
            result.sort(Comparator.comparingLong(Account::getBalance).reversed());
            return result;
        }
        synchronized void addHistory(String line) { history.add(line); }
        synchronized ArrayList<String> getHistory() { return new ArrayList<>(history); }
        synchronized boolean delete(String number) { return accounts.remove(number) != null; }
        synchronized ConcurrentHashMap<String, Account> concurrentCopy() {
            return new ConcurrentHashMap<>(accounts);
        }
    }

    static class Repository<T> {
        private final HashMap<String, T> items = new HashMap<>();

        T save(T entity) {
            String id = readId(entity);
            items.put(id, entity);
            return entity;
        }

        @SafeVarargs
        final void saveAll(T... entities) {
            for (T entity : entities) save(entity);
        }

        T findById(String id) { return items.get(id); }
        ArrayList<T> findAll() { return new ArrayList<>(items.values()); }
        boolean delete(String id) { return items.remove(id) != null; }

        private String readId(T entity) {
            Class<?> currentClass = entity.getClass();

            // Search this class and its parent classes for the @Id field.
            while (currentClass != null) {
                for (Field field : currentClass.getDeclaredFields()) {
                    if (field.isAnnotationPresent(Id.class)) {
                        try {
                            field.setAccessible(true);
                            return String.valueOf(field.get(entity));
                        } catch (IllegalAccessException e) {
                            throw new IllegalArgumentException("Cannot read @Id field", e);
                        }
                    }
                }
                currentClass = currentClass.getSuperclass();
            }

            throw new IllegalArgumentException("No @Id field found in " + entity.getClass());
        }

        static <N extends Number> double average(N[] values) {
            if (values.length == 0) return 0;
            double total = 0;
            for (N value : values) total += value.doubleValue();
            return total / values.length;
        }
    }

    static class DemoResource implements AutoCloseable {
        DemoResource() { System.out.println("Resource opened."); }
        void use() { System.out.println("Using resource."); }
        @Override
        public void close() { System.out.println("Resource closed."); }
    }

    static class TransactionBuffer {
        private final Queue<String> queue = new LinkedList<>();
        private boolean finished = false;

        synchronized void put(String value) throws InterruptedException {
            queue.add(value);
            notify();
        }

        synchronized String take() throws InterruptedException {
            while (queue.isEmpty() && !finished) wait();
            return queue.poll();
        }

        synchronized void finish() {
            finished = true;
            notifyAll();
        }
    }

    static class BankService {
        private final Repository<Account> repository = new Repository<>();
        private final AccountStore store = new AccountStore();
        private final ArrayList<String> transactionHistory = new ArrayList<>();
        private int nextAccount = 1001;

        BankService() {
            try {
                if (Files.exists(ACCOUNT_FILE)) {
                    for (Account account : StatePersister.load(ACCOUNT_FILE)) {
                        repository.save(account);
                        store.add(account);
                    }
                }
            } catch (Exception e) {
                System.out.println("Saved accounts could not be loaded: " + e.getMessage());
            }
        }

        @Audit
        Account openAccount(String owner, String type, long openingBalance)
                throws InvalidAmountException {
            if (owner == null || owner.isBlank() || owner.length() > 40) {
                throw new IllegalArgumentException("Owner name must be 1-40 characters.");
            }
            if (openingBalance < 0) throw new InvalidAmountException("Opening balance cannot be negative.");
            String number;
            do { number = "AC" + nextAccount++; }
            while (store.get(number) != null);

            Account account;
            switch (type.toUpperCase()) {
                case "CURRENT" -> account = new CurrentAccount(number, owner, openingBalance);
                case "FD", "FIXED" -> account = new FixedDepositAccount(number, owner, openingBalance);
                default -> account = new SavingsAccount(number, owner, openingBalance);
            }
            repository.save(account);
            store.add(account);
            log("OPEN", number, openingBalance);
            return account;
        }

        Account find(String number) throws AccountNotFoundException {
            Account account = repository.findById(number);
            if (account == null) throw new AccountNotFoundException(number);
            return account;
        }

        @Audit
        void deposit(String number, long amount) throws BankException {
            Account account = find(number);
            account.deposit(amount);
            record("DEPOSIT", number, amount);
        }

        @Audit
        void withdraw(String number, long amount) throws BankException {
            Account account = find(number);
            account.withdraw(amount);
            record("WITHDRAW", number, amount);
        }

        @Audit
        void transfer(String from, String to, long amount) throws BankException {
            if (amount <= 0) throw new InvalidAmountException("Transfer amount must be positive.");
            Account source = find(from);
            Account destination = find(to);
            Account first = from.compareTo(to) < 0 ? source : destination;
            Account second = first == source ? destination : source;

            synchronized (first) {
                synchronized (second) {
                    try {
                        source.withdraw(amount);
                        destination.deposit(amount);
                        record("TRANSFER", from + "->" + to, amount);
                    } catch (BankException e) {
                        throw e;
                    } catch (ArithmeticException e) {
                        throw new InvalidAmountException("Balance overflow.");
                    } finally {
                        System.out.println("Transfer attempt finished.");
                    }
                }
            }
        }

        Account statement(String number) throws AccountNotFoundException {
            return find(number);
        }

        ArrayList<Account> listAccounts() { return store.sortedByBalance(); }

        synchronized void saveAll() {
            try {
                StatePersister.save(store.getAll().toArray(new Account[0]), ACCOUNT_FILE);
            } catch (IOException e) {
                System.out.println("Could not save accounts: " + e.getMessage());
            }
        }

        void runReport() {
            try {
                System.out.println(ReportGenerator.generate(LOG_DIR, DATA_DIR.resolve("report.txt")));
            } catch (IOException e) {
                System.out.println("Report error: " + e.getMessage());
            }
        }

        void runBatchDemo() {
            TransactionProcessor processor = new TransactionProcessor();
            for (int i = 0; i < 8; i++) {
                final int taskId = i + 1;
                processor.submit(() -> System.out.println(
                    Thread.currentThread().getName() + " processed batch task " + taskId));
            }
            processor.stop();
        }

        private synchronized void record(String type, String number, long amount) {
            String line = type + " " + number + " " + amount;
            transactionHistory.add(line);
            store.addHistory(line);
            log(type, number, amount);
        }

        private void log(String type, String number, long amount) {
            try {
                TransactionLog.append(LOG_FILE,
                    type + " " + number + " " + amount + " " + LocalDateTime.now());
            } catch (IOException e) {
                System.out.println("Warning: transaction log could not be written.");
            }
        }
    }

    private static String readText(String prompt) {
        System.out.print(prompt);
        return SC.nextLine().trim();
    }

    private static long readLong(String prompt) {
        while (true) {
            try { return Long.parseLong(readText(prompt)); }
            catch (NumberFormatException e) { System.out.println("Enter a whole number."); }
        }
    }

    private static void showMenu() {
        System.out.println("\n========== MINIBANK ==========");
        System.out.println("1. Open account");
        System.out.println("2. Deposit");
        System.out.println("3. Withdraw");
        System.out.println("4. Transfer");
        System.out.println("5. Account statement");
        System.out.println("6. List accounts (balance high to low)");
        System.out.println("7. Generate end-of-day report");
        System.out.println("8. Run transaction thread-pool demo");
        System.out.println("9. Run producer-consumer demo");
        System.out.println("10. Save accounts");
        System.out.println("0. Exit");
    }

    private static void producerConsumerDemo() {
        TransactionBuffer buffer = new TransactionBuffer();

        Thread producer = new Thread(() -> {
            try {
                for (int i = 1; i <= 5; i++) {
                    String transaction = "TX-" + i;
                    buffer.put(transaction);
                    System.out.println("Produced " + transaction);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                buffer.finish();
            }
        }, "Producer");

        Thread consumer = new Thread(() -> {
            try {
                String item;
                while ((item = buffer.take()) != null) {
                    System.out.println("Consumed " + item);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Consumer");

        producer.start();
        consumer.start();
        try {
            producer.join();
            consumer.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void printValidationDemo() {
        System.out.println("Mobile valid: " + Validator.isValidMobile("9876543210"));
        System.out.println("Mobile invalid: " + Validator.isValidMobile("12345"));
        System.out.println("Email valid: " + Validator.isValidEmail("student@example.com"));
        System.out.println("PAN valid: " + Validator.isValidPan("ABCDE1234F"));
        System.out.println("IFSC valid: " + Validator.isValidIfsc("ABCD0123456"));
        try {
            Command command = CommandParser.parse("DEPOSIT AC1001 500");
            System.out.println("Parsed command: " + command.type() + ", "
                + command.accountNumber() + ", " + command.amount());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public static void main(String[] args) {
        BankInfo info = new BankInfo("MiniBank", "CHARUSAT Branch");
        System.out.println("================================");
        System.out.println(info.name() + " | " + info.branch());
        System.out.println("Educational console banking demo");
        System.out.println("================================");

        BankService bank = new BankService();
        printValidationDemo();

        boolean running = true;
        while (running) {
            showMenu();
            int choice = (int) readLong("Choose option: ");
            try {
                switch (choice) {
                    case 1 -> {
                        String owner = readText("Owner name: ");
                        String type = readText("Account type (SAVINGS/CURRENT/FD): ");
                        long amount = readLong("Opening balance (0 or more): ");
                        Account account = bank.openAccount(owner, type, amount);
                        System.out.println("Account created: " + account);
                    }
                    case 2 -> {
                        String number = readText("Account number: ");
                        long amount = readLong("Deposit amount: ");
                        bank.deposit(number, amount);
                        System.out.println("Deposit successful.");
                    }
                    case 3 -> {
                        String number = readText("Account number: ");
                        long amount = readLong("Withdrawal amount: ");
                        bank.withdraw(number, amount);
                        System.out.println("Withdrawal successful.");
                    }
                    case 4 -> {
                        String from = readText("From account: ");
                        String to = readText("To account: ");
                        long amount = readLong("Transfer amount: ");
                        bank.transfer(from, to, amount);
                        System.out.println("Transfer successful.");
                    }
                    case 5 -> {
                        String number = readText("Account number: ");
                        System.out.println(StatementFormatter.buildStatement(bank.statement(number)));
                    }
                    case 6 -> {
                        ArrayList<Account> accounts = bank.listAccounts();
                        if (accounts.isEmpty()) System.out.println("No accounts yet.");
                        else for (Account a : accounts) System.out.println(a);
                    }
                    case 7 -> bank.runReport();
                    case 8 -> bank.runBatchDemo();
                    case 9 -> producerConsumerDemo();
                    case 10 -> {
                        bank.saveAll();
                        System.out.println("Accounts saved.");
                    }
                    case 0 -> {
                        bank.saveAll();
                        running = false;
                        System.out.println("Accounts saved. Thank you for using MiniBank.");
                    }
                    default -> System.out.println("Invalid option. Try again.");
                }
            } catch (InsufficientFundsException e) {
                System.out.println("Withdrawal failed: " + e.getMessage());
                System.out.println("Shortfall: " + e.getShortfall());
            } catch (BankException e) {
                System.out.println("Bank operation failed: " + e.getMessage());
            } catch (ArithmeticException e) {
                System.out.println("Amount is too large.");
            } catch (IllegalArgumentException e) {
                System.out.println("Invalid input: " + e.getMessage());
            }
        }

        try (DemoResource resource = new DemoResource()) {
            resource.use();
        }

        Repository<Account> demoRepository = new Repository<>();
        Account sample = new SavingsAccount("AC9999", "Demo Student", 1000);
        demoRepository.save(sample);
        System.out.println("Repository lookup: " + demoRepository.findById("AC9999"));
        Integer[] sampleNumbers = {10, 20, 30};
        System.out.println("Generic average: " + Repository.average(sampleNumbers));
    }
}
